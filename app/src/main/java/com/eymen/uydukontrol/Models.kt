package com.eymen.uydukontrol

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap

enum class Source { TV, WEB }

enum class Screen { SPLASH, LIVE, HOME, EPG, SETTINGS }

class Program(
    val title: String,
    val description: String?,
    val start: Long,
    val end: Long
) {
    fun isLive(now: Long): Boolean = start <= now && end > now
    fun progress(now: Long): Float =
        if (end > start) ((now - start).toFloat() / (end - start).toFloat()).coerceIn(0f, 1f) else 0f
}

class Channel(
    val key: String,
    val source: Source,
    val tvId: Long,
    val inputId: String?,
    val numRaw: String?,
    val originalName: String,
    val streamUrl: String?,
    val videoFormat: String?,
    val type: String?,
    val isRadio: Boolean,
    val logoUrl: String?,
    val groupTitle: String?
) {
    var number: String = numRaw ?: ""
    var name by mutableStateOf(originalName)
    var category by mutableStateOf("Genel")
    var logo by mutableStateOf<ImageBitmap?>(null)
    var logoRequested = false
    var web: Channel? = null
    var matched = false
    var now by mutableStateOf<Program?>(null)
    var next by mutableStateOf<Program?>(null)
    var epgLoadedAt = 0L
    val norm: String = Names.norm(originalName)

    /** Same format v0.12 used, so existing favourites keep working after the update. */
    val favoriteKey: String
        get() = if (source == Source.TV) "$inputId|$numRaw|$originalName" else "web|$originalName"

    val sourceLabel: String
        get() = if (source == Source.TV) "Uydu" else "İnternet"
}

class Reminder(
    val channelKey: String,
    val channelName: String,
    val title: String,
    val start: Long
)

sealed class Dialog {
    class Choice(
        val title: String,
        val items: List<String>,
        val subtitle: String? = null,
        val onPick: (Int) -> Unit
    ) : Dialog() {
        var index by mutableStateOf(0)
    }

    class Pin(
        val title: String,
        val subtitle: String,
        val validate: (String) -> Boolean
    ) : Dialog() {
        var entered by mutableStateOf("")
        var cell by mutableStateOf(0)
        var error by mutableStateOf<String?>(null)
    }

    class TextInput(
        val title: String,
        val initial: String,
        val hint: String,
        val onDone: (String) -> Unit
    ) : Dialog() {
        var submitted = false
    }

    class Confirm(
        val title: String,
        val message: String,
        val yes: String,
        val no: String,
        val onYes: () -> Unit
    ) : Dialog() {
        var index by mutableStateOf(1)
    }

    class ReminderDue(val reminder: Reminder) : Dialog() {
        var index by mutableStateOf(0)
    }

    class Info(val title: String, val message: String) : Dialog()
}

class EpgCache(val from: Long, val to: Long, val programs: List<Program>)
