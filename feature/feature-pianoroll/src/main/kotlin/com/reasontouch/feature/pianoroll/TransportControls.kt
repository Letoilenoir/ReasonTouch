package com.reasontouch.feature.pianoroll

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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


private val GREEN  = Color(0xFF3DDC84)
private val TEXT   = Color(0xFFC8C8D4)

@Composable
fun TransportControls(
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onRewind: () -> Unit,
    modifier: Modifier = Modifier
) {

    Row(
        modifier = modifier
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {

        TransportButton(
            label = "<<",
            onClick = onRewind
        )

        Spacer(modifier = Modifier.width(8.dp))

        TransportButton(
            label =
                if (isPlaying)
                    "[]"
                else
                    ">",
            active = isPlaying,
            onClick = {
                if (isPlaying)
                    onStop()
                else
                    onPlay()
            }
        )
    }
}

@Composable
fun TransportButton(
    label: String,
    onClick: () -> Unit,
    active: Boolean = false
) {

    Box(
        modifier = Modifier
            .width(42.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (active)
                    GREEN.copy(alpha = 0.14f)
                else
                    Color(0xFF202028)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = label,
            color =
                if (active)
                    GREEN
                else
                    TEXT,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}