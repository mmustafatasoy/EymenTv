package com.eymen.uydukontrol

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.max

private val sectionHelp = listOf(
    "Sırasını değiştir, sil, gizle, adını ve logosunu değiştir. Numaralar senin sırana göre yenilenir.",
    "Kanal logoları, köşe logosu, kanal bandının ekranda kalma süresi ve açılış imzası.",
    "İzlediğin kanalın ses dili ve altyazısı.",
    "Seçtiğin kanalları 4 haneli PIN ile kilitle. Kanal yönetiminden bir kanal seçip kilitleyebilirsin.",
    "Telefonun kamerasıyla QR kodu okut, kumanda telefonun tarayıcısında açılır. TV ile telefon aynı Wi-Fi ağında olmalı.",
    "Hazır EYMEN listesi her gün GitHub'da denenir; bu TV'de açılmayan kanallar otomatik gizlenir. Uydu sinyali kesilince aynı kanal internetten devam eder.",
    "Yayın akışından kurduğun hatırlatıcılar. Silmek için OK'ye bas.",
    "Yeni sürümler GitHub Releases'tan indirilip kurulur.",
    "EYMEN TV 2.0 · Atasoy Yazılım"
)

@Composable
fun SettingsScreen(app: AppState) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(listOf(Color(0xFF101A33), Color(0xFF070B16), Color(0xFF05080F)), center = Offset(1800f, 0f), radius = 2400f)
        )
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 64.dp, vertical = 36.dp), horizontalArrangement = Arrangement.spacedBy(36.dp)) {
            Column(Modifier.width(300.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                T("Ayarlar", 28, C.Fg, 600, display = true, modifier = Modifier.padding(bottom = 14.dp))
                app.sections.forEachIndexed { i, name ->
                    val active = i == app.setSection
                    val focused = active && !app.setInPane
                    Row(
                        Modifier.fillMaxWidth().height(48.dp)
                            .then(if (focused) Modifier.shadow(12.dp, RoundedCornerShape(14.dp)) else Modifier)
                            .panel(if (focused) C.Fg else if (active) C.Glass2 else Color.Transparent, 14)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SectionIcon(i, if (focused) C.FocusInk else if (active) C.Gold else C.Fg2)
                        T(name, 15, if (focused) C.FocusInk else if (active) C.Fg else C.Fg2, if (focused) 800 else 700, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.weight(1f))
                T("EYMEN TV ${BuildConfig.VERSION_NAME} · atasoyyazilim.com", 12, C.Dim, 500, modifier = Modifier.padding(start = 16.dp))
            }

            Column(
                Modifier.weight(1f).fillMaxHeight().panel(Color(0x0BD6E2FF), 22, Color(0x14D6E2FF)).padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    T(app.sections[app.setSection], 24, C.Fg, 600, display = true)
                    T(sectionHelp.getOrElse(app.setSection) { "" }, 14, C.Mut, 500, maxLines = 2)
                }
                if (app.setSection == 0) ChannelManager(app) else ItemsPane(app)
            }
        }
    }
}

@Composable
private fun ItemsPane(app: AppState) {
    val items = app.settingItems(app.setSection)
    Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items.forEachIndexed { i, item ->
                val focused = app.setInPane && i == app.setIndex
                Row(
                    Modifier.fillMaxWidth().height(52.dp)
                        .then(if (focused) Modifier.shadow(12.dp, RoundedCornerShape(12.dp)) else Modifier)
                        .panel(if (focused) C.Fg else Color(0x0AD6E2FF), 12)
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    T(
                        item.label, 15,
                        if (focused) C.FocusInk else if (item.enabled) C.Fg else C.Mut,
                        if (focused) 800 else 700,
                        modifier = Modifier.weight(1f)
                    )
                    val v = item.value
                    if (v != null) {
                        val on = v == "Açık"
                        if (on || v == "Kapalı") {
                            Toggle(on, focused)
                        } else {
                            T(v, 14, if (focused) C.FocusSub else C.Fg2, 600)
                        }
                    }
                }
            }
        }
        if (app.setSection == 4) {
            Column(
                Modifier.width(250.dp).panel(Color(0x0FD6E2FF), 18, C.Line).padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val qr = app.remoteQr
                val url = app.remoteUrl
                if (app.remoteEnabled && qr != null && url != null) {
                    Box(Modifier.size(200.dp).panel(Color.White, 12).padding(8.dp)) {
                        Image(bitmap = qr, contentDescription = "Telefon kumandası QR kodu", modifier = Modifier.fillMaxSize())
                    }
                    T("Telefonla okut", 15, C.Fg, 700)
                    T(url.substringBefore("/?"), 12, C.Mut, 500)
                } else if (app.remoteEnabled) {
                    T("Ağ bağlantısı bekleniyor", 15, C.Fg, 700, maxLines = 2)
                    T("TV internete bağlı değilse kumanda adresi oluşmaz.", 12, C.Mut, 500, maxLines = 3)
                } else {
                    T("Telefon kumandası kapalı", 15, C.Fg, 700, maxLines = 2)
                }
            }
        }
    }
}

