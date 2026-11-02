package com.eymen.uydukontrol

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.util.TypedValue
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlin.math.max

@Composable
fun DialogLayer(app: AppState, d: Dialog) {
    Box(Modifier.fillMaxSize().background(Color(0xA6000000)), contentAlignment = Alignment.Center) {
        when (d) {
            is Dialog.Choice -> ChoiceDialog(d)
            is Dialog.Pin -> PinDialog(d)
            is Dialog.TextInput -> TextInputDialog(app, d)
            is Dialog.Confirm -> ConfirmDialog(d)
            is Dialog.ReminderDue -> ReminderDialog(app, d)
            is Dialog.Info -> InfoDialog(d)
            is Dialog.UpdateNotice -> UpdateNoticeDialog(d)
            is Dialog.LogoPicker -> LogoPickerDialog(app, d)
        }
    }
}

@Composable
private fun Frame(title: String, subtitle: String?, width: Int = 620, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.width(width.dp)
            .shadow(30.dp, RoundedCornerShape(22.dp))
            .panel(Color(0xFA0A1224), 22, Color(0x2ED6E2FF))
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            T(title, 24, C.Fg, 600, display = true, maxLines = 2)
            if (subtitle != null) T(subtitle, 14, C.Mut, 500, maxLines = 3)
        }
        content()
    }
}

@Composable
private fun ChoiceDialog(d: Dialog.Choice) {
    Frame(d.title, d.subtitle) {
        val n = d.items.size
        val visible = 8
        val start = (d.index - 3).coerceIn(0, max(0, n - visible))
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            d.items.drop(start).take(visible).forEachIndexed { i, label ->
                val focused = start + i == d.index
                Row(
                    Modifier.fillMaxWidth().height(50.dp)
                        .panel(if (focused) C.Fg else Color(0x0FD6E2FF), 12)
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    T(label, 16, if (focused) C.FocusInk else C.Fg, if (focused) 800 else 600)
                }
            }
        }
        HintRow(listOf(Hint("▲▼", "Seç"), Hint("OK", "Onayla"), Hint("Geri", "Kapat")), size = 12)
    }
}

@Composable
private fun PinDialog(d: Dialog.Pin) {
    Frame(d.title, d.subtitle, width = 460) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally)) {
            for (i in 0 until 4) {
                val filled = i < d.entered.length
                Box(
                    Modifier.size(52.dp, 60.dp).panel(if (filled) C.GoldSoft else Color(0x0FD6E2FF), 12, if (filled) C.Gold else C.Line),
                    contentAlignment = Alignment.Center
                ) {
                    if (filled) Box(Modifier.size(14.dp).panel(C.Gold, 7))
                }
            }
        }
        val err = d.error
        if (err != null) T(err, 14, C.Live, 700, modifier = Modifier.fillMaxWidth(), align = TextAlign.Center)
        val labels = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "Sil", "0", "Tamam")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (r in 0 until 4) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (c in 0 until 3) {
                        val idx = r * 3 + c
                        val focused = d.cell == idx
                        Box(
                            Modifier.weight(1f).height(50.dp).panel(if (focused) C.Fg else Color(0x0FD6E2FF), 12),
                            contentAlignment = Alignment.Center
                        ) {
                            T(labels[idx], if (idx == 9 || idx == 11) 15 else 20, if (focused) C.FocusInk else C.Fg, 700, display = idx != 9 && idx != 11)
                        }
                    }
                }
            }
        }
        T("Kumandanın rakam tuşlarını da kullanabilirsin", 12, C.Dim, 500, modifier = Modifier.fillMaxWidth(), align = TextAlign.Center)
    }
}

