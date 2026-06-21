package com.reasontouch.feature.chords.workspaces

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.reasontouch.core.data.MidiTrack
import com.reasontouch.feature.chords.ChordViewModel
import kotlinx.coroutines.delay
import com.reasontouch.feature.chords.StepPattern
import com.reasontouch.feature.chords.StrumPatterns
import com.reasontouch.feature.chords.components.SendProgressionToPianoRollDialog
import com.reasontouch.feature.chords.components.StrumPatternTray

private val BG = Color(0xFF1A1A1E)
private val PANEL = Color(0xFF2A2A32)
private val BORDER = Color(0xFF3A3A45)
private val ACCENT = Color(0xFFE84040)
private val TEXT = Color(0xFFC8C8D4)
private val TEXT_DIM = Color(0xFF666675)
private val PURPLE = Color(0xFFA78BFA)
private val INFO_BG = Color(0xFF1A2A1A)
private val INFO_BORDER = Color(0xFF3DDC84)

data class Mood(
    val name: String,
    val description: String,
    val icon: String,
    val moodBias: Float
)

val AVAILABLE_MOODS = listOf(
    Mood("Dark", "Minor keys, melancholic", "🌙", -0.8f),
    Mood("Uplifting", "Major keys, hopeful", "☀️", 0.8f),
    Mood("Cinematic", "Epic, dramatic", "🎬", 0.3f),
    Mood("Ambient", "Atmospheric, sparse", "☁️", 0.0f),
    Mood("Energetic", "Fast, driving", "⚡", 0.6f),
    Mood("Melancholic", "Contemplative, sad", "💔", -0.6f)
)

