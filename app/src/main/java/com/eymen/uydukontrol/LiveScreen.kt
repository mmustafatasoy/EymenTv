package com.eymen.uydukontrol

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
fun LiveLayer(app: AppState) {
    val ch = app.current
    Box(Modifier.fillMaxSize()) {
        StatusCard(app)

        if (ch != null && app.showWatermark && !app.guideOpen) {
            Row(
                Modifier.align(Alignment.TopEnd).padding(top = 36.dp, end = 64.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BrandMark(24)
                Row {
                    T("EYMEN ", 16, Color(0xC8F2F4F8), 600, display = true, spacing = 2.2f)
                    T("TV", 16, Color(0xC8E8C27A), 600, display = true, spacing = 2.2f)
                }
            }
        }

        AnimatedVisibility(
            visible = app.bannerVisible && !app.guideOpen && ch != null,
            enter = fadeIn() + slideInVertically { it / 4 },
            exit = fadeOut() + slideOutVertically { it / 4 },
            modifier = Modifier.fillMaxSize()
        ) {
            if (ch != null) Banner(app, ch)
        }

        AnimatedVisibility(
            visible = app.guideOpen,
            enter = fadeIn() + slideInHorizontally { -it / 6 },
            exit = fadeOut() + slideOutHorizontally { -it / 6 },
            modifier = Modifier.fillMaxSize()
        ) {
            Guide(app)
        }

        if (app.digits.isNotEmpty()) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(top = 90.dp, end = 64.dp)
                    .panel(Color(0xF00A1224), 16, C.Gold, 2)
                    .padding(horizontal = 26.dp, vertical = 12.dp)
            ) {
                T(app.digits, 44, C.Fg, 600, display = true)
            }
        }
    }
}

@Composable
private fun StatusCard(app: AppState) {
    val pb = app.playback
    val message: String? = when {
        app.loadError != null && app.current == null -> app.loadError
        app.loading && app.current == null -> "Kanallar yükleniyor…"
        app.current != null && pb.status.isNotEmpty() && !(pb.webActive && pb.status.isEmpty()) -> pb.status
        else -> null
    }
    if (message == null) return
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 760.dp).panel(Color(0xE60A1224), 18, C.Line).padding(horizontal = 30.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            T(message, 20, C.Fg, 600, maxLines = 3, align = androidx.compose.ui.text.style.TextAlign.Center)
            if (app.current == null && app.loadError != null) {
                T("MENU › Kanal kaynakları", 14, C.Mut, 500)
            }
        }
    }
}

