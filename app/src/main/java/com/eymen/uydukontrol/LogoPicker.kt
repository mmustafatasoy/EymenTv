package com.eymen.uydukontrol

import androidx.compose.foundation.Image
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLEncoder
import kotlin.math.max
import kotlin.math.min

private val tabNames = listOf("Telefondan yükle", "Hazır logolar", "USB'den seç")

@Composable
fun LogoPickerDialog(app: AppState, d: Dialog.LogoPicker) {
    val ch = d.channel
    val url = app.remoteUrl
    LaunchedEffect(url) {
        d.qr = if (url == null) null else withContext(Dispatchers.Default) {
            RemoteServer.qr(url + "&logo=" + URLEncoder.encode(ch.key, "UTF-8"))
        }
    }
    LaunchedEffect(ch.key) { app.requestLogo(ch) }
    if (d.tab == 2 && !d.scanned) LaunchedEffect(Unit) { app.scanUsb(d) }

    Row(
        Modifier.width(1140.dp).height(630.dp)
            .shadow(30.dp, RoundedCornerShape(24.dp))
            .panel(Color(0xFC0A1224), 24, Color(0x2ED6E2FF))
            .padding(26.dp),
        horizontalArrangement = Arrangement.spacedBy(26.dp)
    ) {
        Column(Modifier.width(320.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            T("LOGO EKLE", 12, C.Gold, 800, spacing = 2f)
            T("${ch.number} · ${ch.name}", 24, C.Fg, 600, display = true, maxLines = 2)
            Box(
                Modifier.fillMaxWidth().height(180.dp).panel(C.LogoBg, 14).padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                val logo = ch.logo
                if (logo != null) {
                    Image(bitmap = logo, contentDescription = ch.name, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                } else {
                    T(monogram(ch.name), 30, C.FocusInk, 700, display = true, maxLines = 2, align = TextAlign.Center)
                }
            }
            T("Bu logo kanal listesinde ve EYMEN OS ana ekranında görünür.", 14, C.Mut, 500, maxLines = 3, lineHeight = 20)
            Spacer(Modifier.weight(1f))
            HintRow(listOf(Hint("OK", "Seç"), Hint("", "Kaldır", C.Red), Hint("Geri", "Kapat")), size = 12)
        }

        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                tabNames.forEachIndexed { i, name ->
                    val active = i == d.tab
                    val focused = active && !d.inBody
                    Box(
                        Modifier.weight(1f).height(52.dp)
                            .panel(if (focused) C.Fg else if (active) C.Glass2 else Color(0x0FD6E2FF), 14, if (active && !focused) C.Gold else null, 2),
                        contentAlignment = Alignment.Center
                    ) {
                        T(name, 16, if (focused) C.FocusInk else C.Fg, 800)
                    }
                }
            }
            when (d.tab) {
                0 -> PhoneTab(d)
                1 -> ListTab(app, d)
                else -> UsbTab(d)
            }
        }
    }
}

@Composable
private fun PhoneTab(d: Dialog.LogoPicker) {
    Row(
        Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(26.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(290.dp).panel(Color.White, 16).padding(12.dp), contentAlignment = Alignment.Center) {
            val qr = d.qr
            if (qr != null) {
                Image(bitmap = qr, contentDescription = "Logo yükleme QR kodu", modifier = Modifier.fillMaxSize())
            } else {
                T("TV ağa bağlı değil", 15, C.FocusInk, 700, align = TextAlign.Center, maxLines = 2)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            T("Telefonla okut", 24, C.Fg, 600, display = true)
            T("1. Telefon ve TV aynı Wi-Fi'da olsun.", 16, C.Fg2, 600, maxLines = 2, lineHeight = 22)
            T("2. Telefon kamerasını QR koda tut.", 16, C.Fg2, 600, maxLines = 2, lineHeight = 22)
            T("3. Açılan sayfada resmi seç, TV'ye gönder.", 16, C.Fg2, 600, maxLines = 2, lineHeight = 22)
            T("Telefona uygulama kurmak gerekmez.", 14, C.Mut, 500)
        }
    }
}

@Composable
private fun ListTab(app: AppState, d: Dialog.LogoPicker) {
    val top = max(0, d.index / 5 - 1) * 5
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        T("Uygulamada hazır gelen logolar. Kanal adına uyanlar önce.", 15, C.Fg2, 600)
        for (r in 0 until 3) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (c in 0 until 5) {
                    val idx = top + r * 5 + c
                    val name = d.names.getOrNull(idx)
                    if (name != null) {
                        LogoThumb(app, name, d.inBody && idx == d.index, Modifier.weight(1f))
                    } else {
                        Spacer(Modifier.weight(1f).height(118.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LogoThumb(app: AppState, name: String, focused: Boolean, modifier: Modifier) {
    var bmp by remember(name) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(name) {
        bmp = withContext(Dispatchers.IO) { Logos.asset(app.ctx, name)?.asImageBitmap() }
    }
    Box(
        modifier.height(118.dp).panel(C.LogoBg, 14, if (focused) C.Gold else Color.Transparent, 4).padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        val b = bmp
        if (b != null) {
            Image(bitmap = b, contentDescription = name, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        } else {
            T(name.take(8).uppercase(), 12, C.FocusInk, 700, display = true, align = TextAlign.Center)
        }
    }
}

@Composable
private fun UsbTab(d: Dialog.LogoPicker) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!d.scanned) {
            T("USB bellek aranıyor…", 16, C.Fg2, 600)
            return@Column
        }
        T(if (d.files.isEmpty()) "USB bellekte resim bulunamadı. Belleği tak, sekmeyi yeniden aç." else "USB bellekteki resimler", 15, C.Fg2, 600, maxLines = 2)
        val total = d.files.size + 1
        val start = (d.index - 2).coerceIn(0, max(0, total - 6))
        for (i in start until min(total, start + 6)) {
            val focused = d.inBody && i == d.index
            val f = d.files.getOrNull(i)
            Row(
                Modifier.fillMaxWidth().height(66.dp)
                    .panel(if (focused) C.Fg else Color(0x0FD6E2FF), 13)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (f != null) {
                    FileThumb(f)
                    T(f.name, 16, if (focused) C.FocusInk else C.Fg, 700, modifier = Modifier.weight(1f))
                    T("${max(1L, f.length() / 1024)} KB", 13, if (focused) C.FocusSub else C.Fg2, 600)
                } else {
                    T("Dosya seçiciyi aç", 16, if (focused) C.FocusInk else C.Fg, 700)
                }
            }
        }
    }
}

@Composable
private fun FileThumb(f: File) {
    var bmp by remember(f) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(f) {
        bmp = withContext(Dispatchers.IO) { Logos.thumb(f)?.asImageBitmap() }
    }
    Box(Modifier.size(76.dp, 48.dp).panel(C.LogoBg, 9).padding(4.dp), contentAlignment = Alignment.Center) {
        val b = bmp
        if (b != null) Image(bitmap = b, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
    }
}
