package com.eymen.uydukontrol

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.tv.TvContract
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class SettingItem(
    val label: String,
    val value: String? = null,
    val enabled: Boolean = true,
    val action: (() -> Unit)? = null
)

class AppState(val activity: MainActivity) {
    val ctx: Context = activity
    val prefs = Prefs(activity)
    val playback = Playback(activity, this)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private val tr = Locale("tr", "TR")

    // Channels
    val all = mutableStateListOf<Channel>()
    private var lastTv: List<Channel> = emptyList()
    private var lastWeb: List<Channel> = emptyList()
    var tvCount by mutableStateOf(0)
    var webCount by mutableStateOf(0)
    var matchedCount by mutableStateOf(0)
    var loadError by mutableStateOf<String?>(null)
    var loading by mutableStateOf(true)

    val favorites = mutableStateListOf<String>()
    val hidden = mutableStateListOf<String>()
    val locked = mutableStateListOf<String>()
    val reminders = mutableStateListOf<Reminder>()
    private val unlocked = HashSet<String>()

    // Screen state
    var screen by mutableStateOf(Screen.SPLASH)
    var previousScreen = Screen.LIVE
    var current by mutableStateOf<Channel?>(null)
    private var previous: Channel? = null
    var bannerVisible by mutableStateOf(false)
    var guideOpen by mutableStateOf(false)
    var guideTab by mutableStateOf(0)
    var guideIndex by mutableStateOf(0)
    var guideQuery by mutableStateOf("")
    var digits by mutableStateOf("")
    var toastText by mutableStateOf<String?>(null)
    val dialogs = mutableStateListOf<Dialog>()
    var now by mutableStateOf(System.currentTimeMillis())

    // Settings values
    var showLogos by mutableStateOf(prefs.bool("channelLogos", true))
    var showWatermark by mutableStateOf(prefs.bool("showWatermark", true))
    var hideSeconds by mutableStateOf(prefs.int("hideSeconds", 5))
    var splashEnabled by mutableStateOf(prefs.bool("splash", true))
    var autoOnline by mutableStateOf(prefs.bool("autoOnline", true))
    var m3uUrl by mutableStateOf(prefs.str("m3uUrl", ""))
    var remoteEnabled by mutableStateOf(prefs.bool("remoteEnabled", true))

    // Programme guide (EPG)
    var epgTab by mutableStateOf(0)
    var epgRow by mutableStateOf(0)
    var epgFocus by mutableStateOf(0L)
    var epgWindowStart by mutableStateOf(0L)
    val epgCache = mutableStateMapOf<String, EpgCache>()

    // Home
    var homeRow by mutableStateOf(1)
    var homeCol by mutableStateOf(0)

    // Settings screen
    var setSection by mutableStateOf(0)
    var setInPane by mutableStateOf(false)
    var setIndex by mutableStateOf(0)
    var moving by mutableStateOf<Channel?>(null)
    val checked = mutableStateListOf<String>()

    // Phone remote and updates
    var remote: RemoteServer? = null
    var remoteUrl by mutableStateOf<String?>(null)
    var remoteQr by mutableStateOf<ImageBitmap?>(null)
    var updateStatus by mutableStateOf("")
    var updateInfo by mutableStateOf<Updater.Info?>(null)

    // Built-in internet list and stream health
    var builtInList by mutableStateOf(prefs.bool("builtInList", true))
    var hideDead by mutableStateOf(prefs.bool("hideDead", true))
    val deadWeb = mutableStateListOf<String>()
    var testingWeb by mutableStateOf(false)
    private var pendingIntent: Intent? = null

    init {
        favorites.addAll(prefs.stringSet("favorites"))
        hidden.addAll(prefs.stringSet("hidden"))
        locked.addAll(prefs.stringSet("locked"))
        deadWeb.addAll(prefs.stringSet("deadWeb"))
        reminders.addAll(prefs.loadReminders())
        if (!splashEnabled) screen = Screen.LIVE
    }

    /** The list the app downloads: the user's own address, else the daily-tested EYMEN list. */
    fun effectiveListUrl(): String = when {
        m3uUrl.isNotBlank() -> m3uUrl
        builtInList -> DEFAULT_LIST_URL
        else -> ""
    }

    private fun bundledList(): String? = try {
        ctx.assets.open("channels/tr.m3u").use { String(it.readBytes(), Charsets.UTF_8) }
    } catch (e: Exception) {
        null
    }

    private fun isDead(ch: Channel): Boolean = hideDead && ch.source == Source.WEB && deadWeb.contains(ch.key)

    // ---------------------------------------------------------------- startup

    fun start() {
        if (ctx.checkSelfPermission(PERM_TV) == PackageManager.PERMISSION_GRANTED) loadAll(true)
        else activity.requestTvPermission()
        scope.launch {
            while (isActive) {
                tick()
                delay(15000L)
            }
        }
        if (remoteEnabled) startRemote()
    }

    fun onPermissionResult(granted: Boolean) {
        loadAll(true)
        if (!granted) toast("TV kanal izni verilmedi. Yalnızca internet kanalları gösterilebilir.")
    }

    fun loadAll(refreshWeb: Boolean) {
        loading = true
        scope.launch {
            val tv = withContext(Dispatchers.IO) { TvSource.load(ctx) }
            val cached = withContext(Dispatchers.IO) {
                M3u.readCache(ctx) ?: (if (m3uUrl.isBlank() && builtInList) bundledList() else null)
            }
            val web = if (cached != null) withContext(Dispatchers.Default) { M3u.parse(cached) } else emptyList()
            lastTv = tv.channels
            lastWeb = web
            merge()
            loadError = if (all.isEmpty()) {
                tv.error ?: "Kanal bulunamadı (${tv.rows} kayıt, ${tv.inputs} TV girişi). Ayarlar › Kanal kaynakları bölümünden internet listesi ekleyebilirsin."
            } else null
            loading = false
            if (!applyPendingIntent()) startPlaybackIfNeeded()
            if (refreshWeb && effectiveListUrl().isNotBlank()) refreshOnline(false)
            else maybeTestWeb()
        }
    }

