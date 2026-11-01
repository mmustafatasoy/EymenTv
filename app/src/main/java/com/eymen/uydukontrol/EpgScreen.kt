package com.eymen.uydukontrol

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min

private const val HOUR = 3600000L
private const val TIMELINE_X = 204f
private const val TIMELINE_W = 948f

@Composable
fun EpgScreen(app: AppState) {
    val list = app.epgList()
    val row = app.epgRow.coerceIn(0, max(0, list.size - 1))
    val focusedChannel = list.getOrNull(row)
    val sel = if (focusedChannel != null) app.epgSelected(focusedChannel) else null
    val now = app.now
    val ws = app.epgWindowStart
    val we = ws + 2 * HOUR
    val ppm = TIMELINE_W / (2 * HOUR).toFloat()
    val visibleRows = 6
    val start = (row - 2).coerceIn(0, max(0, list.size - visibleRows))
    val rows = list.drop(start).take(visibleRows)

    Box(Modifier.fillMaxSize()) {
        // Background with a window for the live preview (the TV picture sits underneath).
        Canvas(Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
            drawRect(
                Brush.radialGradient(
                    listOf(Color(0xFF101A33), Color(0xFF070B16), Color(0xFF05080F)),
                    center = Offset(size.width * 0.7f, 0f),
                    radius = size.width * 0.9f
                )
            )
            drawRect(
                Color.Transparent,
                topLeft = Offset(832.dp.toPx(), 36.dp.toPx()),
                size = Size(384.dp.toPx(), 216.dp.toPx()),
                blendMode = BlendMode.Clear
            )
        }

        // Preview frame
        Box(
            Modifier.offset(832.dp, 36.dp).size(384.dp, 216.dp)
                .border(1.dp, Color(0x29D6E2FF), RoundedCornerShape(4.dp))
        ) {
            val cur = app.current
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(70.dp)
                    .background(Brush.verticalGradient(listOf(Color(0x0005080F), Color(0xD905080F))))
            )
            if (cur != null) {
                Row(
                    Modifier.align(Alignment.BottomStart).padding(start = 14.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    T(cur.number, 13, C.Gold, 700)
                    T(cur.name, 13, C.Fg, 700)
                    LiveBadge()
                }
            }
            T(
                app.time(now), 16, C.Fg, 500, display = true,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 12.dp)
            )
        }

        // Header and selected programme
        Column(
            Modifier.offset(64.dp, 36.dp).width(720.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                T("Yayın Akışı", 26, C.Fg, 600, display = true)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val dayIndex = dayDiff(System.currentTimeMillis(), app.epgFocus)
                    for (d in 0..3) {
                        val label = when (d) {
                            0 -> "Bugün"
                            1 -> "Yarın"
                            else -> app.shortDay(System.currentTimeMillis() + d * 24 * HOUR)
                        }
                        TabChip(label, d == dayIndex)
                    }
                }
            }
            if (focusedChannel != null) {
                T(
                    if (sel != null) "${focusedChannel.number} · ${focusedChannel.name.uppercase(java.util.Locale("tr", "TR"))} · ${app.time(sel.start)} – ${app.time(sel.end)}"
                    else "${focusedChannel.number} · ${focusedChannel.name.uppercase(java.util.Locale("tr", "TR"))}",
                    12, C.Gold, 700, spacing = 2f
                )
                T(sel?.title ?: (if (focusedChannel.source == Source.WEB) "İnternet yayını" else "Program bilgisi yok"), 32, C.Fg, 600, display = true)
                val info = when {
                    sel == null -> "Bu kanal için yayın akışı bilgisi yok. OK ile kanala geç."
                    sel.isLive(now) -> (sel.description ?: "Şu an yayında") + " · ${max(0L, (sel.end - now) / 60000L)} dk kaldı"
                    else -> (sel.description ?: "") + (if (sel.description.isNullOrBlank()) "" else " · ") + startsIn(sel.start - now)
                }
                T(info, 15, C.Mut, 500, maxLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 6.dp)) {
                    val upcoming = sel != null && sel.start > now
                    val has = sel != null && app.hasReminder(focusedChannel, sel)
                    Row(
                        Modifier.height(42.dp).panel(C.Fg, 12).padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (upcoming) BellIcon(15, C.FocusInk) else PlayIcon(13, C.FocusInk)
                        T(
                            when {
                                !upcoming -> "OK · İzle"
                                has -> "OK · Hatırlatıcıyı kaldır"
                                else -> "OK · Hatırlatıcı kur"
                            },
                            14, C.FocusInk, 800
                        )
                    }
                    if (has) {
                        Row(
                            Modifier.height(42.dp).panel(C.GoldSoft, 12, Color(0x59E8C27A)).padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BellIcon(14, C.Gold)
                            T("Hatırlatıcı kurulu", 14, C.Gold, 700)
                        }
                    }
                }
            }
        }

        // Grid
        Box(Modifier.offset(64.dp, 280.dp).size(1152.dp, 368.dp)) {
            T("KANAL", 12, C.Dim, 700, spacing = 1.5f)
            for (k in 0..3) {
                val t = ws + k * 30 * 60000L
                val x = TIMELINE_X + k * (TIMELINE_W / 4f)
                T(app.time(t), 13, C.Mut, 700, modifier = Modifier.offset(x = x.dp))
                if (k > 0) Box(Modifier.offset(x = x.dp, y = 24.dp).width(1.dp).height(344.dp).background(Color(0x12D6E2FF)))
            }
            if (rows.isEmpty()) {
                T("Bu listede kanal yok.", 16, C.Mut, 500, modifier = Modifier.offset(y = 48.dp))
            }
            rows.forEachIndexed { i, ch ->
                EpgRow(app, ch, start + i == row, sel, (32 + i * 56).toFloat(), ws, we, ppm, now)
            }
            if (now in ws until we) {
                val x = TIMELINE_X + (now - ws) * ppm
                Box(Modifier.offset(x = (x - 1f).dp, y = 24.dp).width(2.dp).height(344.dp).background(C.Gold))
                Box(
                    Modifier.offset(x = (x - 24f).dp).size(48.dp, 20.dp).panel(C.Gold, 6),
                    contentAlignment = Alignment.Center
                ) {
                    T(app.time(now), 12, C.GoldInk, 800)
                }
            }
        }

        Box(Modifier.align(Alignment.BottomStart).padding(start = 64.dp, bottom = 32.dp)) {
            HintRow(
                listOf(
                    Hint("▲▼", "Kanal"),
                    Hint("◀ ▶", "Program"),
                    Hint("OK", "İzle / Hatırlat"),
                    Hint("", "Önceki gün", C.Red),
                    Hint("", "Sonraki gün", C.Yellow),
                    Hint("", "Kategori: ${app.tabs().getOrElse(app.epgTab) { "Tümü" }}", C.Blue)
                )
            )
        }
    }
}

