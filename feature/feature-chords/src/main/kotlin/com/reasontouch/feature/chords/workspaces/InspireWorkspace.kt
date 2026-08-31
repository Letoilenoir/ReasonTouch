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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import com.reasontouch.feature.chords.components.StrumPatternTray
import com.reasontouch.feature.chords.ChordViewModel
import com.reasontouch.feature.chords.StrumPatterns
import com.reasontouch.feature.chords.components.SendProgressionToPianoRollDialog

private val BG = Color(0xFF1A1A1E)
private val PANEL = Color(0xFF2A2A32)
private val BORDER = Color(0xFF3A3A45)
private val ACCENT = Color(0xFFE84040)
private val TEXT = Color(0xFFC8C8D4)
private val TEXT_DIM = Color(0xFF666675)
private val BLUE = Color(0xFF38BDF8)
private val GREEN = Color(0xFF3DDC84)

@Composable
fun InspireWorkspace(
    viewModel: ChordViewModel,
    onGenerateProgression: () -> Unit,
    onContinue: () -> Unit = {}
) {
    val trackList by viewModel.tracks.collectAsState()

    var isGenerating by remember { mutableStateOf(false) }
    var suggestedProgression by remember { mutableStateOf<List<String>?>(null) }
    var selectedStrumPattern by remember { mutableStateOf(StrumPatterns.groups["Core"]?.get("Clear")) }
    var selectedStrumSpeed by remember { mutableStateOf(0.02) }
    var showSendDialog by remember { mutableStateOf(false) }
    var showInspireDescriptor by remember { mutableStateOf(false) }


    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(BG)
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            if (showInspireDescriptor) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(PANEL)
                        .border(1.dp, BORDER, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✨  INSPIRATION MODE",
                            color = BLUE,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF3A3A45))
                                .clickable { showInspireDescriptor = false }
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "✕",
                                color = TEXT_DIM,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "Let the system suggest a complete progression. Perfect for creative block.",
                        color = TEXT_DIM,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "How it works:\n1. Generate a random progression\n2. Preview the chords\n3. Select a strum pattern\n4. Send to piano roll",
                        color = TEXT_DIM,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 14.sp
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(PANEL)
                        .border(1.dp, BORDER, RoundedCornerShape(4.dp))
                        .clickable { showInspireDescriptor = true }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "ℹ SHOW INSPIRE INFO",
                        color = TEXT_DIM,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        item {
            GenerateButton(
                isGenerating = isGenerating,
                onClick = {
                    isGenerating = true
                    onGenerateProgression()  // ← Slow async call
                    suggestedProgression = listOf("C", "Am", "F", "G")
                    isGenerating = false
                }
            )
        }

        if (isGenerating) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = BLUE,
                            strokeWidth = 2.dp
                        )
                        Text(
                            "Generating inspiration...",
                            color = TEXT_DIM,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        if (suggestedProgression != null && !isGenerating) {
            item {
                ProgressionPreviewWithPlay(
                    progression = suggestedProgression!!,
                    viewModel = viewModel,
                    onRegenerate = {
                        // Instant random selection, no async
                        val templates = listOf(
                            listOf("C", "Am", "F", "G"),           // Pop classic
                            listOf("Am", "F", "C", "G"),           // Minor start
                            listOf("C7", "F7", "C7", "G7"),        // Blues
                            listOf("Dm", "G", "Dm", "G"),          // Modal vamp
                            listOf("C", "Bdim", "C", "Bdim"),      // Diminished
                            listOf("Cmaj7", "Bm7", "Em7", "Am7"),  // Jazz
                            listOf("C", "F", "G"),                 // I-IV-V
                            listOf("C", "G", "Am", "F"),           // I-V-vi-IV
                            listOf("C", "Am", "Dm", "G"),          // I-vi-ii-V
                            listOf("Am", "Dm", "G", "C"),          // vi-ii-V-I
                            listOf("C", "Em", "Am", "F"),          // I-iii-vi-IV
                            listOf("Dm", "G", "C", "F"),           // ii-V-I-IV
                            listOf("Am", "F", "G"),                // vi-IV-V
                            listOf("C", "F", "C", "G"),            // I-IV-I-V
                            listOf("Em", "Am", "Dm", "G"),         // iii-vi-ii-V
                            listOf("G", "F", "C"),                 // V-IV-I
                        )
                        suggestedProgression = templates.random()
                    },
                    onAccept = { showSendDialog = true }
                )
            }

            item {
                StrumPatternTray(
                    selectedPattern = selectedStrumPattern,
                    onPatternSelected = { selectedStrumPattern = it },
                    patterns = StrumPatterns.groups,
                    selectedSpeed = selectedStrumSpeed,
                    onSpeedSelected = { speed ->
                        selectedStrumSpeed = speed
                        viewModel.setStrumSpeed(speed)  // keeps ui.value.strumSpeed in sync,
                        // since sendProgressionToPianoRoll()
                        // reads it internally, not as a param
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // SEND DIALOG
    if (showSendDialog && suggestedProgression != null && selectedStrumPattern != null) {
        SendProgressionToPianoRollDialog(
            trackList = trackList,
            onDismiss = { showSendDialog = false },
            onConfirm = { trackIndex, appendMode ->
                viewModel.sendProgressionToPianoRoll(
                    chordNames = suggestedProgression!!,
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
fun GenerateButton(
    isGenerating: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isGenerating) BLUE.copy(alpha = 0.5f) else BLUE)
            .clickable(enabled = !isGenerating, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isGenerating) "GENERATING..." else "GENERATE PROGRESSION",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun ProgressionPreviewWithPlay(
    progression: List<String>,
    viewModel: ChordViewModel,
    onRegenerate: () -> Unit,
    onAccept: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(PANEL)
            .border(1.dp, BORDER, RoundedCornerShape(6.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "SUGGESTED PROGRESSION",
            color = TEXT_DIM,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF1A1A1E))
                .border(1.dp, BORDER, RoundedCornerShape(4.dp))
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            progression.forEachIndexed { index, chord ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF2A2A32))
                        .border(1.dp, BLUE.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${index + 1}",
                            color = TEXT_DIM,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = chord,
                            color = BLUE,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1A2A3A))
                    .border(1.dp, BLUE, RoundedCornerShape(4.dp))
                    .clickable(onClick = onRegenerate)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "REGENERATE",
                    color = BLUE,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(GREEN)
                    .clickable(onClick = {
                        viewModel.playProgression(progression, null)
                    })
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "▶ PLAY",
                    color = Color.Black,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(ACCENT)
                    .clickable(onClick = onAccept)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "ACCEPT",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}