@Composable
private fun TextInputDialog(app: AppState, d: Dialog.TextInput) {
    Frame(d.title, d.hint, width = 680) {
        AndroidView(
            factory = { context ->
                EditText(context).apply {
                    setText(d.initial)
                    setSelection(text.length)
                    setSingleLine(true)
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
                    imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
                    setTextColor(android.graphics.Color.WHITE)
                    setHintTextColor(0xFF8C96AA.toInt())
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                    val bg = GradientDrawable()
                    bg.cornerRadius = 18f
                    bg.setColor(0x14D6E2FF)
                    bg.setStroke(3, 0xFFE8C27A.toInt())
                    background = bg
                    setPadding(28, 18, 28, 18)
                    val submit = {
                        if (!d.submitted) {
                            d.submitted = true
                            app.popDialog(d)
                            d.onDone(text.toString())
                        }
                    }
                    setOnEditorActionListener { _, actionId, event ->
                        if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO ||
                            actionId == EditorInfo.IME_ACTION_SEARCH ||
                            (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                        ) {
                            submit()
                            true
                        } else false
                    }
                    setOnKeyListener { _, keyCode, event ->
                        if (event.action == KeyEvent.ACTION_UP &&
                            (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)
                        ) {
                            submit()
                            true
                        } else false
                    }
                    post {
                        requestFocus()
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(64.dp)
        )
        HintRow(listOf(Hint("OK", "Klavyeyi aç"), Hint("Tamam", "Kaydet"), Hint("Geri", "Vazgeç")), size = 12)
        T("Telefon kumandası açıksa metni telefondan da gönderebilirsin.", 12, C.Dim, 500)
    }
    DisposableEffect(d) {
        onDispose {
            try {
                val imm = app.ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(app.activity.window.decorView.windowToken, 0)
            } catch (e: Exception) {
            }
        }
    }
}

@Composable
private fun ConfirmDialog(d: Dialog.Confirm) {
    Frame(d.title, d.message, width = 560) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DialogButton(d.yes, d.index == 0, Modifier.weight(1f))
            DialogButton(d.no, d.index == 1, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ReminderDialog(app: AppState, d: Dialog.ReminderDue) {
    val r = d.reminder
    Frame("${r.title} başlıyor", "${r.channelName} · ${app.time(r.start)}", width = 560) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            BellIcon(18, C.Gold)
            T("Hatırlatıcı", 14, C.Gold, 700)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DialogButton("Kanala geç", d.index == 0, Modifier.weight(1f))
            DialogButton("Kapat", d.index == 1, Modifier.weight(1f))
        }
    }
}

@Composable
private fun InfoDialog(d: Dialog.Info) {
    Frame(d.title, null, width = 680) {
        T(d.message, 16, C.Fg2, 500, maxLines = 12, lineHeight = 24)
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            DialogButton("Tamam", true, Modifier.width(160.dp))
        }
    }
}

/** Comes up by itself when a newer version is found, on whatever screen the user is. */
@Composable
private fun UpdateNoticeDialog(d: Dialog.UpdateNotice) {
    val sign = remember { Animatable(0f) }
    val site = remember { Animatable(0f) }
    LaunchedEffect(d) {
        delay(350)
        sign.animateTo(1f, tween(1700, easing = LinearEasing))
        site.animateTo(1f, tween(500))
    }
    Column(
        Modifier.width(660.dp)
            .shadow(30.dp, RoundedCornerShape(24.dp))
            .panel(Color(0xFC0A1224), 24, C.Gold, 2)
            .padding(horizontal = 34.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BrandMark(44)
        T("YENİ SÜRÜM ${d.info.version}", 13, C.Gold, 800, spacing = 2f)
        T("Güncellemeniz var", 36, C.Fg, 600, display = true)
        T("Lütfen güncelleyin. Kanallarınız ve ayarlarınız silinmez.", 16, C.Fg2, 500, align = TextAlign.Center, maxLines = 2)
        for (note in d.info.notes) T(note, 14, C.Mut, 500, align = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        SignatureLine(sign.value, 44, 340)
        Box(Modifier.alpha(site.value)) {
            T("atasoyyazilim.com", 17, C.Gold, 700, spacing = 1f)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DialogButton("Şimdi güncelle", d.index == 0, Modifier.weight(1f))
            DialogButton("Sonra", d.index == 1, Modifier.weight(1f))
        }
    }
}

@Composable
private fun DialogButton(label: String, focused: Boolean, modifier: Modifier) {
    Box(
        modifier.height(48.dp).panel(if (focused) C.Fg else Color(0x14D6E2FF), 12, if (focused) null else C.Line),
        contentAlignment = Alignment.Center
    ) {
        T(label, 16, if (focused) C.FocusInk else C.Fg, 800)
    }
}
