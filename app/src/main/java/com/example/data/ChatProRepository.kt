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
     * Sincronización completa con la evidencia de Moodle:
     * - Descarga los usuarios registrados para mostrar sus nombres y fotos reales
     * - Descarga las publicaciones del feed con sus fotos, likes y comentarios
     * - Descarga los mensajes del chat comunitario
     */
    suspend fun syncRemoteData() = withContext(Dispatchers.IO) {
        try {
            val token = cloudStorage.getOrRefreshToken()
            if (token != null) {
                // 1. Usuarios y avatares reales
                val remoteUsers = cloudStorage.fetchRemoteUsers()
                if (remoteUsers.isNotEmpty()) {
                    database.userDao().insertUsers(remoteUsers.map { UserEntity.fromUser(it) })
                }

                // 2. Publicaciones del Feed (posts, likes, comentarios)
                val remotePosts = cloudStorage.fetchRemotePosts()
                if (remotePosts.isNotEmpty()) {
                    database.postDao().insertPosts(remotePosts.map { PostEntity.fromPost(it) })
                }

                // 3. Mensajes del Chat Comunitario
                val remoteCommunityMsgs = cloudStorage.fetchRemoteCommunityMessages()
                if (remoteCommunityMsgs.isNotEmpty()) {
                    database.messageDao().insertMessages(remoteCommunityMsgs.map { MessageEntity.fromChatMessage(it) })
                }
                addLog("SYNC", "Datos sincronizados desde la evidencia de Moodle con éxito.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
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

    suspend fun sendMessage(message: ChatMessage, mediaFile: File? = null) = withContext(Dispatchers.IO) {
        // Subir a la evidencia de Moodle
        val uploaded = cloudStorage.uploadCommunityMessage(message, mediaFile)
        val confirmedEntity = MessageEntity.fromChatMessage(uploaded.copy(status = MessageStatus.SENT))
        database.messageDao().insertMessage(confirmedEntity)
        addLog("CHAT", "Mensaje guardado en la evidencia de Moodle: ${uploaded.text.take(25)}")
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
        val updatedMsg = msg.copy(reactions = updatedReactions)
        database.messageDao().updateMessage(MessageEntity.fromChatMessage(updatedMsg))

        // Si es el chat grupal comunitario, sincronizar a la evidencia en la nube
        if (msg.chatId == "comunidad_general") {
            scope.launch(Dispatchers.IO) {
                try {
                    val currentMsgs = cloudStorage.fetchRemoteCommunityMessages().toMutableList()
                    val idx = currentMsgs.indexOfFirst { it.id == messageId }
                    if (idx != -1) {
                        currentMsgs[idx] = currentMsgs[idx].copy(reactions = updatedReactions)
                        cloudStorage.saveRemoteCommunityMessages(currentMsgs)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // --- Users & Directory ---
    fun getAllUsers(): Flow<List<User>> {
        return database.userDao().getAllUsers().map { list -> list.map { it.toUser() } }
    }

    suspend fun registerUser(user: User, avatarFile: File? = null): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        val clean = user.username.trim().removePrefix("@")

        // 1. Validar nombre de usuario reservado para administración
        if (cloudStorage.isReservedAdminUsername(clean) && !user.isAdmin) {
            return@withContext Pair(false, "Este nombre de usuario está reservado para la administración de ChatPro.")
        }

        // 2. Subir foto de perfil a la EVIDENCIA si se seleccionó una
        var finalAvatarUrl = user.avatarUrl
        if (avatarFile != null && avatarFile.exists()) {
            val uploadedUrl = cloudStorage.uploadUserAvatar(clean, avatarFile)
            if (!uploadedUrl.isNullOrBlank()) {
                finalAvatarUrl = uploadedUrl
            }
        }

        val finalUser = user.copy(username = clean, avatarUrl = finalAvatarUrl)

        // 3. Registrar en chatpro_users.json en la evidencia (valida unicidad online)
        val (success, errorMsg) = cloudStorage.registerUserInEvidence(finalUser)
        if (!success) {
            return@withContext Pair(false, errorMsg ?: "Error al registrar el usuario en el servidor.")
        }

        // 4. Guardar en base de datos local
        database.userDao().insertUser(UserEntity.fromUser(finalUser))
        addLog("AUTH", "Nuevo usuario registrado: @$clean")
        Pair(true, null)
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
            if (!uploadedUrl.isNullOrBlank()) {
                newAvatarUrl = uploadedUrl
            }
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

        // 3. Upload to remote CloudStorage evidencia
        scope.launch {
            try {
                cloudStorage.updateUserInEvidence(updatedUser)
            } catch (e: Exception) {
                // Ignore
            }
        }

        addLog("PROFILE", "Perfil actualizado para @$username")
        updatedUser
    }

    suspend fun checkUsernameAvailability(username: String): Boolean {
        val clean = username.trim().removePrefix("@")
        if (cloudStorage.isReservedAdminUsername(clean)) return false
        val local = database.userDao().getUserByUsername(clean)
        if (local != null) return false
        val remoteUsers = cloudStorage.fetchRemoteUsers()
        return remoteUsers.none { it.username.trim().removePrefix("@").equals(clean, ignoreCase = true) }
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

        // Subir a la evidencia de Moodle y agregar al chatpro_feed.json global
        val uploaded = cloudStorage.uploadPost(initialPost, mediaFile)

        // Guardar en Room local
        database.postDao().insertPost(com.example.data.local.PostEntity.fromPost(uploaded))
        addLog("FEED", "Publicación guardada en la evidencia de Moodle por @$authorUsername")
        uploaded
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

        // Sincronizar actualización de likes a chatpro_feed.json en Moodle
        scope.launch(Dispatchers.IO) {
            try {
                val currentFeed = cloudStorage.fetchRemotePosts().toMutableList()
                val idx = currentFeed.indexOfFirst { it.id == postId }
                if (idx != -1) {
                    currentFeed[idx] = currentFeed[idx].copy(likes = updatedLikes)
                    cloudStorage.saveRemotePosts(currentFeed)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun addCommentToPost(postId: String, comment: com.example.data.model.PostComment) = withContext(Dispatchers.IO) {
        val entity = database.postDao().getPostById(postId) ?: return@withContext
        val post = entity.toPost()
        val updatedComments = post.comments + comment
        val updatedPost = post.copy(comments = updatedComments)
        database.postDao().updatePost(com.example.data.local.PostEntity.fromPost(updatedPost))

        // Sincronizar nuevo comentario a chatpro_feed.json en Moodle
        scope.launch(Dispatchers.IO) {
            try {
                val currentFeed = cloudStorage.fetchRemotePosts().toMutableList()
                val idx = currentFeed.indexOfFirst { it.id == postId }
                if (idx != -1) {
                    currentFeed[idx] = currentFeed[idx].copy(comments = updatedComments)
                    cloudStorage.saveRemotePosts(currentFeed)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun deleteComment(postId: String, commentId: String) = withContext(Dispatchers.IO) {
        val entity = database.postDao().getPostById(postId) ?: return@withContext
        val post = entity.toPost()
        val updatedComments = post.comments.filter { it.id != commentId }
        val updatedPost = post.copy(comments = updatedComments)
        database.postDao().updatePost(com.example.data.local.PostEntity.fromPost(updatedPost))

        // Sincronizar eliminación de comentario a chatpro_feed.json en Moodle
        scope.launch(Dispatchers.IO) {
            try {
                val currentFeed = cloudStorage.fetchRemotePosts().toMutableList()
                val idx = currentFeed.indexOfFirst { it.id == postId }
                if (idx != -1) {
                    currentFeed[idx] = currentFeed[idx].copy(comments = updatedComments)
                    cloudStorage.saveRemotePosts(currentFeed)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun deletePost(postId: String) = withContext(Dispatchers.IO) {
        database.postDao().deletePost(postId)
        scope.launch(Dispatchers.IO) {
            try {
                val currentFeed = cloudStorage.fetchRemotePosts().toMutableList()
                currentFeed.removeAll { it.id == postId }
                cloudStorage.saveRemotePosts(currentFeed)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        addLog("FEED", "Publicación eliminada: $postId")
    }

    suspend fun postStatus(status: UserStatus) = withContext(Dispatchers.IO) {
        database.statusDao().insertStatus(StatusEntity.fromUserStatus(status))
        scope.launch {
            try {
                cloudStorage.uploadTextFile(
                    filename = "status_${status.id}.json",
                    content = status.text
                )
            } catch (e: Exception) {
                // Ignore status upload errors
            }
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