@Composable
private fun EpgRow(
    app: AppState,
    ch: Channel,
    focused: Boolean,
    selected: Program?,
    top: Float,
    ws: Long,
    we: Long,
    ppm: Float,
    now: Long
) {
    Box(Modifier.offset(y = top.dp).size(1152.dp, 52.dp)) {
        Row(
            Modifier.width(196.dp).fillMaxHeight().panel(if (focused) C.Glass2 else Color.Transparent, 12).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            T(ch.number, 14, if (focused) C.Gold else C.Dim, 600, display = true, modifier = Modifier.width(30.dp))
            ChannelTile(app, ch, 52, 32)
            T(ch.name, 14, C.Fg, 700, modifier = Modifier.weight(1f))
        }
        val programs = app.epgPrograms(ch).filter { it.end > ws && it.start < we }
        if (programs.isEmpty()) {
            EpgCell(
                x = TIMELINE_X + 2f,
                w = TIMELINE_W - 4f,
                title = if (ch.source == Source.WEB) "İnternet yayını · akış bilgisi yok" else "Program bilgisi yok",
                time = "",
                focused = focused,
                live = false,
                progress = 0f,
                bell = false
            )
        } else {
            for (p in programs) {
                val l = (max(p.start, ws) - ws) * ppm
                val r = (min(p.end, we) - ws) * ppm
                val w = r - l - 4f
                if (w < 8f) continue
                val live = p.isLive(now)
                val pct = if (live) ((now - max(p.start, ws)).toFloat() / (min(p.end, we) - max(p.start, ws)).toFloat()).coerceIn(0f, 1f) else 0f
                EpgCell(
                    x = TIMELINE_X + l + 2f,
                    w = w,
                    title = (if (p.start < ws) "‹ " else "") + p.title,
                    time = "${app.time(p.start)} – ${app.time(p.end)}",
                    focused = focused && selected === p,
                    live = live,
                    progress = pct,
                    bell = app.hasReminder(ch, p)
                )
            }
        }
    }
}

@Composable
private fun EpgCell(
    x: Float,
    w: Float,
    title: String,
    time: String,
    focused: Boolean,
    live: Boolean,
    progress: Float,
    bell: Boolean
) {
    val bg = when {
        focused -> C.Fg
        live -> Color(0x17D6E2FF)
        else -> Color(0x0BD6E2FF)
    }
    Box(
        Modifier.offset(x = x.dp)
            .size(w.dp, 52.dp)
            .then(if (focused) Modifier.shadow(12.dp, RoundedCornerShape(10.dp)) else Modifier)
            .panel(bg, 10)
    ) {
        if (live && !focused && progress > 0f) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(progress).background(Color(0x1FE8C27A)))
        }
        Column(
            Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                T(title, 14, if (focused) C.FocusInk else C.Fg, 700, modifier = Modifier.weight(1f, fill = false))
                if (bell) BellIcon(12, if (focused) C.FocusInk else C.Gold)
            }
            if (time.isNotEmpty()) T(time, 12, if (focused) C.FocusSub else C.Mut, 500)
        }
    }
}

private fun dayDiff(from: Long, to: Long): Int {
    val a = Calendar.getInstance()
    a.timeInMillis = from
    val b = Calendar.getInstance()
    b.timeInMillis = to
    for (c in listOf(a, b)) {
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
    }
    return ((b.timeInMillis - a.timeInMillis + HOUR) / (24 * HOUR)).toInt()
}

private fun startsIn(ms: Long): String {
    val min = ms / 60000L
    return when {
        min < 1 -> "Birazdan başlıyor"
        min < 60 -> "$min dk sonra başlıyor"
        min < 24 * 60 -> "${min / 60} sa ${min % 60} dk sonra başlıyor"
        else -> "${min / (24 * 60)} gün sonra"
    }
}
