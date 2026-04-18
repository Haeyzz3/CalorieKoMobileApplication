package com.calorieko.app.ui.components

import android.view.ViewGroup
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.calorieko.app.ml.CalorieKoClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Full-screen live camera preview with per-frame AI dish classification.
 *
 * Uses CameraX [Preview] + [ImageAnalysis] use cases bound to the current
 * lifecycle owner. Each frame is converted to a bitmap and classified via
 * the supplied [classifier], emitting results through [onFrameAnalyzed].
 *
 * @param modifier         Layout modifier.
 * @param classifier       The [CalorieKoClassifier] instance to run inference with.
 * @param flashEnabled     Whether the camera torch (flashlight) should be enabled.
 * @param onFrameAnalyzed  Callback receiving the top-3 classification results per frame.
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    classifier: CalorieKoClassifier,
    flashEnabled: Boolean = false,
    onFrameAnalyzed: (List<Pair<String, Float>>) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Use a shared dispatcher as an executor. It never shuts down during the app's life, 
    // ensuring no RejectedExecutionException occurs if CameraX posts a frame during cleanup.
    val executor = remember { Dispatchers.Default.limitedParallelism(1).asExecutor() }

    // Thread-safe flag to prevent the analyzer from calling classify() after disposal.
    // This guards against the race where CameraX delivers a frame after the parent
    // composable has already called classifier.close().
    val isActive = remember { AtomicBoolean(true) }

    // Hold a reference to the Camera so we can control torch
    var camera by remember { mutableStateOf<Camera?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var analysisUseCase by remember { mutableStateOf<ImageAnalysis?>(null) }

    // Toggle torch whenever flashEnabled changes
    LaunchedEffect(flashEnabled, camera) {
        camera?.cameraControl?.enableTorch(flashEnabled)
    }

    // Cleanup when the composable leaves composition.
    // Order matters: deactivate flag → clear analyzer → unbind camera.
    // The flag stops any in-flight frame from reaching the classifier,
    // and clearAnalyzer prevents new frames from being enqueued.
    DisposableEffect(Unit) {
        onDispose {
            // 1. Immediately stop any in-flight frame from calling classify()
            isActive.set(false)

            // 2. Remove the analyzer so no new frames are dispatched
            analysisUseCase?.clearAnalyzer()
            
            // 3. Unbind everything from the lifecycle
            try {
                cameraProvider?.unbindAll()
            } catch (e: Exception) {
                // Ignore if provider not ready
            }
        }
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
                // If the composable was disposed before the future resolved, bail out
                if (!isActive.get()) return@addListener

                val provider = cameraProviderFuture.get()
                cameraProvider = provider

                // 1. Preview use case
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                // 2. Image analysis use case (keeps only latest frame)
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysisUseCase = analysis

                analysis.setAnalyzer(executor) { imageProxy ->
                    try {
                        // Guard: skip classification if we're shutting down
                        if (!isActive.get()) return@setAnalyzer

                        val bitmap = imageProxy.toBitmap()
                        val results = classifier.classify(bitmap)

                        // Only deliver results if still active
                        if (isActive.get()) {
                            onFrameAnalyzed(results)
                        }
                    } catch (e: IllegalStateException) {
                        // Interpreter was closed — safe to ignore
                    } catch (e: Exception) {
                        // Catch-all for any other teardown race
                    } finally {
                        imageProxy.close()
                    }
                }

                try {
                    provider.unbindAll()
                    camera = provider.bindToLifecycle(
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
