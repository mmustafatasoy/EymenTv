package com.eymen.uydukontrol

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Full-screen animated advertisement shown on radio channels (no video).
 * Four scenes loop: brand + signature, services, "find us on Google", atasoyyazilim.com.
 * All drawing is on the 1280×720 design grid; 1.dp = one design pixel.
 */
private const val DUR = 12f
private const val URL = "atasoyyazilim.com"

private val QR = listOf(
    "000000000000000000000000000",
    "011111110011000100011111110",
    "010000010110000000010000010",
    "010111010101110100010111010",
    "010111010101011010010111010",
    "010111010000011001010111010",
    "010000010010000101010000010",
    "011111110101010101011111110",
    "000000000110100000000000000",
    "010000010101101110110011100",
    "000100001101011110101111100",
    "010011111110100011111010110",
    "011110000101100100010010010",
    "011011011110110000011000010",
    "010100000010001011001000100",
    "010001010101110111011110110",
    "010011100110100001111011010",
    "010101111000111001111101000",
    "000000000100011011000100000",
    "011111110011100001010100010",
    "010000010010000011000100110",
    "010111010010111111111101000",
    "010111010010100110110000110",
    "010111010000001001100011010",
    "010000010010100101011100010",
    "011111110100010001000010010",
    "000000000000000000000000000",
)

private fun cl(v: Float, a: Float, b: Float) = if (v < a) a else if (v > b) b else v
private fun seg(t: Float, a: Float, b: Float) = cl((t - a) / (b - a), 0f, 1f)
private fun eo(x: Float) = 1f - (1f - x).pow(3)
private fun eob(x: Float): Float { val c = 2.70158f; return 1f + (c + 1) * (x - 1).pow(3) + c * (x - 1).pow(2) }
private fun bounce(x: Float): Float {
    val n = 7.5625f; val d = 2.75f
    var z = x
    return when {
        z < 1f / d -> n * z * z
        z < 2f / d -> { z -= 1.5f / d; n * z * z + 0.75f }
        z < 2.5f / d -> { z -= 2.25f / d; n * z * z + 0.9375f }
        else -> { z -= 2.625f / d; n * z * z + 0.984375f }
    }
}
/** Cross-fade envelope for a 3-second scene starting at [start]. */
private fun sceneAlpha(t: Float, start: Float) =
    cl(minOf(seg(t, start, start + 0.5f), 1f - seg(t, start + 2.5f, start + 3f)), 0f, 1f)

private class Particle(val x: Float, val y: Float, val r: Float, val sp: Float, val ph: Float)
private class Confetti(val x: Float, val delay: Float, val rot: Float, val rs: Float, val color: Color)

private val ConfColors = listOf(
    Color(0xFFE8C27A), Color(0xFF6BA8FF), Color(0xFF3FE0B0), Color(0xFFFF5468), Color(0xFFF2F4F8), Color(0xFFF4D79E)
)

@Composable
private fun loopTime(): Float {
    val ms by produceState(0L) {
        while (true) { withInfiniteAnimationFrameMillis { value = it } }
    }
    return (ms % (DUR * 1000).toLong()) / 1000f
}

@Composable
fun RadioAd(app: AppState, ch: Channel) {
    val t = loopTime()
    val dens = LocalDensity.current.density
    val parts = remember {
        val rnd = Random(7)
        List(30) { Particle(rnd.nextFloat(), rnd.nextFloat(), 4f + rnd.nextFloat() * 16f, 0.006f + rnd.nextFloat() * 0.02f, rnd.nextFloat() * 6.28f) }
    }
    val conf = remember {
        val rnd = Random(21)
        List(26) { Confetti(rnd.nextFloat(), rnd.nextFloat() * 0.8f, rnd.nextFloat() * 360f, (rnd.nextFloat() - 0.5f) * 600f, ConfColors[it % ConfColors.size]) }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF05080F))) {
        // animated background: glows, particles, waveform, confetti
        val confVis = sceneAlpha(t, 9f)
        Canvas(Modifier.fillMaxSize()) { drawBackdrop(t, parts, conf, confVis) }

        // persistent radio chrome
        Row(
            Modifier.align(Alignment.TopStart).padding(top = 28.dp, start = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(11.dp).clip(RoundedCornerShape(11.dp)).background(C.Live))
            T("CANLI RADYO", 17, C.Live, 800, spacing = 2.5f)
            T("· ${ch.name}" + (if (ch.number.isNotBlank()) " · ${ch.number}" else ""), 17, C.Mut, 500)
        }
        T(app.time(app.now), 26, C.Fg, 300, display = true, modifier = Modifier.align(Alignment.TopEnd).padding(top = 24.dp, end = 48.dp))

        // scenes
        SceneBox(sceneAlpha(t, 0f)) { SceneBrand(t) }
        SceneBox(sceneAlpha(t, 3f)) { SceneServices(t - 3f, dens) }
        SceneBox(sceneAlpha(t, 6f)) { SceneSearch(t - 6f, dens) }
        SceneBox(sceneAlpha(t, 9f)) { SceneUrl(t - 9f, dens) }

        // scene indicator dots
        val sc = (t / 3f).toInt()
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 86.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            for (i in 0 until 4) {
                Box(Modifier.width(if (i == sc) 30.dp else 10.dp).height(10.dp).clip(RoundedCornerShape(5.dp))
                    .background(if (i == sc) C.Gold else Color(0x38D6E2FF)))
            }
        }
    }
}

