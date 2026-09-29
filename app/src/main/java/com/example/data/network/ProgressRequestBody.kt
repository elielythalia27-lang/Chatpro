package com.example.data.network

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File
import java.io.FileInputStream

/**
 * Custom OkHttp RequestBody with high-resolution progress tracking (0-100%).
 */
class ProgressRequestBody(
    private val file: File,
    private val contentType: MediaType?,
    private val onProgress: (percent: Int) -> Unit
) : RequestBody() {

    override fun contentType(): MediaType? = contentType

    override fun contentLength(): Long = file.length()

    override fun writeTo(sink: BufferedSink) {
        val totalLength = file.length()
        val buffer = ByteArray(2048)
        val inputStream = FileInputStream(file)
        var uploaded: Long = 0

        inputStream.use { stream ->
            var read: Int
            while (stream.read(buffer).also { read = it } != -1) {
                sink.write(buffer, 0, read)
                uploaded += read
                val progress = if (totalLength > 0) ((uploaded * 100) / totalLength).toInt() else 0
                onProgress(progress)
            }
        }
    }
}
