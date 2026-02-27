package com.calorieko.app.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import androidx.core.graphics.scale

/**
 * Wraps the CalorieKo TFLite model for on-device Filipino dish classification.
 *
 * Loads `calorieko_model.tflite` and `labels.txt` from the app's assets folder
 * and exposes a [classify] method that returns the top-3 predictions with
 * confidence scores.
 *
 * Implements [AutoCloseable] so the native interpreter resources are released
 * when the classifier is no longer needed.
 */
class CalorieKoClassifier(context: Context) : AutoCloseable {

    private val modelBuffer = loadModelFile(context)
    private val interpreter = Interpreter(modelBuffer)
    private val labels = loadLabels(context)

    private fun loadModelFile(context: Context): ByteBuffer {
        val assetFileDescriptor = context.assets.openFd("calorieko_model.tflite")
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun loadLabels(context: Context): List<String> {
        return context.assets.open("labels.txt").bufferedReader().readLines()
    }

    /**
     * Classify a camera frame bitmap.
     *
     * @return Top-3 predictions as `List<Pair<label, confidence>>`, sorted
     *         descending by confidence. Labels are the raw snake_case model
     *         labels (e.g. `"sinigang_pork"`, `"negative"`).
     */
    fun classify(bitmap: Bitmap): List<Pair<String, Float>> {
        val input = ByteBuffer.allocateDirect(1 * 224 * 224 * 3 * 4).apply {
            order(ByteOrder.nativeOrder())
        }

        // Centre-crop to square
        val size = minOf(bitmap.width, bitmap.height)
        val xOffset = (bitmap.width - size) / 2
        val yOffset = (bitmap.height - size) / 2
        val croppedBitmap = Bitmap.createBitmap(bitmap, xOffset, yOffset, size, size)
        val resizedBitmap = croppedBitmap.scale(224, 224)

        val intValues = IntArray(224 * 224)
        resizedBitmap.getPixels(intValues, 0, 224, 0, 0, 224, 224)

        for (pixel in intValues) {
            input.putFloat(((pixel shr 16) and 0xFF).toFloat())
            input.putFloat(((pixel shr 8) and 0xFF).toFloat())
            input.putFloat((pixel and 0xFF).toFloat())
        }

        val output = Array(1) { FloatArray(labels.size) }
        interpreter.run(input, output)

        return labels.indices.map { labels[it] to output[0][it] }
            .sortedByDescending { it.second }
            .take(3)
    }

    override fun close() {
        interpreter.close()
    }
}
