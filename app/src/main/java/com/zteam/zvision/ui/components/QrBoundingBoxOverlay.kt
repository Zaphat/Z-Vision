package com.zteam.zvision.ui.components

import android.graphics.RectF
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.zteam.zvision.data.model.QrDetection
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun QrBoundingBoxOverlay(viewSize: IntSize, detection: QrDetection?) {
    if (viewSize == IntSize.Zero) return

    val leftA = remember { Animatable(0f) }
    val topA = remember { Animatable(0f) }
    val rightA = remember { Animatable(0f) }
    val bottomA = remember { Animatable(0f) }
    var initialized by remember { mutableStateOf(false) }

    val density = LocalResources.current.displayMetrics.density
    fun Dp.toPxF(): Float = (this.value * density)
    fun clamp(value: Float, min: Float, max: Float) = value.coerceIn(min, max)

    fun defaultCenterRect(vw: Float, vh: Float): RectF {
        val minSide = minOf(vw, vh)
        val size = clamp(minSide * 0.55f, 180.dp.toPxF(), 320.dp.toPxF())
        val l = (vw - size) / 2f
        val t = (vh - size) / 2f
        return RectF(l, t, l + size, t + size)
    }

    // Keep last good target to avoid blinking when detection is briefly lost
    var lastTarget by remember { mutableStateOf<RectF?>(null) }

    val vw = viewSize.width.toFloat()
    val vh = viewSize.height.toFloat()

    LaunchedEffect(vw, vh) {
        if (!initialized) {
            val initRect = defaultCenterRect(vw, vh)
            leftA.snapTo(initRect.left)
            topA.snapTo(initRect.top)
            rightA.snapTo(initRect.right)
            bottomA.snapTo(initRect.bottom)
            lastTarget = initRect
            initialized = true
        }
    }

    // After a while with no detection, go back to center
    val resetDelayMs = 1000L
    var idleCenterTarget by remember { mutableStateOf<RectF?>(null) }
    LaunchedEffect(detection, vw, vh) {
        if (detection == null) {
            delay(resetDelayMs)
            // Re-check to avoid re-centring if a detection happened during the delay
            idleCenterTarget = defaultCenterRect(vw, vh)
        } else {
            idleCenterTarget = null
        }
    }

    // Compute mapping rect from detection points (image space) to view space (center-crop aware)
    fun rectForDetection(vw: Float, vh: Float, det: QrDetection): RectF? {
        if (det.points.isEmpty()) return null
        val iw = det.imageWidth.toFloat().coerceAtLeast(1f)
        val ih = det.imageHeight.toFloat().coerceAtLeast(1f)

        val imageAspect = iw / ih
        val viewAspect = vw / vh
        val (scale, leftOff, topOff) =
            if (abs(imageAspect - viewAspect) < 0.01f) {
                val s = vw / iw
                Triple(s, 0f, 0f)
            } else {
                val s = maxOf(vw / iw, vh / ih) // center crop
                Triple(s, (vw - iw * s) / 2f, (vh - ih * s) / 2f)
            }

        val mapped = det.points.map { p ->
            val x = leftOff + p.x * scale
            val y = topOff + p.y * scale
            Offset(x, y)
        }
        val minX = mapped.minOf { it.x }
        val maxX = mapped.maxOf { it.x }
        val minY = mapped.minOf { it.y }
        val maxY = mapped.maxOf { it.y }

        var l = minX
        var t = minY
        var r = maxX
        var b = maxY

        val width = r - l
        val height = b - t
        val side = maxOf(width, height)

        // Minimal padding to fully wrap QR including quiet zone but not overshoot
        val padding = 8.dp.toPxF()
        val half = (side / 2f) + padding

        val cx = (l + r) / 2f
        val cy = (t + b) / 2f

        l = clamp(cx - half, 0f, vw)
        t = clamp(cy - half, 0f, vh)
        r = clamp(cx + half, 0f, vw)
        b = clamp(cy + half, 0f, vh)

        return RectF(l, t, r, b)
    }

    val detRect = remember(detection, vw, vh) { detection?.let { rectForDetection(vw, vh, it) } }
    val nextTarget = detRect ?: idleCenterTarget ?: lastTarget ?: defaultCenterRect(vw, vh)

    LaunchedEffect(nextTarget) {
        lastTarget = nextTarget
    }

    // Animate all edges concurrently to avoid blinks
    LaunchedEffect(nextTarget.left, nextTarget.top, nextTarget.right, nextTarget.bottom) {
        val spec = tween<Float>(durationMillis = 260)
        coroutineScope {
            launch { leftA.animateTo(nextTarget.left, spec) }
            launch { topA.animateTo(nextTarget.top, spec) }
            launch { rightA.animateTo(nextTarget.right, spec) }
            launch { bottomA.animateTo(nextTarget.bottom, spec) }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val l = leftA.value
        val t = topA.value
        val r = rightA.value
        val b = bottomA.value

        val rectW = (r - l).coerceAtLeast(0f)
        val rectH = (b - t).coerceAtLeast(0f)
        if (rectW <= 0f || rectH <= 0f) return@Canvas

        // Pale blue overlay behind the reticle
        val overlayAlpha = 0.18f
        val overlayCorner = 16.dp.toPx()
        drawRoundRect(
            color = Color(0xFF42A5F5), // pale blue
            topLeft = Offset(l, t),
            size = Size(rectW, rectH),
            cornerRadius = CornerRadius(overlayCorner, overlayCorner),
            alpha = overlayAlpha
        )

        // Reticle style: thicker, white, rounded edges
        val strokeW = 8.dp.toPx()
        val side = minOf(rectW, rectH)
        val cornerLen = (side * 0.24f).coerceIn(12.dp.toPx(), 64.dp.toPx())

        // Top-left
        drawLine(
            color = Color.White,
            start = Offset(l, t),
            end = Offset(l + cornerLen, t),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White,
            start = Offset(l, t),
            end = Offset(l, t + cornerLen),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )

        // Top-right
        drawLine(
            color = Color.White,
            start = Offset(r - cornerLen, t),
            end = Offset(r, t),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White,
            start = Offset(r, t),
            end = Offset(r, t + cornerLen),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )

        // Bottom-left
        drawLine(
            color = Color.White,
            start = Offset(l, b - cornerLen),
            end = Offset(l, b),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White,
            start = Offset(l, b),
            end = Offset(l + cornerLen, b),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )

        // Bottom-right
        drawLine(
            color = Color.White,
            start = Offset(r - cornerLen, b),
            end = Offset(r, b),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White,
            start = Offset(r, b - cornerLen),
            end = Offset(r, b),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
    }
}