@Composable
private fun Banner(app: AppState, ch: Channel) {
    val now = app.now
    val program = ch.now
    val next = ch.next
    val pb = app.playback
    val badges = ArrayList<Pair<String, Boolean>>()
    if (pb.fallbackActive) badges.add("İnternetten izleniyor" to true)
    else if (ch.source == Source.WEB) badges.add("İnternet yayını" to false)
    Labels.type(ch.type)?.let { badges.add(it to false) }
    Labels.format(ch.videoFormat)?.let { badges.add(it to false) }
    pb.audioLabel()?.let { badges.add(it to false) }
    if (pb.subtitleTracks.isNotEmpty()) badges.add("Altyazı" to false)

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(420.dp)
                .background(Brush.verticalGradient(listOf(Color(0x0005080F), Color(0xBD05080F), Color(0xF505080F))))
        )
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 64.dp, end = 64.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                T(ch.number, 52, C.Gold, 600, display = true, modifier = Modifier.widthIn(min = 56.dp))
                ChannelTile(app, ch, 112, 64)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        T(ch.name, 30, C.Fg, 600, display = true, modifier = Modifier.weight(1f, fill = false))
                        LiveBadge()
                        if (app.isFav(ch)) StarIcon(18, C.Gold)
                        if (app.isLocked(ch)) LockIcon(18, C.Gold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for ((text, hl) in badges.take(5)) Chip(text, hl)
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    T(app.time(now), 44, C.Fg, 500, display = true)
                    T(app.dateLabel(now), 15, C.Mut, 500)
                }
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x1FD6E2FF)))

            Row(horizontalArrangement = Arrangement.spacedBy(48.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (program != null) {
                        T("ŞİMDİ · ${app.time(program.start)} – ${app.time(program.end)}", 12, C.Gold, 700, spacing = 2f)
                        T(program.title, 34, C.Fg, 600, display = true)
                        val desc = program.description
                        if (!desc.isNullOrBlank()) T(desc, 16, C.Mut, 500)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 6.dp)) {
                            Progress(program.progress(now), Modifier.weight(1f), height = 6)
                            val left = max(0L, (program.end - now) / 60000L)
                            T("$left dk kaldı", 14, C.Fg2, 600)
                        }
                    } else {
                        T("ŞİMDİ", 12, C.Gold, 700, spacing = 2f)
                        T(if (ch.source == Source.WEB) "İnternet yayını" else "Program bilgisi yok", 30, C.Fg, 600, display = true)
                        T(if (ch.source == Source.WEB) "Bu kanal için yayın akışı bilgisi gelmiyor." else "TV bu kanal için yayın akışı göndermedi.", 15, C.Mut, 500)
                    }
                }
                if (next != null) {
                    Row(Modifier.width(380.dp)) {
                        Box(Modifier.width(1.dp).height(110.dp).background(Color(0x1FD6E2FF)))
                        Column(Modifier.padding(start = 28.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            T("SONRA · ${app.time(next.start)} – ${app.time(next.end)}", 12, C.Mut, 700, spacing = 2f)
                            T(next.title, 22, C.Fg, 600, display = true)
                            if (app.hasReminder(ch, next)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    BellIcon(14, C.Gold)
                                    T("Hatırlatıcı kurulu", 13, C.Gold, 600)
                                }
                            } else {
                                val nd = next.description
                                if (!nd.isNullOrBlank()) T(nd, 14, C.Dim, 500)
                            }
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                HintRow(listOf(Hint("▲▼", "Kanal değiştir"), Hint("OK", "Kanal listesi")))
                Spacer(Modifier.weight(1f))
                HintRow(
                    listOf(
                        Hint("", "Yayın akışı", C.Red),
                        Hint("", "Favori", C.Green),
                        Hint("", "Ayrıntı", C.Yellow),
                        Hint("", "Ses · Altyazı", C.Blue)
                    )
                )
            }
        }
    }
}

