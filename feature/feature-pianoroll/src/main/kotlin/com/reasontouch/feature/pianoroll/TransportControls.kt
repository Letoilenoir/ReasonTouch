package com.reasontouch.feature.pianoroll

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val GREEN = Color(0xFF3DDC84)
private val RED   = Color(0xFFE84040)
private val TEXT  = Color(0xFFC8C8D4)
private val PANEL = Color(0xFF202028)

@Composable
fun TransportControls(
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onRewind: () -> Unit,
    onFastForward: () -> Unit = {},
    onSkipToStart: () -> Unit = {},
    onSkipToEnd: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.height(40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        TBtn(label = "|<", onClick = onSkipToStart)
        Spacer(modifier = Modifier.width(2.dp))
        TBtn(label = "<<", onClick = onRewind)
        Spacer(modifier = Modifier.width(2.dp))
        TBtn(
            label = if (isPlaying) "[]" else ">",
            active = isPlaying,
            activeColor = if (isPlaying) RED else GREEN,
            onClick = { if (isPlaying) onStop() else onPlay() }
        )
        Spacer(modifier = Modifier.width(2.dp))
        TBtn(label = ">>", onClick = onFastForward)
        Spacer(modifier = Modifier.width(2.dp))
        TBtn(label = ">|", onClick = onSkipToEnd)
    }
}

@Composable
fun TBtn(
    label: String,
    onClick: () -> Unit,
    active: Boolean = false,
    activeColor: Color = GREEN
) {
    Box(
        modifier = Modifier
            .size(width = 34.dp, height = 28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) activeColor.copy(alpha = 0.18f) else PANEL)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (active) activeColor else TEXT,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
