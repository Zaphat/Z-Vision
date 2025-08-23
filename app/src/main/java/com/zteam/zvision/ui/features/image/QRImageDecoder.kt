package com.zteam.zvision.ui.features.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object QRImageDecoder {

    private const val MAX_SIZE = 1200
    private val reader = QRCodeReader()

    /**
     * Decode a QR code text from an image [uri]. Returns the decoded text or null if none found.
     */
    fun decodeFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
                // Step 1: get image dimensions
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFileDescriptor(fd.fileDescriptor, null, opts)

                // Step 2: compute downsample factor
                val inSampleSize = calculateInSampleSize(opts, MAX_SIZE, MAX_SIZE)

                // Step 3: decode with downsampling + memory-efficient config
                val options = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                }

                val bmp = BitmapFactory.decodeFileDescriptor(fd.fileDescriptor, null, options)
                bmp?.let { bitmap ->
                    try {
                        decodeFromBitmap(bitmap)
                    } finally {
                        bitmap.recycle()
                    }
                }
            }
        } catch (_: IOException) {
            null
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

    /**
     * Decode text from a [bitmap] containing a QR code. Returns null if not found.
     * Note: call from a background thread (ML Kit does a short blocking wait here).
     */
    fun decodeFromBitmap(bitmap: Bitmap): String? {
        // Fast path: ML Kit (robust against perspective/shear)
        val mlOptions = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        val scanner = BarcodeScanning.getClient(mlOptions)
        val image = InputImage.fromBitmap(bitmap, 0)
        val latch = CountDownLatch(1)
        var mlResult: String? = null
        scanner.process(image)
            .addOnSuccessListener { list ->
                mlResult = list.firstOrNull()?.rawValue
            }
            .addOnCompleteListener { latch.countDown() }
        try {
            latch.await(800, TimeUnit.MILLISECONDS)
        } catch (_: InterruptedException) { }
        scanner.close()
        if (mlResult != null) return mlResult

        // Fallback: ZXing (try hard)
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val source = RGBLuminanceSource(width, height, pixels)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

        val hints = mapOf(
            DecodeHintType.TRY_HARDER to true,
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)
        )

        return try {
            reader.decode(binaryBitmap, hints).text
        } catch (_: ReaderException) {
            null
        } finally {
            reader.reset()
        }
    }
}