@Composable
private fun ChannelManager(app: AppState) {
    val list = app.all.toList()
    val index = app.setIndex.coerceIn(0, max(0, list.size - 1))
    val visibleRows = 7
    val start = (index - 3).coerceIn(0, max(0, list.size - visibleRows))
    val rows = list.drop(start).take(visibleRows)
    val moving = app.moving

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        HintRow(
            listOf(
                Hint("OK", "İşlemler"),
                Hint("", "İşaretle", C.Green),
                Hint("", "Gizle / göster", C.Red),
                Hint("", "Taşı", C.Yellow),
                Hint("", "Sil", C.Blue)
            ),
            size = 12
        )
        Spacer(Modifier.weight(1f))
        T("${list.size} kanal · ${app.hidden.size} gizli · ${app.trash.size} silinen" + (if (app.checked.isNotEmpty()) " · ${app.checked.size} işaretli" else ""), 13, C.Mut, 600)
    }

    Row(Modifier.padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.width(18.dp))
        T("NO", 11, C.Dim, 800, spacing = 1.5f, modifier = Modifier.width(44.dp))
        T("KANAL", 11, C.Dim, 800, spacing = 1.5f, modifier = Modifier.weight(1f))
        T("KATEGORİ", 11, C.Dim, 800, spacing = 1.5f, modifier = Modifier.width(110.dp))
        T("DURUM", 11, C.Dim, 800, spacing = 1.5f, modifier = Modifier.width(80.dp))
        T("KİLİT", 11, C.Dim, 800, spacing = 1.5f, modifier = Modifier.width(40.dp))
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (rows.isEmpty()) T("Kanal yok. Kanal kaynakları bölümünden liste ekleyebilirsin.", 15, C.Mut, 500)
        rows.forEachIndexed { i, ch ->
            val focused = app.setInPane && start + i == index
            val isMoving = moving != null && moving.key == ch.key
            val isHidden = app.isHidden(ch)
            val isChecked = app.checked.contains(ch.key)
            val bg = when {
                isMoving -> Color(0x1FE8C27A)
                focused -> C.Fg
                else -> Color(0x0AD6E2FF)
            }
            val ink = if (focused && !isMoving) C.FocusInk else C.Fg
            Row(
                Modifier.fillMaxWidth().height(48.dp)
                    .then(if (focused || isMoving) Modifier.shadow(12.dp, RoundedCornerShape(12.dp)) else Modifier)
                    .panel(bg, 12, if (isMoving) C.Gold else null, 2)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    Modifier.size(18.dp).panel(if (isChecked) C.Gold else Color.Transparent, 5, if (isChecked) null else (if (focused && !isMoving) C.FocusSub else C.Dim), 2),
                    contentAlignment = Alignment.Center
                ) {
                    if (isChecked) CheckIcon(14, C.GoldInk)
                }
                T(ch.number, 14, if (isMoving) C.Gold else if (focused) C.FocusInk else C.Mut, 600, display = true, modifier = Modifier.width(44.dp))
                Row(Modifier.weight(1f).alpha(if (isHidden) 0.5f else 1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ChannelTile(app, ch, 48, 30, onLight = focused && !isMoving)
                    T(ch.name, 15, ink, if (focused) 800 else 700, modifier = Modifier.weight(1f, fill = false))
                    if (ch.source == Source.WEB) Chip("İnternet")
                    if (app.isFav(ch)) StarIcon(14, if (focused && !isMoving) C.FocusInk else C.Gold)
                }
                if (isMoving) {
                    T(if (app.moveEntry.isEmpty()) "Numara yaz ya da ▲▼ · OK bırak" else "Yeni numara: ${app.moveEntry} · OK", 13, C.Gold, 700)
                } else {
                    T(ch.category, 13, if (focused) C.FocusSub else C.Fg2, 600, modifier = Modifier.width(110.dp))
                    T(if (isHidden) "Gizli" else "Görünür", 13, if (isHidden) C.Live else if (focused) C.FocusSub else C.Fg2, 700, modifier = Modifier.width(80.dp))
                    Box(Modifier.width(40.dp)) {
                        if (app.isLocked(ch)) LockIcon(16, if (focused) C.FocusInk else C.Gold) else T("—", 13, C.Dim, 500)
                    }
                }
            }
        }
    }
}

@Composable
private fun Toggle(on: Boolean, focused: Boolean) {
    Box(
        Modifier.size(42.dp, 24.dp).panel(if (on) C.Gold else if (focused) Color(0x330A1224) else Color(0x33D6E2FF), 12),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(Modifier.padding(horizontal = 3.dp).size(18.dp).panel(if (on) C.FocusInk else if (focused) C.FocusSub else C.Mut, 9))
    }
}

