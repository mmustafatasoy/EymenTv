package com.eymen.uydukontrol

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
fun HomeScreen(app: AppState) {
    val cur = app.current
    val now = app.now
    val favs = app.homeFavorites()
    val reminders = app.sortedReminders().take(3)
    val favLabel = if (favs.any { app.isFav(it) }) "Favori kanallar" else "Kanallar"

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xF505080F), Color(0xD905080F), Color(0x5905080F), Color(0x2605080F)))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.35f to Color(0x0005080F), 0.55f to Color(0xE605080F), 1f to Color(0xFA05080F))))

        Column(
            Modifier.fillMaxSize().padding(horizontal = 64.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Top bar (row 0)
            Row(Modifier.height(40.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BrandMark(28)
                    Row {
                        T("EYMEN ", 18, C.Fg, 600, display = true, spacing = 2.5f)
                        T("TV", 18, C.Gold, 600, display = true, spacing = 2.5f)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    app.homeNav.forEachIndexed { i, label ->
                        val focused = app.homeRow == 0 && app.homeCol == i
                        Box(
                            Modifier.panel(if (focused) C.Fg else Color.Transparent, 999).padding(horizontal = 16.dp, vertical = 9.dp)
                        ) {
                            T(label, 15, if (focused) C.FocusInk else C.Mut, 700)
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    T(app.dateLabel(now), 14, C.Mut, 500)
                    T(app.time(now), 24, C.Fg, 500, display = true)
                }
            }

            // Hero (row 1)
            Column(Modifier.width(620.dp).padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (cur != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        T("KALDIĞIN YERDEN · ${cur.number} · ${cur.name.uppercase(java.util.Locale("tr", "TR"))}", 12, C.Gold, 700, spacing = 2f)
                        LiveBadge()
                    }
                    val p = cur.now
                    T(p?.title ?: cur.name, 38, C.Fg, 600, display = true)
                    if (p != null) {
                        Row(Modifier.width(440.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Progress(p.progress(now), Modifier.weight(1f), height = 5)
                            T("${app.time(p.start)} – ${app.time(p.end)} · ${max(0L, (p.end - now) / 60000L)} dk kaldı", 14, C.Fg2, 600)
                        }
                    }
                } else {
                    T("EYMEN TV", 12, C.Gold, 700, spacing = 2f)
                    T(if (app.loading) "Kanallar yükleniyor…" else "Bir kanal seç", 38, C.Fg, 600, display = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 12.dp)) {
                    HeroButton("İzlemeye devam", app.homeRow == 1 && app.homeCol == 0, play = true)
                    HeroButton("Yayın akışı", app.homeRow == 1 && app.homeCol == 1, play = false)
                }
            }

            Spacer(Modifier.weight(1f))

            // Favourites (row 2)
            if (favs.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        T(favLabel, 19, C.Fg, 600, display = true)
                        T("${favs.size} kanal · Yeşil tuşla favori ekle", 13, C.Dim, 500)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        favs.forEachIndexed { i, ch ->
                            FavCard(app, ch, app.homeRow == 2 && app.homeCol == i, ch.key == cur?.key, now, Modifier.weight(1f))
                        }
                        for (k in favs.size until 6) Spacer(Modifier.weight(1f))
                    }
                }
            }

            // Reminders (row 3)
            if (reminders.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        T("Hatırlatıcılar", 19, C.Fg, 600, display = true)
                        T("Program başlarken ekranda sorulur", 13, C.Dim, 500)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        reminders.forEachIndexed { i, r ->
                            val focused = app.homeRow == 3 && app.homeCol == i
                            Row(
                                Modifier.weight(1f).height(64.dp)
                                    .scale(if (focused) 1.03f else 1f)
                                    .panel(if (focused) C.Fg else Color(0xB30A1224), 14, if (focused) null else C.Line)
                                    .padding(horizontal = 18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                T(app.time(r.start), 22, if (focused) C.FocusInk else C.Gold, 600, display = true)
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    T(r.title, 15, if (focused) C.FocusInk else C.Fg, 800)
                                    T("${r.channelName} · ${app.dayTime(r.start)}", 12, if (focused) C.FocusSub else C.Mut, 500)
                                }
                            }
                        }
                        for (k in reminders.size until 3) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroButton(label: String, focused: Boolean, play: Boolean) {
    Row(
        Modifier.height(48.dp)
            .scale(if (focused) 1.04f else 1f)
            .then(if (focused) Modifier.shadow(14.dp, RoundedCornerShape(14.dp)) else Modifier)
            .panel(if (focused) C.Fg else Color(0x6605080F), 14, if (focused) null else Color(0x38D6E2FF))
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (play) PlayIcon(14, if (focused) C.FocusInk else C.Fg)
        T(label, 16, if (focused) C.FocusInk else C.Fg, 800)
    }
}

@Composable
private fun FavCard(app: AppState, ch: Channel, focused: Boolean, playing: Boolean, now: Long, modifier: Modifier) {
    Column(
        modifier
            .scale(if (focused) 1.05f else 1f)
            .then(if (focused) Modifier.shadow(16.dp, RoundedCornerShape(14.dp)) else Modifier)
            .panel(if (focused) C.Fg else Color(0xB30A1224), 14, if (focused) null else if (playing) Color(0x66E8C27A) else C.Line)
    ) {
        Box(Modifier.fillMaxWidth().height(54.dp).background(if (focused) Color(0xFFE2E6EE) else Color(0xFF131C36)), contentAlignment = Alignment.Center) {
            ChannelTile(app, ch, 92, 40, onLight = focused)
        }
        Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 9.dp, bottom = 11.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                T(ch.number, 13, if (focused) C.FocusInk else if (playing) C.Gold else C.Dim, 800)
                T(if (playing) "İzleniyor" else ch.name, 13, if (focused) C.FocusInk else C.Fg, 800)
            }
            val p = ch.now
            T(p?.title ?: (if (ch.source == Source.WEB) "İnternet yayını" else "Program bilgisi yok"), 12, if (focused) C.FocusSub else C.Mut, 500)
            Progress(
                p?.progress(now) ?: 0f,
                Modifier.fillMaxWidth(),
                color = if (focused) C.FocusInk else if (playing) C.Gold else Color(0xFF6C7891),
                track = if (focused) Color(0x240A1224) else Color(0x1FD6E2FF),
                height = 3
            )
        }
    }
}
