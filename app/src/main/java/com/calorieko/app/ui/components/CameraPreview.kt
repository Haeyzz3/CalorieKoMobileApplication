package com.calorieko.app.ui.components

import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.calorieko.app.ml.CalorieKoClassifier
import java.util.concurrent.Executors

/**
 * Full-screen live camera preview with per-frame AI dish classification.
 *
 * Uses CameraX [Preview] + [ImageAnalysis] use cases bound to the current
 * lifecycle owner. Each frame is converted to a bitmap and classified via
 * the supplied [classifier], emitting results through [onFrameAnalyzed].
 *
 * @param modifier         Layout modifier.
 * @param classifier       The [CalorieKoClassifier] instance to run inference with.
 * @param onFrameAnalyzed  Callback receiving the top-3 classification results per frame.
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    classifier: CalorieKoClassifier,
    onFrameAnalyzed: (List<Pair<String, Float>>) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val executor = remember { Executors.newSingleThreadExecutor() }

    // Shut down the executor when the composable leaves composition
    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                // 1. Preview use case
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                // 2. Image analysis use case (keeps only latest frame)
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analysis.setAnalyzer(executor) { imageProxy ->
                    val bitmap = imageProxy.toBitmap()
                    val results = classifier.classify(bitmap)
                    onFrameAnalyzed(results)
                    imageProxy.close()
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}
