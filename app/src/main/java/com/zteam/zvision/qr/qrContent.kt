package com.zteam.zvision.qr

sealed interface QRContent {
    fun toEncodedString(): String
}

data class UrlQR(val url: String) : QRContent {
    override fun toEncodedString(): String = url
}

data class TextQR(val text: String) : QRContent {
    override fun toEncodedString(): String = text
}

data class WifiQR(
    val ssid: String,
    val password: String,
    val encryption: WifiEncryption = WifiEncryption.WPA
) : QRContent {
    override fun toEncodedString(): String =
        "WIFI:S:$ssid;T:$encryption;P:$password;;"
}

enum class WifiEncryption(val type: String) {
    WEP("WEP"),
    WPA("WPA"),
    NOPASS("nopass");

    override fun toString(): String = type
}