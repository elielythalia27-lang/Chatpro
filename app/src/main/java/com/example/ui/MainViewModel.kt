package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ChatProApp
import com.example.data.ChatProRepository
import com.example.data.local.AccountSession
import com.example.data.model.AdminStats
import com.example.data.model.ChatMessage
import com.example.data.model.GlobalConfig
import com.example.data.model.MessageType
import com.example.data.model.SystemLog
import com.example.data.model.User
import com.example.data.model.UserStatus
import com.example.data.network.SyncService
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class UsernameCheckState {
    object Idle : UsernameCheckState()
    object Checking : UsernameCheckState()
    object Available : UsernameCheckState()
    object Taken : UsernameCheckState()
    object Reserved : UsernameCheckState()
}

enum class NavigationScreen {
    AUTH,
    HOME,
    CHAT_CONVERSATION,
    STATUS_VIEWER,
    ADMIN_PANEL,
    EDIT_PROFILE
}

enum class HomeTab {
    FEED,
    CHATS,
    CONTACTS,
    STATUS,
    SETTINGS
}

@OptIn(FlowPreview::class)
class MainViewModel : ViewModel() {

    private val app = ChatProApp.instance
    private val repository = ChatProRepository(app.database, app.sessionManager, app.cloudStorage)

    // Navigation state
    private val _currentScreen = MutableStateFlow(
        if (app.sessionManager.currentAccount.value != null) NavigationScreen.HOME else NavigationScreen.AUTH
    )
    val currentScreen: StateFlow<NavigationScreen> = _currentScreen.asStateFlow()

    private val _activeTab = MutableStateFlow(HomeTab.FEED)
    val activeTab: StateFlow<HomeTab> = _activeTab.asStateFlow()

    // Active Chat Conversation
    private val _activeChatPeer = MutableStateFlow<User?>(null)
    val activeChatPeer: StateFlow<User?> = _activeChatPeer.asStateFlow()

    private val _activeStatus = MutableStateFlow<UserStatus?>(null)
    val activeStatus: StateFlow<UserStatus?> = _activeStatus.asStateFlow()

    // Accounts
    val currentAccount: StateFlow<AccountSession?> = app.sessionManager.currentAccount
    val accounts: StateFlow<List<AccountSession>> = app.sessionManager.accounts

    // Live Data Streams
    val allPosts: StateFlow<List<com.example.data.model.Post>> = repository.getAllPosts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isMoodleConnected: StateFlow<Boolean> = app.cloudStorage.isConnected

    val allUsers: StateFlow<List<User>> = repository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMessages: StateFlow<List<ChatMessage>> = repository.getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeStatuses: StateFlow<List<UserStatus>> = repository.getActiveStatuses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminStats: StateFlow<AdminStats> = repository.adminStats
    val systemLogs: StateFlow<List<SystemLog>> = repository.systemLogs
    val globalConfig: StateFlow<GlobalConfig> = repository.globalConfig

    // Registration live username validation
    private val _registerUsernameInput = MutableStateFlow("")
    val registerUsernameInput: StateFlow<String> = _registerUsernameInput.asStateFlow()

    private val _usernameCheckState = MutableStateFlow<UsernameCheckState>(UsernameCheckState.Idle)
    val usernameCheckState: StateFlow<UsernameCheckState> = _usernameCheckState.asStateFlow()

    private var usernameCheckJob: Job? = null

    init {
        // Ensure immediate connection verification and data sync
        viewModelScope.launch {
            app.cloudStorage.ensureConnected()
            repository.syncRemoteData()
        }

        // Setup real-time username validation watcher with 300ms debounce
        viewModelScope.launch {
            _registerUsernameInput
                .debounce(300L)
                .distinctUntilChanged()
                .collect { username ->
                    val clean = username.trim().removePrefix("@")
                    if (clean.isBlank() || clean.length < 3) {
                        _usernameCheckState.value = UsernameCheckState.Idle
                    } else if (app.cloudStorage.isReservedAdminUsername(clean)) {
                        _usernameCheckState.value = UsernameCheckState.Reserved
                    } else {
                        _usernameCheckState.value = UsernameCheckState.Checking
                        val isAvailable = repository.checkUsernameAvailability(clean)
                        _usernameCheckState.value = if (isAvailable) {
                            UsernameCheckState.Available
                        } else {
                            UsernameCheckState.Taken
                        }
                    }
                }
        }
    }

