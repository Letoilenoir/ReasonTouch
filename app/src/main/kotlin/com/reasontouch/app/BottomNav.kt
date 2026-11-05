package com.reasontouch.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reasontouch.feature.pianoroll.TransportControls

@Composable
fun BottomNav(
    currentRoute:     String,
    isPlaying:        Boolean,
    onPlay:           () -> Unit,
    onStop:           () -> Unit,
    onRewind:         () -> Unit,
    onFastForward:    () -> Unit,
    onSkipToStart:    () -> Unit,
    onSkipToEnd:      () -> Unit,
    onChordsClick:    () -> Unit,
    onPianoRollClick: () -> Unit,
    onDrumsClick:     () -> Unit,
    modifier:         Modifier = Modifier
) {
    val isChords = currentRoute.startsWith("chords")
    val isPianoRoll = currentRoute.startsWith("piano_roll")
    val isDrums = currentRoute.startsWith("drums")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color(0xFF222228)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Non-active tabs on the left
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isChords) {
                BottomNavTab(
                    label    = "CHORDS",
                    selected = false,
                    onClick  = onChordsClick,
                    modifier = Modifier.weight(1f)
                )
            }
            if (!isPianoRoll) {
                BottomNavTab(
                    label    = "ARRANGE",
                    selected = false,
                    onClick  = onPianoRollClick,
                    modifier = Modifier.weight(1f)
                )
            }
            if (!isDrums) {
                BottomNavTab(
                    label    = "DRUMS",
                    selected = false,
                    onClick  = onDrumsClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // TransportControls fixed on the right
        Box(
            modifier = Modifier
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            TransportControls(
                isPlaying = isPlaying,
                onPlay = onPlay,
                onStop = onStop,
                onRewind = onRewind,
                onFastForward = onFastForward,
                onSkipToStart = onSkipToStart,
                onSkipToEnd = onSkipToEnd
            )
        }
    }
}

@Composable
fun BottomNavTab(
    label:    String,
    selected: Boolean,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(56.dp)
            .clickable(onClick = onClick)
            .background(if (selected) Color(0xFF2A2A38) else Color(0xFF222228))
            .padding(top = if (selected) 0.dp else 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color(0xFFE84040))
                    .align(Alignment.CenterHorizontally)
            )
        }
        Text(
            text = label,
            color = if (selected) Color(0xFFE84040) else Color(0xFF666675),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