@Composable
private fun SceneBox(alpha: Float, content: @Composable () -> Unit) {
    if (alpha <= 0.001f) return
    Box(Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha }, contentAlignment = Alignment.Center) {
        content()
    }
}

// ---------------- Scene A: brand + signature ----------------
@Composable
private fun SceneBrand(t: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val mp = eob(seg(t, 0f, 0.7f))
        Box(
            Modifier.graphicsLayer {
                scaleX = 0.6f + 0.4f * mp; scaleY = 0.6f + 0.4f * mp
                rotationZ = -8f * (1f - mp); alpha = seg(t, 0f, 0.4f)
            }
        ) { MonoBox(150) }
        Spacer(Modifier.height(28.dp))
        val bp = seg(t, 0.5f, 1.1f)
        Box(Modifier.graphicsLayer { alpha = bp; translationY = 22f * (1f - eo(bp)) }) {
            T("ATASOY YAZILIM", 42, C.Fg, 600, display = true, spacing = 8f)
        }
        Spacer(Modifier.height(16.dp))
        SignatureLine(eo(seg(t, 1.1f, 2.5f)), 30, 360)
    }
}

@Composable
private fun MonoBox(s: Int) {
    Box(
        Modifier.size(s.dp)
            .clip(RoundedCornerShape((s * 0.22f).dp))
            .background(Brush.linearGradient(listOf(Color(0xFF20305A), Color(0xFF0A1224))))
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        T("A", (s * 0.6f).toInt(), C.Gold, 700, display = true)
    }
}

// ---------------- Scene B: services ----------------
@Composable
private fun SceneServices(b: Float, dens: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.graphicsLayer { alpha = seg(b, 0f, 0.5f) }) {
            T("NELER YAPIYORUZ", 20, C.Dim, 700, spacing = 5f)
        }
        Spacer(Modifier.height(30.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            ServiceCard(b, 0, dens, Color(0xFF6BA8FF), Color(0xFF3B6FD4), "Web Sitesi", "Modern, hızlı ve", "mobil uyumlu siteler", Icon.WEB)
            ServiceCard(b, 1, dens, Color(0xFF5FE0B0), Color(0xFF1FA882), "Mobil Uygulama", "Android ve iOS için", "özel uygulamalar", Icon.PHONE)
            ServiceCard(b, 2, dens, Color(0xFFF4D79E), Color(0xFFE0A94A), "Android TV", "Akıllı TV ve kutular", "için uygulamalar", Icon.TV)
        }
    }
}

private enum class Icon { WEB, PHONE, TV }

@Composable
private fun ServiceCard(b: Float, i: Int, dens: Float, c1: Color, c2: Color, title: String, l1: String, l2: String, icon: Icon) {
    val p = seg(b, 0.2f + i * 0.22f, 0.95f + i * 0.22f)
    Column(
        Modifier
            .graphicsLayer {
                alpha = p
                val sc = 0.92f + 0.08f * eo(p); scaleX = sc; scaleY = sc
                translationY = 60f * (1f - eob(p)) * dens
            }
            .width(280.dp).height(300.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xBD0C1428))
            .padding(horizontal = 26.dp, vertical = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(100.dp).clip(RoundedCornerShape(26.dp)).background(Brush.linearGradient(listOf(c1, c2))),
            contentAlignment = Alignment.Center
        ) { Canvas(Modifier.size(56.dp)) { drawServiceIcon(icon) } }
        Spacer(Modifier.height(24.dp))
        T(title, 27, C.Fg, 700, display = true)
        Spacer(Modifier.height(12.dp))
        T(l1, 16, C.Mut, 500)
        T(l2, 16, C.Mut, 500)
    }
}

