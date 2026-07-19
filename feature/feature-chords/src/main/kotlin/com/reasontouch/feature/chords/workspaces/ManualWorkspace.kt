package com.reasontouch.feature.chords.workspaces

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reasontouch.feature.chords.ChordViewModel
import com.reasontouch.feature.chords.GeneratedProgression
import com.reasontouch.feature.chords.components.StrumPatternTray
import com.reasontouch.feature.chords.components.SendProgressionToPianoRollDialog
import com.reasontouch.feature.chords.StrumPatterns
import com.reasontouch.core.midi.StepState
import com.reasontouch.feature.chords.PairingDecision
import com.reasontouch.feature.chords.components.SuggestNextDialog


// Theme colors
private val PANEL = Color(0xFF2A2A32)
private val BORDER = Color(0xFF3A3A45)
private val ACCENT = Color(0xFFE84040)
private val TEXT_DIM = Color(0xFF666675)
private val TEXT = Color(0xFFC8C8D4)

@Composable
fun ManualWorkspace(
    viewModel: ChordViewModel,
    onMoodSelected: (String) -> Unit = {},
    onAudition: (String) -> Unit = {},
    onContinue: () -> Unit = {}
) {
    val ui by viewModel.ui.collectAsState()
    val selectedCategory = ui.selectedCategory
    val selectedChord = ui.selectedChord
    val selectedPosition = ui.selectedPosition
    val filteredChords by viewModel.filteredChords.collectAsState()
    val availablePositions by viewModel.availablePositions.collectAsState()
    val stepStates by viewModel.stepStates.collectAsState()
    val progression by viewModel.progression.collectAsState()
    val harmonyState = ui.harmony
    val tracks by viewModel.tracks.collectAsState()

    var showDescriptor by remember { mutableStateOf(false) }
    var showHarmonyPanel by remember { mutableStateOf(false) }
    var showSendDialog by remember { mutableStateOf(false) }
    var selectedPattern by remember { mutableStateOf<com.reasontouch.feature.chords.StepPattern?>(null) }
    var showSuggestDialog by remember { mutableStateOf(false) }
    var currentSuggestion by remember { mutableStateOf<PairingDecision?>(null) }
    var currentGeneratedPhrases by remember { mutableStateOf<List<GeneratedProgression>>(emptyList()) }
    var currentOptions by remember {
        mutableStateOf<List<String>>(emptyList())
    }

    // Auto-apply preset on first non-null pattern selection (handled in StrumPatternTray callback)

    Column(modifier = Modifier.fillMaxWidth()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // DESCRIPTOR HEADER (collapsible)
            item {
                if (showDescriptor) {
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
                                "MANUAL COMPOSITION",
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
                                    .clickable { showDescriptor = false }
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("HIDE", color = TEXT_DIM, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            "Build your progression from scratch. Full creative control.",
                            color = TEXT_DIM,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 12.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "How it works:",
                            color = TEXT,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "1. Select a chord and voicing\n2. Choose a strum pattern\n3. Add bars to build your progression\n4. Send to Piano Roll for arrangement",
                            color = TEXT_DIM,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF2A2A32))
                            .border(1.dp, BORDER, RoundedCornerShape(4.dp))
                            .clickable { showDescriptor = true }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "SHOW MANUAL INFO",
                            color = TEXT_DIM,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            // STRUM PATTERN (using shared StrumPatternTray)
            item {
                SectionLabel("STRUM PATTERN")
            }
            item {
                StrumPatternTray(
                    selectedPattern = selectedPattern,
                    onPatternSelected = { pattern ->
                        selectedPattern = pattern
                        viewModel.applyPreset(pattern)  // Update viewModel stepStates
                    },
                    patterns = StrumPatterns.groups,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            // CATEGORY FILTER
            item {
                SectionLabel("CATEGORY")
            }
            item {
                CategoryFilter(
                    categories = listOf("All", "Major", "Minor", "7th", "Sus", "Power", "Dim", "Aug"),
                    selected = selectedCategory,
                    onSelect = viewModel::selectCategory
                )
            }

            // CHORD GRID
            item {
                SectionLabel("CHORD")
            }
            item {
                ChordGrid(
                    chords = filteredChords,
                    selected = selectedChord,
                    onSelect = viewModel::selectChord
                )
            }

            // POSITION SELECTOR
            if (availablePositions.size > 1) {
                item {
                    SectionLabel("VOICING / POSITION")
                }
                item {
                    PositionSelector(
                        positions = availablePositions,
                        selected = selectedPosition,
                        onSelect = viewModel::selectPosition
                    )
                }
            }
            // HARMONY PANEL (collapsible) - will be added once extracted to shared components
            // For now, harmony suggestions are disabled

            // ACTION BUTTONS (Send to Roll only)
            // Note: Harmony suggest and Bass buttons will be added once those components are extracted to shared
            item {
                if (progression.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Send to Piano Roll button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF1A2A1A))
                                .border(1.dp, Color(0xFF3DDC84), RoundedCornerShape(4.dp))
                                .clickable { showSendDialog = true }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "-> ROLL",
                                color = Color(0xFF3DDC84),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // PROGRESSION DISPLAY (fixed bottom bar)
        FixedBottomBar(
            progression = progression,
            chordName = "$selectedChord $selectedPosition",
            statusMessage = ui.statusMessage,
            onAddBar = viewModel::addBar,
            onRemoveBar = viewModel::removeBar,
            onClear = viewModel::clearProgression,
            onSuggestNext = {
                val suggestion = viewModel.suggestNextSection()
                currentSuggestion = suggestion

                currentGeneratedPhrases = viewModel.suggestNextPhrases()

                currentOptions = if (currentGeneratedPhrases.isNotEmpty()) {
                    currentGeneratedPhrases.map { it.explanation }
                } else {
                    listOf(
                        "${suggestion.type.name} pathway not yet implemented — " +
                                "this suggestion would use ${suggestion.type.name} " +
                                "generation once available."
                    )
                }

                showSuggestDialog = true
            }
        )

    }

    // MODALS
    // BassStyleSheet modal will be added once extracted to shared components
    // For now, bass generation is disabled

    if (showSendDialog) {
        SendProgressionToPianoRollDialog(
            trackList = tracks,
            onDismiss = { showSendDialog = false },
            onConfirm = { trackIndex, appendMode ->
                // Send with the selected pattern
                val chordNames = progression.map {
                    it.chordName.substringBefore(" ")
                }

                if (selectedPattern != null) {
                    viewModel.sendProgressionToPianoRoll(
                        chordNames = chordNames,
                        strumPattern = selectedPattern!!,
                        trackIndex = trackIndex,
                        appendMode = appendMode,
                        onComplete = {}
                    )
                } else {
                    viewModel.sendToPianoRoll(
                        trackIndex,
                        false,
                        null,
                        appendMode
                    ) {}
                }
                showSendDialog = false
            }
        )
        if (showSuggestDialog && currentSuggestion != null) {
            SuggestNextDialog(
                decision = currentSuggestion!!,
                options = currentOptions,
                onOptionSelected = { selected ->

                    currentGeneratedPhrases.getOrNull(selected)?.let { phrase ->
                        viewModel.addPhrase(phrase)
                    }

                    showSuggestDialog = false
                    currentSuggestion = null
                    currentGeneratedPhrases = emptyList()
                },
                onDismiss = {
                    showSuggestDialog = false
                    currentSuggestion = null
                }
            )
        }
    }
}

// SHARED COMPONENTS FROM ChordScreen.kt

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        color = TEXT_DIM,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 4.dp)
    )
}

