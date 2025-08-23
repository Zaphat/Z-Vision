package com.zteam.zvision.ui.features.camera

import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Offset
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.zteam.zvision.data.model.QrDetection

object QrCameraDecoder {
    fun decodeFromImageProxy(
        imageProxy: ImageProxy,
        reader: MultiFormatReader
    ): QrDetection? {
        val width = imageProxy.width
        val height = imageProxy.height
        val rotation = imageProxy.imageInfo.rotationDegrees
        val yPlane = imageProxy.planes[0]
        val yBuffer = yPlane.buffer.duplicate().apply { position(0) }
        val rowStride = yPlane.rowStride

        // Copy Y plane into a contiguous buffer of width*height (strip row stride)
        val yBytes = ByteArray(width * height)
        var dstOffset = 0
        for (row in 0 until height) {
            yBuffer.position(row * rowStride)
            yBuffer.get(yBytes, dstOffset, width)
            dstOffset += width
        }

        // Fix rotation math: apply the clockwise rotation needed to match the target rotation
        var data = yBytes
        var iw = width
        var ih = height
        when (rotation % 360) {
            0 -> { /* no-op */ }
            90 -> {
                // Rotate 90 degrees clockwise: (x,y) -> (H-1-y, x)
                val out = ByteArray(width * height)
                val newW = height
                val newH = width
                for (y in 0 until height) {
                    val ySrcRow = y * width
                    for (x in 0 until width) {
                        val nx = height - 1 - y
                        val ny = x
                        out[ny * newW + nx] = yBytes[ySrcRow + x]
                    }
                }
                data = out
                iw = newW
                ih = newH
            }
            180 -> {
                // Rotate 180: (x,y) -> (W-1-x, H-1-y)
                val out = ByteArray(width * height)
                for (y in 0 until height) {
                    val ySrcRow = y * width
                    val yDstRow = (height - 1 - y) * width
                    for (x in 0 until width) {
                        out[yDstRow + (width - 1 - x)] = yBytes[ySrcRow + x]
                    }
                }
                data = out
                iw = width
                ih = height
            }
            270 -> {
                // Rotate 90 degrees counter-clockwise: (x,y) -> (y, W-1-x)
                val out = ByteArray(width * height)
                val newW = height
                val newH = width
                for (y in 0 until height) {
                    val ySrcRow = y * width
                    for (x in 0 until width) {
                        val nx = y
                        val ny = width - 1 - x
                        out[ny * newW + nx] = yBytes[ySrcRow + x]
                    }
                }
                data = out
                iw = newW
                ih = newH
            }
        }

        val source = PlanarYUVLuminanceSource(
            data, iw, ih, 0, 0, iw, ih, false
        )
        val binary = BinaryBitmap(HybridBinarizer(source))
        return try {
            val result = reader.decodeWithState(binary)
            val pts = result.resultPoints?.map { Offset(it.x, it.y) } ?: emptyList()
            QrDetection(
                text = result.text,
                points = pts,
                imageWidth = source.width,
                imageHeight = source.height
            )
        } catch (_: NotFoundException) {
            null
        } finally {
            reader.reset()
        }
    }
}