private fun DrawScope.drawServiceIcon(icon: Icon) {
    val w = size.width
    val ink = Color(0xFF05080F)
    val st = Stroke(width = w * 0.065f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    when (icon) {
        Icon.WEB -> {
            drawRoundRectStroke(w * 0.1f, w * 0.17f, w * 0.8f, w * 0.56f, w * 0.08f, ink, st)
            drawLine(ink, Offset(w * 0.1f, w * 0.36f), Offset(w * 0.9f, w * 0.36f), st.width, StrokeCap.Round)
            drawLine(ink, Offset(w * 0.36f, w * 0.86f), Offset(w * 0.64f, w * 0.86f), st.width, StrokeCap.Round)
        }
        Icon.PHONE -> {
            drawRoundRectStroke(w * 0.28f, w * 0.1f, w * 0.44f, w * 0.8f, w * 0.1f, ink, st)
            drawLine(ink, Offset(w * 0.44f, w * 0.78f), Offset(w * 0.56f, w * 0.78f), st.width, StrokeCap.Round)
        }
        Icon.TV -> {
            drawRoundRectStroke(w * 0.1f, w * 0.2f, w * 0.8f, w * 0.5f, w * 0.07f, ink, st)
            drawLine(ink, Offset(w * 0.35f, w * 0.86f), Offset(w * 0.65f, w * 0.86f), st.width, StrokeCap.Round)
            drawLine(ink, Offset(w * 0.5f, w * 0.7f), Offset(w * 0.5f, w * 0.86f), st.width, StrokeCap.Round)
        }
    }
}

private fun DrawScope.drawRoundRectStroke(x: Float, y: Float, w: Float, h: Float, r: Float, color: Color, st: Stroke) {
    val p = Path().apply {
        moveTo(x + r, y); lineTo(x + w - r, y); quadraticBezierTo(x + w, y, x + w, y + r)
        lineTo(x + w, y + h - r); quadraticBezierTo(x + w, y + h, x + w - r, y + h)
        lineTo(x + r, y + h); quadraticBezierTo(x, y + h, x, y + h - r)
        lineTo(x, y + r); quadraticBezierTo(x, y, x + r, y); close()
    }
    drawPath(p, color, style = st)
}

// ---------------- Scene C: find us on Google ----------------
@Composable
private fun SceneSearch(c: Float, dens: Float) {
    val full = "Atasoy Yazılım"
    val n = (cl(seg(c, 0.3f, 1.6f), 0f, 1f) * full.length).toInt()
    val cursorOn = c < 1.7f && ((c * 2f).toInt() % 2 == 0)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.graphicsLayer { alpha = seg(c, 0f, 0.5f) }) {
            T("BİZE NASIL ULAŞIRSINIZ", 20, C.Dim, 700, spacing = 5f)
        }
        Spacer(Modifier.height(28.dp))
        Row(
            Modifier.graphicsLayer { alpha = seg(c, 0.1f, 0.6f) }
                .width(780.dp).height(72.dp).clip(RoundedCornerShape(36.dp)).background(Color.White)
                .padding(start = 28.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(Modifier.size(30.dp)) {
                val w = size.width
                drawCircle(Color(0xFF5F6368), w * 0.3f, Offset(w * 0.42f, w * 0.42f), style = Stroke(w * 0.1f))
                drawLine(Color(0xFF5F6368), Offset(w * 0.64f, w * 0.64f), Offset(w * 0.9f, w * 0.9f), w * 0.1f, StrokeCap.Round)
            }
            Spacer(Modifier.width(18.dp))
            Row(Modifier.width(560.dp), verticalAlignment = Alignment.CenterVertically) {
                T(full.take(n), 30, Color(0xFF202124), 600)
                if (cursorOn) {
                    Spacer(Modifier.width(2.dp))
                    Box(Modifier.width(3.dp).height(32.dp).background(C.Blue))
                }
            }
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier.height(48.dp).clip(RoundedCornerShape(24.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFFF4D79E), Color(0xFFE0A94A))))
                    .padding(horizontal = 26.dp),
                contentAlignment = Alignment.Center
            ) { T("Ara", 22, C.GoldInk, 700) }
        }
        Spacer(Modifier.height(26.dp))
        val rp = seg(c, 1.7f, 2.2f)
        Column(
            Modifier.graphicsLayer { alpha = rp; translationY = 30f * (1f - eo(rp)) * dens }
                .width(780.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xBD0C1428))
                .padding(horizontal = 30.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            T("atasoyyazilim.com", 17, C.Green, 500)
            T("Atasoy Yazılım — Web, Mobil ve TV Yazılımları", 26, C.Blue, 700)
            T("Modern web siteleri, Android ve iOS uygulamaları ve Akıllı TV yazılımları.", 17, C.Mut, 500, maxLines = 2)
        }
    }
}