@Composable
fun CategoryFilter(categories: List<String>, selected: String, onSelect: (String) -> Unit) {
    androidx.compose.foundation.lazy.LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(categories) { cat ->
            val isSelected = cat == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) ACCENT else PANEL)
                    .border(1.dp, if (isSelected) ACCENT else BORDER, RoundedCornerShape(20.dp))
                    .clickable { onSelect(cat) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    cat,
                    color = if (isSelected) Color.White else TEXT_DIM,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun ChordGrid(chords: List<String>, selected: String, onSelect: (String) -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        chords.forEach { chord ->
            val isSelected = chord == selected
            val bgColor = when {
                isSelected -> ACCENT
                chord.contains("m") && !chord.contains("maj") &&
                        !chord.contains("dim") -> Color(0xFF1A2A4A)
                chord.contains("7") -> Color(0xFF2A1A2A)
                chord.contains("sus") -> Color(0xFF1A2A1A)
                chord.contains("dim") -> Color(0xFF2A1A1A)
                chord.contains("aug") -> Color(0xFF2A2A1A)
                chord.endsWith("5") -> Color(0xFF1A1A2A)
                else -> PANEL
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(bgColor)
                    .border(1.dp, if (isSelected) ACCENT else BORDER, RoundedCornerShape(4.dp))
                    .clickable { onSelect(chord) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    chord,
                    color = if (isSelected) Color.White else TEXT,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun PositionSelector(positions: List<String>, selected: String, onSelect: (String) -> Unit) {
    androidx.compose.foundation.lazy.LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(positions) { pos ->
            val isSelected = pos == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isSelected) Color(0xFF1A3A5A) else PANEL)
                    .border(1.dp, if (isSelected) Color(0xFF38BDF8) else BORDER, RoundedCornerShape(4.dp))
                    .clickable { onSelect(pos) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    pos,
                    color = if (isSelected) Color(0xFF38BDF8) else TEXT_DIM,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// FIXED BOTTOM BAR - unique to Manual progression management

@Composable
fun FixedBottomBar(
    progression: List<com.reasontouch.core.data.ChordEvent>,
    chordName: String,
    statusMessage: String?,
    onAddBar: () -> Unit,
    onRemoveBar: (com.reasontouch.core.data.ChordEvent) -> Unit,
    onClear: () -> Unit,
    onSuggestNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF16161A))
            .border(1.dp, BORDER,
                RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
    ) {
        if (progression.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF222228))
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(progression) { bar ->
                        ProgressionChip(bar = bar, onRemove = { onRemoveBar(bar) })
                    }
                }
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF2A1A1A))
                        .border(1.dp, ACCENT, RoundedCornerShape(4.dp))
                        .clickable(onClick = onClear)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("CLR", color = ACCENT, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace)
                }
            }
        }
        statusMessage?.let { msg ->
            Text(text = msg, color = Color(0xFFFF6B35), fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF1A3A5A))
                .clickable(onClick = onSuggestNext)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "SUGGEST NEXT",
                color = Color(0xFF38BDF8), fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace, letterSpacing = 0.5.sp
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(ACCENT)
                .clickable(onClick = onAddBar)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+ ADD BAR  ${chordName.uppercase()}",
                color = Color.White, fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace, letterSpacing = 1.sp
            )
        }
    }}

@Composable
fun ProgressionChip(bar: com.reasontouch.core.data.ChordEvent, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(PANEL)
            .border(1.dp, BORDER, RoundedCornerShape(4.dp))
            .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("${bar.barIndex + 1}", color = Color(0xFFFF6B35), fontSize = 10.sp,
            fontFamily = FontFamily.Monospace)
        Text(bar.chordName.uppercase(), color = TEXT, fontSize = 11.sp,
            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
            maxLines = 1)
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF3A1A1A))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Text("x", color = ACCENT, fontSize = 10.sp,
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
    }
}