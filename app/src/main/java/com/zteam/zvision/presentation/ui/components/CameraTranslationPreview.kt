package com.zteam.zvision.presentation.ui.components

import android.os.SystemClock
import android.util.Log
import androidx.camera.core.CameraSelector
import android.util.Rational
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraTranslationPreview(
    modifier: Modifier = Modifier,
    analyzer: (ImageProxy) -> Unit,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    onImageCaptureUseCase: ((androidx.camera.core.ImageCapture) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val cameraExecutor: ExecutorService = remember {
        Executors.newSingleThreadExecutor()
    }

    var lastAnalysisTime by remember { mutableStateOf(0L) }
    val THROTTLE_INTERVAL_MS = 2000L

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        var cameraProvider: ProcessCameraProvider? = null

        val listener = Runnable {
            try {
                cameraProvider = cameraProviderFuture.get()
                val rotation = previewView.display.rotation

                val preview = Preview.Builder()
                    .setTargetRotation(rotation)
                    .build()
                    .also { it.surfaceProvider = previewView.surfaceProvider }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetRotation(rotation)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                // Set the user-provided analyzer function
//                imageAnalysis.setAnalyzer(cameraExecutor) { proxy ->
//                    val now = SystemClock.uptimeMillis()
//                    // Throttle the analysis
//                    if (now - lastAnalysisTime < THROTTLE_INTERVAL_MS) {
//                        proxy.close()
//                        return@setAnalyzer
//                    }
//                    lastAnalysisTime = now
//                    Log.i("CameraTranslationPreview", "Analyzing image")
//                    analyzer(proxy)
//                }

                // Create ImageCapture use case if callback provided
                val imageCapture = androidx.camera.core.ImageCapture.Builder()
                    .setTargetRotation(rotation)
                    .setCaptureMode(androidx.camera.core.ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                // UseCaseGroup with ViewPort for correct analysis dimensions
                val viewPort = previewView.let {
                    val vpWidth = it.width.coerceAtLeast(1)
                    val vpHeight = it.height.coerceAtLeast(1)
                    ViewPort.Builder(
                        Rational(vpWidth, vpHeight),
                        rotation
                    ).build()
                }

                val useCaseGroupBuilder = UseCaseGroup.Builder()
                    .setViewPort(viewPort)
                    .addUseCase(preview)
                    .addUseCase(imageAnalysis)

                // Add ImageCapture if callback is provided
                if (onImageCaptureUseCase != null) {
                    useCaseGroupBuilder.addUseCase(imageCapture)
                }

                val useCaseGroup = useCaseGroupBuilder.build()

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(lifecycleOwner, cameraSelector, useCaseGroup)

                // Pass ImageCapture instance back to caller
                if (onImageCaptureUseCase != null) {
                    onImageCaptureUseCase(imageCapture)
                }
            } catch (e: Exception) {
                Log.e("CameraTranslationPreview", "Binding failed", e)
            }
        }
        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        Log.i("CameraTranslationPreview", "Adding listener")
        onDispose {
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.matchParentSize()
        )
    }
}
