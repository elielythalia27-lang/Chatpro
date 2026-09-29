package com.example.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.model.ChatMessage
import com.example.data.model.Post
import com.example.data.model.User
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

class CloudStorage(private val context: Context) {

    companion object {
        const val DEFAULT_BASE_URL = "https://cursos.ucf.edu.cu/"
        const val DEFAULT_USERNAME = "julianrene"
        const val DEFAULT_PASSWORD = "Transfer60*"
        const val DEFAULT_USER_ID = "2886"
        const val DEFAULT_CONTEXT_ID = 44640L
        const val EVIDENCE_ID = "480" // Evidencia activa de almacenamiento ChatPro
    }

    private val prefs = context.getSharedPreferences("chatpro_moodle_config", Context.MODE_PRIVATE)

    var moodleBaseUrl: String
        get() {
            val url = prefs.getString("moodle_url", DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
            return if (url.endsWith("/")) url else "$url/"
        }
        set(value) {
            val formatted = if (value.trim().endsWith("/")) value.trim() else "${value.trim()}/"
            prefs.edit().putString("moodle_url", formatted).apply()
            resetApi()
        }

    var moodleUsername: String
        get() = prefs.getString("moodle_user", DEFAULT_USERNAME) ?: DEFAULT_USERNAME
        set(value) {
            prefs.edit().putString("moodle_user", value.trim()).apply()
            cachedToken = null
            cachedSesskey = null
        }

    var moodlePassword: String
        get() = prefs.getString("moodle_pass", DEFAULT_PASSWORD) ?: DEFAULT_PASSWORD
        set(value) {
            prefs.edit().putString("moodle_pass", value).apply()
            cachedToken = null
            cachedSesskey = null
        }

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val postAdapter = moshi.adapter(Post::class.java)
    private val postListAdapter = moshi.adapter<List<Post>>(
        Types.newParameterizedType(List::class.java, Post::class.java)
    )
    private val userAdapter = moshi.adapter(User::class.java)
    private val userListAdapter = moshi.adapter<List<User>>(
        Types.newParameterizedType(List::class.java, User::class.java)
    )
    private val messageAdapter = moshi.adapter(ChatMessage::class.java)
    private val messageListAdapter = moshi.adapter<List<ChatMessage>>(
        Types.newParameterizedType(List::class.java, ChatMessage::class.java)
    )

    // In-memory cookie jar for web session authentication (user_evidence_edit.php)
    private val cookieJar = object : CookieJar {
        private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val list = cookieStore.getOrPut(url.host) { mutableListOf() }
            cookies.forEach { newCookie ->
                list.removeAll { it.name == newCookie.name }
                list.add(newCookie)
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieStore[url.host] ?: emptyList()
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .cookieJar(cookieJar)
        .build()

    private var _api: ApiService? = null
    private fun getApi(): ApiService {
        return _api ?: synchronized(this) {
            _api ?: Retrofit.Builder()
                .baseUrl(moodleBaseUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(ApiService::class.java)
                .also { _api = it }
        }
    }

    private fun resetApi() {
        _api = null
        cachedToken = null
        cachedSesskey = null
    }

    private var cachedToken: String? = null
    private var cachedSesskey: String? = null
    private var cachedContextId: Long = DEFAULT_CONTEXT_ID
    private val tokenMutex = Mutex()
    private val evidenceMutex = Mutex()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    fun isNetworkAvailable(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val net = cm?.activeNetwork ?: return false
            val cap = cm.getNetworkCapabilities(net) ?: return false
            cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            true
        }
    }

    suspend fun ensureConnected(): Boolean = withContext(Dispatchers.IO) {
        val token = getOrRefreshToken()
        val online = token != null
        _isConnected.value = online
        online
    }

    /**
     * Obtains or refreshes token from Moodle login/token.php
     */
    suspend fun getOrRefreshToken(forceRefresh: Boolean = false): String? = withContext(Dispatchers.IO) {
        tokenMutex.withLock {
            if (!forceRefresh && !cachedToken.isNullOrBlank()) {
                _isConnected.value = true
                return@withContext cachedToken
            }

            try {
                val api = getApi()
                val response = api.obtenerToken(
                    user = moodleUsername,
                    pass = moodlePassword,
                    service = "moodle_mobile_app"
                )

                if (response.isSuccessful && response.body()?.token != null) {
                    cachedToken = response.body()?.token
                    _isConnected.value = true
                    cachedToken
                } else {
                    _isConnected.value = false
                    null
                }
            } catch (e: Exception) {
                _isConnected.value = false
                null
            }
        }
    }

    /**
     * Obtiene la sesskey activa del usuario en Moodle web frontend para gestionar la evidencia
     */
    private suspend fun getWebSesskey(): String? = withContext(Dispatchers.IO) {
        if (!cachedSesskey.isNullOrBlank()) return@withContext cachedSesskey

        try {
            // 1. Fetch login/index.php to extract logintoken
            val loginPageReq = Request.Builder()
                .url("${moodleBaseUrl}login/index.php")
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .build()
            val loginPageResp = okHttpClient.newCall(loginPageReq).execute()
            val loginHtml = loginPageResp.body?.string() ?: ""

            val logintokenMatch = Regex("""name="logintoken" value="([^"]+)"""").find(loginHtml)
            val logintoken = logintokenMatch?.groupValues?.get(1) ?: ""

            // 2. POST login credentials
            val loginBody = FormBody.Builder()
                .add("username", moodleUsername)
                .add("password", moodlePassword)
                .add("logintoken", logintoken)
                .build()

            val loginPostReq = Request.Builder()
                .url("${moodleBaseUrl}login/index.php")
                .post(loginBody)
                .build()

            val loginPostResp = okHttpClient.newCall(loginPostReq).execute()
            loginPostResp.close()

            // 3. Fetch my/ to extract sesskey
            val myReq = Request.Builder()
                .url("${moodleBaseUrl}my/")
                .build()

            val myResp = okHttpClient.newCall(myReq).execute()
            val myHtml = myResp.body?.string() ?: ""

            val sesskeyMatch = Regex(""""sesskey":"([^"]+)"""").find(myHtml)
                ?: Regex("""name="sesskey" value="([^"]+)"""").find(myHtml)

            cachedSesskey = sesskeyMatch?.groupValues?.get(1)
            cachedSesskey
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Subida de archivos multimedia a EVIDENCIA (SIEMPRE evidence, nunca private files)
     */
    suspend fun uploadMedia(
        file: File,
        remoteFilename: String = file.name,
        onProgress: (Int) -> Unit = {}
    ): String = withContext(Dispatchers.IO) {
        val token = getOrRefreshToken()
            ?: throw IllegalStateException("No hay conexión con la Moodle de la UCF. Verifica tu internet.")

        val api = getApi()

        // 1. Obtener draftitemid no utilizado vía WS
        val unusedResp = try { api.getUnusedDraftItemId(token = token) } catch (e: Exception) { null }
        val draftItemId = unusedResp?.body()?.itemId
            ?: (System.currentTimeMillis() % 1_000_000_000)

        if (unusedResp?.body()?.contextId != null && unusedResp.body()!!.contextId!! > 0) {
            cachedContextId = unusedResp.body()!!.contextId!!
        }

        // 2. Subir archivo a la zona draft
        val mimeType = when (file.extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "m4a", "aac", "mp3" -> "audio/mp4"
            "mp4" -> "video/mp4"
            else -> "application/octet-stream"
        }

        val requestBody = ProgressRequestBody(file, mimeType.toMediaTypeOrNull(), onProgress)
        val filePart = MultipartBody.Part.createFormData("file", remoteFilename, requestBody)
        val filepathBody = "/".toRequestBody("text/plain".toMediaTypeOrNull())
        val fileareaBody = "draft".toRequestBody("text/plain".toMediaTypeOrNull())

        val uploadResp = api.uploadDraftFile(
            token = token,
            itemId = draftItemId,
            file = filePart,
            filepath = filepathBody,
            filearea = fileareaBody
        )

        if (!uploadResp.isSuccessful || uploadResp.body().isNullOrEmpty()) {
            throw IllegalStateException("Fallo en la subida del archivo a Moodle: ${uploadResp.code()}")
        }

        val uploadedItem = uploadResp.body()!!.first()
        if (uploadedItem.contextId != null && uploadedItem.contextId > 0) {
            cachedContextId = uploadedItem.contextId
        }

        // 3. Vincular y guardar exclusivamente en la EVIDENCIA
        commitToEvidence(draftItemId)

        // URL permanente de descarga en la Evidencia
        "${moodleBaseUrl}webservice/pluginfile.php/$cachedContextId/core_competency/userevidence/$EVIDENCE_ID/$remoteFilename?token=$token"
    }

    /**
     * Sube un archivo de texto JSON exclusivamente a la EVIDENCIA
     */
    suspend fun uploadTextFile(
        filename: String,
        content: String
    ): String = withContext(Dispatchers.IO) {
        val token = getOrRefreshToken()
            ?: throw IllegalStateException("No se pudo autenticar con Moodle. Verifica tu conexión.")

        val api = getApi()

        // 1. Obtener draftitemid
        val unusedResp = try { api.getUnusedDraftItemId(token = token) } catch (e: Exception) { null }
        val draftItemId = unusedResp?.body()?.itemId
            ?: (System.currentTimeMillis() % 1_000_000_000)

        if (unusedResp?.body()?.contextId != null && unusedResp.body()!!.contextId!! > 0) {
            cachedContextId = unusedResp.body()!!.contextId!!
        }

        // 2. Subir contenido JSON a la zona draft
        val requestBody = content.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val filePart = MultipartBody.Part.createFormData("file", filename, requestBody)
        val filepathBody = "/".toRequestBody("text/plain".toMediaTypeOrNull())
        val fileareaBody = "draft".toRequestBody("text/plain".toMediaTypeOrNull())

        val uploadResp = api.uploadDraftFile(
            token = token,
            itemId = draftItemId,
            file = filePart,
            filepath = filepathBody,
            filearea = fileareaBody
        )

        if (!uploadResp.isSuccessful || uploadResp.body().isNullOrEmpty()) {
            throw IllegalStateException("Error al subir archivo $filename a Moodle.")
        }

        val uploadedItem = uploadResp.body()!!.first()
        if (uploadedItem.contextId != null && uploadedItem.contextId > 0) {
            cachedContextId = uploadedItem.contextId
        }

        // 3. Guardar en Evidencia
        commitToEvidence(draftItemId)

        "${moodleBaseUrl}webservice/pluginfile.php/$cachedContextId/core_competency/userevidence/$EVIDENCE_ID/$filename?token=$token"
    }

    /**
     * Descarga contenido de texto JSON directamente desde la EVIDENCIA (con cache-buster)
     */
    suspend fun downloadTextFile(filename: String): String? = withContext(Dispatchers.IO) {
        try {
            val token = getOrRefreshToken() ?: return@withContext null
            val url = "${moodleBaseUrl}webservice/pluginfile.php/$cachedContextId/core_competency/userevidence/$EVIDENCE_ID/$filename?token=$token&_t=${System.currentTimeMillis()}"
            val req = Request.Builder()
                .url(url)
                .header("Cache-Control", "no-cache")
                .build()
            val resp = okHttpClient.newCall(req).execute()
            if (resp.isSuccessful && resp.body != null) {
                val str = resp.body!!.string()
                // Si Moodle devuelve error json, ignorar
                if (str.startsWith("{\"error\":") || str.startsWith("{\"exception\":")) null else str
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Vincula el draftitemid a la EVIDENCIA 480 vía formulario oficial
     */
    private suspend fun commitToEvidence(draftItemId: Long): Boolean = withContext(Dispatchers.IO) {
        evidenceMutex.withLock {
            try {
                val sesskey = getWebSesskey() ?: return@withLock false

                val payload = FormBody.Builder()
                    .add("id", EVIDENCE_ID)
                    .add("userid", DEFAULT_USER_ID)
                    .add("sesskey", sesskey)
                    .add("_qf__tool_lp_form_user_evidence", "1")
                    .add("name", "ChatPro Storage")
                    .add("description[text]", "Almacenamiento multimedia ChatPro")
                    .add("description[format]", "1")
                    .add("url", "")
                    .add("files", draftItemId.toString())
                    .add("submitbutton", "Guardar cambios")
                    .build()

                val saveReq = Request.Builder()
                    .url("${moodleBaseUrl}admin/tool/lp/user_evidence_edit.php?id=$EVIDENCE_ID&userid=$DEFAULT_USER_ID&return=list")
                    .post(payload)
                    .build()

                val resp = okHttpClient.newCall(saveReq).execute()
                resp.isSuccessful || resp.code in 300..399
            } catch (e: Exception) {
                false
            }
        }
    }

    // ==========================================
    // GESTIÓN DE USUARIOS EN EVIDENCE (JSON)
    // ==========================================

    fun isReservedAdminUsername(username: String): Boolean {
        val clean = username.trim().removePrefix("@").lowercase()
        val reserved = listOf(
            "eliel_21", "eliel21", "eliel", "admin", "administrador",
            "administrator", "root", "chatpro_admin", "staff", "soporte",
            "support", "moderador", "moderator", "oficial", "official"
        )
        if (clean in reserved) return true
        if (clean.contains("admin") || clean.contains("soporte") || clean.contains("staff")) return true
        if (clean.startsWith("eliel")) return true
        return false
    }

    suspend fun fetchRemoteUsers(): List<User> = withContext(Dispatchers.IO) {
        try {
            val raw = downloadTextFile("chatpro_users.json") ?: return@withContext emptyList()
            userListAdapter.fromJson(raw) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveRemoteUsers(users: List<User>): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = userListAdapter.toJson(users)
            uploadTextFile("chatpro_users.json", json)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun registerUserInEvidence(user: User): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        val cleanUser = user.username.trim().removePrefix("@").lowercase()

        // 1. Validar nombres reservados
        if (isReservedAdminUsername(cleanUser) && !user.isAdmin) {
            return@withContext Pair(false, "Este nombre de usuario está reservado para la administración de ChatPro.")
        }

        // 2. Verificar existencia en chatpro_users.json
        val currentUsers = fetchRemoteUsers().toMutableList()
        val exists = currentUsers.any { it.username.trim().removePrefix("@").equals(cleanUser, ignoreCase = true) }
        if (exists) {
            return@withContext Pair(false, "El nombre de usuario @${user.username} ya está registrado. Por favor elige otro.")
        }

        // 3. Registrar y subir a evidence
        currentUsers.removeAll { it.username.equals(user.username, ignoreCase = true) }
        currentUsers.add(user)
        val success = saveRemoteUsers(currentUsers)
        if (success) {
            Pair(true, null)
        } else {
            Pair(false, "No se pudo conectar con el servidor para registrar el usuario.")
        }
    }

    suspend fun updateUserInEvidence(user: User): Boolean = withContext(Dispatchers.IO) {
        val currentUsers = fetchRemoteUsers().toMutableList()
        currentUsers.removeAll { it.username.equals(user.username, ignoreCase = true) }
        currentUsers.add(user)
        saveRemoteUsers(currentUsers)
    }

    // ==========================================
    // GESTIÓN DE PUBLICACIONES (FEED) EN EVIDENCE
    // ==========================================

    suspend fun fetchRemotePosts(): List<Post> = withContext(Dispatchers.IO) {
        try {
            val raw = downloadTextFile("chatpro_feed.json") ?: return@withContext emptyList()
            postListAdapter.fromJson(raw) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveRemotePosts(posts: List<Post>): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = postListAdapter.toJson(posts)
            uploadTextFile("chatpro_feed.json", json)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun uploadPost(
        post: Post,
        mediaFile: File? = null,
        onProgress: (Int) -> Unit = {}
    ): Post = withContext(Dispatchers.IO) {
        var finalMediaUrl = post.mediaUrl

        if (mediaFile != null && mediaFile.exists()) {
            val ext = mediaFile.extension.ifBlank { "jpg" }
            val remoteMediaName = "post_media_${System.currentTimeMillis()}_${post.id.take(8)}.$ext"
            finalMediaUrl = uploadMedia(
                file = mediaFile,
                remoteFilename = remoteMediaName,
                onProgress = onProgress
            )
        }

        val updatedPost = post.copy(mediaUrl = finalMediaUrl)

        // Actualizar lista global de posts en evidence
        val currentFeed = fetchRemotePosts().toMutableList()
        currentFeed.removeAll { it.id == updatedPost.id }
        currentFeed.add(0, updatedPost)
        saveRemotePosts(currentFeed)

        updatedPost
    }

    // ==========================================
    // GESTIÓN DE CHAT GRUPAL (COMMUNITY) EN EVIDENCE
    // ==========================================

    suspend fun fetchRemoteCommunityMessages(): List<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            val raw = downloadTextFile("chatpro_community.json") ?: return@withContext emptyList()
            messageListAdapter.fromJson(raw) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveRemoteCommunityMessages(messages: List<ChatMessage>): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = messageListAdapter.toJson(messages)
            uploadTextFile("chatpro_community.json", json)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun uploadCommunityMessage(
        message: ChatMessage,
        mediaFile: File? = null
    ): ChatMessage = withContext(Dispatchers.IO) {
        var finalMediaUrl = message.mediaUrl

        if (mediaFile != null && mediaFile.exists()) {
            val ext = mediaFile.extension.ifBlank { "bin" }
            val remoteMediaName = "msg_media_${System.currentTimeMillis()}_${message.id.take(8)}.$ext"
            finalMediaUrl = uploadMedia(
                file = mediaFile,
                remoteFilename = remoteMediaName
            )
        }

        val updatedMessage = message.copy(mediaUrl = finalMediaUrl)

        val currentMsgs = fetchRemoteCommunityMessages().toMutableList()
        currentMsgs.removeAll { it.id == updatedMessage.id }
        currentMsgs.add(updatedMessage)
        saveRemoteCommunityMessages(currentMsgs)

        updatedMessage
    }

    suspend fun uploadUserAvatar(
        username: String,
        avatarFile: File,
        onProgress: (Int) -> Unit = {}
    ): String = withContext(Dispatchers.IO) {
        val remoteName = "avatar_${username}_${System.currentTimeMillis()}.jpg"
        uploadMedia(avatarFile, remoteName, onProgress)
    }
}