// ---------------- Scene D: url + QR ----------------
@Composable
private fun SceneUrl(d: Float, dens: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.graphicsLayer { alpha = seg(d, 0f, 0.5f) }) {
            T("HEMEN ZİYARET EDİN", 20, C.Dim, 700, spacing = 5f)
        }
        Spacer(Modifier.height(26.dp))
        val dotIndex = URL.indexOf('.')
        Row(verticalAlignment = Alignment.Bottom) {
            for (i in URL.indices) {
                val dl = i * 0.09f
                val p = seg(d, 0.2f + dl, 0.95f + dl)
                val yy = -200f * (1f - bounce(p)) * dens
                Box(Modifier.graphicsLayer { alpha = if (p > 0f) 1f else 0f; translationY = yy }) {
                    T(URL[i].toString(), 84, if (i >= dotIndex) C.Live else C.Gold, 700, display = true)
                }
            }
        }
        Spacer(Modifier.height(40.dp))
        val cp = seg(d, 1.8f, 2.4f)
        Row(
            Modifier.graphicsLayer { alpha = cp; translationY = 30f * (1f - eo(cp)) * dens },
            horizontalArrangement = Arrangement.spacedBy(30.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(120.dp).clip(RoundedCornerShape(16.dp)).background(Color.White).padding(10.dp)) {
                Canvas(Modifier.fillMaxSize()) { drawQr() }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                T("Telefonunla okut", 28, C.Fg, 700, display = true)
                T("Siteye hemen git", 20, C.Mut, 500)
            }
        }
    }
}

private fun DrawScope.drawQr() {
    val n = QR.size
    val cell = size.width / n
    for (y in 0 until n) {
        val row = QR[y]
        for (x in 0 until n) {
            if (row[x] == '1') {
                drawRect(Color(0xFF05080F), Offset(x * cell, y * cell), Size(cell + 0.6f, cell + 0.6f))
            }
        }
    }
}

// ---------------- animated backdrop ----------------
private fun DrawScope.drawBackdrop(t: Float, parts: List<Particle>, conf: List<Confetti>, confVis: Float) {
    val w = size.width; val h = size.height
    // moving colour glows
    drawRect(Brush.radialGradient(listOf(Color(0x384C8DFF), Color.Transparent), center = Offset(w * (0.28f + 0.12f * sin(t * 0.5f)), h * (0.30f + 0.08f * cos(t * 0.45f))), radius = w * 0.52f), size = size)
    drawRect(Brush.radialGradient(listOf(Color(0x2EE8C27A), Color.Transparent), center = Offset(w * (0.74f + 0.10f * sin(t * 0.4f + 2f)), h * (0.70f + 0.08f * cos(t * 0.5f))), radius = w * 0.48f), size = size)
    drawRect(Brush.radialGradient(listOf(Color(0x243FE0B0), Color.Transparent), center = Offset(w * (0.50f + 0.16f * sin(t * 0.3f + 1f)), h * (0.45f + 0.12f * cos(t * 0.35f + 2f))), radius = w * 0.40f), size = size)

    // floating particles
    for (p in parts) {
        var yy = (p.y - t * p.sp) % 1f; if (yy < 0f) yy += 1f
        val a = 0.10f + 0.20f * (0.5f + 0.5f * sin(t * 1.2f + p.ph))
        drawCircle(Color(0xFFE8C27A).copy(alpha = a), p.r * (w / 1280f), Offset((p.x * w), yy * h))
    }

    // bottom equaliser (radio is playing)
    val bars = 54
    val gap = 8f * (w / 1280f)
    val left = 48f * (w / 1280f)
    val right = w - left
    val bw = (right - left - gap * (bars - 1)) / bars
    val baseY = h - 46f * (h / 720f)
    for (i in 0 until bars) {
        val bh = (18f + 40f * abs(sin(t * 3f + i * 0.5f)) * abs(cos(t * 1.7f + i * 0.3f)) + 8f * sin(t * 5f + i)) * (h / 720f)
        val x = left + i * (bw + gap)
        drawRect(
            Brush.verticalGradient(listOf(Color(0xFFF4D79E), Color(0xFF8A6A34)), startY = baseY - bh, endY = baseY),
            topLeft = Offset(x, baseY - bh), size = Size(bw, bh)
        )
    }

    // confetti (scene D only)
    if (confVis > 0.02f) {
        val d = (t - 9f)
        for (c in conf) {
            val p = cl((d - c.delay) / 2.6f, 0f, 1f)
            if (p <= 0f || p >= 1f) continue
            val cx = (c.x * w) + 6f * sin(d + c.rot)
            val cy = (-0.05f + p * 1.12f) * h
            rotate(c.rot + c.rs * p, Offset(cx, cy)) {
                drawRect(c.color.copy(alpha = confVis), topLeft = Offset(cx - 9f, cy - 14f), size = Size(18f, 28f))
            }
        }
    }
}