@Composable
fun MoodWorkspace(
    viewModel: ChordViewModel,
    onMoodSelected: (Mood) -> Unit,
    onAudition: (String) -> Unit = {},
    onContinue: () -> Unit = {}
) {
    var selectedMood by remember { mutableStateOf<Mood?>(null) }
    var moodBiasFine by remember { mutableStateOf(0f) }
    var showMoodDescriptor by remember { mutableStateOf(false) }
    var showSendDialog by remember { mutableStateOf(false) }
    var selectedTrackIndex by remember { mutableStateOf(0) }
    var pendingProgressionChords by remember { mutableStateOf<List<String>>(emptyList()) }
    var pendingStrumPattern by remember { mutableStateOf<StepPattern?>(null) }
    val trackList by viewModel.tracks.collectAsState()

    LaunchedEffect(showSendDialog) {
        android.util.Log.d("DialogState", "showSendDialog changed to: $showSendDialog")
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BG)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                if (selectedMood == null) {
                    if (showMoodDescriptor) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(PANEL)
                                .border(1.dp, BORDER, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🎵 MOOD-DRIVEN COMPOSITION",
                                    color = ACCENT,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color(0xFF3A3A45))
                                        .clickable { showMoodDescriptor = false }
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
                                text = "Start with a feeling. The harmony engine will suggest compatible chords.",
                                color = TEXT_DIM,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 12.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "How it works:",
                                color = TEXT_DIM,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )

                            Text(
                                text = "1. Choose a mood\n2. Refine the exact harmonic character\n3. Build your progression with AI-suggested chords\n4. Switch modes anytime",
                                color = TEXT_DIM,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 13.sp
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(PANEL)
                                .border(1.dp, BORDER, RoundedCornerShape(4.dp))
                                .clickable { showMoodDescriptor = true }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "ℹ SHOW MOOD INFO",
                                color = TEXT_DIM,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                } else {
                    SuggestedProgressionDisplay(
                        mood = selectedMood!!,
                        harmonyBias = moodBiasFine,
                        viewModel = viewModel,
                        onPlayProgression = { chords, pattern ->
                            viewModel.playProgression(chords, pattern)
                        },
                        onProgressionReady = { chords, pattern ->
                            pendingProgressionChords = chords
                            pendingStrumPattern = pattern
                            showSendDialog = true
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Text(
                    text = "SELECT A MOOD",
                    color = TEXT_DIM,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(AVAILABLE_MOODS.chunked(3)) { moodRow ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    moodRow.forEach { mood ->
                        MoodCard(
                            mood = mood,
                            isSelected = selectedMood == mood,
                            onSelect = {
                                selectedMood = mood
                                moodBiasFine = mood.moodBias
                                onMoodSelected(mood)
                            },
                            onAudition = onAudition,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(3 - moodRow.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (selectedMood != null) {
                item {
                    MoodBiasTuner(
                        mood = selectedMood!!,
                        biasFine = moodBiasFine,
                        onBiasChange = { moodBiasFine = it }
                    )
                }

                item {
                    MoodNextSteps(
                        mood = selectedMood!!,
                        onContinue = onContinue
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
// Send to Piano Roll Dialog
    if (showSendDialog) {
        SendProgressionToPianoRollDialog(
        trackList = trackList,
        onDismiss = { showSendDialog = false },
        onConfirm = { trackIndex, appendMode ->

                viewModel.sendProgressionToPianoRoll(
                    chordNames = pendingProgressionChords,
                    strumPattern = pendingStrumPattern ?: StepPattern.EMPTY,
                    trackIndex = trackIndex,
                    appendMode = appendMode,
                    onComplete = {
                        showSendDialog = false
                        selectedMood = null
                    }
                )
            }
        )
    }
}


@Composable
private fun CollapsibleHeaderTray(
    isExpanded: Boolean,
    onToggle: () -> Unit,

) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PANEL)
            .border(1.dp, BORDER, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "🎵 MOOD-DRIVEN COMPOSITION",
                color = ACCENT,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = if (isExpanded) Icons.Default.Clear else Icons.Default.Add,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = ACCENT,
                modifier = Modifier.size(20.dp)
            )
        }

        if (isExpanded) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Start with a feeling. The harmony engine will suggest compatible chords.",
                color = TEXT_DIM,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 12.sp
            )
        }
    }
}



@Composable
private fun SuggestedProgressionDisplay(
    mood: Mood,
    harmonyBias: Float,
    viewModel: ChordViewModel,
    onPlayProgression: (List<String>, StepPattern?) -> Unit,
    onProgressionReady: (List<String>, StepPattern) -> Unit
) {
    val chords = viewModel.generateProgressionForMood(mood.name, harmonyBias)
    var isPlaying by remember { mutableStateOf(false) }
    var selectedStrumPattern by remember { mutableStateOf<StepPattern?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PANEL)
            .border(1.dp, BORDER, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "SUGGESTED PROGRESSION",
                color = TEXT_DIM,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isPlaying) ACCENT else ACCENT.copy(alpha = 0.6f))
                    .clickable(enabled = !isPlaying) {
                    isPlaying = true
                    onPlayProgression(chords, selectedStrumPattern)
                }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isPlaying) "♫ Playing..." else "► Play",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            chords.forEach { chord ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1A1A1E))
                        .border(1.dp, ACCENT.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chord,
                        color = ACCENT,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
        StrumPatternTray(
            selectedPattern = selectedStrumPattern,
            onPatternSelected = { selectedStrumPattern = it },
            patterns = StrumPatterns.groups,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(BORDER.copy(alpha = 0.5f))
                    .clickable(enabled = !isPlaying) {
                        isPlaying = true
                        onPlayProgression(chords, selectedStrumPattern)
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "REGENERATE",
                    color = TEXT_DIM,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (selectedStrumPattern != null) ACCENT else ACCENT.copy(alpha = 0.4f)
                    )
                    .clickable(enabled = selectedStrumPattern != null && !isPlaying) {
                        android.util.Log.d("AcceptButton", "ACCEPT clicked with pattern: ${selectedStrumPattern?.steps}")
                        onProgressionReady(chords, selectedStrumPattern!!)
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ACCEPT",
                    color = if (selectedStrumPattern != null) Color.White else TEXT_DIM,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            delay(chords.size * 1200L + 500)
            isPlaying = false
        }
    }
}

@Composable
fun MoodCard(
    mood: Mood,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onAudition: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) Color(0xFF2A1A2A) else PANEL
    val borderColor = if (isSelected) PURPLE else BORDER

    Box(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(2.dp, borderColor, RoundedCornerShape(6.dp))
            .clickable {
                onAudition(mood.name)
                onSelect()
            }
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = mood.name,
                color = if (isSelected) PURPLE else TEXT,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = mood.description,
                        color = TEXT_DIM,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 10.sp,
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) PURPLE.copy(alpha = 0.2f) else BORDER.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mood.icon,
                        fontSize = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MoodBiasTuner(
    mood: Mood,
    biasFine: Float,
    onBiasChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(PANEL)
            .border(1.dp, BORDER, RoundedCornerShape(6.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "HARMONIC CHARACTER",
            color = TEXT_DIM,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Dark", color = PURPLE, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Slider(
                value = biasFine,
                onValueChange = onBiasChange,
                valueRange = -1f..1f,
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp),
                colors = SliderDefaults.colors(
                    thumbColor = if (biasFine > 0) Color(0xFFF5C518) else PURPLE,
                    activeTrackColor = if (biasFine > 0) Color(0xFFF5C518).copy(alpha = 0.6f) else PURPLE.copy(alpha = 0.6f)
                )
            )
            Text("Bright", color = Color(0xFFF5C518), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = when {
                biasFine > 0.5f -> "Strongly major — very bright and hopeful"
                biasFine > 0.1f -> "Leaning major — predominantly bright"
                biasFine < -0.5f -> "Strongly minor — very dark and introspective"
                biasFine < -0.1f -> "Leaning minor — predominantly dark"
                else -> "Neutral — balanced major and minor possibilities"
            },
            color = TEXT_DIM,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun MoodNextSteps(
    mood: Mood,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(INFO_BG)
            .border(1.dp, INFO_BORDER.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "✓ Ready to compose",
            color = INFO_BORDER,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "Your progression will suggest compatible chords based on '${mood.name}' in Assisted mode. You can override suggestions at any time.",
            color = TEXT_DIM,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 12.sp
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(INFO_BORDER)
                .clickable(onClick = onContinue)
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "START COMPOSING",
                color = BG,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}