package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query
import retrofit2.http.Url

@JsonClass(generateAdapter = true)
data class RemoteTokenResponse(
    @Json(name = "token") val token: String? = null,
    @Json(name = "privatetoken") val privateToken: String? = null,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class RemoteUploadItem(
    @Json(name = "component") val component: String? = null,
    @Json(name = "contextid") val contextId: Long? = null,
    @Json(name = "itemid") val itemId: Long? = null,
    @Json(name = "filename") val filename: String? = null,
    @Json(name = "filepath") val filepath: String? = null,
    @Json(name = "filesize") val filesize: Long? = null,
    @Json(name = "url") val url: String? = null
)

@JsonClass(generateAdapter = true)
data class RemoteFileListResponse(
    @Json(name = "files") val files: List<RemoteFileItem> = emptyList(),
    @Json(name = "parents") val parents: List<RemoteFileItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class RemoteFileItem(
    @Json(name = "filename") val filename: String? = null,
    @Json(name = "filepath") val filepath: String? = null,
    @Json(name = "filesize") val filesize: Long? = null,
    @Json(name = "fileurl") val fileurl: String? = null,
    @Json(name = "timecreated") val timecreated: Long? = null,
    @Json(name = "timemodified") val timemodified: Long? = null
)

interface ApiService {

    @FormUrlEncoded
    @POST("login/token.php")
    suspend fun obtenerToken(
        @Field("username") user: String,
        @Field("password") pass: String,
        @Field("service") service: String = "moodle_mobile_app"
    ): Response<RemoteTokenResponse>

    @FormUrlEncoded
    @POST("login/token.php")
    suspend fun requestToken(
        @Field("username") user: String,
        @Field("password") pass: String,
        @Field("service") service: String = "moodle_mobile_app"
    ): Response<RemoteTokenResponse>

    @Multipart
    @POST("webservice/upload.php")
    suspend fun subirArchivo(
        @Query("token") token: String,
        @Part file: MultipartBody.Part,
        @Part("filepath") filepath: RequestBody,
        @Part("filearea") filearea: RequestBody
    ): Response<List<RemoteUploadItem>>

    @Multipart
    @POST("webservice/upload.php")
    suspend fun uploadFile(
        @Query("token") token: String,
        @Part file: MultipartBody.Part,
        @Part("filepath") filepath: RequestBody,
        @Part("filearea") filearea: RequestBody
    ): Response<List<RemoteUploadItem>>

    @GET
    suspend fun descargarArchivo(
        @Url fileUrl: String,
        @Header("Authorization") authHeader: String? = null
    ): Response<ResponseBody>

    @GET
    suspend fun downloadPayload(
        @Url fileUrl: String,
        @Header("Authorization") authHeader: String? = null
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST("webservice/rest/server.php")
    suspend fun listarArchivos(
        @Query("wstoken") token: String,
        @Query("moodlewsrestformat") format: String = "json",
        @Field("wsfunction") wsFunction: String = "core_files_get_files",
        @Field("contextid") contextId: Int = 0,
        @Field("component") component: String = "user",
        @Field("filearea") filearea: String = "draft",
        @Field("itemid") itemId: Int = 0,
        @Field("filepath") filepath: String = "/",
        @Field("filename") filename: String = ""
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST("webservice/rest/server.php")
    suspend fun eliminarArchivo(
        @Query("wstoken") token: String,
        @Query("moodlewsrestformat") format: String = "json",
        @Field("wsfunction") wsFunction: String = "core_files_delete_draft_files",
        @Field("draftitemid") draftItemId: Int = 0,
        @Field("filepath") filepath: String = "/",
        @Field("filename") filename: String = ""
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST("webservice/rest/server.php")
    suspend fun executeRestAction(
        @Query("wstoken") token: String,
        @Query("moodlewsrestformat") format: String = "json",
        @Field("wsfunction") wsFunction: String,
        @Field("params") params: String = ""
    ): Response<ResponseBody>
}
