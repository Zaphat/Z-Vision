package com.zteam.zvision.qr

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zteam.zvision.R
import android.graphics.Color

@Composable
fun QRImage(content: QRContent, modifier: Modifier = Modifier, logoPath: String? = null) {
    val qrGenerator = QRGenerator()
    val bitmap = if (logoPath == null) {
        qrGenerator.generateQRCode(content = content)
    } else {
        val context = LocalContext.current
        val options = BitmapFactory.Options()
        options.inSampleSize = 3 //save memory
        BitmapFactory.decodeResource(context.resources, R.drawable.logo_bk, options)
            ?.let {
                qrGenerator.generateQRCodeWithLogo(
                    content = content,
                    logoBitmap = it,
                    foregroundColor = Color.RED,
                    backgroundColor = Color.BLUE
                )
            }
    }
    if (bitmap != null) Image(
        bitmap = bitmap.asImageBitmap(), contentDescription = "QR Code", modifier = modifier
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
        )
        Spacer(modifier = Modifier.height(32.dp))
        QRImage(
            content = TextQR("Hello, This is a message for help"),
            logoPath = "logo_bk.png",
        )
    }
}
