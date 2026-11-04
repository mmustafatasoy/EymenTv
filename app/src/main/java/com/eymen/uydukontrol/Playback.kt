package com.eymen.uydukontrol

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.tv.TvContentRating
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.media.tv.TvTrackInfo
import android.media.tv.TvView
import android.os.Handler
import android.os.Looper
import android.view.TextureView
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Plays a channel either from the TV tuner (TvView) or from the internet (ExoPlayer).
 * When the satellite signal drops, the same channel continues from its internet stream;
 * the tuner stays tuned underneath (muted) and playback returns to satellite as soon as
 * the picture comes back.
 */
class Playback(private val ctx: Context, private val app: AppState) {
    val tvView = TvView(ctx)
    val texture = TextureView(ctx)
    val exo: ExoPlayer = ExoPlayer.Builder(ctx).build()

    private val audio = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val handler = Handler(Looper.getMainLooper())
    private var epoch = 0
    private var fallbackPending: Runnable? = null
    private var focusRequest: AudioFocusRequest? = null

    var webActive by mutableStateOf(false)
    var fallbackActive by mutableStateOf(false)
    var videoAvailable by mutableStateOf(false)
    var status by mutableStateOf("")
    var playingChannel: Channel? = null
    private var webUrl: String? = null

    val audioTracks = mutableStateListOf<TvTrackInfo>()
    val subtitleTracks = mutableStateListOf<TvTrackInfo>()
    var selectedAudio by mutableStateOf<String?>(null)
    var selectedSubtitle by mutableStateOf<String?>(null)

