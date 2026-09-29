package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object FileDownloadHelper {

    /**
     * Descarga una imagen desde una URL o ruta local y la guarda en la Galería del teléfono (Pictures/ChatPro)
     */
    suspend fun saveImageToGallery(context: Context, imageUrl: String, customName: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val fileName = customName ?: "ChatPro_${System.currentTimeMillis()}.jpg"

            val bitmap: Bitmap? = if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                val url = URL(imageUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 20000
                connection.connect()
                val input: InputStream = connection.inputStream
                BitmapFactory.decodeStream(input)
            } else {
                val file = File(imageUrl)
                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
            }

            if (bitmap == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error al descargar la foto", Toast.LENGTH_SHORT).show()
                }
                return@withContext false
            }

            val savedUri: Uri?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/ChatPro")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
                savedUri = uri
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val chatProDir = File(picturesDir, "ChatPro")
                if (!chatProDir.exists()) chatProDir.mkdirs()
                val destFile = File(chatProDir, fileName)
                FileOutputStream(destFile).use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                }
                savedUri = Uri.fromFile(destFile)
            }

            withContext(Dispatchers.Main) {
                if (savedUri != null) {
                    Toast.makeText(context, "Foto guardada en tu Galería (Carpeta ChatPro)", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "No se pudo guardar la foto", Toast.LENGTH_SHORT).show()
                }
            }
            savedUri != null
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error guardando foto: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
            false
        }
    }
}
