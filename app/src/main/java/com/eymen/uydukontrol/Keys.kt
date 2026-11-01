package com.eymen.uydukontrol

import android.view.KeyEvent
import kotlin.math.max
import kotlin.math.min

private fun isOk(code: Int): Boolean =
    code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER ||
        code == KeyEvent.KEYCODE_NUMPAD_ENTER || code == KeyEvent.KEYCODE_BUTTON_A

private fun isBack(code: Int): Boolean = code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_ESCAPE

private fun digitOf(code: Int): Int = when (code) {
    in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> code - KeyEvent.KEYCODE_0
    in KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_9 -> code - KeyEvent.KEYCODE_NUMPAD_0
    else -> -1
}

/** Every remote-control key goes through here. Returns false for keys the system should handle. */
fun AppState.onKey(code: Int): Boolean {
    val top = dialogs.lastOrNull()
    if (top != null) {
        dialogKey(top, code)
        return true
    }
    return when (screen) {
        Screen.SPLASH -> {
            if (isOk(code) || isBack(code) || digitOf(code) >= 0 ||
                code == KeyEvent.KEYCODE_DPAD_UP || code == KeyEvent.KEYCODE_DPAD_DOWN ||
                code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT
            ) {
                onSplashFinished()
                true
            } else false
        }
        Screen.LIVE -> if (guideOpen) guideKey(code) else liveKey(code)
        Screen.HOME -> homeKey(code)
        Screen.EPG -> epgKey(code)
        Screen.SETTINGS -> settingsKey(code)
    }
}

private fun AppState.liveKey(code: Int): Boolean {
    val d = digitOf(code)
    if (d >= 0) {
        digit(d)
        return true
    }
    when {
        isOk(code) -> if (digits.isNotEmpty()) commitNumber() else openGuide()
        code == KeyEvent.KEYCODE_DPAD_UP || code == KeyEvent.KEYCODE_CHANNEL_UP -> zap(1)
        code == KeyEvent.KEYCODE_DPAD_DOWN || code == KeyEvent.KEYCODE_CHANNEL_DOWN -> zap(-1)
        code == KeyEvent.KEYCODE_DPAD_LEFT -> openGuide()
        code == KeyEvent.KEYCODE_DPAD_RIGHT -> showBanner()
        code == KeyEvent.KEYCODE_INFO || code == KeyEvent.KEYCODE_PROG_YELLOW ->
            if (bannerVisible) showDetails() else showBanner()
        code == KeyEvent.KEYCODE_PROG_RED || code == KeyEvent.KEYCODE_GUIDE -> openEpg()
        code == KeyEvent.KEYCODE_PROG_GREEN || code == KeyEvent.KEYCODE_BOOKMARK -> {
            val c = current
            if (c != null) toggleFavorite(c)
        }
        code == KeyEvent.KEYCODE_PROG_BLUE || code == KeyEvent.KEYCODE_CAPTIONS -> audioSubtitleMenu()
        code == KeyEvent.KEYCODE_MENU || code == KeyEvent.KEYCODE_SETTINGS -> openSettings()
        code == KeyEvent.KEYCODE_LAST_CHANNEL -> lastChannel()
        isBack(code) -> when {
            digits.isNotEmpty() -> digits = ""
            bannerVisible -> hideBanner()
            else -> goHome()
        }
        else -> return false
    }
    return true
}

