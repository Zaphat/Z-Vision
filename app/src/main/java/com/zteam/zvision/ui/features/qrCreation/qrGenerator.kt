package com.zteam.zvision.ui.features.qrCreation

import android.graphics.Color
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Canvas
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import androidx.core.graphics.set
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale

class QRGenerator() {

    companion object {
        private const val DEFAULT_SIZE = 512
        private const val LOGO_SIZE_RATIO = 1 / 4f // Logo will be 1/4 of QR code size
        private const val LOGO_PADDING = 0
    }

    fun generateQRCode(
        content: QRContent,
        size: Int = DEFAULT_SIZE,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE
    ): Bitmap? {
        return try {
            val hints = hashMapOf<EncodeHintType, Any>().apply {
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.MARGIN, 1)
            }

            val qrCodeWriter = QRCodeWriter()
            val bitMatrix = qrCodeWriter.encode(
                content.toEncodedString(), BarcodeFormat.QR_CODE, size, size, hints
            )

            createBitmapFromBitMatrix(bitMatrix, foregroundColor, backgroundColor)
        } catch (e: WriterException) {
            e.printStackTrace()
            null
        }
    }

    fun generateQRCodeWithLogo(
        content: QRContent,
        logoBitmap: Bitmap,
        size: Int = DEFAULT_SIZE,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE
    ): Bitmap? {
        return try {
            // Consider about using only 100-150 chars for this level of correction
            val hints = hashMapOf<EncodeHintType, Any>().apply {
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.MARGIN, 1)
            }

            val qrCodeWriter = QRCodeWriter()
            val bitMatrix = qrCodeWriter.encode(
                content.toEncodedString(), BarcodeFormat.QR_CODE, size, size, hints
            )

            val qrBitmap = createBitmapFromBitMatrix(bitMatrix, foregroundColor, backgroundColor)
            overlayLogo(qrBitmap, logoBitmap)
        } catch (e: WriterException) {
            e.printStackTrace()
            null
        }
    }

    private fun createBitmapFromBitMatrix(
        bitMatrix: BitMatrix, foregroundColor: Int, backgroundColor: Int
    ): Bitmap {
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = createBitmap(width, height, Bitmap.Config.RGB_565)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap[x, y] = if (bitMatrix[x, y]) foregroundColor else backgroundColor
            }
        }

        return bitmap
    }

    private fun overlayLogo(qrBitmap: Bitmap, logoBitmap: Bitmap): Bitmap? {
        return try {
            val qrWidth = qrBitmap.width
            val qrHeight = qrBitmap.height

            val logoSize = (minOf(qrWidth, qrHeight) * LOGO_SIZE_RATIO).toInt()
            val resizedLogo = logoBitmap.scale(width = logoSize, height = logoSize, filter = true)

            val combined = createBitmap(qrWidth, qrHeight)
            val canvas = Canvas(combined)
            canvas.drawBitmap(qrBitmap, 0f, 0f, null)

            val logoX = (qrWidth - logoSize) / 2f
            val logoY = (qrHeight - logoSize) / 2f

            // Add white background for logo (optional, for better visibility)
            val backgroundPaint = Paint().apply {
                color = Color.WHITE
                isAntiAlias = true
            }

            val backgroundRect = RectF(
                logoX - LOGO_PADDING,
                logoY - LOGO_PADDING,
                logoX + logoSize + LOGO_PADDING,
                logoY + logoSize + LOGO_PADDING
            )
            canvas.drawRoundRect(backgroundRect, 8f, 8f, backgroundPaint)

            canvas.drawBitmap(resizedLogo, logoX, logoY, null)
            combined
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

//    fun copyBitmapToClipboard(bitmap: Bitmap, label: String = "QR Code") {
//        try {
//            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
//
//            // Save bitmap to MediaStore and get URI
//            val uri = saveBitmapToMediaStore(bitmap, "qr_code_${System.currentTimeMillis()}")
//
//            if (uri != null) {
//                val clip = ClipData.newUri(context.contentResolver, label, uri)
//                clipboardManager.setPrimaryClip(clip)
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
}
