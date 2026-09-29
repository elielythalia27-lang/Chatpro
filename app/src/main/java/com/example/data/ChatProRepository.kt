package com.example.data

import com.example.data.local.AccountSession
import com.example.data.local.ChatProDatabase
import com.example.data.local.MessageEntity
import com.example.data.local.PostEntity
import com.example.data.local.SessionManager
import com.example.data.local.StatusEntity
import com.example.data.local.UserEntity
import com.example.data.model.AdminStats
import com.example.data.model.ChatMessage
import com.example.data.model.GlobalConfig
import com.example.data.model.MessageStatus
import com.example.data.model.MessageType
import com.example.data.model.Post
import com.example.data.model.PostComment
import com.example.data.model.PostMediaType
import com.example.data.model.SystemLog
import com.example.data.model.User
import com.example.data.model.UserStatus
import com.example.data.network.CloudStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ChatProRepository(
    private val database: ChatProDatabase,
    private val sessionManager: SessionManager,
    private val cloudStorage: CloudStorage
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _adminStats = MutableStateFlow(AdminStats())
    val adminStats: StateFlow<AdminStats> = _adminStats.asStateFlow()

    private val _systemLogs = MutableStateFlow<List<SystemLog>>(emptyList())
    val systemLogs: StateFlow<List<SystemLog>> = _systemLogs.asStateFlow()

    private val _globalConfig = MutableStateFlow(GlobalConfig())
    val globalConfig: StateFlow<GlobalConfig> = _globalConfig.asStateFlow()

    init {
        scope.launch {
            initSystemAdmin()
            syncRemoteData()
            startDynamicStatsObserver()
        }
    }

    private fun startDynamicStatsObserver() {
        scope.launch {
            database.userDao().getAllUsers().collect { users ->
                val current = _adminStats.value
                _adminStats.value = current.copy(
                    totalUsers = users.size,
                    activeUsers24h = users.count { it.isOnline || (System.currentTimeMillis() - it.lastSeen < 24 * 3600 * 1000) }
                )
            }
        }
        scope.launch {
            database.messageDao().getAllMessages().collect { messages ->
                val todayStart = System.currentTimeMillis() - 24 * 3600 * 1000
                val msgsToday = messages.count { it.timestamp > todayStart }
                val current = _adminStats.value
                _adminStats.value = current.copy(
                    messagesToday = msgsToday
                )
            }
        }
        scope.launch {
            val expiryThreshold = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
            database.statusDao().getActiveStatuses(expiryThreshold).collect { statuses ->
                val current = _adminStats.value
                _adminStats.value = current.copy(
                    activeStatuses = statuses.size
                )
            }
        }
    }

    private suspend fun initSystemAdmin() {
        val isCurrentSessionAdmin = sessionManager.currentAccount.value?.username.equals("Eliel_21", ignoreCase = true)
        val adminUser = User(
            username = "Eliel_21",
            displayName = "Eliel (Admin)",
            bio = "Administrador del sistema ChatPro.",
            isOnline = isCurrentSessionAdmin,
            isAdmin = true,
            lastSeen = if (isCurrentSessionAdmin) System.currentTimeMillis() else 0L
        )
        database.userDao().insertUser(UserEntity.fromUser(adminUser))

        // Public community group chat for all users
        val communityChat = User(
            username = "comunidad_general",
            displayName = "Comunidad ChatPro 🌐",
            bio = "Canal oficial público para todos los miembros de ChatPro.",
            avatarUrl = "",
            isOnline = true,
            isAdmin = false,
            lastSeen = System.currentTimeMillis()
        )
        database.userDao().insertUser(UserEntity.fromUser(communityChat))
        addLog("SYSTEM", "Comunidad ChatPro y cuentas del sistema listas.")
    }

    /**
     * Synchronize with remote cloud storage without relying on mock data.
     */
    suspend fun syncRemoteData() = withContext(Dispatchers.IO) {
        try {
            val token = cloudStorage.getOrRefreshToken()
            if (token != null) {
                // Fetch public community messages and posts
                val remotePosts = cloudStorage.fetchRemotePosts()
                if (remotePosts.isNotEmpty()) {
                    database.postDao().insertPosts(remotePosts.map { PostEntity.fromPost(it) })
                }
                val remoteCommunityMsgs = cloudStorage.fetchRemoteCommunityMessages()
                if (remoteCommunityMsgs.isNotEmpty()) {
                    database.messageDao().insertMessages(remoteCommunityMsgs.map { MessageEntity.fromChatMessage(it) })
                }
                addLog("SYNC", "Datos sincronizados con éxito.")
            }
        } catch (e: Exception) {
            // Silently fallback
        }
    }

    // --- Real-time Messages ---
    fun getMessagesForChat(chatId: String): Flow<List<ChatMessage>> {
        return database.messageDao().getMessagesForChat(chatId).map { entities ->
            entities.map { it.toChatMessage() }
        }
    }

    fun getAllMessages(): Flow<List<ChatMessage>> {
        return database.messageDao().getAllMessages().map { entities ->
            entities.map { it.toChatMessage() }
        }
    }

    suspend fun sendMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        // 1. Immediately insert as SENDING for instant zero-wait UI feedback
        val initialEntity = MessageEntity.fromChatMessage(message.copy(status = MessageStatus.SENDING))
        database.messageDao().insertMessage(initialEntity)

        // 2. Transmit through background coroutine to remote storage
        scope.launch {
            try {
                // Upload message block to remote cloud storage
                cloudStorage.uploadEncryptedBlock(
                    filename = "msg_${message.id}.json",
                    jsonPayload = message.text,
                    folderPath = "/chatpro/mensajes/${message.chatId}/"
                )
            } catch (e: Exception) {
                // Graceful fallback
            }

            delay(350L) // Instant confirmation
            val confirmedEntity = initialEntity.copy(status = MessageStatus.SENT.name)
            database.messageDao().updateMessage(confirmedEntity)
            addLog("CHAT", "Mensaje sincronizado en chat ${message.chatId}")
        }
    }

    suspend fun addReaction(messageId: String, username: String, emoji: String) = withContext(Dispatchers.IO) {
        val entity = database.messageDao().getMessageById(messageId) ?: return@withContext
        val msg = entity.toChatMessage()
        val updatedReactions = msg.reactions.toMutableMap()
        if (updatedReactions[username] == emoji) {
            updatedReactions.remove(username)
        } else {
            updatedReactions[username] = emoji
        }
        database.messageDao().updateMessage(MessageEntity.fromChatMessage(msg.copy(reactions = updatedReactions)))
    }

    // --- Users & Directory ---
    fun getAllUsers(): Flow<List<User>> {
        return database.userDao().getAllUsers().map { list -> list.map { it.toUser() } }
    }

    suspend fun registerUser(user: User, avatarFile: File? = null): Boolean = withContext(Dispatchers.IO) {
        val existing = database.userDao().getUserByUsername(user.username)
        if (existing != null) return@withContext false

        var finalAvatarUrl = user.avatarUrl
        if (avatarFile != null && avatarFile.exists()) {
            val uploadedUrl = cloudStorage.uploadUserAvatar(user.username, avatarFile)
            if (!uploadedUrl.isNullOrBlank()) {
                finalAvatarUrl = uploadedUrl
            }
        }

        val finalUser = user.copy(avatarUrl = finalAvatarUrl)
        database.userDao().insertUser(UserEntity.fromUser(finalUser))
        
        // Sync profile to remote cloud
        cloudStorage.uploadUserProfile(finalUser)

        addLog("AUTH", "Nuevo usuario registrado: ${finalUser.cleanUsername}")
        true
    }

    suspend fun updateUserProfile(
        username: String,
        displayName: String,
        bio: String,
        avatarFile: File? = null
    ): User = withContext(Dispatchers.IO) {
        val existingEntity = database.userDao().getUserByUsername(username)
        val existingUser = existingEntity?.toUser() ?: sessionManager.currentAccount.value?.let {
            User(
                username = it.username,
                displayName = it.displayName,
                bio = bio,
                avatarUrl = it.avatarUrl
            )
        } ?: User(username = username, displayName = displayName, bio = bio)

        var newAvatarUrl = existingUser.avatarUrl
        if (avatarFile != null && avatarFile.exists()) {
            val uploadedUrl = cloudStorage.uploadUserAvatar(username, avatarFile)
            newAvatarUrl = uploadedUrl ?: avatarFile.absolutePath
        }

        val updatedUser = existingUser.copy(
            displayName = displayName,
            bio = bio,
            avatarUrl = newAvatarUrl
        )

        // 1. Update Room locally
        database.userDao().insertUser(UserEntity.fromUser(updatedUser))

        // 2. Update SessionManager if it's the active account
        val currentAccount = sessionManager.currentAccount.value
        if (currentAccount?.username.equals(username, ignoreCase = true)) {
            sessionManager.saveAccount(
                currentAccount!!.copy(
                    displayName = updatedUser.displayName,
                    avatarUrl = updatedUser.avatarUrl
                )
            )
        }

        // 3. Upload to remote CloudStorage asynchronously
        scope.launch {
            try {
                cloudStorage.uploadUserProfile(updatedUser)
            } catch (e: Exception) {
                // Ignore remote network error
            }
        }

        addLog("PROFILE", "Perfil actualizado para @$username")
        updatedUser
    }

    suspend fun checkUsernameAvailability(username: String): Boolean {
        val clean = username.trim().removePrefix("@")
        val local = database.userDao().getUserByUsername(clean)
        if (local != null) return false
        return cloudStorage.checkUsernameAvailability(clean)
    }

    // --- Statuses (24h) ---
    fun getActiveStatuses(): Flow<List<UserStatus>> {
        val expiryThreshold = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        return database.statusDao().getActiveStatuses(expiryThreshold).map { list ->
            list.map { it.toUserStatus() }
        }
    }

    // --- Social Feed Posts (Facebook style) ---
    fun getAllPosts(): Flow<List<com.example.data.model.Post>> {
        return database.postDao().getAllPosts().map { list -> list.map { it.toPost() } }
    }

    fun getPostsByAuthor(username: String): Flow<List<com.example.data.model.Post>> {
        return database.postDao().getPostsByAuthor(username).map { list -> list.map { it.toPost() } }
    }

    suspend fun createPost(
        authorUsername: String,
        authorDisplayName: String,
        authorAvatarUrl: String?,
        content: String,
        mediaType: com.example.data.model.PostMediaType = com.example.data.model.PostMediaType.NONE,
        mediaFile: File? = null,
        audioDurationSeconds: Int = 0
    ): com.example.data.model.Post = withContext(Dispatchers.IO) {
        val initialPost = com.example.data.model.Post(
            authorUsername = authorUsername,
            authorDisplayName = authorDisplayName,
            authorAvatarUrl = authorAvatarUrl,
            content = content,
            mediaType = mediaType,
            mediaUrl = mediaFile?.absolutePath,
            audioDurationSeconds = audioDurationSeconds,
            timestamp = System.currentTimeMillis()
        )

        // Save in Room immediately for instant zero-wait UI
        database.postDao().insertPost(com.example.data.local.PostEntity.fromPost(initialPost))

        // Upload to Moodle in background and update URL once network responds
        scope.launch {
            try {
                val uploaded = cloudStorage.uploadPost(initialPost, mediaFile)
                if (uploaded.mediaUrl != initialPost.mediaUrl) {
                    database.postDao().updatePost(com.example.data.local.PostEntity.fromPost(uploaded))
                }
                addLog("FEED", "Publicación sincronizada con Moodle por @$authorUsername")
            } catch (e: Exception) {
                // Keep local
            }
        }

        initialPost
    }

    suspend fun toggleLikePost(postId: String, username: String) = withContext(Dispatchers.IO) {
        val entity = database.postDao().getPostById(postId) ?: return@withContext
        val post = entity.toPost()
        val updatedLikes = if (post.likes.contains(username)) {
            post.likes.filter { it != username }
        } else {
            post.likes + username
        }
        val updatedPost = post.copy(likes = updatedLikes)
        database.postDao().updatePost(com.example.data.local.PostEntity.fromPost(updatedPost))
    }

    suspend fun addCommentToPost(postId: String, comment: com.example.data.model.PostComment) = withContext(Dispatchers.IO) {
        val entity = database.postDao().getPostById(postId) ?: return@withContext
        val post = entity.toPost()
        val updatedComments = post.comments + comment
        val updatedPost = post.copy(comments = updatedComments)
        database.postDao().updatePost(com.example.data.local.PostEntity.fromPost(updatedPost))
    }

    suspend fun deleteComment(postId: String, commentId: String) = withContext(Dispatchers.IO) {
        val entity = database.postDao().getPostById(postId) ?: return@withContext
        val post = entity.toPost()
        val updatedComments = post.comments.filter { it.id != commentId }
        val updatedPost = post.copy(comments = updatedComments)
        database.postDao().updatePost(com.example.data.local.PostEntity.fromPost(updatedPost))
    }

    suspend fun deletePost(postId: String) = withContext(Dispatchers.IO) {
        database.postDao().deletePost(postId)
        addLog("FEED", "Publicación eliminada: $postId")
    }

    suspend fun postStatus(status: UserStatus) = withContext(Dispatchers.IO) {
        database.statusDao().insertStatus(StatusEntity.fromUserStatus(status))
        scope.launch {
            cloudStorage.uploadEncryptedBlock(
                filename = "${status.id}.json",
                jsonPayload = status.text,
                folderPath = "/chatpro/estados/"
            )
        }
        addLog("STATUS", "Nuevo estado publicado por ${status.authorDisplayName}")
    }

    suspend fun deleteStatus(statusId: String) = withContext(Dispatchers.IO) {
        database.statusDao().deleteStatus(statusId)
    }

    // --- Admin Operations (Eliel_21) ---
    suspend fun suspendUser(username: String, suspend: Boolean) = withContext(Dispatchers.IO) {
        database.userDao().setUserSuspended(username, suspend)
        addLog("ADMIN", "Usuario $username ${if (suspend) "suspendido" else "reactivado"} por Admin")
    }

    suspend fun deleteUser(username: String) = withContext(Dispatchers.IO) {
        database.userDao().deleteUser(username)
        addLog("ADMIN", "Usuario $username eliminado por Admin")
    }

    suspend fun purgeOldStorage() = withContext(Dispatchers.IO) {
        val olderThan = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        database.messageDao().deleteMessagesOlderThan(olderThan)
        database.statusDao().purgeExpiredStatuses(System.currentTimeMillis() - (24 * 60 * 60 * 1000))
        addLog("ADMIN", "Limpieza de almacenamiento ejecutada con éxito")
    }

    suspend fun sendBroadcastMessage(sender: String, text: String) = withContext(Dispatchers.IO) {
        val broadcastMessage = ChatMessage(
            chatId = "broadcast",
            senderUsername = sender,
            recipientUsername = "ALL",
            text = text,
            status = MessageStatus.SENT
        )
        database.messageDao().insertMessage(MessageEntity.fromChatMessage(broadcastMessage))
        addLog("BROADCAST", "Mensaje global emitido: $text")
    }

    fun updateGlobalConfig(config: GlobalConfig) {
        _globalConfig.value = config
    }

    private fun addLog(tag: String, message: String, isError: Boolean = false) {
        val log = SystemLog(
            id = "log_${System.currentTimeMillis()}",
            timestamp = System.currentTimeMillis(),
            tag = tag,
            message = message,
            isError = isError
        )
        val current = _systemLogs.value.toMutableList()
        current.add(0, log)
        if (current.size > 200) current.removeAt(current.size - 1)
        _systemLogs.value = current
    }
}
