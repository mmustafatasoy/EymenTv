package com.eymen.uydukontrol

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer
import java.util.Locale

object Names {
    private val marks = Regex("\\p{M}+")
    private val quality = Regex("\\b(hd|sd|fhd|uhd|4k|hevc|h265|h264)\\b")
    private val nonAlnum = Regex("[^a-z0-9]")
    private val aliases = mapOf(
        "fox" to "now",
        "foxtv" to "now",
        "nowtv" to "now",
        "star" to "startv",
        "show" to "showtv",
        "kanald" to "kanald",
        "atvturkiye" to "atv"
    )

    fun norm(s: String?): String {
        if (s.isNullOrBlank()) return ""
        var n = Normalizer.normalize(s, Normalizer.Form.NFD).replace(marks, "")
        n = n.lowercase(Locale.ROOT).replace('ı', 'i')
        n = n.replace(quality, " ")
        n = n.replace(nonAlnum, "")
        return aliases[n] ?: n
    }

    val CATEGORY_ORDER = listOf("Haber", "Spor", "Çocuk", "Belgesel", "Müzik", "Film", "Dini", "Genel", "Radyo")

    private val rules = listOf(
        "Haber" to listOf("haber", "news", "cnn", "ntv", "bloomberg", "ekoturk", "tele1", "halktv", "sozcu", "tgrt", "tvnet", "ulke", "cnbc", "global", "tv100", "flash"),
        "Spor" to listOf("spor", "sport", "bein"),
        "Çocuk" to listOf("cocuk", "minika", "kids", "cartoon", "disney", "nick", "baby", "boomerang", "cizgi"),
        "Belgesel" to listOf("belgesel", "discovery", "natgeo", "national", "history", "animal", "dmax", "viasat", "tlc", "documentary"),
        "Müzik" to listOf("muzik", "music", "kral", "power", "dream", "number1", "mtv"),
        "Film" to listOf("sinema", "film", "movie", "dizi", "cine"),
        "Dini" to listOf("diyanet", "semerkand", "lalegul", "hilal", "mesaj", "religious")
    )

    fun category(norm: String, group: String?, radio: Boolean): String {
        if (radio) return "Radyo"
        val g = norm(group)
        if (g.isNotEmpty()) {
            for ((cat, words) in rules) if (words.any { g.contains(it) }) return cat
            if (g.contains("radyo") || g.contains("radio")) return "Radyo"
        }
        for ((cat, words) in rules) if (words.any { norm.contains(it) }) return cat
        return "Genel"
    }
}

object Labels {
    fun format(vf: String?): String? = when (vf) {
        "VIDEO_FORMAT_240P" -> "240p"
        "VIDEO_FORMAT_360P" -> "360p"
        "VIDEO_FORMAT_480I" -> "480i"
        "VIDEO_FORMAT_480P" -> "480p"
        "VIDEO_FORMAT_576I" -> "576i"
        "VIDEO_FORMAT_576P" -> "576p"
        "VIDEO_FORMAT_720P" -> "720p"
        "VIDEO_FORMAT_1080I" -> "1080i"
        "VIDEO_FORMAT_1080P" -> "1080p"
        "VIDEO_FORMAT_2160P" -> "4K"
        "VIDEO_FORMAT_4320P" -> "8K"
        else -> null
    }

    fun type(t: String?): String? {
        if (t == null) return null
        val code = t.removePrefix("TYPE_").replace('_', '-')
        return when {
            t.startsWith("TYPE_DVB_S") || t.startsWith("TYPE_ISDB_S") || t.startsWith("TYPE_ATSC_S") -> "Uydu · $code"
            t.startsWith("TYPE_DVB_T") || t.startsWith("TYPE_ISDB_T") || t.startsWith("TYPE_ATSC_T") || t.startsWith("TYPE_DTMB") -> "Karasal · $code"
            t.startsWith("TYPE_DVB_C") || t.startsWith("TYPE_ISDB_C") || t.startsWith("TYPE_ATSC_C") -> "Kablo · $code"
            else -> null
        }
    }