    private fun merge() {
        val tv = lastTv
        val web = lastWeb
        for (t in tv) t.web = null
        for (w in web) w.matched = false
        val byNorm = HashMap<String, Channel>()
        for (w in web) if (w.norm.isNotEmpty() && !isDead(w) && !byNorm.containsKey(w.norm)) byNorm[w.norm] = w
        var matched = 0
        for (t in tv) {
            val w = byNorm[t.norm]
            if (w != null) {
                t.web = w
                w.matched = true
                matched++
            }
        }
        val webOnly = web.filter { !it.matched }
        val base = if (tv.isEmpty()) 0 else 1000
        webOnly.forEachIndexed { i, w -> w.number = (base + i + 1).toString() }
        val combined = ArrayList<Channel>(tv.size + webOnly.size)
        combined.addAll(tv)
        combined.addAll(webOnly)
        for (c in combined) {
            c.name = prefs.customName(c.key) ?: c.originalName
            c.category = prefs.customCategory(c.key) ?: Names.category(c.norm, c.groupTitle, c.isRadio)
        }
        val ranks = HashMap<String, Int>()
        prefs.order.split(',').filter { it.isNotBlank() }.forEachIndexed { i, k -> ranks[k] = i }
        val sorted = combined.withIndex()
            .sortedWith(compareBy<IndexedValue<Channel>>({ ranks[it.value.key] ?: Int.MAX_VALUE }, { it.index }))
            .map { it.value }
        all.clear()
        all.addAll(sorted)
        tvCount = tv.size
        webCount = web.size
        matchedCount = matched
        writeSnapshot()
        val cur = current
        if (cur != null) {
            val fresh = all.firstOrNull { it.key == cur.key }
            if (fresh != null && fresh !== cur) {
                fresh.now = cur.now
                fresh.next = cur.next
                fresh.epgLoadedAt = cur.epgLoadedAt
                current = fresh
            }
        }
    }

    private fun startPlaybackIfNeeded() {
        if (current != null) return
        val list = visible()
        if (list.isEmpty()) return
        val last = prefs.lastChannel
        val ch = list.firstOrNull { it.key == last } ?: list[0]
        play(ch, withBanner = screen == Screen.LIVE)
    }

    fun onSplashFinished() {
        if (screen != Screen.SPLASH) return
        screen = Screen.LIVE
        if (current != null) showBanner()
    }

    // ---------------------------------------------------------------- lists

    fun visible(): List<Channel> = all.filter { !hidden.contains(it.key) && !isDead(it) }
    fun isFav(ch: Channel): Boolean = favorites.contains(ch.favoriteKey)
    fun isLocked(ch: Channel): Boolean = locked.contains(ch.key)
    fun isHidden(ch: Channel): Boolean = hidden.contains(ch.key)

    fun tabs(): List<String> {
        val v = visible()
        val out = arrayListOf("Tümü", "Favoriler")
        for (c in Names.CATEGORY_ORDER) if (v.any { it.category == c }) out.add(c)
        if (lastTv.isNotEmpty() && v.any { it.source == Source.WEB }) out.add("İnternet")
        return out
    }

    fun filtered(tab: String, query: String = ""): List<Channel> {
        val v = visible()
        val base = when (tab) {
            "Tümü" -> v
            "Favoriler" -> v.filter { isFav(it) }
            "İnternet" -> v.filter { it.source == Source.WEB }
            else -> v.filter { it.category == tab }
        }
        if (query.isBlank()) return base
        val q = Names.norm(query)
        val raw = query.trim()
        return base.filter { ch ->
            Names.norm(ch.name).contains(q) || ch.number == raw ||
                (ch.now?.title?.let { Names.norm(it).contains(q) } ?: false)
        }
    }

    fun guideTabName(): String = tabs().getOrElse(guideTab) { "Tümü" }
    fun guideList(): List<Channel> = filtered(guideTabName(), guideQuery)

    // ---------------------------------------------------------------- playback

    fun play(ch: Channel, withBanner: Boolean = true) {
        if (isLocked(ch) && !unlocked.contains(ch.key) && prefs.pin.isNotEmpty()) {
            pushDialog(Dialog.Pin("${ch.name} kilitli", "İzlemek için PIN kodunu gir") { pin ->
                if (pin == prefs.pin) {
                    unlocked.add(ch.key)
                    play(ch, withBanner)
                    true
                } else false
            })
            return
        }
        val cur = current
        if (cur != null && cur.key != ch.key) previous = cur
        current = ch
        prefs.lastChannel = ch.key
        playback.play(ch)
        loadNowNext(ch, true)
        if (withBanner) showBanner()
        writeSnapshot()
    }

    fun zap(delta: Int) {
        val list = visible()
        if (list.isEmpty()) return
        var i = list.indexOfFirst { it.key == current?.key }
        if (i < 0) i = if (delta > 0) -1 else 0
        i = ((i + delta) % list.size + list.size) % list.size
        play(list[i])
    }

    fun lastChannel() {
        val p = previous ?: return
        val ch = all.firstOrNull { it.key == p.key } ?: return
        play(ch)
    }

    fun loadNowNext(ch: Channel, force: Boolean) {
        if (ch.source != Source.TV) return
        val t = System.currentTimeMillis()
        if (!force && t - ch.epgLoadedAt < 120000L) return
        ch.epgLoadedAt = t
        scope.launch {
            val list = withContext(Dispatchers.IO) {
                TvSource.programs(ctx, ch.tvId, t - 6 * 3600000L, t + 24 * 3600000L)
            }
            val n = list.firstOrNull { it.isLive(t) }
            val nx = list.firstOrNull { it.start >= (n?.end ?: t) }
            ch.now = n
            ch.next = nx
        }
    }

    fun prefetch(list: List<Channel>, from: Int, count: Int) {
        val start = max(0, from)
        val end = min(list.size, start + count)
        for (i in start until end) loadNowNext(list[i], false)
    }

    private val hideBannerRunnable = Runnable { bannerVisible = false }

    fun showBanner() {
        bannerVisible = true
        handler.removeCallbacks(hideBannerRunnable)
        handler.postDelayed(hideBannerRunnable, hideSeconds * 1000L)
    }

    fun hideBanner() {
        bannerVisible = false
        handler.removeCallbacks(hideBannerRunnable)
    }

    private val hideToastRunnable = Runnable { toastText = null }

    fun toast(s: String) {
        toastText = s
        handler.removeCallbacks(hideToastRunnable)
        handler.postDelayed(hideToastRunnable, 3200L)
    }

    private val enterNumber = Runnable { commitNumber() }

    fun digit(d: Int) {
        if (digits.length >= 4) digits = ""
        digits += d.toString()
        handler.removeCallbacks(enterNumber)
        handler.postDelayed(enterNumber, 1500L)
    }

