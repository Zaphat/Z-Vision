package com.zteam.zvision.ui.features.camera

import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Offset
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.zteam.zvision.data.model.QrDetection
import java.nio.ByteBuffer

object QrCameraDecoder {

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

        // Focus on a centered crop to reduce noise and improve decode chance.
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
                val altBinary = BinaryBitmap(com.google.zxing.common.GlobalHistogramBinarizer(source))
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