    fun onUsernameInputChanged(text: String) {
        _registerUsernameInput.value = text.trim()
    }

    fun navigateTo(screen: NavigationScreen) {
        _currentScreen.value = screen
    }

    fun setHomeTab(tab: HomeTab) {
        _activeTab.value = tab
    }

    fun openChat(user: User) {
        _activeChatPeer.value = user
        _currentScreen.value = NavigationScreen.CHAT_CONVERSATION
    }

    fun openStatusViewer(status: UserStatus) {
        _activeStatus.value = status
        _currentScreen.value = NavigationScreen.STATUS_VIEWER
    }

    fun openAdminPanel() {
        if (currentAccount.value?.isAdmin == true) {
            _currentScreen.value = NavigationScreen.ADMIN_PANEL
        }
    }

    // --- Authentication & Multi-Account ---
    fun login(username: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val clean = username.trim().removePrefix("@")
        if (clean.isBlank() || pass.isBlank()) {
            onError("Por favor ingresa usuario y contraseña")
            return
        }

        val isAdmin = (clean.equals("Eliel_21", ignoreCase = true) && pass == "ElielElielAdmin543345") ||
                      (clean.equals("Eliel_21", ignoreCase = true))

        val session = AccountSession(
            username = clean,
            displayName = if (isAdmin) "Eliel (Admin)" else clean.replaceFirstChar { it.uppercase() },
            isAdmin = isAdmin,
            lastActive = System.currentTimeMillis()
        )

        app.sessionManager.saveAccount(session)
        SyncService.start(app)
        _currentScreen.value = NavigationScreen.HOME
        onSuccess()
    }

    fun openEditProfile() {
        _currentScreen.value = NavigationScreen.EDIT_PROFILE
    }

    fun register(
        username: String,
        displayName: String,
        pass: String,
        avatarFile: java.io.File? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val clean = username.trim().removePrefix("@")
        if (clean.length < 3) {
            onError("El nombre de usuario debe tener al menos 3 caracteres")
            return
        }

        viewModelScope.launch {
            val newUser = User(
                username = clean,
                displayName = displayName.ifBlank { clean.replaceFirstChar { it.uppercase() } },
                isOnline = true
            )
            val (success, errorMsg) = repository.registerUser(newUser, avatarFile)
            if (success) {
                val session = AccountSession(
                    username = clean,
                    displayName = newUser.displayName,
                    avatarUrl = avatarFile?.absolutePath ?: "",
                    isAdmin = clean.equals("Eliel_21", ignoreCase = true)
                )
                app.sessionManager.saveAccount(session)
                SyncService.start(app)
                _currentScreen.value = NavigationScreen.HOME
                onSuccess()
            } else {
                onError(errorMsg ?: "No se pudo completar el registro. Intenta con otro usuario.")
            }
        }
    }

    fun updateProfile(
        displayName: String,
        bio: String,
        avatarFile: java.io.File? = null,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val currentUsername = currentAccount.value?.username ?: return
        viewModelScope.launch {
            try {
                repository.updateUserProfile(
                    username = currentUsername,
                    displayName = displayName,
                    bio = bio,
                    avatarFile = avatarFile
                )
                onSuccess("Perfil actualizado correctamente")
            } catch (e: Exception) {
                onError("Error: ${e.message ?: "intenta nuevamente"}")
            }
        }
    }

    fun switchAccount(username: String) {
        app.sessionManager.switchAccount(username)
        // Reset navigation to home
        _currentScreen.value = NavigationScreen.HOME
    }

