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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reasontouch.core.ui.components.StrumPatternTray
import com.reasontouch.feature.chords.ChordViewModel
import com.reasontouch.feature.chords.StrumPatterns
import com.reasontouch.feature.chords.components.SendProgressionToPianoRollDialog

private val BG = Color(0xFF1A1A1E)
private val PANEL = Color(0xFF2A2A32)
private val BORDER = Color(0xFF3A3A45)
private val ACCENT = Color(0xFFE84040)
private val TEXT = Color(0xFFC8C8D4)
private val TEXT_DIM = Color(0xFF666675)
private val GOLD = Color(0xFFF5C518)
private val GREEN = Color(0xFF3DDC84)

data class ProgressionTemplate(
    val name: String,
    val chords: List<String>,
    val style: String,
    val mood: String,
    val difficulty: String
)

val PROGRESSION_TEMPLATES = listOf(
    ProgressionTemplate(
        "I-vi-IV-V",
        listOf("Cmaj", "Am", "F", "G"),
        "Pop",
        "Balanced",
        "Beginner"
    ),
    ProgressionTemplate(
        "Jazz Turnaround",
        listOf("Cmaj7", "Bm7", "E7", "Am7"),
        "Jazz",
        "Sophisticated",
        "Advanced"
    ),
    ProgressionTemplate(
        "Blues 12-Bar",
        listOf("C7", "F7", "C7", "G7"),
        "Blues",
        "Gritty",
        "Intermediate"
    ),
    ProgressionTemplate(
        "Minor Odyssey",
        listOf("Am", "F", "C", "G"),
        "Dark",
        "Melancholic",
        "Beginner"
    ),
    ProgressionTemplate(
        "Modal Vamp",
        listOf("Dm", "G", "Dm", "G"),
        "Contemporary",
        "Contemplative",
        "Intermediate"
    ),
    ProgressionTemplate(
        "Diminished Tension",
        listOf("Cmaj7", "Bdim", "Cmaj7", "Bdim"),
        "Modern",
        "Dramatic",
        "Advanced"
    )
)

@Composable
fun ProgressionWorkspace(
    viewModel: ChordViewModel,
    onProgressionSelected: (ProgressionTemplate) -> Unit,
    onContinue: () -> Unit = {}
) {
    val trackList by viewModel.tracks.collectAsState()

    var selectedProgression by remember { mutableStateOf<ProgressionTemplate?>(null) }
    var selectedStrumPattern by remember { mutableStateOf(StrumPatterns.groups["Core"]?.get("Clear")) }
    var showSendDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(BG)
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ProgressionHeader()
        }

        item {
            ProgressionDescription()
        }

        item {
            Text(
                text = "CHOOSE A TEMPLATE",
                color = TEXT_DIM,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        if (selectedProgression == null) {
            items(PROGRESSION_TEMPLATES) { template ->
                ProgressionTemplateCard(
                    template = template,
                    isSelected = selectedProgression == template,
                    onSelect = {
                        selectedProgression = template
                        onProgressionSelected(template)
                    },
                    onPlay = {
                        viewModel.playProgression(template.chords, null)
                    }
                )
            }
        } else {
            item {
                ProgressionTemplateCard(
                    template = selectedProgression!!,
                    isSelected = true,
                    onSelect = { },
                    onPlay = {
                        viewModel.playProgression(selectedProgression!!.chords, null)
                    }
                )
            }
        }

        if (selectedProgression != null) {
            item {
                StrumPatternTray(
                    selectedPattern = selectedStrumPattern,
                    onPatternSelected = { selectedStrumPattern = it },
                    patterns = StrumPatterns.groups,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                ProgressionAcceptBar(
                    template = selectedProgression!!,
                    onContinue = { showSendDialog = true }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // SEND DIALOG
    if (showSendDialog && selectedProgression != null && selectedStrumPattern != null) {
        SendProgressionToPianoRollDialog(
            trackList = trackList,
            onDismiss = { showSendDialog = false },
            onConfirm = { trackIndex, appendMode ->
                viewModel.sendProgressionToPianoRoll(
                    chordNames = selectedProgression!!.chords,
                    strumPattern = selectedStrumPattern!!,
                    trackIndex = trackIndex,
                    appendMode = appendMode,
                    onComplete = {
                        showSendDialog = false
                        onContinue()
                    }
                )
            }
        )
    }
}

@Composable
fun ProgressionHeader() {
    Column {
        Text(
            text = "🎵  PROGRESSION TEMPLATES",
            color = GOLD,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Start with a classic progression. Great for learning harmonic structure.",
            color = TEXT_DIM,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun ProgressionDescription() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1A1A28))
            .border(1.dp, GOLD.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "How it works:",
            color = GOLD,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "1. Select a progression template\n2. Audition with Play button\n3. Choose strum pattern\n4. Send to piano roll",
            color = TEXT_DIM,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 14.sp
        )
    }
}

@Composable
fun ProgressionTemplateCard(
    template: ProgressionTemplate,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onPlay: () -> Unit
) {
    val bgColor = if (isSelected) Color(0xFF2A2A1A) else PANEL
    val borderColor = if (isSelected) GOLD else BORDER

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .clickable(onClick = onSelect)
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.name,
                        color = if (isSelected) GOLD else TEXT,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = template.style,
                        color = TEXT_DIM,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF1A1A1E))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = template.difficulty,
                        color = if (template.difficulty == "Advanced") Color(0xFFE84040)
                        else if (template.difficulty == "Intermediate") GOLD
                        else Color(0xFF3DDC84),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                template.chords.forEach { chord ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF1A1A1E))
                            .border(1.dp, BORDER, RoundedCornerShape(3.dp))
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chord,
                            color = GOLD,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mood: ${template.mood}",
                    color = TEXT_DIM,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF1A2A2A))
                        .border(1.dp, GREEN.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
                        .clickable(onClick = onPlay)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "▶ PLAY",
                        color = GREEN,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun ProgressionAcceptBar(
    template: ProgressionTemplate,
    onContinue: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1A2A1A))
            .border(1.dp, GREEN.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .clickable(onClick = onContinue)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "✓ START WITH THIS PROGRESSION",
            color = GREEN,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}