    init {
        tvView.isFocusable = false
        tvView.isFocusableInTouchMode = false
        texture.isFocusable = false
        texture.visibility = View.INVISIBLE
        exo.setVideoTextureView(texture)
        exo.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (!webActive) return
                status = "İnternet yayını açılamadı"
                app.toast("İnternet yayını açılamadı")
                app.onWebPlaybackError(webUrl)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying && webActive) status = ""
            }
        })
    }

    fun play(ch: Channel) {
        val token = ++epoch
        playingChannel = ch
        cancelFallback()
        stopWeb()
        fallbackActive = false
        videoAvailable = false
        audioTracks.clear()
        subtitleTracks.clear()
        selectedAudio = null
        selectedSubtitle = null
        requestFocus()

        if (ch.source == Source.WEB) {
            try {
                tvView.reset()
            } catch (e: Exception) {
            }
            val url = ch.streamUrl
            if (url == null) {
                status = "Bu kanalın yayın adresi yok"
                return
            }
            status = "Yayın açılıyor…"
            startWeb(url)
            return
        }

        status = "Kanal açılıyor…"
        tvView.setCallback(object : TvView.TvInputCallback() {
            var audioChosen = false

            override fun onVideoAvailable(inputId: String?) {
                if (token != epoch) return
                videoAvailable = true
                status = ""
                cancelFallback()
                if (fallbackActive) {
                    stopWeb()
                    fallbackActive = false
                    app.toast("Uydu sinyali geri geldi")
                }
                tvView.setStreamVolume(1f)
            }

            override fun onVideoUnavailable(inputId: String?, reason: Int) {
                if (token != epoch) return
                videoAvailable = false
                if (ch.isRadio) {
                    // Radio has no picture by design; the on-screen ad fills the frame.
                    status = ""
                    return
                }
                if (!fallbackActive) {
                    status = when (reason) {
                        TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING -> "Kanal ayarlanıyor…"
                        TvInputManager.VIDEO_UNAVAILABLE_REASON_WEAK_SIGNAL -> "Uydu sinyali zayıf"
                        TvInputManager.VIDEO_UNAVAILABLE_REASON_BUFFERING -> "Yayın hazırlanıyor…"
                        TvInputManager.VIDEO_UNAVAILABLE_REASON_AUDIO_ONLY -> "Yalnızca ses yayını"
                        else -> "Görüntü bekleniyor"
                    }
                }
                if (reason == TvInputManager.VIDEO_UNAVAILABLE_REASON_WEAK_SIGNAL) scheduleFallback(ch, token, 4000L)
            }

            override fun onConnectionFailed(inputId: String?) {
                if (token != epoch) return
                status = "Yayın servisine bağlanılamadı"
                startFallback(ch, token)
            }

            override fun onDisconnected(inputId: String?) {
                if (token != epoch) return
                status = "Yayın bağlantısı kesildi"
                startFallback(ch, token)
            }

            override fun onContentBlocked(inputId: String?, rating: TvContentRating?) {
                if (token != epoch) return
                status = "TV'nin ebeveyn denetimi bu yayını engelledi"
            }

            override fun onTracksChanged(inputId: String?, tracks: List<TvTrackInfo>?) {
                if (token != epoch) return
                audioTracks.clear()
                subtitleTracks.clear()
                if (tracks != null) {
                    for (t in tracks) {
                        if (t.type == TvTrackInfo.TYPE_AUDIO) audioTracks.add(t)
                        else if (t.type == TvTrackInfo.TYPE_SUBTITLE) subtitleTracks.add(t)
                    }
                }
                if (!audioChosen && audioTracks.isNotEmpty()) {
                    audioChosen = true
                    val preferred = audioTracks.firstOrNull { it.language == "tur" || it.language == "tr" } ?: audioTracks[0]
                    try {
                        tvView.selectTrack(TvTrackInfo.TYPE_AUDIO, preferred.id)
                    } catch (e: Exception) {
                    }
                }
            }

            override fun onTrackSelected(inputId: String?, type: Int, trackId: String?) {
                if (token != epoch) return
                if (type == TvTrackInfo.TYPE_AUDIO) selectedAudio = trackId
                else if (type == TvTrackInfo.TYPE_SUBTITLE) selectedSubtitle = trackId
            }
        })
        try {
            tvView.tune(ch.inputId ?: "", TvContract.buildChannelUri(ch.tvId))
            tvView.setStreamVolume(1f)
        } catch (e: Exception) {
            status = "Kanal açılamadı: ${e.message ?: e.javaClass.simpleName}"
        }
        // No picture at all after 9 seconds: try the internet stream.
        scheduleFallback(ch, token, 9000L)
    }

    private fun scheduleFallback(ch: Channel, token: Int, delay: Long) {
        if (fallbackActive || fallbackPending != null) return
        val r = Runnable {
            fallbackPending = null
            if (token == epoch && !videoAvailable) startFallback(ch, token)
        }
        fallbackPending = r
        handler.postDelayed(r, delay)
    }

    private fun cancelFallback() {
        fallbackPending?.let { handler.removeCallbacks(it) }
        fallbackPending = null
    }

    private fun startFallback(ch: Channel, token: Int) {
        if (token != epoch || fallbackActive) return
        cancelFallback()
        if (!app.autoOnline) {
            status = "Uydu sinyali yok"
            return
        }
        val url = ch.web?.streamUrl
        if (url == null) {
            status = "Uydu sinyali yok · Bu kanalın internet yayını listede yok"
            return
        }
        fallbackActive = true
        try {
            tvView.setStreamVolume(0f)
        } catch (e: Exception) {
        }
        status = "İnternet yayınına geçiliyor…"
        startWeb(url)
        app.toast("Uydu sinyali yok, internetten devam ediliyor")
    }

    /** Lets the user switch the current TV channel to its internet stream by hand. */
    fun switchToWeb(ch: Channel): Boolean {
        val url = ch.web?.streamUrl ?: return false
        cancelFallback()
        fallbackActive = true
        try {
            tvView.setStreamVolume(0f)
        } catch (e: Exception) {
        }
        startWeb(url)
        return true
    }

    private fun startWeb(url: String) {
        val item = if (url.contains(".m3u8", ignoreCase = true)) {
            MediaItem.Builder().setUri(url).setMimeType(MimeTypes.APPLICATION_M3U8).build()
        } else {
            MediaItem.fromUri(url)
        }
        webUrl = url
        exo.setMediaItem(item)
        exo.prepare()
        exo.playWhenReady = true
        exo.volume = 1f
        webActive = true
        texture.visibility = View.VISIBLE
    }

    fun stopWeb() {
        if (webActive) {
            try {
                exo.stop()
                exo.clearMediaItems()
            } catch (e: Exception) {
            }
        }
        webActive = false
        texture.visibility = View.INVISIBLE
    }

    fun selectAudio(track: TvTrackInfo) {
        try {
            tvView.selectTrack(TvTrackInfo.TYPE_AUDIO, track.id)
        } catch (e: Exception) {
            app.toast("Ses izi seçilemedi")
        }
    }

    fun selectSubtitle(track: TvTrackInfo?) {
        try {
            if (track == null) {
                tvView.selectTrack(TvTrackInfo.TYPE_SUBTITLE, null)
                tvView.setCaptionEnabled(false)
            } else {
                tvView.setCaptionEnabled(true)
                tvView.selectTrack(TvTrackInfo.TYPE_SUBTITLE, track.id)
            }
        } catch (e: Exception) {
            app.toast("Altyazı değiştirilemedi")
        }
    }

    fun audioLabel(): String? {
        if (webActive) return null
        val t = audioTracks.firstOrNull { it.id == selectedAudio } ?: audioTracks.firstOrNull() ?: return null
        val ch = Labels.audioChannels(t.audioChannelCount)
        val lang = Labels.language(t.language)
        return if (ch.isEmpty()) lang else "$lang · $ch"
    }

    fun subtitleLabel(): String {
        val t = subtitleTracks.firstOrNull { it.id == selectedSubtitle } ?: return "Kapalı"
        return Labels.language(t.language)
    }

    private fun requestFocus() {
        val req = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build()
            )
            .setOnAudioFocusChangeListener({ change -> onFocusChange(change) }, handler)
            .build()
            .also { focusRequest = it }
        try {
            audio.requestAudioFocus(req)
        } catch (e: Exception) {
        }
    }

    private fun onFocusChange(change: Int) {
        val gain = change == AudioManager.AUDIOFOCUS_GAIN
        try {
            if (!fallbackActive) tvView.setStreamVolume(if (gain) 1f else 0f)
            exo.volume = if (gain) 1f else 0f
        } catch (e: Exception) {
        }
    }

    fun stop() {
        epoch++
        cancelFallback()
        try {
            tvView.reset()
        } catch (e: Exception) {
        }
        stopWeb()
        fallbackActive = false
        videoAvailable = false
        focusRequest?.let {
            try {
                audio.abandonAudioFocusRequest(it)
            } catch (e: Exception) {
            }
        }
    }

    fun release() {
        stop()
        try {
            exo.release()
        } catch (e: Exception) {
        }
    }
}
