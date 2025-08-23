package com.zteam.zvision.ui.features.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.RGBLuminanceSource

object QRImageDecoder {
    /**
     * Decode a QR code text from an image [uri]. Returns the decoded text or null if none found.
     */
    fun decodeFromUri(context: Context, uri: Uri): String? {
        return context.contentResolver.openInputStream(uri)?.use { input ->
            val options = BitmapFactory.Options().apply { inSampleSize = calculateInSampleSizeForStream(context, uri) }
            val bmp = BitmapFactory.decodeStream(input, null, options)
            bmp?.let { bitmap ->
                try {
                    decodeFromBitmap(bitmap)
                } finally {
                    bitmap.recycle()
                }
            }
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun calculateInSampleSizeForStream(context: Context, uri: Uri): Int {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        return calculateInSampleSize(opts, 1200, 1200)
    }

    /**
     * Decode text from a [bitmap] containing a QR code. Returns null if not found.
     */
    fun decodeFromBitmap(bitmap: Bitmap): String? {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val source = RGBLuminanceSource(width, height, pixels)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

        val hints = mapOf(
            DecodeHintType.TRY_HARDER to java.lang.Boolean.TRUE,
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)
        )

        return try {
            val reader = QRCodeReader()
            val result = reader.decode(binaryBitmap, hints)
            result.text
        } catch (_: NotFoundException) {
            null
        } catch (_: ChecksumException) {
            null
        } catch (_: FormatException) {
            null
        } finally {
            // Reset reader state
        }
    }
}