    fun commitNumber() {
        handler.removeCallbacks(enterNumber)
        val n = digits
        digits = ""
        if (n.isEmpty()) return
        val key = n.trimStart('0')
        val ch = visible().firstOrNull { it.number == n || (key.isNotEmpty() && it.number.trimStart('0') == key) }
        if (ch != null) {
            if (screen != Screen.LIVE) goLive()
            play(ch)
        } else toast("$n numaralı kanal bulunamadı")
    }

    // ---------------------------------------------------------------- navigation

    fun goLive() {
        screen = Screen.LIVE
        guideOpen = false
        if (current != null) showBanner()
    }

    fun goHome() {
        screen = Screen.HOME
        guideOpen = false
        hideBanner()
        homeRow = 1
        homeCol = 0
        prefetch(homeFavorites(), 0, 6)
    }

    fun openGuide() {
        screen = Screen.LIVE
        guideOpen = true
        hideBanner()
        if (guideTab >= tabs().size) guideTab = 0
        val list = guideList()
        val i = list.indexOfFirst { it.key == current?.key }
        guideIndex = if (i >= 0) i else 0
        prefetch(list, guideIndex - 4, 12)
    }

    fun openSettings(section: Int = 0) {
        if (screen != Screen.SETTINGS) previousScreen = screen
        screen = Screen.SETTINGS
        guideOpen = false
        hideBanner()
        setSection = section
        setInPane = false
        setIndex = 0
        moving = null
        checked.clear()
    }

    fun closeSettings() {
        moving = null
        checked.clear()
        if (previousScreen == Screen.HOME) goHome() else goLive()
    }

    fun askExit() {
        pushDialog(Dialog.Confirm("EYMEN TV kapatılsın mı?", "Yayın duracak.", "Çık", "İzlemeye devam et") {
            activity.finish()
        })
    }

    // ---------------------------------------------------------------- dialogs

    fun pushDialog(d: Dialog) {
        dialogs.add(d)
    }

    fun popDialog(d: Dialog) {
        dialogs.remove(d)
    }

    fun textInputActive(): Boolean = dialogs.lastOrNull() is Dialog.TextInput

    fun showDetails() {
        val c = current ?: return
        val n = c.now
        val body = if (n == null) "Bu kanal için yayın bilgisi yok."
        else "${time(n.start)} – ${time(n.end)} · ${c.name}\n\n${n.description ?: "Açıklama yok."}"
        pushDialog(Dialog.Info(n?.title ?: c.name, body))
    }

    fun audioSubtitleMenu() {
        val c = current
        val items = arrayListOf("Ses izi · ${playback.audioLabel() ?: "Yok"}", "Altyazı · ${playback.subtitleLabel()}")
        val canWeb = c != null && c.source == Source.TV && c.web != null
        if (canWeb) items.add(if (playback.fallbackActive) "Uydu yayınına dön" else "İnternet yayınına geç")
        pushDialog(Dialog.Choice("Ses ve görüntü", items) { i ->
            when (i) {
                0 -> chooseAudio()
                1 -> chooseSubtitle()
                2 -> if (c != null) {
                    if (playback.fallbackActive) play(c)
                    else if (playback.switchToWeb(c)) toast("İnternet yayınına geçildi")
                }
            }
        })
    }

    fun chooseAudio() {
        val tracks = playback.audioTracks.toList()
        if (tracks.isEmpty()) {
            val msg = if (playback.webActive) "İnternet yayınında ses izi seçimi yok." else "Bu kanal için ses izi bildirilmedi."
            pushDialog(Dialog.Info("Ses izi", msg))
            return
        }
        val labels = tracks.map { t ->
            val c = Labels.audioChannels(t.audioChannelCount)
            Labels.language(t.language) + (if (c.isEmpty()) "" else " · $c")
        }
        val d = Dialog.Choice("Ses izi", labels) { i -> playback.selectAudio(tracks[i]) }
        d.index = max(0, tracks.indexOfFirst { it.id == playback.selectedAudio })
        pushDialog(d)
    }

    fun chooseSubtitle() {
        val tracks = playback.subtitleTracks.toList()
        val labels = listOf("Kapalı") + tracks.map { Labels.language(it.language) }
        val d = Dialog.Choice("Altyazı", labels) { i -> playback.selectSubtitle(if (i == 0) null else tracks[i - 1]) }
        d.index = tracks.indexOfFirst { it.id == playback.selectedSubtitle } + 1
        pushDialog(d)
    }

    // ---------------------------------------------------------------- channel edits

    fun toggleFavorite(ch: Channel) {
        val k = ch.favoriteKey
        val added = if (favorites.contains(k)) {
            favorites.remove(k)
            false
        } else {
            favorites.add(k)
            true
        }
        prefs.saveSet("favorites", favorites)
        toast(if (added) "${ch.name} favorilere eklendi" else "${ch.name} favorilerden çıkarıldı")
        writeSnapshot()
    }

    fun setHidden(keys: Collection<String>, hide: Boolean) {
        for (k in keys) {
            if (hide) {
                if (!hidden.contains(k)) hidden.add(k)
            } else hidden.remove(k)
        }
        prefs.saveSet("hidden", hidden)
        writeSnapshot()
    }

    fun toggleLock(ch: Channel) {
        if (prefs.pin.isEmpty()) {
            askNewPin { toggleLock(ch) }
            return
        }
        if (locked.contains(ch.key)) {
            requirePin("Kilidi açmak için PIN") {
                locked.remove(ch.key)
                prefs.saveSet("locked", locked)
                toast("${ch.name} kilidi açıldı")
            }
        } else {
            locked.add(ch.key)
            prefs.saveSet("locked", locked)
            toast("${ch.name} PIN ile kilitlendi")
        }
    }

    fun askNewPin(then: () -> Unit) {
        pushDialog(Dialog.Pin("Yeni PIN belirle", "4 haneli bir kod gir. Kilitli kanallar bu kodla açılır.") { p ->
            prefs.pin = p
            toast("PIN kaydedildi")
            then()
            true
        })
    }

    fun requirePin(title: String, then: () -> Unit) {
        if (prefs.pin.isEmpty()) {
            then()
            return
        }
        pushDialog(Dialog.Pin(title, "Mevcut PIN kodunu gir") { p ->
            if (p == prefs.pin) {
                then()
                true
            } else false
        })
    }

