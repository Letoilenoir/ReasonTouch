package com.reasontouch.feature.chords

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import com.reasontouch.feature.chords.components.StartingPointCard
import com.reasontouch.feature.chords.model.StartingPoint

private val BG = Color(0xFF141418)
private val PANEL = Color(0xFF222228)
private val PANEL_ALT = Color(0xFF2A2A32)
private val BORDER = Color(0xFF3A3A45)

private val TEXT = Color(0xFFC8C8D4)
private val TEXT_DIM = Color(0xFF666675)

private val ACCENT = Color(0xFFE84040)
private val GREEN = Color(0xFF3DDC84)
private val PURPLE = Color(0xFFA78BFA)
@Composable
fun ChordScreenV2(
    sessionId: String
) {

    var selectedStartingPoint by remember {
        mutableStateOf(StartingPoint.MOOD)
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BG)
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {

        // ─────────────────────────────────────────────
        // TITLE
        // ─────────────────────────────────────────────

        Text(
            text = "COMPOSITION WORKSPACE",
            color = TEXT,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ─────────────────────────────────────────────
        // STARTING POINT
        // ─────────────────────────────────────────────

        SectionCard(
            title = "STARTING POINT"
        ) {

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {

                StartingPointCard(
                    title = "Inspire Me",
                    subtitle = "Generate\nSomething",
                    icon = "✨",
                    selected = selectedStartingPoint == StartingPoint.INSPIRE,
                    onClick = {
                        selectedStartingPoint = StartingPoint.INSPIRE
                    }
                )

                StartingPointCard(
                    title = "Mood",
                    subtitle = "Choose a\nFeeling",
                    icon = "💜",
                    selected = selectedStartingPoint == StartingPoint.MOOD,
                    onClick = {
                        selectedStartingPoint = StartingPoint.MOOD
                    }
                )

                StartingPointCard(
                    title = "Progression",
                    subtitle = "Start with a\nChord Idea",
                    icon = "🎵",
                    selected = selectedStartingPoint == StartingPoint.PROGRESSION,
                    onClick = {
                        selectedStartingPoint = StartingPoint.PROGRESSION
                    }
                )

                StartingPointCard(
                    title = "Manual",
                    subtitle = "Build from\nScratch",
                    icon = "✏️",
                    selected = selectedStartingPoint == StartingPoint.MANUAL,
                    onClick = {
                        selectedStartingPoint = StartingPoint.MANUAL
                    }
                )
            }
        }
        when (selectedStartingPoint) {

            StartingPoint.INSPIRE -> {
            Text("Inspire Workspace")
        }

            StartingPoint.MOOD -> {
            MoodWorkspace()
        }

            StartingPoint.PROGRESSION -> {
            Text("Progression Workspace")
        }

            StartingPoint.MANUAL -> {
            Text("Manual Workspace")
        }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ─────────────────────────────────────────────
        // MOOD SECTION
        // ─────────────────────────────────────────────

        SectionCard(
            title = "MOOD"
        ) {

            ChipRow(
                chips = listOf(
                    "DARK",
                    "UPLIFTING",
                    "CINEMATIC",
                    "AMBIENT"
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            PlaceholderPanel(
                text = "Mood-driven harmonic generation"
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ─────────────────────────────────────────────
        // PROGRESSION SECTION
        // ─────────────────────────────────────────────

        SectionCard(
            title = "PROGRESSION"
        ) {

            ChipRow(
                chips = listOf(
                    "POP",
                    "JAZZ",
                    "FILM",
                    "MODAL"
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            PlaceholderPanel(
                text = "Progression browser / preset cards"
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ─────────────────────────────────────────────
        // GROOVE SECTION
        // ─────────────────────────────────────────────

        SectionCard(
            title = "GROOVE"
        ) {

            ChipRow(
                chips = listOf(
                    "STRAIGHT",
                    "SWING",
                    "BROKEN",
                    "PULSE"
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            PlaceholderPanel(
                text = "Rhythmic interpretation controls"
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ─────────────────────────────────────────────
        // RESULT SECTION
        // ─────────────────────────────────────────────

        SectionCard(
            title = "RESULT"
        ) {

            PlaceholderPanel(
                text = "Generated chord bars appear here"
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PANEL)
            .padding(14.dp)
    ) {

        Text(
            text = title,
            color = TEXT,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(12.dp))

        content()
    }
}

@Composable
fun WorkflowModeCard(
    label: String
) {

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(PANEL_ALT)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = label,
            color = TEXT,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun ChipRow(
    chips: List<String>
) {

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        chips.forEach { chip ->

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(PANEL_ALT)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {

                Text(
                    text = chip,
                    color = TEXT_DIM,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun PlaceholderPanel(
    text: String
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(BG)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = text,
            color = TEXT_DIM,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

