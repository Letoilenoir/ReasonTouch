package com.reasontouch.feature.chords.workspaces

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

private val BG = Color(0xFF1A1A1E)
private val PANEL = Color(0xFF2A2A32)
private val BORDER = Color(0xFF3A3A45)
private val ACCENT = Color(0xFFE84040)
private val TEXT = Color(0xFFC8C8D4)
private val TEXT_DIM = Color(0xFF666675)

data class StartingPoint(
    val name: String,
    val description: String,
    val icon: String,
    val detail: String,
    val color: Color
)

val STARTING_POINTS = listOf(
    StartingPoint(
        "Mood",
        "Start with a feeling",
        "🎵",
        "Choose a mood (Dark, Uplifting, etc). Theory suggestions follow.",
        Color(0xFFA78BFA)
    ),
    StartingPoint(
        "Inspire",
        "Random generation",
        "✨",
        "Get a complete progression instantly. Regenerate until inspired.",
        Color(0xFF38BDF8)
    ),
    StartingPoint(
        "Progression",
        "Learn from templates",
        "🎸",
        "Start with a classic progression. Understand harmonic structure.",
        Color(0xFFF5C518)
    )
)

@Composable
fun StartingPointSelector(
    onStartingPointSelected: (StartingPoint) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(BG)
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {






        items(STARTING_POINTS) { point ->
            StartingPointCard(
                point = point,
                onSelect = { onStartingPointSelected(point) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}




@Composable
fun StartingPointCard(
    point: StartingPoint,
    onSelect: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PANEL)
            .border(2.dp, point.color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable(onClick = onSelect)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = point.icon,
                    fontSize = 24.sp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = point.name,
                        color = point.color,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = point.description,
                        color = TEXT_DIM,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1A1A1E))
                    .border(1.dp, BORDER, RoundedCornerShape(4.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = point.detail,
                    color = TEXT_DIM,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 12.sp
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(point.color.copy(alpha = 0.15f))
                    .border(1.dp, point.color.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .clickable(onClick = onSelect)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SELECT",
                    color = point.color,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