    fun rename(ch: Channel) {
        pushDialog(Dialog.TextInput("Kanal adı", ch.name, "Yeni adı yaz ve klavyede Tamam'a bas") { v ->
            val name = v.trim()
            if (name.isEmpty() || name == ch.originalName) {
                prefs.setCustomName(ch.key, null)
                ch.name = ch.originalName
            } else {
                prefs.setCustomName(ch.key, name)
                ch.name = name
            }
            toast("Kanal adı kaydedildi")
        })
    }

    fun chooseCategory(ch: Channel) {
        val cats = Names.CATEGORY_ORDER
        val d = Dialog.Choice("${ch.name} · Kategori", cats + "Otomatik") { i ->
            if (i >= cats.size) {
                prefs.setCustomCategory(ch.key, null)
                ch.category = Names.category(ch.norm, ch.groupTitle, ch.isRadio)
            } else {
                prefs.setCustomCategory(ch.key, cats[i])
                ch.category = cats[i]
            }
            toast("Kategori: ${ch.category}")
        }
        d.index = max(0, cats.indexOf(ch.category))
        pushDialog(d)
    }

    fun saveOrder() {
        prefs.order = all.joinToString(",") { it.key }
    }

    fun moveChannel(ch: Channel, delta: Int) {
        val i = all.indexOf(ch)
        val j = i + delta
        if (i < 0 || j < 0 || j >= all.size) return
        all.removeAt(i)
        all.add(j, ch)
        saveOrder()
    }

    fun requestLogo(ch: Channel) {
        if (ch.logoRequested) return
        ch.logoRequested = true
        scope.launch {
            val bmp = withContext(Dispatchers.IO) { Logos.load(ctx, ch) }
            if (bmp != null) ch.logo = bmp.asImageBitmap()
        }
    }

    fun pickLogo(ch: Channel) {
        activity.pickDocument(arrayOf("image/*")) { uri ->
            if (uri == null) return@pickDocument
            scope.launch {
                val bmp = withContext(Dispatchers.IO) {
                    try {
                        ctx.contentResolver.openInputStream(uri)?.use { Logos.saveCustom(ctx, ch.key, it) }
                    } catch (e: Exception) {
                        null
                    }
                }
                if (bmp != null) {
                    ch.logo = bmp.asImageBitmap()
                    ch.logoRequested = true
                    toast("Kanal logosu kaydedildi")
                } else toast("Logo yüklenemedi")
            }
        }
    }

    fun resetLogo(ch: Channel) {
        val f = Logos.customFile(ctx, ch.key)
        if (f.exists()) f.delete()
        ch.logo = null
        ch.logoRequested = false
        toast("Varsayılan logoya dönüldü")
    }

    fun channelActions(ch: Channel) {
        val hiddenNow = isHidden(ch)
        val lockedNow = isLocked(ch)
        val fav = isFav(ch)
        val labels = listOf(
            "İzle",
            "Yeniden adlandır",
            "Kendi logomu seç (USB)",
            "Özel logoyu kaldır",
            "Kategori değiştir",
            if (hiddenNow) "Listede göster" else "Listeden gizle",
            if (lockedNow) "Kilidi aç" else "PIN ile kilitle",
            "Sırasını değiştir",
            if (fav) "Favorilerden çıkar" else "Favorilere ekle",
            "Adı ve kategoriyi sıfırla"
        )
        pushDialog(Dialog.Choice(ch.name, labels, "${ch.number} · ${ch.category} · ${ch.sourceLabel}") { i ->
            when (i) {
                0 -> {
                    moving = null
                    goLive()
                    play(ch)
                }
                1 -> rename(ch)
                2 -> pickLogo(ch)
                3 -> resetLogo(ch)
                4 -> chooseCategory(ch)
                5 -> {
                    setHidden(listOf(ch.key), !hiddenNow)
                    toast(if (hiddenNow) "${ch.name} yeniden listede" else "${ch.name} gizlendi")
                }
                6 -> toggleLock(ch)
                7 -> {
                    moving = ch
                    toast("▲▼ ile taşı, OK ile bırak")
                }
                8 -> toggleFavorite(ch)
                9 -> {
                    prefs.setCustomName(ch.key, null)
                    prefs.setCustomCategory(ch.key, null)
                    ch.name = ch.originalName
                    ch.category = Names.category(ch.norm, ch.groupTitle, ch.isRadio)
                    toast("Ad ve kategori sıfırlandı")
                }
            }
        })
    }

    fun bulkHideToggle() {
        if (checked.isEmpty()) {
            toast("Önce Yeşil tuşla kanalları işaretle")
            return
        }
        val keys = checked.toList()
        val allHidden = keys.all { hidden.contains(it) }
        setHidden(keys, !allHidden)
        toast(if (allHidden) "${keys.size} kanal yeniden listede" else "${keys.size} kanal gizlendi")
        checked.clear()
    }

    // ---------------------------------------------------------------- internet list

    fun saveM3uUrl(url: String) {
        m3uUrl = url.trim()
        prefs.putStr("m3uUrl", if (m3uUrl.isEmpty()) null else m3uUrl)
        if (effectiveListUrl().isNotEmpty()) refreshOnline(true) else clearWebList()
    }

    fun toggleBuiltInList() {
        builtInList = !builtInList
        prefs.putBool("builtInList", builtInList)
        if (effectiveListUrl().isNotEmpty()) refreshOnline(true) else clearWebList()
    }

    private fun clearWebList() {
        try {
            M3u.cacheFile(ctx).delete()
        } catch (e: Exception) {
        }
        lastWeb = emptyList()
        merge()
        toast("İnternet kanalları kaldırıldı")
    }

    private fun maybeTestWeb() {
        if (lastWeb.isEmpty()) return
        val last = prefs.str("deadWebAt", "0").toLongOrNull() ?: 0L
        if (System.currentTimeMillis() - last > 20 * 3600000L) testWebChannels(false)
    }

    /** Tries every internet channel from this TV's own network and hides the ones that do not open. */
    fun testWebChannels(verbose: Boolean) {
        if (testingWeb) return
        val list = lastWeb.toList()
        if (list.isEmpty()) {
            if (verbose) toast("Test edilecek internet kanalı yok")
            return
        }
        testingWeb = true
        if (verbose) toast("${list.size} internet kanalı deneniyor…")
        scope.launch {
            val dead = ArrayList<String>()
            withContext(Dispatchers.IO) {
                for (chunk in list.chunked(8)) {
                    val results = chunk.map { ch -> async { ch.key to WebHealth.check(ch.streamUrl) } }.awaitAll()
                    for ((key, ok) in results) if (!ok) dead.add(key)
                }
            }
            deadWeb.clear()
            deadWeb.addAll(dead)
            prefs.saveSet("deadWeb", dead)
            prefs.putStr("deadWebAt", System.currentTimeMillis().toString())
            merge()
            testingWeb = false
            if (verbose) toast("${list.size - dead.size} kanal çalışıyor, ${dead.size} kanal açılmadığı için gizlendi")
        }
    }