    fun language(code: String?): String = when (code?.lowercase(Locale.ROOT)) {
        null, "", "und" -> "Dil belirtilmemiş"
        "tur", "tr" -> "Türkçe"
        "eng", "en" -> "İngilizce"
        "ara", "ar" -> "Arapça"
        "kur", "ku", "kmr" -> "Kürtçe"
        "deu", "ger", "de" -> "Almanca"
        "fra", "fre", "fr" -> "Fransızca"
        "rus", "ru" -> "Rusça"
        "aze", "az" -> "Azerice"
        else -> code?.uppercase(Locale.ROOT) ?: ""
    }

    fun audioChannels(count: Int): String = when (count) {
        1 -> "Mono"
        2 -> "Stereo"
        6 -> "5.1"
        8 -> "7.1"
        else -> ""
    }
}

object TvSource {
    private const val DTVKIT = "com.droidlogic.dtvkit.inputsource/"

    class Result(val channels: List<Channel>, val error: String?, val rows: Int, val inputs: Int)

    fun load(ctx: Context): Result {
        val raw = ArrayList<Channel>()
        var err: String? = null
        var rows = 0
        var inputs = 0
        try {
            val m = ctx.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager
            inputs = m?.tvInputList?.size ?: 0
        } catch (e: Exception) {
        }
        try {
            val cursor = ctx.contentResolver.query(TvContract.Channels.CONTENT_URI, null, null, null, null)
            if (cursor == null) {
                err = "TV kanal sağlayıcısı yanıt vermedi. Bu TV, kanallarını uygulamalara kapatmış olabilir."
            } else {
                cursor.use { c ->
                    rows = c.count
                    val iId = c.getColumnIndex("_id")
                    val iNum = c.getColumnIndex("display_number")
                    val iName = c.getColumnIndex("display_name")
                    val iInput = c.getColumnIndex("input_id")
                    val iVf = c.getColumnIndex("video_format")
                    val iType = c.getColumnIndex("type")
                    val iSvc = c.getColumnIndex("service_type")
                    while (c.moveToNext()) {
                        if (iId < 0) break
                        val input = if (iInput >= 0) c.getString(iInput) else null
                        if (input.isNullOrBlank()) continue
                        val id = c.getLong(iId)
                        val num = if (iNum >= 0) c.getString(iNum) else null
                        val name = (if (iName >= 0) c.getString(iName) else null) ?: (num ?: "Kanal $id")
                        val vf = if (iVf >= 0) c.getString(iVf) else null
                        val type = if (iType >= 0) c.getString(iType) else null
                        val svc = if (iSvc >= 0) c.getString(iSvc) else null
                        raw.add(
                            Channel(
                                key = id.toString(),
                                source = Source.TV,
                                tvId = id,
                                inputId = input,
                                numRaw = num,
                                originalName = name,
                                streamUrl = null,
                                videoFormat = vf,
                                type = type,
                                isRadio = svc == "SERVICE_TYPE_AUDIO",
                                logoUrl = null,
                                groupTitle = null
                            )
                        )
                    }
                }
            }
        } catch (e: SecurityException) {
            err = "TV kanallarını okuma izni verilmedi."
        } catch (e: Exception) {
            err = "Kanal listesi okunamadı: ${e.message}"
        }
        // Whale TVs: keep the known-good DTVKit list. Other TVs: use whatever TIF exposes.
        val dtv = raw.any { it.inputId?.startsWith(DTVKIT) == true }
        val list = raw.filter { !dtv || it.inputId?.startsWith(DTVKIT) == true }
            .sortedWith(compareBy<Channel>({ it.number.toIntOrNull() ?: Int.MAX_VALUE }, { it.number }))
        return Result(list, err, rows, inputs)
    }

