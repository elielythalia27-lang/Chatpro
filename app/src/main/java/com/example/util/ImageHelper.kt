package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max

object ImageHelper {

    /**
     * Compresses and resizes an image from a content Uri to a maximum dimension of maxDimension (e.g. 800x800).
     * Saves the resulting JPEG in the application's cache directory.
     */
    fun compressAndSaveImage(
        context: Context,
        sourceUri: Uri,
        fileName: String = "avatar_${System.currentTimeMillis()}.jpg",
        maxDimension: Int = 800
    ): File? {
        return try {
            val contentResolver = context.contentResolver

            // 1. Decode bounds to avoid OutOfMemory
            var input: InputStream? = contentResolver.openInputStream(sourceUri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(input, null, options)
            input?.close()

            val origWidth = options.outWidth
            val origHeight = options.outHeight
            if (origWidth <= 0 || origHeight <= 0) return null

            // 2. Calculate inSampleSize
            var inSampleSize = 1
            val maxSide = max(origWidth, origHeight)
            if (maxSide > maxDimension) {
                inSampleSize = maxSide / maxDimension
            }
            options.inJustDecodeBounds = false
            options.inSampleSize = max(1, inSampleSize)

            // 3. Decode scaled bitmap
            input = contentResolver.openInputStream(sourceUri)
            val decodedBitmap = BitmapFactory.decodeStream(input, null, options)
            input?.close()

            if (decodedBitmap == null) return null

            // 4. Exact scaling if still larger than maxDimension
            val currentMax = max(decodedBitmap.width, decodedBitmap.height)
            val finalBitmap = if (currentMax > maxDimension) {
                val scale = maxDimension.toFloat() / currentMax.toFloat()
                val matrix = Matrix().apply { postScale(scale, scale) }
                Bitmap.createBitmap(
                    decodedBitmap,
                    0,
                    0,
                    decodedBitmap.width,
                    decodedBitmap.height,
                    matrix,
                    true
                )
            } else {
                decodedBitmap
            }

            // 5. Save to cacheDir as JPEG with 80% quality
            val destFile = File(context.cacheDir, fileName)
            FileOutputStream(destFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }

            destFile
        } catch (e: Exception) {
            null
        }
    }
}