    /** Called when an internet stream fails while playing: if it really does not answer, hide it. */
    fun onWebPlaybackError(url: String?) {
        if (url == null) return
        val ch = lastWeb.firstOrNull { it.streamUrl == url } ?: return
        scope.launch {
            val ok = withContext(Dispatchers.IO) { WebHealth.check(url) }
            if (!ok && !deadWeb.contains(ch.key)) {
                deadWeb.add(ch.key)
                prefs.saveSet("deadWeb", deadWeb)
                merge()
                if (hideDead) toast("${ch.name} şu an açılmıyor, listeden gizlendi")
            }
        }
    }

    fun toggleHideDead() {
        hideDead = !hideDead
        prefs.putBool("hideDead", hideDead)
        merge()
    }

    fun refreshOnline(verbose: Boolean) {
        val url = effectiveListUrl()
        if (url.isBlank()) {
            if (verbose) toast("Önce M3U liste adresini gir ya da hazır listeyi aç")
            return
        }
        if (verbose) toast("İnternet kanal listesi indiriliyor…")
        scope.launch {
            try {
                val text = withContext(Dispatchers.IO) { Http.text(url) }
                val list = withContext(Dispatchers.Default) { M3u.parse(text) }
                if (list.isEmpty()) {
                    if (verbose) toast("Listede kanal bulunamadı")
                    return@launch
                }
                withContext(Dispatchers.IO) { M3u.writeCache(ctx, text) }
                lastWeb = list
                merge()
                if (all.isNotEmpty()) loadError = null
                if (!applyPendingIntent()) startPlaybackIfNeeded()
                if (verbose) toast("${list.size} internet kanalı yüklendi, $matchedCount kanal uyduyla eşleşti")
                if (verbose) testWebChannels(false) else maybeTestWeb()
            } catch (e: Exception) {
                if (verbose) toast("Liste indirilemedi: ${e.message ?: "bağlantı hatası"}")
            }
        }
    }

    fun importM3uFile() {
        activity.pickDocument(arrayOf("*/*")) { uri ->
            if (uri == null) return@pickDocument
            scope.launch {
                try {
                    val text = withContext(Dispatchers.IO) {
                        ctx.contentResolver.openInputStream(uri)?.use { String(it.readBytes(), Charsets.UTF_8) } ?: ""
                    }
                    val list = withContext(Dispatchers.Default) { M3u.parse(text) }
                    if (list.isEmpty()) {
                        toast("Dosyada kanal bulunamadı")
                        return@launch
                    }
                    withContext(Dispatchers.IO) { M3u.writeCache(ctx, text) }
                    lastWeb = list
                    merge()
                    if (all.isNotEmpty()) loadError = null
                    startPlaybackIfNeeded()
                    toast("${list.size} internet kanalı yüklendi")
                    testWebChannels(false)
                } catch (e: Exception) {
                    toast("Dosya okunamadı")
                }
            }
        }
    }

    // ---------------------------------------------------------------- opening from the launcher

