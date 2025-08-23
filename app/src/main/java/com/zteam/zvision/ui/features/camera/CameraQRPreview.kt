package com.zteam.zvision.ui.features.camera

import android.util.Log
import android.os.SystemClock
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.google.zxing.*
import com.zteam.zvision.data.model.QrDetection
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import android.util.Rational

@Composable
fun CameraQRPreview(
    modifier: Modifier = Modifier,
    onQrDetected: (QrDetection?) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val lastAnalyzedAt = remember { AtomicLong(0L) }
    val scanRate = 200L

    // Hold last detection to avoid blinking when decode intermittently fails
    val holdMs = 200L
    val lastDetAt = remember { AtomicLong(0L) }
    var lastDet: QrDetection? by remember { mutableStateOf(null) }

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        var cameraProvider: ProcessCameraProvider? = null

        val listener = Runnable {
            try {
                cameraProvider = cameraProviderFuture.get()
                val rotation = previewView.display.rotation

                val preview = Preview.Builder()
                    .setTargetRotation(rotation)
                    .build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                val analysis = ImageAnalysis.Builder()
                    .setTargetRotation(rotation)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                val hints = mapOf(
                    DecodeHintType.TRY_HARDER to java.lang.Boolean.TRUE,
                    DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                    DecodeHintType.CHARACTER_SET to "UTF-8"
                )
                val reader = MultiFormatReader().apply { setHints(hints) }

                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    val now = SystemClock.elapsedRealtime()
                    if (now - lastAnalyzedAt.get() < scanRate) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    lastAnalyzedAt.set(now)

                    val det = QrCameraDecoder.decodeFromImageProxy(imageProxy, reader)
                    val outDet = if (det != null && det.points.isNotEmpty()) {
                        lastDet = det
                        lastDetAt.set(now)
                        det
                    } else {
                        val age = now - lastDetAt.get()
                        if (age <= holdMs) lastDet else null
                    }

                    mainExecutor.execute {
                        onQrDetected(outDet)
                    }
                    imageProxy.close()
                }

                // UseCaseGroup with ViewPort
                val vpWidth = previewView.width.coerceAtLeast(1)
                val vpHeight = previewView.height.coerceAtLeast(1)
                val viewPort = ViewPort.Builder(
                    Rational(vpWidth, vpHeight),
                    rotation
                ).build()

                val useCaseGroup = UseCaseGroup.Builder()
                    .setViewPort(viewPort)
                    .addUseCase(preview)
                    .addUseCase(analysis)
                    .build()

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(lifecycleOwner, cameraSelector, useCaseGroup)
            } catch (e: Exception) {
                Log.e("CameraX", "Binding failed", e)
            }
        }
        cameraProviderFuture.addListener(listener, executor)

        onDispose {
            try {
                onQrDetected(null)
                cameraProvider?.unbindAll()
                cameraExecutor.shutdown()
            } catch (_: Exception) { }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}
