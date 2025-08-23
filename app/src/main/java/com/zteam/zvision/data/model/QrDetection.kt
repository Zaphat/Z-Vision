package com.zteam.zvision.data.model

import androidx.compose.ui.geometry.Offset

data class QrDetection(
    val text: String,
    val points: List<Offset>,
    val imageWidth: Int,
    val imageHeight: Int
)