    /**
     * EYMEN Başlatıcı (or any app) can open a channel or a screen:
     * extras channel_key / channel_number / open=guide|epg|home|settings / skip_splash,
     * or a TvContract channel URI as the intent data.
     */
    fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val hasTarget = intent.hasExtra(EXTRA_CHANNEL_KEY) || intent.hasExtra(EXTRA_CHANNEL_NUMBER) ||
            intent.hasExtra(EXTRA_OPEN) || intent.data != null
        if (intent.getBooleanExtra(EXTRA_SKIP_SPLASH, false)) onSplashFinished()
        if (!hasTarget) return
        pendingIntent = intent
        if (!loading) applyPendingIntent()
    }

    private fun applyPendingIntent(): Boolean {
        val intent = pendingIntent ?: return false
        pendingIntent = null
        val data = intent.data
        val key = intent.getStringExtra(EXTRA_CHANNEL_KEY)
            ?: if (data != null && data.scheme == "content" && data.authority == TvContract.AUTHORITY) data.lastPathSegment else null
        val number = intent.getStringExtra(EXTRA_CHANNEL_NUMBER)
        val ch = (if (key != null) all.firstOrNull { it.key == key } else null)
            ?: (if (number != null) visible().firstOrNull { it.number == number } else null)
        if (ch != null) {
            if (screen != Screen.SPLASH) goLive()
            play(ch)
        } else if (current == null) {
            startPlaybackIfNeeded()
        }
        when (intent.getStringExtra(EXTRA_OPEN)) {
            "guide" -> openGuide()
            "epg" -> openEpg()
            "home" -> goHome()
            "settings" -> openSettings()
        }
        return true
    }

    // ---------------------------------------------------------------- shared with EYMEN Başlatıcı

    /** Writes the visible channel list and the last channel for EYMEN Başlatıcı (served by StateProvider). */
    fun writeSnapshot() {
        val o = JSONObject()
        try {
            o.put("last", current?.key ?: prefs.lastChannel ?: "")
            o.put("updated", System.currentTimeMillis())
            val arr = JSONArray()
            for (c in visible().take(600)) {
                val j = JSONObject()
                j.put("key", c.key)
                j.put("number", c.number)
                j.put("name", c.name)
                j.put("source", c.source.name)
                j.put("tvId", c.tvId)
                j.put("inputId", c.inputId ?: "")
                j.put("url", c.streamUrl ?: c.web?.streamUrl ?: "")
                j.put("logo", c.logoUrl ?: c.web?.logoUrl ?: "")
                j.put("fav", isFav(c))
                j.put("category", c.category)
                arr.put(j)
            }
            o.put("channels", arr)
        } catch (e: Exception) {
            return
        }
        val text = o.toString()
        scope.launch(Dispatchers.IO) {
            try {
                val f = File(ctx.filesDir, "launcher_state.json")
                val tmp = File(ctx.filesDir, "launcher_state.json.tmp")
                tmp.writeText(text)
                if (!tmp.renameTo(f)) {
                    f.writeText(text)
                    tmp.delete()
                }
            } catch (e: Exception) {
            }
        }
    }

    // ---------------------------------------------------------------- reminders

    fun hasReminder(ch: Channel, p: Program): Boolean = reminders.any { it.channelKey == ch.key && it.start == p.start }

    fun toggleReminder(ch: Channel, p: Program) {
        val existing = reminders.firstOrNull { it.channelKey == ch.key && it.start == p.start }
        if (existing != null) {
            reminders.remove(existing)
            toast("Hatırlatıcı kaldırıldı")
        } else {
            reminders.add(Reminder(ch.key, ch.name, p.title, p.start))
            toast("${p.title} için ${time(p.start)} hatırlatıcısı kuruldu")
        }
        prefs.saveReminders(reminders)
    }

    fun removeReminder(r: Reminder) {
        reminders.remove(r)
        prefs.saveReminders(reminders)
    }

    fun sortedReminders(): List<Reminder> = reminders.sortedBy { it.start }

    private fun tick() {
        now = System.currentTimeMillis()
        val due = reminders.filter { it.start - 60000L <= now }
        if (due.isNotEmpty()) {
            for (r in due) reminders.remove(r)
            prefs.saveReminders(reminders)
            val r = due.maxByOrNull { it.start }
            if (r != null && r.start + 30 * 60000L > now && dialogs.none { it is Dialog.ReminderDue }) {
                pushDialog(Dialog.ReminderDue(r))
            }
        }
        val c = current
        if (c != null && c.source == Source.TV) {
            val n = c.now
            if (n == null || n.end <= now) loadNowNext(c, false)
        }
    }

    // ---------------------------------------------------------------- programme guide

    fun epgList(): List<Channel> = filtered(tabs().getOrElse(epgTab) { "Tümü" })

    fun floor30(t: Long): Long {
        val m = 30 * 60000L
        return t - Math.floorMod(t, m)
    }

    fun openEpg() {
        screen = Screen.EPG
        guideOpen = false
        hideBanner()
        if (epgTab >= tabs().size) epgTab = 0
        val t = System.currentTimeMillis()
        epgFocus = t
        epgWindowStart = floor30(t)
        val list = epgList()
        val i = list.indexOfFirst { it.key == current?.key }
        epgRow = if (i >= 0) i else 0
        ensureEpg()
    }

    fun ensureEpg() {
        val list = epgList()
        val from = epgWindowStart - 3 * 3600000L
        val to = epgWindowStart + 27 * 3600000L
        val first = max(0, epgRow - 3)
        val last = min(list.size, epgRow + 9)
        for (i in first until last) {
            val ch = list[i]
            if (ch.source != Source.TV) continue
            val c = epgCache[ch.key]
            if (c != null && c.from <= epgWindowStart && c.to >= epgWindowStart + 2 * 3600000L) continue
            epgCache[ch.key] = EpgCache(from, to, c?.programs ?: emptyList())
            scope.launch {
                val p = withContext(Dispatchers.IO) { TvSource.programs(ctx, ch.tvId, from, to) }
                epgCache[ch.key] = EpgCache(from, to, p)
            }
        }
    }

    fun epgPrograms(ch: Channel): List<Program> = epgCache[ch.key]?.programs ?: emptyList()

    fun epgSelected(ch: Channel): Program? {
        val f = epgFocus
        val p = epgPrograms(ch)
        return p.firstOrNull { it.start <= f && it.end > f } ?: p.firstOrNull { it.start > f }
    }

    fun epgMove(dx: Int) {
        val ch = epgList().getOrNull(epgRow) ?: return
        val sel = epgSelected(ch)
        epgFocus = if (sel != null && sel.start <= epgFocus && sel.end > epgFocus) {
            if (dx > 0) sel.end else sel.start - 1
        } else if (sel != null && dx > 0) {
            sel.start
        } else {
            epgFocus + dx * 30 * 60000L
        }
        val lower = System.currentTimeMillis() - 2 * 3600000L
        if (epgFocus < lower) epgFocus = lower
        adjustEpgWindow()
    }

    fun epgDay(dir: Int) {
        val target = epgFocus + dir * 24 * 3600000L
        val lower = System.currentTimeMillis()
        epgFocus = if (target < lower) lower else target
        epgWindowStart = floor30(epgFocus)
        ensureEpg()
    }

    private fun adjustEpgWindow() {
        val win = 2 * 3600000L
        if (epgFocus < epgWindowStart) epgWindowStart = floor30(epgFocus)
        else if (epgFocus >= epgWindowStart + win) epgWindowStart = floor30(epgFocus) - 90 * 60000L
        ensureEpg()
    }

    fun epgOk() {
        val ch = epgList().getOrNull(epgRow) ?: return
        val sel = epgSelected(ch)
        val t = System.currentTimeMillis()
        if (sel == null || sel.start <= t) {
            goLive()
            play(ch)
        } else toggleReminder(ch, sel)
    }

    // ---------------------------------------------------------------- home

    val homeNav = listOf("Canlı TV", "Kanal rehberi", "Yayın akışı", "Ayarlar")

    fun homeFavorites(): List<Channel> {
        val v = visible()
        val f = v.filter { isFav(it) }
        return (if (f.isNotEmpty()) f else v).take(6)
    }

    fun homeRows(): List<Int> {
        val rows = arrayListOf(0, 1)
        if (homeFavorites().isNotEmpty()) rows.add(2)
        if (reminders.isNotEmpty()) rows.add(3)
        return rows
    }

    fun homeCols(row: Int): Int = when (row) {
        0 -> homeNav.size
        1 -> 2
        2 -> homeFavorites().size
        3 -> min(3, reminders.size)
        else -> 0
    }

    fun homeOk() {
        when (homeRow) {
            0 -> when (homeCol) {
                0 -> goLive()
                1 -> openGuide()
                2 -> openEpg()
                else -> openSettings()
            }
            1 -> if (homeCol == 0) goLive() else openEpg()
            2 -> {
                val ch = homeFavorites().getOrNull(homeCol)
                if (ch != null) {
                    goLive()
                    play(ch)
                }
            }
            3 -> {
                val r = sortedReminders().getOrNull(homeCol)
                val ch = if (r != null) all.firstOrNull { it.key == r.channelKey } else null
                if (ch != null) {
                    goLive()
                    play(ch)
                }
            }
        }
    }

    // ---------------------------------------------------------------- settings

    val sections = listOf(
        "Kanal yönetimi",
        "Görünüm",
        "Ses ve altyazı",
        "Ebeveyn kilidi",
        "Telefon kumandası",
        "Kanal kaynakları",
        "Hatırlatıcılar",
        "Güncelleme",
        "Hakkında"
    )

    private fun onOff(v: Boolean) = if (v) "Açık" else "Kapalı"

    private fun shorten(s: String): String = if (s.length > 42) s.take(20) + "…" + s.takeLast(18) else s

    fun settingItems(section: Int): List<SettingItem> = when (section) {
        1 -> listOf(
            SettingItem("Kanal logoları", onOff(showLogos)) {
                showLogos = !showLogos
                prefs.putBool("channelLogos", showLogos)
            },
            SettingItem("Köşe logosu (yayın sırasında)", onOff(showWatermark)) {
                showWatermark = !showWatermark
                prefs.putBool("showWatermark", showWatermark)
            },
            SettingItem("Kanal bandı süresi", "$hideSeconds sn") {
                hideSeconds = when (hideSeconds) {
                    3 -> 5
                    5 -> 8
                    8 -> 12
                    else -> 3
                }
                prefs.putInt("hideSeconds", hideSeconds)
            },
            SettingItem("Açılış imzası", onOff(splashEnabled)) {
                splashEnabled = !splashEnabled
                prefs.putBool("splash", splashEnabled)
            }
        )
        2 -> listOf(
            SettingItem("Ses izi", playback.audioLabel() ?: "Yok") { chooseAudio() },
            SettingItem("Altyazı", playback.subtitleLabel()) { chooseSubtitle() }
        )
        3 -> if (prefs.pin.isEmpty()) {
            listOf(
                SettingItem("PIN belirle") { askNewPin {} },
                SettingItem("Kilitli kanal", "${locked.size}", enabled = false)
            )
        } else {
            listOf(
                SettingItem("PIN'i değiştir") { requirePin("Mevcut PIN") { askNewPin {} } },
                SettingItem("PIN'i ve kilitleri kaldır") {
                    requirePin("Mevcut PIN") {
                        prefs.pin = ""
                        locked.clear()
                        prefs.saveSet("locked", locked)
                        toast("PIN ve kanal kilitleri kaldırıldı")
                    }
                },
                SettingItem("Kilitli kanal", "${locked.size}", enabled = false)
            )
        }
        4 -> listOf(
            SettingItem("Telefon kumandası", onOff(remoteEnabled)) {
                remoteEnabled = !remoteEnabled
                prefs.putBool("remoteEnabled", remoteEnabled)
                if (remoteEnabled) startRemote() else stopRemote()
            },
            SettingItem("Adresi yenile") {
                stopRemote()
                startRemote()
            }
        )
        5 -> listOf(
            SettingItem("Hazır internet listesi (EYMEN)", onOff(builtInList)) { toggleBuiltInList() },
            SettingItem("Uydu yoksa internetten devam et", onOff(autoOnline)) {
                autoOnline = !autoOnline
                prefs.putBool("autoOnline", autoOnline)
            },
            SettingItem("Açılmayan internet kanallarını gizle", onOff(hideDead)) { toggleHideDead() },
            SettingItem("İnternet kanallarını şimdi dene", if (testingWeb) "Deneniyor…" else "${deadWeb.size} gizli") { testWebChannels(true) },
            SettingItem("Özel M3U liste adresi", if (m3uUrl.isBlank()) "Girilmedi" else shorten(m3uUrl)) {
                pushDialog(Dialog.TextInput("Özel M3U liste adresi", m3uUrl, "Boş bırakırsan hazır EYMEN listesi kullanılır") { saveM3uUrl(it) })
            },
            SettingItem("USB'den M3U dosyası seç") { importM3uFile() },
            SettingItem("İnternet listesini yenile") { refreshOnline(true) },
            SettingItem("TV kanallarını yeniden oku") {
                loadAll(false)
                toast("Kanallar yeniden okunuyor")
            },
            SettingItem("Kanallar", "TV $tvCount · İnternet $webCount · Eşleşen $matchedCount", enabled = false)
        )
        6 -> if (reminders.isEmpty()) {
            listOf(SettingItem("Kurulu hatırlatıcı yok", enabled = false))
        } else {
            sortedReminders().map { r ->
                SettingItem("${dayTime(r.start)} · ${r.title}", r.channelName) {
                    removeReminder(r)
                    toast("Hatırlatıcı silindi")
                }
            }
        }
        7 -> {
            val list = arrayListOf(
                SettingItem("Yüklü sürüm", BuildConfig.VERSION_NAME, enabled = false),
                SettingItem("Güncellemeleri denetle", if (updateStatus.isEmpty()) null else updateStatus) { checkUpdate() }
            )
            val info = updateInfo
            if (info != null) list.add(SettingItem("Sürüm ${info.version} indir ve kur") { installUpdate(info) })
            list
        }
        8 -> listOf(
            SettingItem("EYMEN TV", "Sürüm ${BuildConfig.VERSION_NAME}", enabled = false),
            SettingItem("Geliştiren", "M. Muhammet Atasoy", enabled = false),
            SettingItem("Atasoy Yazılım", "atasoyyazilim.com", enabled = false),
            SettingItem("TV kanal izni", if (ctx.checkSelfPermission(PERM_TV) == PackageManager.PERMISSION_GRANTED) "Verildi" else "Verilmedi") {
                activity.openAppSettings()
            }
        )
        else -> emptyList()
    }

    // ---------------------------------------------------------------- phone remote

    fun startRemote() {
        if (remote != null) return
        var token = prefs.str("remoteToken", "")
        if (token.isEmpty()) {
            token = randomToken()
            prefs.putStr("remoteToken", token)
        }
        val r = RemoteServer(this, token)
        remote = r
        scope.launch {
            val ok = withContext(Dispatchers.IO) { r.start() }
            if (!ok) {
                remote = null
                remoteUrl = null
                remoteQr = null
                return@launch
            }
            val ip = withContext(Dispatchers.IO) { RemoteServer.localIp() }
            if (ip == null) {
                remoteUrl = null
                remoteQr = null
                return@launch
            }
            val url = "http://$ip:${r.port}/?t=$token"
            remoteUrl = url
            remoteQr = withContext(Dispatchers.Default) { RemoteServer.qr(url) }
        }
    }

    fun stopRemote() {
        remote?.stop()
        remote = null
        remoteUrl = null
        remoteQr = null
    }

    private fun randomToken(): String {
        val chars = "abcdefghjkmnpqrstuvwxyz23456789"
        val rnd = SecureRandom()
        return (1..6).map { chars[rnd.nextInt(chars.length)] }.joinToString("")
    }

    fun remoteState(): String {
        val o = JSONObject()
        val c = current
        o.put("channel", c?.name ?: "")
        o.put("number", c?.number ?: "")
        o.put("title", c?.now?.title ?: "")
        o.put("progress", (c?.now?.progress(System.currentTimeMillis()) ?: 0f).toDouble())
        o.put("source", if (c == null) "" else if (playback.webActive) "İnternet" else c.sourceLabel)
        o.put("textDialog", textInputActive())
        val favs = JSONArray()
        for (f in homeFavorites()) {
            val j = JSONObject()
            j.put("key", f.key)
            j.put("number", f.number)
            j.put("name", f.name)
            favs.put(j)
        }
        o.put("favorites", favs)
        return o.toString()
    }

    fun remoteSearch(q: String): String {
        val arr = JSONArray()
        for (ch in filtered("Tümü", q).take(40)) {
            val j = JSONObject()
            j.put("key", ch.key)
            j.put("number", ch.number)
            j.put("name", ch.name)
            j.put("title", ch.now?.title ?: "")
            arr.put(j)
        }
        return arr.toString()
    }

    fun remoteTune(key: String) {
        val ch = all.firstOrNull { it.key == key } ?: return
        dialogs.clear()
        goLive()
        play(ch)
    }

    fun remoteText(v: String) {
        val d = dialogs.lastOrNull()
        if (d is Dialog.TextInput) {
            d.submitted = true
            popDialog(d)
            d.onDone(v)
        } else {
            guideQuery = v.trim()
            guideTab = 0
            openGuide()
        }
    }

    fun remoteKey(k: String) {
        when (k) {
            "home" -> {
                dialogs.clear()
                goHome()
                return
            }
            "volup" -> {
                adjustVolume(AudioManager.ADJUST_RAISE)
                return
            }
            "voldown" -> {
                adjustVolume(AudioManager.ADJUST_LOWER)
                return
            }
            "mute" -> {
                adjustVolume(AudioManager.ADJUST_TOGGLE_MUTE)
                return
            }
        }
        val code = when (k) {
            "up" -> KeyEvent.KEYCODE_DPAD_UP
            "down" -> KeyEvent.KEYCODE_DPAD_DOWN
            "left" -> KeyEvent.KEYCODE_DPAD_LEFT
            "right" -> KeyEvent.KEYCODE_DPAD_RIGHT
            "ok" -> KeyEvent.KEYCODE_DPAD_CENTER
            "back" -> KeyEvent.KEYCODE_BACK
            "menu" -> KeyEvent.KEYCODE_MENU
            "guide" -> KeyEvent.KEYCODE_GUIDE
            "info" -> KeyEvent.KEYCODE_INFO
            "red" -> KeyEvent.KEYCODE_PROG_RED
            "green" -> KeyEvent.KEYCODE_PROG_GREEN
            "yellow" -> KeyEvent.KEYCODE_PROG_YELLOW
            "blue" -> KeyEvent.KEYCODE_PROG_BLUE
            "chup" -> KeyEvent.KEYCODE_CHANNEL_UP
            "chdown" -> KeyEvent.KEYCODE_CHANNEL_DOWN
            else -> if (k.length == 2 && k[0] == 'd' && k[1].isDigit()) KeyEvent.KEYCODE_0 + (k[1] - '0') else -1
        }
        if (code >= 0) onKey(code)
    }

    private fun adjustVolume(direction: Int) {
        try {
            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
        } catch (e: Exception) {
        }
    }

    // ---------------------------------------------------------------- updates

    fun checkUpdate() {
        updateStatus = "Denetleniyor…"
        scope.launch {
            try {
                val info = withContext(Dispatchers.IO) { Updater.check() }
                if (info == null) {
                    updateStatus = "Sürüm bilgisi alınamadı"
                    return@launch
                }
                if (Updater.newer(info.version, BuildConfig.VERSION_NAME)) {
                    updateInfo = info
                    updateStatus = "Yeni sürüm var: ${info.version}"
                } else {
                    updateInfo = null
                    updateStatus = "En güncel sürüm yüklü"
                }
            } catch (e: Exception) {
                updateStatus = "Denetlenemedi: ${e.message ?: "bağlantı hatası"}"
            }
        }
    }

    fun installUpdate(info: Updater.Info) {
        updateStatus = "İndiriliyor… %0"
        scope.launch {
            try {
                val f = File(File(ctx.cacheDir, "updates"), "EYMEN-TV-${info.version}.apk")
                withContext(Dispatchers.IO) {
                    Http.download(info.url, f) { p -> handler.post { updateStatus = "İndiriliyor… %$p" } }
                }
                updateStatus = "Kurulum ekranı açılıyor"
                val msg = Updater.install(activity, f)
                if (msg != null) updateStatus = msg
            } catch (e: Exception) {
                updateStatus = "İndirilemedi: ${e.message ?: "bağlantı hatası"}"
            }
        }
    }

    // ---------------------------------------------------------------- formatting

    fun time(t: Long): String = SimpleDateFormat("HH:mm", tr).format(Date(t))
    fun dateLabel(t: Long): String = SimpleDateFormat("EEEE, d MMMM", tr).format(Date(t))
    fun shortDay(t: Long): String = SimpleDateFormat("EEE d MMM", tr).format(Date(t))
    fun dayTime(t: Long): String {
        val today = floorDay(System.currentTimeMillis())
        val day = floorDay(t)
        val prefix = when (day) {
            today -> ""
            today + 86400000L -> "Yarın "
            else -> shortDay(t) + " "
        }
        return prefix + time(t)
    }

    private fun floorDay(t: Long): Long {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = t
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    // ---------------------------------------------------------------- lifecycle

    fun onStart() {
        val c = current ?: return
        playback.play(c)
    }

    fun onStop() {
        playback.stop()
    }

    fun onDestroy() {
        stopRemote()
        playback.release()
        handler.removeCallbacksAndMessages(null)
        scope.cancel()
    }

    companion object {
        const val PERM_TV = "android.permission.READ_TV_LISTINGS"
        const val DEFAULT_LIST_URL = "https://raw.githubusercontent.com/mmustafatasoy/EymenTv/main/channels/tr.m3u"
        const val EXTRA_CHANNEL_KEY = "channel_key"
        const val EXTRA_CHANNEL_NUMBER = "channel_number"
        const val EXTRA_OPEN = "open"
        const val EXTRA_SKIP_SPLASH = "skip_splash"
    }
}
