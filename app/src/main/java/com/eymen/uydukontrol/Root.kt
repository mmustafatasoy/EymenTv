package com.eymen.uydukontrol

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun EymenRoot(app: AppState) {
    DesignCanvas {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            val videoModifier = if (app.screen == Screen.EPG) {
                Modifier.offset(832.dp, 36.dp).size(384.dp, 216.dp)
            } else {
                Modifier.fillMaxSize()
            }
            AndroidView(factory = { app.playback.tvView }, modifier = videoModifier)
            AndroidView(factory = { app.playback.texture }, modifier = videoModifier)

            when (app.screen) {
                Screen.SPLASH -> {}
                Screen.LIVE -> LiveLayer(app)
                Screen.HOME -> HomeScreen(app)
                Screen.EPG -> EpgScreen(app)
                Screen.SETTINGS -> SettingsScreen(app)
            }

            if (app.screen == Screen.SPLASH) Splash(app)

            ToastLayer(app)

            val top = app.dialogs.lastOrNull()
            if (top != null) DialogLayer(app, top)
        }
    }
}

@Composable
fun ToastLayer(app: AppState) {
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = app.toastText != null,
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 36.dp)
        ) {
            Row(
                Modifier.panel(Color(0xF00A1224), 999, Color(0x40E8C27A)).padding(horizontal = 22.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BrandMark(18)
                T(app.toastText ?: "", 16, C.Fg, 600)
            }
        }
    }
}
