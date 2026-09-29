package com.example.data.network

import android.content.Context
import com.example.data.model.ChatMessage
import com.example.data.model.Post
import com.example.data.model.PostMediaType
import com.example.data.model.User
import com.example.data.model.UserStatus
import com.example.security.CryptoManager
import com.example.security.XorCipher
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class CloudStorage(private val context: Context) {

    // XOR-obfuscated base connection parameters
    // "https://cursos.uo.edu.cu/"
    private val encodedBaseUrl = "PjY2OjVkbW05NzQ1PTVsNz1sPz43bDk3bQ=="
    // "julianrene"
    private val encodedUser = "MDcyMys0ND80Pw=="
    // "Trasnfer60*"
    private val encodedPass = "CjQrNTQ8PzRsalA="

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val postAdapter = moshi.adapter(Post::class.java)
    private val postListAdapter = moshi.adapter<List<Post>>(
        Types.newParameterizedType(List::class.java, Post::class.java)
    )
    private val userAdapter = moshi.adapter(User::class.java)
    private val messageAdapter = moshi.adapter(ChatMessage::class.java)
    private val messageListAdapter = moshi.adapter<List<ChatMessage>>(
        Types.newParameterizedType(List::class.java, ChatMessage::class.java)
    )

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val api: ApiService by lazy {
        val baseUrl = XorCipher.decode(encodedBaseUrl)
        Retrofit.Builder()
            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ApiService::class.java)
    }

    private var cachedToken: String? = null

    private val _isConnected = MutableStateFlow(true) // Default to true if network is present
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    fun isNetworkAvailable(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            val net = cm?.activeNetwork ?: return false
            val cap = cm.getNetworkCapabilities(net) ?: return false
            cap.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            true
        }
    }

    suspend fun ensureConnected(): Boolean = withContext(Dispatchers.IO) {
        val token = getOrRefreshToken()
        val online = token != null || isNetworkAvailable()
        _isConnected.value = online
        online
    }

    /**
     * Always maintains the session token active and auto-refreshes if needed.
     */
    suspend fun getOrRefreshToken(): String? = withContext(Dispatchers.IO) {
        if (!cachedToken.isNullOrBlank()) {
            _isConnected.value = true
            return@withContext cachedToken
        }
        try {
            val u = XorCipher.decode(encodedUser)
            val p = XorCipher.decode(encodedPass)
            val response = api.obtenerToken(user = u, pass = p)
            if (response.isSuccessful && response.body()?.token != null) {
                cachedToken = response.body()?.token
                _isConnected.value = true
                cachedToken
            } else {
                _isConnected.value = isNetworkAvailable()
                null
            }
        } catch (e: Exception) {
            _isConnected.value = isNetworkAvailable()
            null
        }
    }

    /**
     * Upload post (publicación) with attached media (photo/audio) and save metadata to Moodle
     */
    suspend fun uploadPost(
        post: Post,
        mediaFile: File? = null,
        onProgress: (Int) -> Unit = {}
    ): Post = withContext(Dispatchers.IO) {
        var finalMediaUrl = post.mediaUrl

        if (mediaFile != null && mediaFile.exists()) {
            val uploadedUrl = uploadMedia(
                file = mediaFile,
                folderPath = "/chatpro/archivos/",
                onProgress = onProgress
            )
            if (!uploadedUrl.isNullOrBlank()) {
                finalMediaUrl = uploadedUrl
            }
        }

        val updatedPost = post.copy(mediaUrl = finalMediaUrl)

        // Upload post JSON metadata to /chatpro/publicaciones/
        try {
            val json = postAdapter.toJson(updatedPost)
            uploadEncryptedBlock(
                filename = "${updatedPost.id}.json",
                jsonPayload = json,
                folderPath = "/chatpro/publicaciones/"
            )
        } catch (e: Exception) {
            // Keep local
        }

        updatedPost
    }

    /**
     * Upload user avatar to /chatpro/usuarios/{username}/avatar/avatar.jpg
     */
    suspend fun uploadUserAvatar(
        username: String,
        avatarFile: File,
        onProgress: (Int) -> Unit = {}
    ): String? = withContext(Dispatchers.IO) {
        if (!avatarFile.exists()) return@withContext null
        try {
            val token = getOrRefreshToken() ?: return@withContext avatarFile.absolutePath
            val path = "/chatpro/usuarios/$username/avatar/"
            val requestBody = ProgressRequestBody(avatarFile, "image/jpeg".toMediaTypeOrNull(), onProgress)
            val part = MultipartBody.Part.createFormData("file", "avatar.jpg", requestBody)
            val filepathBody = path.toRequestBody("text/plain".toMediaTypeOrNull())
            val fileareaBody = "draft".toRequestBody("text/plain".toMediaTypeOrNull())

            val response = api.subirArchivo(token, part, filepathBody, fileareaBody)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                response.body()?.firstOrNull()?.url ?: avatarFile.absolutePath
            } else {
                avatarFile.absolutePath
            }
        } catch (e: Exception) {
            avatarFile.absolutePath
        }
    }

    /**
     * Upload user profile JSON to /chatpro/usuarios/{id_usuario}.json
     */
    suspend fun uploadUserProfile(user: User): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = userAdapter.toJson(user)
            val filename = "${user.username}.json"
            val uploadedUrl = uploadEncryptedBlock(
                filename = filename,
                jsonPayload = json,
                folderPath = "/chatpro/usuarios/"
            )
            uploadedUrl != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Upload multimedia file (Photo, Audio) up to 4MB with progress
     */
    suspend fun uploadMedia(
        file: File,
        folderPath: String = "/chatpro/archivos/",
        onProgress: (Int) -> Unit = {}
    ): String? = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() > 4 * 1024 * 1024) {
            return@withContext file.absolutePath
        }
        try {
            val token = getOrRefreshToken() ?: return@withContext file.absolutePath
            val mimeType = when (file.extension.lowercase()) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "webp" -> "image/webp"
                "m4a", "aac", "mp3" -> "audio/mp4"
                "mp4" -> "video/mp4"
                else -> "application/octet-stream"
            }

            val requestBody = ProgressRequestBody(file, mimeType.toMediaTypeOrNull(), onProgress)
            val part = MultipartBody.Part.createFormData("file", file.name, requestBody)
            val filepathBody = folderPath.toRequestBody("text/plain".toMediaTypeOrNull())
            val fileareaBody = "draft".toRequestBody("text/plain".toMediaTypeOrNull())

            val response = api.subirArchivo(token, part, filepathBody, fileareaBody)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                response.body()?.firstOrNull()?.url ?: file.absolutePath
            } else {
                file.absolutePath
            }
        } catch (e: Exception) {
            file.absolutePath
        }
    }

    /**
     * Upload encrypted block with specified remote folder path
     */
    suspend fun uploadEncryptedBlock(
        filename: String,
        jsonPayload: String,
        folderPath: String = "/",
        onProgress: (Int) -> Unit = {}
    ): String? = withContext(Dispatchers.IO) {
        try {
            val token = getOrRefreshToken() ?: return@withContext null
            // 1. Encrypt with AES-256
            val encrypted = CryptoManager.encrypt(jsonPayload)
            // 2. Compress with GZIP
            val compressedBytes = gzipCompress(encrypted.toByteArray(Charsets.UTF_8))
            
            // Check 4MB internal limit
            if (compressedBytes.size > 4 * 1024 * 1024) {
                return@withContext null
            }

            val tempFile = File(context.cacheDir, filename).apply {
                writeBytes(compressedBytes)
            }

            val requestBody = ProgressRequestBody(tempFile, "application/octet-stream".toMediaTypeOrNull()) {
                onProgress(it)
            }
            val part = MultipartBody.Part.createFormData("file", filename, requestBody)
            val filepathBody = folderPath.toRequestBody("text/plain".toMediaTypeOrNull())
            val fileareaBody = "draft".toRequestBody("text/plain".toMediaTypeOrNull())

            val response = api.subirArchivo(token, part, filepathBody, fileareaBody)
            tempFile.delete()

            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                response.body()?.firstOrNull()?.url
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun downloadEncryptedBlock(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val token = getOrRefreshToken() ?: return@withContext null
            val authHeader = "Bearer $token"
            val response = api.descargarArchivo(url, authHeader)
            if (response.isSuccessful && response.body() != null) {
                val compressedBytes = response.body()!!.bytes()
                val decrypted = try {
                    val decompressed = gzipDecompress(compressedBytes)
                    val rawEncrypted = String(decompressed, Charsets.UTF_8)
                    CryptoManager.decrypt(rawEncrypted)
                } catch (e: Exception) {
                    CryptoManager.decrypt(String(compressedBytes, Charsets.UTF_8))
                }
                decrypted
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchRemotePosts(): List<Post> = withContext(Dispatchers.IO) {
        try {
            val token = getOrRefreshToken() ?: return@withContext emptyList()
            val url = "${XorCipher.decode(encodedBaseUrl)}webservice/pluginfile.php?file=/chatpro/publicaciones/feed_index.json"
            val decrypted = downloadEncryptedBlock(url)
            if (!decrypted.isNullOrBlank()) {
                postListAdapter.fromJson(decrypted) ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun fetchRemoteCommunityMessages(): List<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            val token = getOrRefreshToken() ?: return@withContext emptyList()
            val url = "${XorCipher.decode(encodedBaseUrl)}webservice/pluginfile.php?file=/chatpro/comunidad/community_index.json"
            val decrypted = downloadEncryptedBlock(url)
            if (!decrypted.isNullOrBlank()) {
                messageListAdapter.fromJson(decrypted) ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun checkUsernameAvailability(username: String): Boolean = withContext(Dispatchers.IO) {
        val clean = username.trim().removePrefix("@").lowercase()
        clean.length >= 3
    }

    private fun gzipCompress(data: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(data) }
        return bos.toByteArray()
    }

    private fun gzipDecompress(data: ByteArray): ByteArray {
        val bis = java.io.ByteArrayInputStream(data)
        val gis = GZIPInputStream(bis)
        val bos = ByteArrayOutputStream()
        val buffer = ByteArray(2048)
        var read: Int
        while (gis.read(buffer).also { read = it } != -1) {
            bos.write(buffer, 0, read)
        }
        return bos.toByteArray()
    }
}
