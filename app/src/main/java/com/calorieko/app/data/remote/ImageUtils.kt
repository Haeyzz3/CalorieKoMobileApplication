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
     * Maximum raw byte size before Base64 encoding.
     *
     * Firestore has a 1 MiB (1,048,576 bytes) per-document limit.
     * Since a document may contain multiple Base64 fields (e.g.,
     * `photoUri` + `encodedPath` on ActivityLogEntity), we cap each
     * individual encoded image at ~500 KB of Base64 output.
     *
     * Base64 expands data by ~4/3, so we target raw JPEG bytes
     * under this threshold to keep the Base64 string safe.
     */
    private const val MAX_RAW_BYTES = 375 * 1024  // ~500 KB after Base64 encoding

    /** Minimum quality floor to prevent unacceptable image degradation. */
    private const val MIN_QUALITY = 10

    /**
     * Reads an image from the given Uri, scales it down, compresses it to JPEG,
     * and returns a Base64 string prefixed with "base64:".
     *
     * If the initial compression exceeds the safe Firestore document size
     * threshold, quality is progressively reduced until it fits or the
     * minimum quality floor is reached.
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

            // ── Size-aware compression ──
            // Compress to JPEG, reducing quality if the result is too large
            var currentQuality = quality
            var byteArray: ByteArray

            do {
                val outputStream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, currentQuality, outputStream)
                byteArray = outputStream.toByteArray()

                if (byteArray.size > MAX_RAW_BYTES && currentQuality > MIN_QUALITY) {
                    currentQuality -= 10
                    Log.w(TAG, "Image too large (${byteArray.size / 1024} KB) — " +
                            "reducing quality to $currentQuality")
                }
            } while (byteArray.size > MAX_RAW_BYTES && currentQuality > MIN_QUALITY)

            // Encode to Base64
            val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)

            if (base64String.length > 500 * 1024) {
                Log.w(TAG, "⚠ Base64 string is ${base64String.length / 1024} KB — " +
                        "approaching Firestore document size limit")
            }

            Log.d(TAG, "Image compressed: ${byteArray.size / 1024} KB, " +
                    "quality=$currentQuality, base64=${base64String.length / 1024} KB")

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