/** Small line icons for the settings menu. */
@Composable
private fun SectionIcon(index: Int, color: Color) {
    Canvas(Modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        val sw = w * 0.11f
        val st = Stroke(width = sw, cap = StrokeCap.Round)
        when (index) {
            0 -> {
                for (k in 0..2) {
                    val y = h * (0.25f + k * 0.25f)
                    drawCircle(color, radius = sw * 0.6f, center = Offset(w * 0.12f, y))
                    drawLine(color, Offset(w * 0.32f, y), Offset(w * 0.92f, y), strokeWidth = sw, cap = StrokeCap.Round)
                }
            }
            1 -> {
                drawRoundRect(color, topLeft = Offset(w * 0.08f, h * 0.15f), size = Size(w * 0.84f, h * 0.55f), cornerRadius = CornerRadius(w * 0.08f), style = st)
                drawLine(color, Offset(w * 0.32f, h * 0.88f), Offset(w * 0.68f, h * 0.88f), strokeWidth = sw, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.5f, h * 0.70f), Offset(w * 0.5f, h * 0.88f), strokeWidth = sw)
            }
            2 -> {
                drawRect(color, topLeft = Offset(w * 0.12f, h * 0.36f), size = Size(w * 0.18f, h * 0.28f))
                drawLine(color, Offset(w * 0.30f, h * 0.36f), Offset(w * 0.52f, h * 0.16f), strokeWidth = sw, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.30f, h * 0.64f), Offset(w * 0.52f, h * 0.84f), strokeWidth = sw, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.52f, h * 0.16f), Offset(w * 0.52f, h * 0.84f), strokeWidth = sw, cap = StrokeCap.Round)
                drawArc(color, -50f, 100f, false, topLeft = Offset(w * 0.45f, h * 0.25f), size = Size(w * 0.4f, h * 0.5f), style = st)
            }
            3 -> {
                drawRoundRect(color, topLeft = Offset(w * 0.18f, h * 0.46f), size = Size(w * 0.64f, h * 0.44f), cornerRadius = CornerRadius(w * 0.08f), style = st)
                drawArc(color, 180f, 180f, false, topLeft = Offset(w * 0.30f, h * 0.12f), size = Size(w * 0.40f, h * 0.48f), style = st)
            }
            4 -> {
                drawRoundRect(color, topLeft = Offset(w * 0.28f, h * 0.06f), size = Size(w * 0.44f, h * 0.88f), cornerRadius = CornerRadius(w * 0.1f), style = st)
                drawLine(color, Offset(w * 0.44f, h * 0.78f), Offset(w * 0.56f, h * 0.78f), strokeWidth = sw, cap = StrokeCap.Round)
            }
            5 -> {
                drawLine(color, Offset(w * 0.5f, h * 0.5f), Offset(w * 0.5f, h * 0.95f), strokeWidth = sw, cap = StrokeCap.Round)
                drawCircle(color, radius = w * 0.09f, center = Offset(w * 0.5f, h * 0.42f))
                drawArc(color, 135f, 90f, false, topLeft = Offset(w * 0.18f, h * 0.10f), size = Size(w * 0.64f, h * 0.64f), style = st)
                drawArc(color, -45f, 90f, false, topLeft = Offset(w * 0.18f, h * 0.10f), size = Size(w * 0.64f, h * 0.64f), style = st)
            }
            6 -> {
                drawArc(color, 180f, 180f, false, topLeft = Offset(w * 0.22f, h * 0.14f), size = Size(w * 0.56f, h * 0.56f), style = st)
                drawLine(color, Offset(w * 0.22f, h * 0.42f), Offset(w * 0.22f, h * 0.70f), strokeWidth = sw)
                drawLine(color, Offset(w * 0.78f, h * 0.42f), Offset(w * 0.78f, h * 0.70f), strokeWidth = sw)
                drawLine(color, Offset(w * 0.12f, h * 0.72f), Offset(w * 0.88f, h * 0.72f), strokeWidth = sw, cap = StrokeCap.Round)
                drawCircle(color, radius = w * 0.08f, center = Offset(w * 0.5f, h * 0.87f))
            }
            7 -> {
                drawArc(color, 20f, 290f, false, topLeft = Offset(w * 0.14f, h * 0.14f), size = Size(w * 0.72f, h * 0.72f), style = st)
                drawLine(color, Offset(w * 0.86f, h * 0.18f), Offset(w * 0.86f, h * 0.42f), strokeWidth = sw, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.86f, h * 0.42f), Offset(w * 0.62f, h * 0.42f), strokeWidth = sw, cap = StrokeCap.Round)
            }
            else -> {
                drawCircle(color, radius = w * 0.42f, center = Offset(w * 0.5f, h * 0.5f), style = st)
                drawLine(color, Offset(w * 0.5f, h * 0.46f), Offset(w * 0.5f, h * 0.72f), strokeWidth = sw, cap = StrokeCap.Round)
                drawCircle(color, radius = sw * 0.6f, center = Offset(w * 0.5f, h * 0.30f))
            }
        }
    }
}
