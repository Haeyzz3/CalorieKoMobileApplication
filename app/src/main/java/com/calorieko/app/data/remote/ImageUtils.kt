package com.calorieko.app.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream

/**
 * Utility class for compressing local images and encoding them to Base64
 * for direct storage in Firestore, replacing the need for paid cloud storage.
 */
object ImageUtils {
    private const val TAG = "ImageUtils"
    const val BASE64_PREFIX = "base64:"

    /**
     * Reads an image from the given Uri, scales it down, compresses it to JPEG,
     * and returns a Base64 string prefixed with "base64:".
     */
    fun compressAndEncode(context: Context, uri: Uri, maxDimension: Int = 800, quality: Int = 70): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) return null

            // Calculate scaling
            val width = originalBitmap.width
            val height = originalBitmap.height
            val scale = if (width > height) {
                maxDimension.toFloat() / width
            } else {
                maxDimension.toFloat() / height
            }

            val scaledBitmap = if (scale < 1.0f) {
                Bitmap.createScaledBitmap(
                    originalBitmap,
                    (width * scale).toInt(),
                    (height * scale).toInt(),
                    true
                )
            } else {
                originalBitmap // Already small enough
            }

            // Compress to JPEG format
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val byteArray = outputStream.toByteArray()

            // Encode to Base64
            val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)
            
            Log.d(TAG, "Image successfully compressed and encoded. Size: ${byteArray.size / 1024} KB")
            
            // Clean up
            if (scaledBitmap != originalBitmap) {
                scaledBitmap.recycle()
            }
            originalBitmap.recycle()

            return "$BASE64_PREFIX$base64String"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compress and encode image", e)
            null
        }
    }

    /**
     * Decodes a "base64:..." prefixed string back into a Jetpack Compose ImageBitmap.
     */
    fun decodeBase64ToBitmap(base64Str: String): ImageBitmap? {
        return try {
            if (!base64Str.startsWith(BASE64_PREFIX)) return null
            
            val cleanBase64 = base64Str.removePrefix(BASE64_PREFIX)
            val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            
            bitmap?.asImageBitmap()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode Base64 image", e)
            null
        }
    }
}