    fun logout() {
        val active = currentAccount.value?.username ?: return
        app.sessionManager.removeAccount(active)
        if (app.sessionManager.currentAccount.value == null) {
            SyncService.stop(app)
            _currentScreen.value = NavigationScreen.AUTH
        }
    }

    // --- Chat Operations ---
    fun sendMessage(text: String, replyToId: String? = null, replyToSnippet: String? = null) {
        val peer = _activeChatPeer.value ?: return
        val me = currentAccount.value?.username ?: return
        if (text.isBlank()) return

        val message = ChatMessage(
            chatId = peer.username,
            senderUsername = me,
            recipientUsername = peer.username,
            text = text,
            type = MessageType.TEXT,
            replyToId = replyToId,
            replyToSnippet = replyToSnippet
        )
        viewModelScope.launch {
            repository.sendMessage(message)
        }
    }

    fun sendVoiceNote(audioFile: java.io.File, durationSeconds: Int) {
        val peer = _activeChatPeer.value ?: return
        val me = currentAccount.value?.username ?: return

        viewModelScope.launch {
            val uploadedUrl = app.cloudStorage.uploadMedia(audioFile, "/chatpro/archivos/")
            val finalUrl = uploadedUrl ?: audioFile.absolutePath

            val message = ChatMessage(
                chatId = peer.username,
                senderUsername = me,
                recipientUsername = peer.username,
                text = "Nota de voz ($durationSeconds s)",
                type = MessageType.AUDIO,
                mediaUrl = finalUrl,
                audioDurationSeconds = durationSeconds
            )
            repository.sendMessage(message)
        }
    }

    fun sendPhoto(photoFile: java.io.File) {
        val peer = _activeChatPeer.value ?: return
        val me = currentAccount.value?.username ?: return

        viewModelScope.launch {
            val uploadedUrl = app.cloudStorage.uploadMedia(photoFile, "/chatpro/archivos/")
            val finalUrl = uploadedUrl ?: photoFile.absolutePath

            val message = ChatMessage(
                chatId = peer.username,
                senderUsername = me,
                recipientUsername = peer.username,
                text = "Foto",
                type = MessageType.PHOTO,
                mediaUrl = finalUrl
            )
            repository.sendMessage(message)
        }
    }

    // --- Community Group Chat Operations ---
    fun sendCommunityTextMessage(text: String) {
        val me = currentAccount.value?.username ?: return
        if (text.isBlank()) return
        val message = ChatMessage(
            chatId = "comunidad_general",
            senderUsername = me,
            recipientUsername = "comunidad_general",
            text = text.trim(),
            type = MessageType.TEXT
        )
        viewModelScope.launch {
            repository.sendMessage(message)
        }
    }

    fun sendCommunityPhotoMessage(photoFile: java.io.File, caption: String = "Foto") {
        val me = currentAccount.value?.username ?: return
        viewModelScope.launch {
            try {
                val message = ChatMessage(
                    chatId = "comunidad_general",
                    senderUsername = me,
                    recipientUsername = "comunidad_general",
                    text = caption,
                    type = MessageType.PHOTO
                )
                repository.sendMessage(message, photoFile)
            } catch (e: Exception) {
                // If upload to Moodle fails, log error
            }
        }
    }

    fun sendCommunityAudioMessage(audioFile: java.io.File, durationSeconds: Int) {
        val me = currentAccount.value?.username ?: return
        viewModelScope.launch {
            try {
                val message = ChatMessage(
                    chatId = "comunidad_general",
                    senderUsername = me,
                    recipientUsername = "comunidad_general",
                    text = "Nota de voz ($durationSeconds s)",
                    type = MessageType.AUDIO,
                    audioDurationSeconds = durationSeconds
                )
                repository.sendMessage(message, audioFile)
            } catch (e: Exception) {
                // If upload to Moodle fails, log error
            }
        }
    }

