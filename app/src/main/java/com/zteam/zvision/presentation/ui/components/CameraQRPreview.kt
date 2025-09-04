package com.zteam.zvision.presentation.ui.components

import android.util.Log
import android.util.Rational
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.zteam.zvision.domain.model.QrDetection
import com.zteam.zvision.presentation.viewmodel.QRViewModel
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
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
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val qrViewModel: QRViewModel = hiltViewModel()

    // Observe detection updates from the ViewModel and forward to the composable callback.
    val detection by qrViewModel.detection.collectAsState()
    LaunchedEffect(detection) {
        onQrDetected(detection)
    }

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
                    .build().also { it.surfaceProvider = previewView.surfaceProvider }

                val analysis = ImageAnalysis.Builder()
                    .setTargetRotation(rotation)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                // Delegate frame analysis entirely to the ViewModel
                analysis.setAnalyzer(cameraExecutor) { proxy ->
                    qrViewModel.analyze(proxy)
                }

                // UseCaseGroup with ViewPort
                val vpWidth = previewView.width.fastCoerceAtLeast(1)
                val vpHeight = previewView.height.fastCoerceAtLeast(1)
                val viewPort = ViewPort.Builder(
                    Rational(vpWidth, vpHeight),
                    rotation
                ).build()

                val useCaseGroup = UseCaseGroup.Builder()
                    .setViewPort(viewPort)
                    .addUseCase(preview)
                    .addUseCase(analysis)
                    .build()

                val selector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(lifecycleOwner, selector, useCaseGroup)
            } catch (e: Exception) {
                Log.e("CameraX", "Binding failed", e)
            }
        }
        cameraProviderFuture.addListener(listener, executor)

        onDispose {
            try {
                onQrDetected(null)
                qrViewModel.clear()
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
