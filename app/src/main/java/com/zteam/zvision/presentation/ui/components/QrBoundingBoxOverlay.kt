package com.zteam.zvision.presentation.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.zteam.zvision.domain.model.QrDetection
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max

@Composable
fun QrBoundingBoxOverlay(viewSize: IntSize, detection: QrDetection?) {
    if (viewSize == IntSize.Zero) return

    // Animate 4 corners instead of rect edges
    val p0 = remember { Animatable(Offset.Zero, Offset.VectorConverter) } // TL
    val p1 = remember { Animatable(Offset.Zero, Offset.VectorConverter) } // TR
    val p2 = remember { Animatable(Offset.Zero, Offset.VectorConverter) } // BR
    val p3 = remember { Animatable(Offset.Zero, Offset.VectorConverter) } // BL
    var initialized by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    fun Dp.toPxF(): Float = with(density) { this@toPxF.toPx() }
    fun clamp(value: Float, min: Float, max: Float) = value.coerceIn(min, max)

    val vw = viewSize.width.toFloat()
    val vh = viewSize.height.toFloat()

    // Centered square as default/idling target
    fun centerSquareCorners(vw: Float, vh: Float): List<Offset> {
        val minSide = minOf(vw, vh)
        val size = clamp(minSide * 0.55f, 180.dp.toPxF(), 320.dp.toPxF())
        val l = (vw - size) / 2f
        val t = (vh - size) / 2f
        val r = l + size
        val b = t + size
        return listOf(Offset(l, t), Offset(r, t), Offset(r, b), Offset(l, b))
    }

    // Keep last polygon to avoid blinking on intermittent failures
    var lastTarget by remember { mutableStateOf<List<Offset>?>(null) }

    LaunchedEffect(vw, vh) {
        if (!initialized) {
            val init = centerSquareCorners(vw, vh)
            p0.snapTo(init[0]); p1.snapTo(init[1]); p2.snapTo(init[2]); p3.snapTo(init[3])
            lastTarget = init
            initialized = true
        }
    }

    // After no detection for a while, return to center
    val resetDelayMs = 1000L
    var idleCenterTarget by remember { mutableStateOf<List<Offset>?>(null) }
    LaunchedEffect(detection, vw, vh) {
        if (detection == null) {
            delay(resetDelayMs)
            idleCenterTarget = centerSquareCorners(vw, vh)
        } else {
            idleCenterTarget = null
        }
    }

    // Map image-space points into view-space (center-crop aware)
    fun mapPointsToView(vw: Float, vh: Float, iw: Float, ih: Float, pts: List<Offset>): List<Offset> {
        val imageAspect = iw / ih
        val viewAspect = vw / vh
        val (scale, leftOff, topOff) =
            if (abs(imageAspect - viewAspect) < 0.01f) {
                val s = vw / iw
                Triple(s, 0f, 0f)
            } else {
                val s = max(vw / iw, vh / ih)
                Triple(s, (vw - iw * s) / 2f, (vh - ih * s) / 2f)
            }
        return pts.map { p -> Offset(leftOff + p.x * scale, topOff + p.y * scale) }
    }

    // Order corners clockwise starting near top-left for stable drawing
    fun orderCornersClockwise(points: List<Offset>): List<Offset> {
        if (points.isEmpty()) return points
        val cx = points.map { it.x }.average().toFloat()
        val cy = points.map { it.y }.average().toFloat()
        val sorted = points.sortedBy { atan2((it.y - cy), (it.x - cx)) } // -PI..PI CCW
        // rotate to start from top-left-ish (min y+x)
        val startIdx = sorted.indices.minByOrNull { i -> sorted[i].x + sorted[i].y } ?: 0
        return (0 until minOf(4, sorted.size)).map { sorted[(startIdx + it) % sorted.size] }
    }

    // Build next target polygon from detection or idle/default
    val detPoly: List<Offset>? = remember(detection, vw, vh) {
        detection?.let {
            if (it.points.isEmpty()) null
            else {
                val mapped = mapPointsToView(
                    vw, vh,
                    it.imageWidth.toFloat().coerceAtLeast(1f),
                    it.imageHeight.toFloat().coerceAtLeast(1f),
                    it.points
                )
                // Prefer 4-corner polygon (QR has 4 corners). Order them.
                val ordered = orderCornersClockwise(mapped)
                if (ordered.size == 4) ordered else null
            }
        }
    }
    val nextTarget = detPoly ?: idleCenterTarget ?: lastTarget ?: centerSquareCorners(vw, vh)

    LaunchedEffect(nextTarget) {
        lastTarget = nextTarget
    }

    // Animate corners concurrently
    LaunchedEffect(nextTarget) {
        val spec = tween<Offset>(durationMillis = 260)
        coroutineScope {
            launch { p0.animateTo(nextTarget[0], spec) }
            launch { p1.animateTo(nextTarget[1], spec) }
            launch { p2.animateTo(nextTarget[2], spec) }
            launch { p3.animateTo(nextTarget[3], spec) }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val pts = listOf(p0.value, p1.value, p2.value, p3.value)

        // Build path
        val path = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
            close()
        }

        // Fill
        drawPath(
            path = path,
            color = Color(0xFF42A5F5),
            alpha = 0.18f
        )

        // Draw 4-corner reticles along polygon edges (two short segments per corner)
        fun norm(v: Offset): Offset {
            val d = v.getDistance().coerceAtLeast(1e-3f)
            return Offset(v.x / d, v.y / d)
        }

        val strokeW = 8.dp.toPx()
        val minCorner = 12.dp.toPx()
        val maxCorner = 64.dp.toPx()

        // Corners: p0 (TL-ish), p1 (TR-ish), p2 (BR-ish), p3 (BL-ish)
        val (a, b, c, d) = pts

        // Corner A (between edges A->B and A->D)
        run {
            val vAB = b - a
            val vAD = d - a
            val len = (minOf(vAB.getDistance(), vAD.getDistance()) * 0.24f).coerceIn(minCorner, maxCorner)
            val nAB = norm(vAB)
            val nAD = norm(vAD)
            drawLine(color = Color.White, start = a, end = a + nAB * len, strokeWidth = strokeW, cap = StrokeCap.Round)
            drawLine(color = Color.White, start = a, end = a + nAD * len, strokeWidth = strokeW, cap = StrokeCap.Round)
        }

        // Corner B (between edges B->C and B->A)
        run {
            val vBC = c - b
            val vBA = a - b
            val len = (minOf(vBC.getDistance(), vBA.getDistance()) * 0.24f).coerceIn(minCorner, maxCorner)
            val nBC = norm(vBC)
            val nBA = norm(vBA)
            drawLine(color = Color.White, start = b, end = b + nBC * len, strokeWidth = strokeW, cap = StrokeCap.Round)
            drawLine(color = Color.White, start = b, end = b + nBA * len, strokeWidth = strokeW, cap = StrokeCap.Round)
        }

        // Corner C (between edges C->D and C->B)
        run {
            val vCD = d - c
            val vCB = b - c
            val len = (minOf(vCD.getDistance(), vCB.getDistance()) * 0.24f).coerceIn(minCorner, maxCorner)
            val nCD = norm(vCD)
            val nCB = norm(vCB)
            drawLine(color = Color.White, start = c, end = c + nCD * len, strokeWidth = strokeW, cap = StrokeCap.Round)
            drawLine(color = Color.White, start = c, end = c + nCB * len, strokeWidth = strokeW, cap = StrokeCap.Round)
        }

        // Corner D (between edges D->A and D->C)
        run {
            val vDA = a - d
            val vDC = c - d
            val len = (minOf(vDA.getDistance(), vDC.getDistance()) * 0.24f).coerceIn(minCorner, maxCorner)
            val nDA = norm(vDA)
            val nDC = norm(vDC)
            drawLine(color = Color.White, start = d, end = d + nDA * len, strokeWidth = strokeW, cap = StrokeCap.Round)
            drawLine(color = Color.White, start = d, end = d + nDC * len, strokeWidth = strokeW, cap = StrokeCap.Round)
        }
    }
}