@Composable
private fun Guide(app: AppState) {
    val tabs = app.tabs()
    val tabIndex = if (app.guideTab < tabs.size) app.guideTab else 0
    val list = app.guideList()
    val index = app.guideIndex.coerceIn(0, max(0, list.size - 1))
    val visibleRows = 7
    val start = (index - 3).coerceIn(0, max(0, list.size - visibleRows))
    val rows = list.drop(start).take(visibleRows)
    val selected = list.getOrNull(index)
    val now = app.now

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.offset(x = 540.dp).fillMaxHeight().width(220.dp)
                .background(Brush.horizontalGradient(listOf(Color(0xB305080F), Color(0x0005080F))))
        )
        Column(
            Modifier.fillMaxHeight().width(540.dp).background(Color(0xF2070B16))
                .padding(start = 64.dp, end = 28.dp, top = 36.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                T("Kanallar", 28, C.Fg, 600, display = true)
                Spacer(Modifier.weight(1f))
                T("${list.size} kanal", 14, C.Mut, 500)
            }
            Row(
                Modifier.fillMaxWidth().height(42.dp).panel(C.Glass, 12, C.Line).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                T(if (app.guideQuery.isEmpty()) "Kanal veya program ara" else "Arama: ${app.guideQuery}", 14, if (app.guideQuery.isEmpty()) C.Mut else C.Fg, 600, modifier = Modifier.weight(1f))
                ColorDot(C.Yellow, 9)
                Spacer(Modifier.width(6.dp))
                T("Sarı tuş", 12, C.Dim, 500)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val first = max(0, tabIndex - 2)
                for (i in first until tabs.size) {
                    if (i - first >= 5) break
                    TabChip(tabs[i], i == tabIndex)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                if (rows.isEmpty()) {
                    T(if (tabs.getOrNull(tabIndex) == "Favoriler") "Favori kanal yok. Yeşil tuşla ekle." else "Bu listede kanal yok.", 16, C.Mut, 500, modifier = Modifier.padding(top = 20.dp))
                }
                rows.forEachIndexed { i, ch ->
                    GuideRow(app, ch, start + i == index, ch.key == app.current?.key, now)
                }
            }
            Spacer(Modifier.weight(1f))
            HintRow(
                listOf(
                    Hint("◀ ▶", "Kategori"),
                    Hint("OK", "İzle"),
                    Hint("", "Favori", C.Green),
                    Hint("", "Yayın akışı", C.Red)
                ),
                size = 12
            )
        }

        if (selected != null) {
            Column(
                Modifier.align(Alignment.BottomEnd).padding(end = 64.dp, bottom = 36.dp).width(540.dp)
                    .panel(Color(0xE0070B16), 20, C.Line).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    T("${selected.number} · ${selected.name.uppercase(java.util.Locale("tr", "TR"))}", 12, C.Gold, 700, spacing = 1.8f)
                    if (selected.source == Source.WEB) Chip("İnternet")
                    if (app.isLocked(selected)) LockIcon(14, C.Gold)
                }
                val p = selected.now
                T(p?.title ?: selected.name, 28, C.Fg, 600, display = true)
                if (p != null) {
                    val d = p.description
                    T("${app.time(p.start)} – ${app.time(p.end)}" + (if (!d.isNullOrBlank()) " · $d" else ""), 15, C.Mut, 500, maxLines = 2)
                    Progress(p.progress(now), Modifier.fillMaxWidth(), height = 4)
                    val nx = selected.next
                    if (nx != null) T("Sonra ${app.time(nx.start)} · ${nx.title}", 14, C.Dim, 500)
                } else {
                    T(if (selected.source == Source.WEB) "İnternet yayını · yayın akışı bilgisi yok" else "Yayın akışı bilgisi yok", 15, C.Mut, 500)
                }
                if (selected.key != app.current?.key && app.current != null) {
                    T("Arkada ${app.current?.name} oynamaya devam ediyor · OK ile geç", 12, C.Dim, 500)
                }
            }
        }
    }
}

@Composable
private fun GuideRow(app: AppState, ch: Channel, focused: Boolean, playing: Boolean, now: Long) {
    val fav = app.isFav(ch)
    val bg = when {
        focused -> C.Fg
        playing -> C.GoldSoft
        else -> Color.Transparent
    }
    val border = if (!focused && playing) Color(0x47E8C27A) else null
    val textColor = if (focused) C.FocusInk else C.Fg
    val subColor = if (focused) C.FocusSub else C.Mut
    Row(
        Modifier.fillMaxWidth()
            .scale(if (focused) 1.03f else 1f)
            .then(if (focused) Modifier.shadow(16.dp, RoundedCornerShape(14.dp)) else Modifier)
            .height(if (focused) 60.dp else 56.dp)
            .panel(bg, if (focused) 14 else 12, border)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        T(ch.number, 15, if (focused) C.FocusInk else if (playing) C.Gold else C.Dim, 600, display = true, modifier = Modifier.width(38.dp))
        ChannelTile(app, ch, 64, 38, onLight = focused)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            T(ch.name, 16, textColor, if (focused) 800 else 700)
            val p = ch.now
            T(
                if (p != null) "${p.title} · ${app.time(p.start)}–${app.time(p.end)}" else if (ch.source == Source.WEB) "İnternet yayını" else "Program bilgisi yok",
                13,
                subColor,
                500
            )
            if (p != null) {
                Progress(
                    p.progress(now),
                    Modifier.fillMaxWidth(),
                    color = if (focused) C.FocusInk else if (playing) C.Gold else Color(0xFF6C7891),
                    track = if (focused) Color(0x240A1224) else Color(0x1FD6E2FF),
                    height = 3
                )
            }
        }
        if (app.isLocked(ch)) LockIcon(15, if (focused) C.FocusInk else C.Gold)
        if (fav) StarIcon(16, if (focused) C.FocusInk else C.Gold)
        if (playing && !focused) PlayingBars(C.Gold)
    }
}