private fun AppState.guideKey(code: Int): Boolean {
    val tabList = tabs()
    if (guideTab >= tabList.size) guideTab = 0
    val list = guideList()
    val lastIndex = max(0, list.size - 1)
    val d = digitOf(code)
    if (d >= 0) {
        guideOpen = false
        digit(d)
        return true
    }
    when {
        code == KeyEvent.KEYCODE_DPAD_UP -> {
            if (list.isNotEmpty()) guideIndex = (guideIndex - 1 + list.size) % list.size
            prefetch(list, guideIndex - 4, 10)
        }
        code == KeyEvent.KEYCODE_DPAD_DOWN -> {
            if (list.isNotEmpty()) guideIndex = (guideIndex + 1) % list.size
            prefetch(list, guideIndex - 4, 10)
        }
        code == KeyEvent.KEYCODE_CHANNEL_UP || code == KeyEvent.KEYCODE_PAGE_UP -> {
            guideIndex = (guideIndex - 7).coerceIn(0, lastIndex)
            prefetch(list, guideIndex - 4, 10)
        }
        code == KeyEvent.KEYCODE_CHANNEL_DOWN || code == KeyEvent.KEYCODE_PAGE_DOWN -> {
            guideIndex = (guideIndex + 7).coerceIn(0, lastIndex)
            prefetch(list, guideIndex - 4, 10)
        }
        code == KeyEvent.KEYCODE_DPAD_LEFT -> {
            guideTab = (guideTab - 1 + tabList.size) % tabList.size
            guideIndex = 0
            prefetch(guideList(), 0, 10)
        }
        code == KeyEvent.KEYCODE_DPAD_RIGHT -> {
            guideTab = (guideTab + 1) % tabList.size
            guideIndex = 0
            prefetch(guideList(), 0, 10)
        }
        isOk(code) -> {
            val ch = list.getOrNull(guideIndex)
            if (ch != null) {
                guideOpen = false
                play(ch)
            }
        }
        code == KeyEvent.KEYCODE_PROG_GREEN || code == KeyEvent.KEYCODE_BOOKMARK -> {
            val ch = list.getOrNull(guideIndex)
            if (ch != null) toggleFavorite(ch)
        }
        code == KeyEvent.KEYCODE_PROG_YELLOW || code == KeyEvent.KEYCODE_SEARCH -> {
            pushDialog(Dialog.TextInput("Kanal veya program ara", guideQuery, "Aradığını yaz ve klavyede Tamam'a bas") { q ->
                guideQuery = q.trim()
                guideTab = 0
                guideIndex = 0
                prefetch(guideList(), 0, 10)
            })
        }
        code == KeyEvent.KEYCODE_PROG_RED || code == KeyEvent.KEYCODE_GUIDE -> openEpg()
        code == KeyEvent.KEYCODE_MENU || code == KeyEvent.KEYCODE_SETTINGS -> openSettings()
        isBack(code) -> {
            if (guideQuery.isNotEmpty()) {
                guideQuery = ""
                guideIndex = 0
            } else {
                guideOpen = false
                showBanner()
            }
        }
        else -> return false
    }
    return true
}

private fun AppState.homeKey(code: Int): Boolean {
    val rows = homeRows()
    if (!rows.contains(homeRow)) {
        homeRow = 1
        homeCol = 0
    }
    when {
        code == KeyEvent.KEYCODE_DPAD_UP -> {
            val i = rows.indexOf(homeRow)
            if (i > 0) {
                homeRow = rows[i - 1]
                homeCol = min(homeCol, homeCols(homeRow) - 1).coerceAtLeast(0)
            }
        }
        code == KeyEvent.KEYCODE_DPAD_DOWN -> {
            val i = rows.indexOf(homeRow)
            if (i >= 0 && i < rows.size - 1) {
                homeRow = rows[i + 1]
                homeCol = min(homeCol, homeCols(homeRow) - 1).coerceAtLeast(0)
            }
        }
        code == KeyEvent.KEYCODE_DPAD_LEFT -> if (homeCol > 0) homeCol -= 1
        code == KeyEvent.KEYCODE_DPAD_RIGHT -> if (homeCol < homeCols(homeRow) - 1) homeCol += 1
        isOk(code) -> homeOk()
        code == KeyEvent.KEYCODE_CHANNEL_UP -> {
            goLive()
            zap(1)
        }
        code == KeyEvent.KEYCODE_CHANNEL_DOWN -> {
            goLive()
            zap(-1)
        }
        code == KeyEvent.KEYCODE_PROG_RED || code == KeyEvent.KEYCODE_GUIDE -> openEpg()
        code == KeyEvent.KEYCODE_MENU || code == KeyEvent.KEYCODE_SETTINGS -> openSettings()
        digitOf(code) >= 0 -> {
            goLive()
            digit(digitOf(code))
        }
        isBack(code) -> askExit()
        else -> return false
    }
    return true
}

