package com.reasontouch.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
fun BottomNav(
    currentRoute:     String,
    onChordsClick:    () -> Unit,
    onPianoRollClick: () -> Unit,
    onDrumsClick:     () -> Unit,
    modifier:         Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color(0xFF222228)),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        BottomNavTab(
            label    = "CHORDS",
            selected = currentRoute.startsWith("chords"),
            onClick  = onChordsClick,
            modifier = Modifier.weight(1f)
        )
        BottomNavTab(
            label    = "ARRANGE",
            selected = currentRoute.startsWith("piano_roll"),
            onClick  = onPianoRollClick,
            modifier = Modifier.weight(1f)
        )
        BottomNavTab(
            label    = "DRUMS",
            selected = currentRoute.startsWith("drums"),
            onClick  = onDrumsClick,
            modifier = Modifier.weight(1f)
        )
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