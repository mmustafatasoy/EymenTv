package com.eymen.uydukontrol

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import android.view.TextureView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay

private const val FIRST_DELAY = 90_000L        // ilk gösterim açılıştan 90 sn sonra
private const val EVERY = 60L * 60L * 1000L    // sonra saatte bir
private const val ASSET = "asset:///ads/alt_reklam.mp4"

/**
 * The Atasoy Yazılım promo strip (atasoyyazilim.com) that slides up at the bottom of the
 * live picture once an hour and plays the bundled 18-second clip, silently so the channel
 * audio keeps playing, then slides away.
 */
@Composable
fun BottomAd(app: AppState, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(ctx).build().apply {
            volume = 0f
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }
    val view = remember { TextureView(ctx).also { player.setVideoTextureView(it) } }
    var playing by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    player.playWhenReady = false
                    playing = false
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                player.playWhenReady = false
                playing = false
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(Unit) {
        if (app.bottomAdNextAt == 0L) app.bottomAdNextAt = System.currentTimeMillis() + FIRST_DELAY
        while (true) {
            delay(15_000L)
            val canShow = app.screen == Screen.LIVE && app.current != null &&
                app.current?.isRadio != true && !app.guideOpen && app.dialogs.isEmpty()
            if (!playing && canShow && System.currentTimeMillis() >= app.bottomAdNextAt) {
                app.bottomAdNextAt = System.currentTimeMillis() + EVERY
                try {
                    player.setMediaItem(MediaItem.fromUri(ASSET))
                    player.prepare()
                    player.seekTo(0)
                    player.volume = 0f
                    player.playWhenReady = true
                    playing = true
                } catch (e: Exception) {
                    playing = false
                }
            }
        }
    }

    AnimatedVisibility(
        visible = playing,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it },
        modifier = modifier.fillMaxWidth()
    ) {
        Box(Modifier.fillMaxWidth().height(170.dp)) {
            AndroidView(factory = { view }, modifier = Modifier.fillMaxSize())
        }
    }
}
