package com.eymen.uydukontrol

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private val trLocale = Locale("tr", "TR")

fun Modifier.panel(color: Color, radius: Int, border: Color? = null, borderWidth: Int = 1): Modifier {
    val shape = RoundedCornerShape(radius.dp)
    var m = this.clip(shape).background(color, shape)
    if (border != null) m = m.border(borderWidth.dp, border, shape)
    return m
}

fun monogram(name: String): String {
    val n = name.trim()
    if (n.length <= 7) return n.uppercase(trLocale)
    val words = n.split(Regex("\\s+")).filter { it.isNotEmpty() }
    return words.take(2).joinToString("\n") { it.take(8).uppercase(trLocale) }
}

@Composable
fun ChannelTile(app: AppState, ch: Channel, w: Int, h: Int, onLight: Boolean = false) {
    LaunchedEffect(ch.key) { app.requestLogo(ch) }
    val logo = ch.logo
    val shape = RoundedCornerShape((h / 5).coerceAtLeast(6).dp)
    if (app.showLogos && logo != null) {
        Box(
            Modifier.size(w.dp, h.dp).clip(shape).background(C.LogoBg).padding((h / 10).coerceAtLeast(3).dp),
            contentAlignment = Alignment.Center
        ) {
            Image(bitmap = logo, contentDescription = ch.name, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        }
    } else {
        Box(
            Modifier.size(w.dp, h.dp).clip(shape).background(if (onLight) C.Navy else C.Tile),
            contentAlignment = Alignment.Center
        ) {
            T(
                monogram(ch.name),
                (h / 3.4f).toInt().coerceIn(8, 15),
                C.Fg,
                700,
                display = true,
                maxLines = 2,
                align = TextAlign.Center,
                spacing = 0.4f,
                lineHeight = (h / 3.0f).toInt().coerceIn(9, 17)
            )
        }
    }
}

@Composable
fun Progress(fraction: Float, modifier: Modifier = Modifier, color: Color = C.Gold, track: Color = Color(0x24D6E2FF), height: Int = 4) {
    Box(modifier.height(height.dp).clip(RoundedCornerShape(height.dp)).background(track)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).background(color))
    }
}

@Composable
fun LiveBadge() {
    Box(Modifier.panel(C.Live, 6).padding(horizontal = 8.dp, vertical = 3.dp)) {
        T("CANLI", 11, Color.White, 800, spacing = 1.5f)
    }
}

@Composable
fun Chip(text: String, highlight: Boolean = false) {
    Box(
        Modifier.panel(if (highlight) C.GoldSoft else Color.Transparent, 6, if (highlight) C.Gold else Color(0x33D6E2FF))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        T(text, 12, if (highlight) C.Gold else C.Fg2, 600)
    }
}

@Composable
fun TabChip(text: String, active: Boolean) {
    Box(
        Modifier.panel(if (active) C.Gold else C.Glass2, 999).padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        T(text, 13, if (active) C.GoldInk else C.Fg2, 700)
    }
}

@Composable
fun ColorDot(color: Color, size: Int = 10) {
    Box(Modifier.size(size.dp).clip(RoundedCornerShape(size.dp)).background(color))
}

@Composable
fun KeyLabel(text: String) {
    Box(Modifier.panel(Color.Transparent, 5, Color(0x40D6E2FF)).padding(horizontal = 6.dp, vertical = 1.dp)) {
        T(text, 12, C.Fg2, 700)
    }
}

/** A hint at the bottom of a screen: either a remote key label ("OK") or one of the four colour keys. */
class Hint(val label: String, val text: String, val color: Color? = null)

@Composable
fun HintRow(hints: List<Hint>, size: Int = 13) {
    Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
        for (h in hints) {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                val c = h.color
                if (c != null) ColorDot(c, 9) else KeyLabel(h.label)
                T(h.text, size, C.Dim, 500)
            }
        }
    }
}

