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
import com.reasontouch.core.ui.components.StrumPatternTray
import com.reasontouch.feature.chords.StepPattern
import com.reasontouch.feature.chords.StrumPatterns

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
    var headerExpanded by remember { mutableStateOf(true) }
    var howItWorksExpanded by remember { mutableStateOf(false) }
    var showSendDialog by remember { mutableStateOf(false) }
    var selectedTrackIndex by remember { mutableStateOf(0) }
    var useStrum by remember { mutableStateOf(false) }
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
            if (selectedMood == null) {
                item {
                    CollapsibleHeaderTray(
                        isExpanded = headerExpanded,
                        onToggle = { headerExpanded = !headerExpanded }
                    )
                }

                item {
                    CollapsibleHowItWorksTray(
                        isExpanded = howItWorksExpanded,
                        onToggle = { howItWorksExpanded = !howItWorksExpanded }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                item {
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

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
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
    }  // ← LazyColumn closing brace

// Send to Piano Roll Dialog
    if (showSendDialog) {
        SendProgressionToPianoRollDialog(
            progression = pendingProgressionChords,
            tracks = trackList,  // ← USE ACTUAL TRACKS
            onConfirm = { trackIndex, useStrum, appendMode ->

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
            },
            onDismiss = { showSendDialog = false }
        )
    }
}

@Composable
private fun SendProgressionToPianoRollDialog(
    progression: List<String>,
    tracks: List<Any>,
    onConfirm: (trackIndex: Int, useStrum: Boolean, appendMode: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTrackIndex by remember { mutableStateOf(0) }
    var useStrum by remember { mutableStateOf(false) }
    var appendMode by remember { mutableStateOf(false) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PANEL,
        titleContentColor = ACCENT,
        textContentColor = TEXT,
        title = {
            Text(
                "SEND TO PIANO ROLL",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontSize = 14.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // TRACK SELECTOR
                if (tracks.isNotEmpty()) {
                    Text(
                        "Track",
                        color = TEXT_DIM,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tracks.size) { index ->
                            val track = tracks[index] as? MidiTrack
                            val isSelected = selectedTrackIndex == index
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSelected) ACCENT.copy(alpha = 0.2f) else PANEL)
                                    .border(
                                        1.dp,
                                        if (isSelected) ACCENT else BORDER,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable { selectedTrackIndex = index }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    track?.name ?: "Track $index",
                                    color = if (isSelected) ACCENT else TEXT_DIM,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text(
                    "Mode",
                    color = TEXT_DIM,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(false to "Block", true to "Strum")
                        .forEach { (mode, label) ->
                            val active = useStrum == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (active) ACCENT.copy(alpha = 0.2f) else PANEL)
                                    .border(
                                        1.dp,
                                        if (active) ACCENT else BORDER,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable { useStrum = mode }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    color = if (active) ACCENT else TEXT_DIM,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (appendMode) BORDER.copy(alpha = 0.3f) else PANEL)
                        .border(1.dp, if (appendMode) ACCENT else BORDER, RoundedCornerShape(4.dp))
                        .clickable { appendMode = !appendMode }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Append to existing",
                        color = if (appendMode) ACCENT else TEXT_DIM,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    androidx.compose.material3.Switch(
                        checked = appendMode,
                        onCheckedChange = { appendMode = it },
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ACCENT
                        )
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = { onConfirm(selectedTrackIndex, useStrum, appendMode) },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = ACCENT
                )
            ) {
                Text(
                    "SEND",
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(
                    "CANCEL",
                    color = TEXT_DIM,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    )
}  // ← MoodWorkspace closing brace

@Composable
private fun CollapsibleHeaderTray(
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PANEL)
            .border(1.dp, BORDER, RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle)
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
private fun CollapsibleHowItWorksTray(
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(INFO_BG)
            .border(1.dp, INFO_BORDER.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle)
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
                text = "ℹ  How it works",
                color = TEXT_DIM,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.Clear else Icons.Default.Add,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = TEXT_DIM,
                modifier = Modifier.size(20.dp)
            )
        }

        if (isExpanded) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "1. Choose a mood\n" +
                        "2. Refine the exact harmonic character\n" +
                        "3. Build your progression with AI-suggested chords\n" +
                        "4. Switch modes anytime",
                color = TEXT_DIM,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 13.sp
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