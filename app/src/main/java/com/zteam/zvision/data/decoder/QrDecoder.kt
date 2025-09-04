package com.zteam.zvision.data.decoder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Offset
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.zteam.zvision.domain.model.QrDetection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.Boolean
import java.nio.ByteBuffer
import kotlin.ByteArray
import kotlin.Exception
import kotlin.Int
import kotlin.IntArray
import kotlin.String
import kotlin.Triple
import kotlin.Unit
import kotlin.apply
import kotlin.let
import kotlin.to

object QrDecoder {

    // Hints and default reader shared across decodes
    private val hints = mapOf(
        DecodeHintType.TRY_HARDER to Boolean.TRUE,
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        DecodeHintType.CHARACTER_SET to "UTF-8"
    )
    private val defaultReader = MultiFormatReader().apply { setHints(hints) }

    // ML Kit scanner (QR only)
    private val mlOptions = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()
    private val barcodeScanner = BarcodeScanning.getClient(mlOptions)

    @OptIn(ExperimentalGetImage::class)
    fun analyze(imageProxy: ImageProxy, onResult: (QrDetection?) -> Unit) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val rotationDeg = imageProxy.imageInfo.rotationDegrees % 360
            val orientedW = if (rotationDeg == 90 || rotationDeg == 270) imageProxy.height else imageProxy.width
            val orientedH = if (rotationDeg == 90 || rotationDeg == 270) imageProxy.width else imageProxy.height

            val image = InputImage.fromMediaImage(mediaImage, rotationDeg)
            barcodeScanner
                .process(image)
                .addOnCompleteListener { task ->
                    val mlDet = if (task.isSuccessful) {
                        val first = task.result?.firstOrNull()
                        if (first?.rawValue?.isNotEmpty() == true) {
                            val pts = first.cornerPoints?.map { Offset(it.x.toFloat(), it.y.toFloat()) }
                                ?: first.boundingBox?.let { box ->
                                    listOf(
                                        Offset(box.left.toFloat(), box.top.toFloat()),
                                        Offset(box.right.toFloat(), box.top.toFloat()),
                                        Offset(box.right.toFloat(), box.bottom.toFloat()),
                                        Offset(box.left.toFloat(), box.bottom.toFloat())
                                    )
                                } ?: emptyList()
                            QrDetection(first.rawValue!!, pts, orientedW, orientedH)
                        } else null
                    } else null

                    val result = mlDet ?: decodeFromImageProxy(imageProxy, defaultReader)
                    onResult(result)
                    imageProxy.close()
                }
        } else {
            val result = decodeFromImageProxy(imageProxy, defaultReader)
            onResult(result)
            imageProxy.close()
        }
    }

    // Existing API kept as-is (ZXing from Y plane)
    fun decodeFromImageProxy(
        imageProxy: ImageProxy,
        reader: MultiFormatReader
    ): QrDetection? {
        val width = imageProxy.width
        val height = imageProxy.height
        val rotation = imageProxy.imageInfo.rotationDegrees % 360
        val yPlane = imageProxy.planes[0]
        val rowStride = yPlane.rowStride

        val yBytes = extractYBytes(yPlane.buffer, width, height, rowStride)
        val (rotatedData, iw, ih) = rotateYUV(yBytes, width, height, rotation)

        val cropScale = 0.75f // analyze central 75% of the image
        val cropW = (iw * cropScale).toInt().coerceAtLeast(1)
        val cropH = (ih * cropScale).toInt().coerceAtLeast(1)
        val left = ((iw - cropW) / 2).coerceAtLeast(0)
        val top = ((ih - cropH) / 2).coerceAtLeast(0)
        val source = PlanarYUVLuminanceSource(
            rotatedData, iw, ih, left, top, cropW, cropH, false
        )

        val binary = BinaryBitmap(HybridBinarizer(source))

        return try {
            val result = try {
                reader.decodeWithState(binary)
            } catch (_: NotFoundException) {
                val altBinary = BinaryBitmap(GlobalHistogramBinarizer(source))
                reader.decodeWithState(altBinary)
            }
            // Map points from cropped source back to full rotated image cords
            val pts = result.resultPoints?.map { Offset(it.x + left, it.y + top) } ?: emptyList()
            QrDetection(
                text = result.text,
                points = pts,
                imageWidth = iw,
                imageHeight = ih
            )
        } catch (_: NotFoundException) {
            null
        } finally {
            reader.reset()
        }
    }

    fun decodeBitmap(bitmap: Bitmap): QrDetection? {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return null
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val source = RGBLuminanceSource(w, h, pixels)
        val binary = BinaryBitmap(HybridBinarizer(source))
        return try {
            val result = defaultReader.decode(binary)
            QrDetection(result.text, emptyList(), w, h)
        } catch (_: Exception) {
            null
        } finally {
            defaultReader.reset()
        }
    }

    fun decodeImageBytes(bytes: ByteArray): QrDetection? {
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        return decodeBitmap(bmp)
    }

    suspend fun decodeFromUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null
            decodeImageBytes(bytes)?.text
        } catch (_: Exception) {
            null
        }
    }

    fun close() {
        try { barcodeScanner.close() } catch (_: Exception) {}
    }

    /** Extracts contiguous Y plane bytes, stripping stride padding. */
    private fun extractYBytes(buffer: ByteBuffer, width: Int, height: Int, rowStride: Int): ByteArray {
        val yBytes = ByteArray(width * height)
        val dup = buffer.duplicate().apply { position(0) }
        var dst = 0
        for (row in 0 until height) {
            dup.position(row * rowStride)
            dup.get(yBytes, dst, width)
            dst += width
        }
        return yBytes
    }

    /** Rotates YUV data according to rotation degrees. */
    private fun rotateYUV(data: ByteArray, width: Int, height: Int, rotation: Int): Triple<ByteArray, Int, Int> {
        if (rotation == 0) return Triple(data, width, height)

        val out: ByteArray
        val iw: Int
        val ih: Int

        when (rotation) {
            90 -> {
                out = ByteArray(data.size)
                for (y in 0 until height) {
                    val srcRow = y * width
                    for (x in 0 until width) {
                        val nx = height - 1 - y
                        val ny = x
                        out[ny * height + nx] = data[srcRow + x]
                    }
                }
                iw = height
                ih = width
            }
            180 -> {
                out = ByteArray(data.size)
                for (y in 0 until height) {
                    val srcRow = y * width
                    val dstRow = (height - 1 - y) * width
                    for (x in 0 until width) {
                        out[dstRow + (width - 1 - x)] = data[srcRow + x]
                    }
                }
                iw = width
                ih = height
            }
            270 -> {
                out = ByteArray(data.size)
                for (y in 0 until height) {
                    val srcRow = y * width
                    for (x in 0 until width) {
                        val nx = y
                        val ny = width - 1 - x
                        out[ny * height + nx] = data[srcRow + x]
                    }
                }
                iw = height
                ih = width
            }
            else -> return Triple(data, width, height) // fallback
        }

        return Triple(out, iw, ih)
    }
}
