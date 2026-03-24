package com.reasontouch.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TransportBar(
    bpm: Int,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onRewind: () -> Unit,
    onExport: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color(0xFF222228))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TransportButton(label = "REW", onClick = onRewind)
        TransportButton(
            label = if (isPlaying) "PAUSE" else "PLAY",
            onClick = onPlay,
            active = isPlaying,
            activeColor = Color(0xFF3DDC84)
        )
        TransportButton(label = "STOP", onClick = onStop)
        Text(
            text = "$bpm BPM",
            color = Color(0xFFFF6B35),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f).padding(start = 4.dp)
        )
        TransportButton(label = "MIDI", onClick = onExport)
    }
}

@Composable
fun TransportButton(
    label: String,
    onClick: () -> Unit,
    active: Boolean = false,
    activeColor: Color = Color(0xFFE84040)
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) activeColor else Color(0xFF2A2A32),
            contentColor = Color.White
        ),
        modifier = Modifier.height(34.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 8.dp, vertical = 0.dp
        )
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}