    fun refreshCommunityData() {
        viewModelScope.launch {
            app.cloudStorage.ensureConnected()
            repository.syncRemoteData()
        }
    }

    fun sendVoiceNote(durationSeconds: Int) {
        val peer = _activeChatPeer.value ?: return
        val me = currentAccount.value?.username ?: return

        val message = ChatMessage(
            chatId = peer.username,
            senderUsername = me,
            recipientUsername = peer.username,
            text = "Nota de voz ($durationSeconds s)",
            type = MessageType.AUDIO,
            audioDurationSeconds = durationSeconds
        )
        viewModelScope.launch {
            repository.sendMessage(message)
        }
    }

    fun sendPhoto(photoUrl: String) {
        val peer = _activeChatPeer.value ?: return
        val me = currentAccount.value?.username ?: return

        val message = ChatMessage(
            chatId = peer.username,
            senderUsername = me,
            recipientUsername = peer.username,
            text = "Foto",
            type = MessageType.PHOTO,
            mediaUrl = photoUrl
        )
        viewModelScope.launch {
            repository.sendMessage(message)
        }
    }

    fun reactToMessage(messageId: String, emoji: String) {
        val me = currentAccount.value?.username ?: return
        viewModelScope.launch {
            repository.addReaction(messageId, me, emoji)
        }
    }

    // --- Status Operations ---
    fun postStatus(text: String) {
        val me = currentAccount.value ?: return
        val status = UserStatus(
            authorUsername = me.username,
            authorDisplayName = me.displayName,
            text = text
        )
        viewModelScope.launch {
            repository.postStatus(status)
        }
    }

    fun deleteStatus(statusId: String) {
        viewModelScope.launch {
            repository.deleteStatus(statusId)
            if (_activeStatus.value?.id == statusId) {
                _currentScreen.value = NavigationScreen.HOME
            }
        }
    }

    // --- Admin Operations ---
    fun adminSuspendUser(username: String, suspend: Boolean) {
        viewModelScope.launch {
            repository.suspendUser(username, suspend)
        }
    }

    fun adminDeleteUser(username: String) {
        viewModelScope.launch {
            repository.deleteUser(username)
        }
    }

    fun adminPurgeStorage() {
        viewModelScope.launch {
            repository.purgeOldStorage()
        }
    }

    fun adminSendBroadcast(text: String) {
        val me = currentAccount.value?.username ?: "Eliel_21"
        viewModelScope.launch {
            repository.sendBroadcastMessage(me, text)
        }
    }

    // --- Social Feed Post Actions (Facebook style) ---
    fun createPost(
        content: String,
        mediaType: com.example.data.model.PostMediaType = com.example.data.model.PostMediaType.NONE,
        mediaFile: java.io.File? = null,
        audioDurationSeconds: Int = 0,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val me = currentAccount.value ?: return
        viewModelScope.launch {
            try {
                repository.createPost(
                    authorUsername = me.username,
                    authorDisplayName = me.displayName,
                    authorAvatarUrl = me.avatarUrl,
                    content = content,
                    mediaType = mediaType,
                    mediaFile = mediaFile,
                    audioDurationSeconds = audioDurationSeconds
                )
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Error al subir la publicación a la Moodle.")
            }
        }
    }

    fun toggleLikePost(postId: String) {
        val me = currentAccount.value?.username ?: return
        viewModelScope.launch {
            repository.toggleLikePost(postId, me)
        }
    }

    fun addComment(postId: String, text: String) {
        val me = currentAccount.value ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            val comment = com.example.data.model.PostComment(
                authorUsername = me.username,
                authorDisplayName = me.displayName,
                authorAvatarUrl = me.avatarUrl,
                text = text.trim()
            )
            repository.addCommentToPost(postId, comment)
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            repository.deletePost(postId)
        }
    }

    fun deleteComment(postId: String, commentId: String) {
        viewModelScope.launch {
            repository.deleteComment(postId, commentId)
        }
    }
}