private fun AppState.epgKey(code: Int): Boolean {
    val list = epgList()
    when {
        code == KeyEvent.KEYCODE_DPAD_UP -> if (epgRow > 0) {
            epgRow -= 1
            ensureEpg()
        }
        code == KeyEvent.KEYCODE_DPAD_DOWN -> if (epgRow < list.size - 1) {
            epgRow += 1
            ensureEpg()
        }
        code == KeyEvent.KEYCODE_CHANNEL_UP || code == KeyEvent.KEYCODE_PAGE_UP -> {
            epgRow = max(0, epgRow - 6)
            ensureEpg()
        }
        code == KeyEvent.KEYCODE_CHANNEL_DOWN || code == KeyEvent.KEYCODE_PAGE_DOWN -> {
            epgRow = min(max(0, list.size - 1), epgRow + 6)
            ensureEpg()
        }
        code == KeyEvent.KEYCODE_DPAD_LEFT -> epgMove(-1)
        code == KeyEvent.KEYCODE_DPAD_RIGHT -> epgMove(1)
        isOk(code) -> epgOk()
        code == KeyEvent.KEYCODE_PROG_YELLOW -> epgDay(1)
        code == KeyEvent.KEYCODE_PROG_RED -> epgDay(-1)
        code == KeyEvent.KEYCODE_PROG_BLUE -> {
            val t = tabs()
            epgTab = (epgTab + 1) % t.size
            epgRow = 0
            ensureEpg()
        }
        code == KeyEvent.KEYCODE_PROG_GREEN || code == KeyEvent.KEYCODE_BOOKMARK -> {
            val ch = list.getOrNull(epgRow)
            if (ch != null) toggleFavorite(ch)
        }
        code == KeyEvent.KEYCODE_MENU || code == KeyEvent.KEYCODE_SETTINGS -> openSettings()
        isBack(code) || code == KeyEvent.KEYCODE_GUIDE -> goLive()
        else -> return false
    }
    return true
}

private fun AppState.settingsKey(code: Int): Boolean {
    val mv = moving
    if (mv != null) {
        when {
            code == KeyEvent.KEYCODE_DPAD_UP -> {
                moveChannel(mv, -1)
                setIndex = all.indexOf(mv)
            }
            code == KeyEvent.KEYCODE_DPAD_DOWN -> {
                moveChannel(mv, 1)
                setIndex = all.indexOf(mv)
            }
            isOk(code) || isBack(code) -> {
                moving = null
                toast("Sıra kaydedildi")
            }
        }
        return true
    }
    if (!setInPane) {
        when {
            code == KeyEvent.KEYCODE_DPAD_UP -> setSection = (setSection - 1 + sections.size) % sections.size
            code == KeyEvent.KEYCODE_DPAD_DOWN -> setSection = (setSection + 1) % sections.size
            code == KeyEvent.KEYCODE_DPAD_RIGHT || isOk(code) -> {
                setInPane = true
                setIndex = 0
            }
            isBack(code) || code == KeyEvent.KEYCODE_MENU || code == KeyEvent.KEYCODE_SETTINGS -> closeSettings()
            code == KeyEvent.KEYCODE_DPAD_LEFT -> {}
            else -> return false
        }
        return true
    }
    if (setSection == 0) {
        val list = all.toList()
        val lastIndex = max(0, list.size - 1)
        when {
            code == KeyEvent.KEYCODE_DPAD_UP -> setIndex = (setIndex - 1).coerceIn(0, lastIndex)
            code == KeyEvent.KEYCODE_DPAD_DOWN -> setIndex = (setIndex + 1).coerceIn(0, lastIndex)
            code == KeyEvent.KEYCODE_CHANNEL_UP || code == KeyEvent.KEYCODE_PAGE_UP -> setIndex = (setIndex - 8).coerceIn(0, lastIndex)
            code == KeyEvent.KEYCODE_CHANNEL_DOWN || code == KeyEvent.KEYCODE_PAGE_DOWN -> setIndex = (setIndex + 8).coerceIn(0, lastIndex)
            isOk(code) -> {
                val ch = list.getOrNull(setIndex)
                if (ch != null) channelActions(ch)
            }
            code == KeyEvent.KEYCODE_PROG_GREEN -> {
                val ch = list.getOrNull(setIndex)
                if (ch != null) {
                    if (checked.contains(ch.key)) checked.remove(ch.key) else checked.add(ch.key)
                }
            }
            code == KeyEvent.KEYCODE_PROG_RED -> bulkHideToggle()
            code == KeyEvent.KEYCODE_PROG_YELLOW -> {
                val ch = list.getOrNull(setIndex)
                if (ch != null) {
                    moving = ch
                    toast("▲▼ ile taşı, OK ile bırak")
                }
            }
            code == KeyEvent.KEYCODE_DPAD_LEFT || isBack(code) -> {
                setInPane = false
                checked.clear()
            }
            code == KeyEvent.KEYCODE_DPAD_RIGHT -> {}
            else -> return false
        }
        return true
    }
    val items = settingItems(setSection)
    val lastIndex = max(0, items.size - 1)
    when {
        code == KeyEvent.KEYCODE_DPAD_UP -> setIndex = (setIndex - 1).coerceIn(0, lastIndex)
        code == KeyEvent.KEYCODE_DPAD_DOWN -> setIndex = (setIndex + 1).coerceIn(0, lastIndex)
        isOk(code) -> {
            val item = items.getOrNull(setIndex)
            if (item != null && item.enabled) item.action?.invoke()
        }
        code == KeyEvent.KEYCODE_DPAD_LEFT || isBack(code) -> setInPane = false
        code == KeyEvent.KEYCODE_DPAD_RIGHT -> {}
        else -> return false
    }
    return true
}

