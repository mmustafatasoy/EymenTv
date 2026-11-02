package com.eymen.uydukontrol

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** "M. Muhammet Atasoy" writing itself from left to right as [progress] goes from 0 to 1. */
@Composable
fun SignatureLine(progress: Float, fontSize: Int, lineWidth: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(
            Modifier.drawWithContent {
                clipRect(right = size.width * progress) {
                    this@drawWithContent.drawContent()
                }
            }
        ) {
            BasicText(
                "M. Muhammet Atasoy",
                style = TextStyle(fontFamily = Fonts.signature(), fontSize = fontSize.sp, color = C.Gold)
            )
        }
        Canvas(Modifier.size(lineWidth.dp, 8.dp)) {
            val end = size.width * progress
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
    }
}