    fun programs(ctx: Context, tvId: Long, from: Long, to: Long): List<Program> {
        val out = ArrayList<Program>()
        try {
            val uri = TvContract.buildProgramsUriForChannel(tvId, from, to)
            val projection = arrayOf("title", "short_description", "start_time_utc_millis", "end_time_utc_millis")
            ctx.contentResolver.query(uri, projection, null, null, "start_time_utc_millis ASC")?.use { c ->
                while (c.moveToNext()) {
                    val start = c.getLong(2)
                    val end = c.getLong(3)
                    if (end <= start) continue
                    out.add(Program(c.getString(0) ?: "Program", c.getString(1), start, end))
                }
            }
        } catch (e: Exception) {
        }
        return out
    }
}

object Http {
    fun get(url: String, timeoutMs: Int = 15000): ByteArray {
        var current = url
        for (i in 0 until 6) {
            val conn = URL(current).openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = timeoutMs
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "EYMEN-TV/2.0 (Android TV)")
            conn.setRequestProperty("Accept", "*/*")
            val code = conn.responseCode
            if (code in 300..399) {
                val loc = conn.getHeaderField("Location")
                conn.disconnect()
                if (loc == null) throw IOException("Yönlendirme adresi yok")
                current = URL(URL(current), loc).toString()
                continue
            }
            if (code != 200) {
                conn.disconnect()
                throw IOException("Sunucu yanıtı: HTTP $code")
            }
            return conn.inputStream.use { it.readBytes() }
        }
        throw IOException("Çok fazla yönlendirme")
    }

    fun text(url: String): String = String(get(url), Charsets.UTF_8)

    fun download(url: String, target: File, progress: (Int) -> Unit) {
        var current = url
        for (i in 0 until 6) {
            val conn = URL(current).openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "EYMEN-TV/2.0 (Android TV)")
            val code = conn.responseCode
            if (code in 300..399) {
                val loc = conn.getHeaderField("Location")
                conn.disconnect()
                if (loc == null) throw IOException("Yönlendirme adresi yok")
                current = URL(URL(current), loc).toString()
                continue
            }
            if (code != 200) {
                conn.disconnect()
                throw IOException("Sunucu yanıtı: HTTP $code")
            }
            val total = conn.contentLength
            target.parentFile?.mkdirs()
            conn.inputStream.use { input ->
                target.outputStream().use { out ->
                    val buf = ByteArray(16384)
                    var done = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        if (total > 0) progress(((done * 100) / total).toInt())
                    }
                }
            }
            return
        }
        throw IOException("Çok fazla yönlendirme")
    }
}

object M3u {
    private val attr = Regex("([A-Za-z0-9_-]+)=\"([^\"]*)\"")

    fun parse(text: String): List<Channel> {
        val out = ArrayList<Channel>()
        val seen = HashSet<String>()
        var name: String? = null
        var logo: String? = null
        var group: String? = null
        for (rawLine in text.lineSequence()) {
            val line = rawLine.trim().removePrefix("﻿")
            if (line.isEmpty()) continue
            if (line.startsWith("#EXTINF", ignoreCase = true)) {
                val attrs = HashMap<String, String>()
                for (m in attr.findAll(line)) attrs[m.groupValues[1].lowercase(Locale.ROOT)] = m.groupValues[2]
                logo = attrs["tvg-logo"]?.takeIf { it.isNotBlank() }
                group = attrs["group-title"]
                val comma = line.lastIndexOf(',')
                name = if (comma >= 0 && comma < line.length - 1) line.substring(comma + 1).trim() else attrs["tvg-name"]
                continue
            }
            if (line.startsWith("#")) continue
            val n = name
            if (n != null && (line.startsWith("http://", true) || line.startsWith("https://", true))) {
                val norm = Names.norm(n)
                val key = "web:" + (if (norm.isNotEmpty()) norm else Integer.toHexString(n.hashCode()))
                if (seen.add(key)) {
                    out.add(
                        Channel(
                            key = key,
                            source = Source.WEB,
                            tvId = -1L,
                            inputId = null,
                            numRaw = null,
                            originalName = n,
                            streamUrl = line,
                            videoFormat = null,
                            type = null,
                            isRadio = false,
                            logoUrl = logo,
                            groupTitle = group
                        )
                    )
                }
            }
            name = null
            logo = null
            group = null
        }
        return out
    }

