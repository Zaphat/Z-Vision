package com.zteam.zvision.qr

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.io.File
import androidx.compose.ui.platform.LocalContext
import android.util.Log

@Composable
fun QRImage(content: QRContent, modifier: Modifier = Modifier, logoPath: String? = null) {
    val qrGenerator = QRGenerator()
    val bitmap =
        if (logoPath == null) {
            qrGenerator.generateQRCode(content = content)
        } else {
            val context = LocalContext.current
            val file = File(context.filesDir, logoPath)
            Log.i("QRImage", "Logo path: ${file.absolutePath}")
            val logo = BitmapFactory.decodeFile(file.absolutePath)
            if (logo == null) {
                Log.e("QRImage", "Failed to decode logo bitmap")
                null
            } else {
                qrGenerator.generateQRCodeWithLogo(content = content, logoBitmap = logo)
            }
        }
    if (bitmap != null) Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "QR Code",
        modifier = modifier
    ) else {
        Text("Error generating QR code")
    }
}

@Preview(showBackground = true)
@Composable
fun BasicQRPreview() {
    Column {
        QRImage(
            content = UrlQR("https://example.com"),
            modifier = Modifier.size(200.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        QRImage(
            content = TextQR("Hello"),
            logoPath = "Logo_BK.png",
            modifier = Modifier.size(200.dp)
        )
    }
}