@Composable
fun BrandMark(s: Int) {
    Canvas(Modifier.size(s.dp)) {
        val w = size.width
        drawRoundRect(C.Gold, cornerRadius = CornerRadius(w * 0.28f))
        val p = Path()
        p.moveTo(w * 0.40f, w * 0.31f)
        p.lineTo(w * 0.40f, w * 0.69f)
        p.lineTo(w * 0.69f, w * 0.50f)
        p.close()
        drawPath(p, C.FocusInk)
    }
}

@Composable
fun StarIcon(s: Int, color: Color) {
    Canvas(Modifier.size(s.dp)) {
        val w = size.width
        val h = size.height
        val p = Path()
        val cx = w / 2f
        val cy = h / 2f + h * 0.04f
        val outer = w * 0.5f
        val inner = outer * 0.45f
        for (i in 0 until 10) {
            val ang = Math.toRadians(-90.0 + i * 36.0)
            val r = if (i % 2 == 0) outer else inner
            val x = cx + (r * cos(ang)).toFloat()
            val y = cy + (r * sin(ang)).toFloat()
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
        drawPath(p, color)
    }
}

@Composable
fun PlayIcon(s: Int, color: Color) {
    Canvas(Modifier.size(s.dp)) {
        val w = size.width
        val p = Path()
        p.moveTo(w * 0.22f, w * 0.12f)
        p.lineTo(w * 0.22f, w * 0.88f)
        p.lineTo(w * 0.85f, w * 0.5f)
        p.close()
        drawPath(p, color)
    }
}

@Composable
fun BellIcon(s: Int, color: Color) {
    Canvas(Modifier.size(s.dp)) {
        val w = size.width
        val h = size.height
        val st = Stroke(width = w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val p = Path()
        p.moveTo(w * 0.22f, h * 0.70f)
        p.lineTo(w * 0.22f, h * 0.45f)
        p.cubicTo(w * 0.22f, h * 0.14f, w * 0.78f, h * 0.14f, w * 0.78f, h * 0.45f)
        p.lineTo(w * 0.78f, h * 0.70f)
        drawPath(p, color, style = st)
        drawLine(color, Offset(w * 0.12f, h * 0.72f), Offset(w * 0.88f, h * 0.72f), strokeWidth = w * 0.11f, cap = StrokeCap.Round)
        drawCircle(color, radius = w * 0.09f, center = Offset(w * 0.5f, h * 0.87f))
    }
}

@Composable
fun LockIcon(s: Int, color: Color) {
    Canvas(Modifier.size(s.dp)) {
        val w = size.width
        val h = size.height
        val sw = w * 0.11f
        drawRoundRect(color, topLeft = Offset(w * 0.18f, h * 0.46f), size = Size(w * 0.64f, h * 0.44f), cornerRadius = CornerRadius(w * 0.08f))
        drawArc(color, 180f, 180f, false, topLeft = Offset(w * 0.30f, h * 0.12f), size = Size(w * 0.40f, h * 0.48f), style = Stroke(width = sw, cap = StrokeCap.Round))
        drawLine(color, Offset(w * 0.30f, h * 0.36f), Offset(w * 0.30f, h * 0.48f), strokeWidth = sw)
        drawLine(color, Offset(w * 0.70f, h * 0.36f), Offset(w * 0.70f, h * 0.48f), strokeWidth = sw)
    }
}

@Composable
fun CheckIcon(s: Int, color: Color) {
    Canvas(Modifier.size(s.dp)) {
        val w = size.width
        val p = Path()
        p.moveTo(w * 0.2f, w * 0.52f)
        p.lineTo(w * 0.42f, w * 0.72f)
        p.lineTo(w * 0.82f, w * 0.30f)
        drawPath(p, color, style = Stroke(width = w * 0.13f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun PlayingBars(color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom, modifier = Modifier.height(14.dp)) {
        Box(Modifier.size(3.dp, 8.dp).background(color))
        Box(Modifier.size(3.dp, 14.dp).background(color))
        Box(Modifier.size(3.dp, 10.dp).background(color))
    }
}