    fun cacheFile(ctx: Context): File = File(ctx.filesDir, "online.m3u")

    fun readCache(ctx: Context): String? = try {
        val f = cacheFile(ctx)
        if (f.exists()) f.readText() else null
    } catch (e: Exception) {
        null
    }

    fun writeCache(ctx: Context, text: String) {
        try {
            cacheFile(ctx).writeText(text)
        } catch (e: Exception) {
        }
    }
}

object Logos {
    private val aliases = mapOf(
        "trt1" to "trt-1-hd",
        "showtv" to "show",
        "startv" to "star-tv",
        "now" to "now",
        "cnnturk" to "cnn-turk",
        "ahaber" to "a-haber",
        "aspor" to "a-spor"
    )
    private var assetIndex: Map<String, String>? = null

    private fun index(ctx: Context): Map<String, String> {
        assetIndex?.let { return it }
        val map = HashMap<String, String>()
        try {
            for (file in ctx.assets.list("logos") ?: emptyArray()) {
                if (!file.endsWith(".png")) continue
                val base = file.removeSuffix(".png")
                map[base.replace(Regex("[^a-z0-9]"), "")] = base
            }
        } catch (e: Exception) {
        }
        assetIndex = map
        return map
    }

    /** The logos that ship with the app, the ones that look like this channel's name first. */
    fun bundled(ctx: Context, ch: Channel): List<String> {
        val names = try {
            (ctx.assets.list("logos") ?: emptyArray()).filter { it.endsWith(".png") }.map { it.removeSuffix(".png") }
        } catch (e: Exception) {
            emptyList()
        }
        val key = ch.norm
        fun score(name: String): Int {
            val n = name.replace(Regex("[^a-z0-9]"), "")
            return when {
                n == key -> 0
                key.isNotEmpty() && (n.contains(key) || key.contains(n)) -> 1
                key.length >= 3 && n.startsWith(key.take(3)) -> 2
                else -> 3
            }
        }
        return names.sortedWith(compareBy<String>({ score(it) }, { it }))
    }

    fun asset(ctx: Context, name: String): Bitmap? = try {
        decode(ctx.assets.open("logos/$name.png"), 160, 90)
    } catch (e: Exception) {
        null
    }

    fun saveAsset(ctx: Context, key: String, name: String): Bitmap =
        ctx.assets.open("logos/$name.png").use { saveCustom(ctx, key, it) }

    fun thumb(file: File): Bitmap? = try {
        decode(file.inputStream(), 160, 90)
    } catch (e: Exception) {
        null
    }

    fun customFile(ctx: Context, key: String): File {
        val safe = key.replace(Regex("[^A-Za-z0-9_-]"), "_")
        return File(File(ctx.filesDir, "channel_logos"), "channel_$safe.png")
    }

