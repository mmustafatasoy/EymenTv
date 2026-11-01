package com.eymen.uydukontrol

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Opening: logo, EYMEN TV, then the hand-written "M. Muhammet Atasoy" signature drawing itself in. */
@Composable
fun Splash(app: AppState) {
    val logoScale = remember { Animatable(0.6f) }
    val logoAlpha = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val sign = remember { Animatable(0f) }
    val footAlpha = remember { Animatable(0f) }
    val screenAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        launch { logoAlpha.animateTo(1f, tween(500)) }
        logoScale.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        titleAlpha.animateTo(1f, tween(450))
        sign.animateTo(1f, tween(1400, easing = LinearEasing))
        footAlpha.animateTo(1f, tween(400))
        delay(800)
        screenAlpha.animateTo(0f, tween(450))
        app.onSplashFinished()
    }

    Box(
        Modifier.fillMaxSize()
            .alpha(screenAlpha.value)
            .background(Brush.radialGradient(listOf(Color(0xFF14204A), Color(0xFF070B16), Color(0xFF05080F)), radius = 1100f))
    ) {
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier.graphicsLayer {
                    scaleX = logoScale.value
                    scaleY = logoScale.value
                    alpha = logoAlpha.value
                }
            ) {
                BrandMark(112)
            }
            Spacer(Modifier.height(26.dp))
            Row(Modifier.alpha(titleAlpha.value), verticalAlignment = Alignment.CenterVertically) {
                T("EYMEN ", 58, C.Fg, 600, display = true, spacing = 9f)
                T("TV", 58, C.Gold, 600, display = true, spacing = 9f)
            }
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier.drawWithContent {
                    clipRect(right = size.width * sign.value) {
                        this@drawWithContent.drawContent()
                    }
                }
            ) {
                BasicText(
                    "M. Muhammet Atasoy",
                    style = TextStyle(fontFamily = FontFamily.Cursive, fontSize = 46.sp, color = C.Gold)
                )
            }
            Canvas(Modifier.size(360.dp, 10.dp)) {
                val end = size.width * sign.value
                if (end > 2f) {
                    drawLine(
                        Color(0x99E8C27A),
                        Offset(size.width * 0.06f, size.height / 2f),
                        Offset(size.width * 0.06f + end * 0.88f, size.height / 2f),
                        strokeWidth = 2.5f,
                        cap = StrokeCap.Round
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
            Box(Modifier.alpha(footAlpha.value)) {
                T("Atasoy Yazılım · atasoyyazilim.com", 17, C.Mut, 500, spacing = 1f)
            }
        }
    }
}