private fun AppState.dialogKey(d: Dialog, code: Int) {
    when (d) {
        is Dialog.Choice -> {
            val n = d.items.size
            when {
                code == KeyEvent.KEYCODE_DPAD_UP -> if (n > 0) d.index = (d.index - 1 + n) % n
                code == KeyEvent.KEYCODE_DPAD_DOWN -> if (n > 0) d.index = (d.index + 1) % n
                isOk(code) -> {
                    popDialog(d)
                    if (d.index in 0 until n) d.onPick(d.index)
                }
                isBack(code) -> popDialog(d)
            }
        }
        is Dialog.Pin -> pinKey(d, code)
        is Dialog.Confirm -> {
            when {
                code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT -> d.index = 1 - d.index
                isOk(code) -> {
                    popDialog(d)
                    if (d.index == 0) d.onYes()
                }
                isBack(code) -> popDialog(d)
            }
        }
        is Dialog.ReminderDue -> {
            when {
                code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT -> d.index = 1 - d.index
                isOk(code) -> {
                    popDialog(d)
                    if (d.index == 0) {
                        val ch = all.firstOrNull { it.key == d.reminder.channelKey }
                        if (ch != null) {
                            goLive()
                            play(ch)
                        }
                    }
                }
                isBack(code) -> popDialog(d)
            }
        }
        is Dialog.Info -> if (isOk(code) || isBack(code)) popDialog(d)
        is Dialog.TextInput -> if (isBack(code)) popDialog(d)
    }
}

private fun AppState.pinKey(d: Dialog.Pin, code: Int) {
    val dg = digitOf(code)
    if (dg >= 0) {
        pinInput(d, dg.toString())
        return
    }
    when {
        code == KeyEvent.KEYCODE_DPAD_UP -> if (d.cell >= 3) d.cell -= 3
        code == KeyEvent.KEYCODE_DPAD_DOWN -> if (d.cell < 9) d.cell += 3
        code == KeyEvent.KEYCODE_DPAD_LEFT -> if (d.cell % 3 > 0) d.cell -= 1
        code == KeyEvent.KEYCODE_DPAD_RIGHT -> if (d.cell % 3 < 2) d.cell += 1
        isOk(code) -> when (d.cell) {
            9 -> d.entered = d.entered.dropLast(1)
            10 -> pinInput(d, "0")
            11 -> pinSubmit(d)
            else -> pinInput(d, (d.cell + 1).toString())
        }
        code == KeyEvent.KEYCODE_DEL -> d.entered = d.entered.dropLast(1)
        isBack(code) -> popDialog(d)
    }
}

private fun AppState.pinInput(d: Dialog.Pin, s: String) {
    d.error = null
    if (d.entered.length < 4) d.entered += s
    if (d.entered.length == 4) pinSubmit(d)
}

private fun AppState.pinSubmit(d: Dialog.Pin) {
    if (d.entered.length < 4) {
        d.error = "4 haneli kod gir"
        return
    }
    val v = d.entered
    if (d.validate(v)) {
        popDialog(d)
    } else {
        d.entered = ""
        d.error = "Yanlış PIN, tekrar dene"
    }
}