    private fun decode(input: InputStream?, maxW: Int = 320, maxH: Int = 180): Bitmap? {
        if (input == null) return null
        val bytes = input.use { it.readBytes() }
        val o = BitmapFactory.Options()
        o.inJustDecodeBounds = true
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
        if (o.outWidth <= 0 || o.outHeight <= 0) return null
        var sample = 1
        while (o.outWidth / sample > maxW * 2 || o.outHeight / sample > maxH * 2) sample *= 2
        val d = BitmapFactory.Options()
        d.inSampleSize = sample
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, d)
    }

    /** Order: user's own logo → bundled logo → TV provider logo → M3U logo. Runs off the main thread. */
    fun load(ctx: Context, ch: Channel): Bitmap? {
        try {
            val f = customFile(ctx, ch.key)
            if (f.exists()) decode(f.inputStream())?.let { return it }
        } catch (e: Exception) {
        }
        try {
            val idx = index(ctx)
            val name = aliases[ch.norm] ?: idx[ch.norm]
            if (name != null) decode(ctx.assets.open("logos/$name.png"))?.let { return it }
        } catch (e: Exception) {
        }
        if (ch.source == Source.TV) {
            try {
                decode(ctx.contentResolver.openInputStream(TvContract.buildChannelLogoUri(ch.tvId)))?.let { return it }
            } catch (e: Exception) {
            }
        }
        val url = ch.logoUrl ?: ch.web?.logoUrl
        if (url != null) {
            try {
                val bytes = Http.get(url, 8000)
                decode(bytes.inputStream())?.let { return it }
            } catch (e: Exception) {
            }
        }
        return null
    }

    fun saveCustom(ctx: Context, key: String, input: InputStream): Bitmap {
        val bmp = decode(input, 512, 512) ?: throw IOException("Resim okunamadı")
        val f = customFile(ctx, key)
        f.parentFile?.mkdirs()
        val tmp = File(f.parentFile, f.name + ".tmp")
        tmp.outputStream().use { out ->
            if (!bmp.compress(Bitmap.CompressFormat.PNG, 100, out)) throw IOException("Logo kaydedilemedi")
        }
        if (!tmp.renameTo(f)) {
            tmp.copyTo(f, overwrite = true)
            tmp.delete()
        }
        return bmp
    }
}

/** Pictures on a USB stick, for choosing a channel logo with the remote. */
object UsbImages {
    private val extensions = listOf("png", "jpg", "jpeg", "webp")

    fun scan(limit: Int = 60): List<File> {
        val out = ArrayList<File>()
        for (base in listOf("/storage", "/mnt/media_rw")) {
            val roots = try {
                File(base).listFiles()
            } catch (e: Exception) {
                null
            } ?: continue
            for (root in roots) {
                if (root.isDirectory && root.name != "emulated" && root.name != "self") walk(root, 0, out, limit)
            }
        }
        return out.distinctBy { it.name + it.length() }
    }

    private fun walk(dir: File, depth: Int, out: MutableList<File>, limit: Int) {
        if (depth > 3 || out.size >= limit) return
        val list = try {
            dir.listFiles()
        } catch (e: Exception) {
            null
        } ?: return
        for (f in list) {
            if (out.size >= limit) return
            if (f.isDirectory) {
                if (!f.name.startsWith(".") && f.name != "Android") walk(f, depth + 1, out, limit)
            } else if (f.extension.lowercase(Locale.ROOT) in extensions && f.length() < 15_000_000L) {
                out.add(f)
            }
        }
    }
}

/** Checks whether an internet stream answers from this TV's network (HLS playlist or a video response). */
object WebHealth {
    fun check(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        var current: String = url
        try {
            for (i in 0 until 5) {
                val conn = URL(current).openConnection() as HttpURLConnection
                conn.connectTimeout = 6000
                conn.readTimeout = 6000
                conn.instanceFollowRedirects = false
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 11; Android TV) EYMEN-TV/2.1")
                conn.setRequestProperty("Accept", "*/*")
                val code = conn.responseCode
                if (code in 300..399) {
                    val loc = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (loc == null) return false
                    current = URL(URL(current), loc).toString()
                    continue
                }
                if (code !in 200..299) {
                    conn.disconnect()
                    return false
                }
                val type = (conn.contentType ?: "").lowercase(Locale.ROOT)
                val head = ByteArray(4096)
                var read = 0
                conn.inputStream.use { input ->
                    while (read < head.size) {
                        val n = input.read(head, read, head.size - read)
                        if (n < 0) break
                        read += n
                    }
                }
                conn.disconnect()
                val text = String(head, 0, read, Charsets.ISO_8859_1)
                return text.contains("#EXTM3U") || type.contains("mpegurl") || type.startsWith("video/") ||
                    type.contains("octet-stream") || type.contains("mp2t")
            }
        } catch (e: Exception) {
            return false
        }
        return false
    }
}
