package com.reasontouch.feature.chords

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.reasontouch.core.data.ChordEvent
import com.reasontouch.core.midi.StepState

private val BG       = Color(0xFF1A1A1E)
private val RACK     = Color(0xFF222228)
private val PANEL    = Color(0xFF2A2A32)
private val BORDER   = Color(0xFF3A3A45)
private val ACCENT   = Color(0xFFE84040)
private val ACCENT2  = Color(0xFFFF6B35)
private val GREEN    = Color(0xFF3DDC84)
private val TEXT     = Color(0xFFC8C8D4)
private val TEXT_DIM = Color(0xFF666675)
private val DOWN_COL = Color(0xFF1A3A5A)
private val UP_COL   = Color(0xFF2A1A3A)

@Composable
fun ChordScreen(sessionId: String, viewModel: ChordViewModel = hiltViewModel()) {
    var showSettings   by remember { mutableStateOf(false) }
    var showSendDialog by remember { mutableStateOf(false) }
    val progression    by viewModel.progression.collectAsState()
    val tracks         by viewModel.tracks.collectAsState()
    val selectedChord  by viewModel.selectedChord.collectAsState()
    val selectedPosition by viewModel.selectedPosition.collectAsState()
    val statusMessage  by viewModel.statusMessage.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(BG)) {

        // â”€â”€ Fixed toolbar â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        ChordToolbar(
            showSettings     = showSettings,
            onToggleSettings = { showSettings = !showSettings },
            onSendToRoll     = { showSendDialog = true },
            hasBars          = progression.isNotEmpty()
        )

        // â”€â”€ Scrollable content area â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        AnimatedContent(
            targetState = showSettings,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.weight(1f)
        ) { isSettings ->
            if (isSettings) SettingsPanel(viewModel = viewModel)
            else ChordPanel(viewModel = viewModel)
        }

        // â”€â”€ Fixed bottom bar â€” always visible â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        FixedBottomBar(
            progression      = progression,
            chordName        = "$selectedChord $selectedPosition",
            statusMessage    = statusMessage,
            onAddBar         = viewModel::addBar,
            onRemoveBar      = viewModel::removeBar,
            onClear          = viewModel::clearProgression
        )
    }

    if (showSendDialog) {
        SendToPianoRollDialog(
            tracks    = tracks,
            onConfirm = { trackIndex, useStrum, appendMode ->
                viewModel.sendToPianoRoll(trackIndex, useStrum, appendMode) {}
                showSendDialog = false
            },
            onDismiss = { showSendDialog = false }
        )
    }
}

// â”€â”€ Fixed bottom bar â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
fun FixedBottomBar(
    progression:   List<ChordEvent>,
    chordName:     String,
    statusMessage: String?,
    onAddBar:      () -> Unit,
    onRemoveBar:   (ChordEvent) -> Unit,
    onClear:       () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF16161A))
            .border(
                width = 1.dp,
                color = BORDER,
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
            )
    ) {
        // Progression strip â€” horizontal scroll
        if (progression.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RACK)
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(progression) { bar ->
                        ProgressionChip(
                            bar      = bar,
                            onRemove = { onRemoveBar(bar) }
                        )
                    }
                }
                // Clear all button
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF2A1A1A))
                        .border(1.dp, ACCENT, RoundedCornerShape(4.dp))
                        .clickable(onClick = onClear)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "CLR",
                        color = ACCENT,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Status message
        statusMessage?.let { msg ->
            Text(
                text = msg,
                color = ACCENT2,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
            )
        }

        // Add Bar button â€” always visible
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
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun ProgressionChip(bar: ChordEvent, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(PANEL)
            .border(1.dp, BORDER, RoundedCornerShape(4.dp))
            .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "${bar.barIndex + 1}",
            color = ACCENT2,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = bar.chordName.uppercase(),
            color = TEXT,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF3A1A1A))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "x",
                color = ACCENT,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// â”€â”€ Toolbar â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
fun ChordToolbar(
    showSettings:    Boolean,
    onToggleSettings: () -> Unit,
    onSendToRoll:    () -> Unit,
    hasBars:         Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(RACK)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = if (showSettings) "SETTINGS" else "CHORD GENERATOR",
            color = ACCENT,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (hasBars && !showSettings) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF1A3A2A))
                        .border(1.dp, GREEN, RoundedCornerShape(4.dp))
                        .clickable(onClick = onSendToRoll)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "\u2192 ROLL",
                        color = GREEN,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (showSettings) ACCENT else PANEL)
                    .border(1.dp, if (showSettings) ACCENT else BORDER, RoundedCornerShape(4.dp))
                    .clickable(onClick = onToggleSettings)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (showSettings) "X CLOSE" else "SETTINGS",
                    color = if (showSettings) Color.White else TEXT_DIM,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }
    }
    Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(ACCENT))
}

// â”€â”€ Chord panel â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
fun ChordPanel(viewModel: ChordViewModel) {
    val selectedCategory   by viewModel.selectedCategory.collectAsState()
    val selectedChord      by viewModel.selectedChord.collectAsState()
    val selectedPosition   by viewModel.selectedPosition.collectAsState()
    val filteredChords     by viewModel.filteredChords.collectAsState()
    val availablePositions by viewModel.availablePositions.collectAsState()
    val stepStates         by viewModel.stepStates.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
    ) {
        item { SectionLabel("CATEGORY") }
        item {
            CategoryFilter(
                categories = GuitarVoicings.categories.keys.toList(),
                selected   = selectedCategory,
                onSelect   = viewModel::selectCategory
            )
        }

        item { SectionLabel("CHORD") }
        item {
            ChordGrid(
                chords   = filteredChords,
                selected = selectedChord,
                onSelect = viewModel::selectChord
            )
        }

        if (availablePositions.size > 1) {
            item { SectionLabel("VOICING / POSITION") }
            item {
                PositionSelector(
                    positions = availablePositions,
                    selected  = selectedPosition,
                    onSelect  = viewModel::selectPosition
                )
            }
        }

        item { SectionLabel("PATTERN") }
        item { PatternGrid(steps = stepStates, onCycleStep = viewModel::cycleStep) }

        item { SectionLabel("PRESETS") }
        item { PresetPatternsDropdown(onApply = viewModel::applyPreset) }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

// â”€â”€ Settings panel â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPanel(viewModel: ChordViewModel) {
    val strumEnabled by viewModel.strumEnabled.collectAsState()
    val strumSpeed   by viewModel.strumSpeed.collectAsState()
    val instrument   by viewModel.instrument.collectAsState()
    val barDuration  by viewModel.barDuration.collectAsState()
    val tempo        by viewModel.tempo.collectAsState()

    var strumDropdownExpanded by remember { mutableStateOf(false) }
    var gmDropdownExpanded    by remember { mutableStateOf(false) }

    val currentPreset = STRUM_SPEED_PRESETS.firstOrNull {
        it.beatsPerString == strumSpeed
    } ?: STRUM_SPEED_PRESETS.first()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
    ) {
        item {
            // â”€â”€ Tempo â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            SectionLabel("TEMPO & BAR DURATION")
            SettingsRow(label = "Tempo") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StepperButton("<") { viewModel.setTempo(tempo - 1) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$tempo BPM",
                        color = ACCENT2, fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StepperButton(">") { viewModel.setTempo(tempo + 1) }
                }
            }
            SettingsRow(label = "Bar Duration") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StepperButton("<") { viewModel.setBarDuration(barDuration - 0.5) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$barDuration beats",
                        color = ACCENT2, fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StepperButton(">") { viewModel.setBarDuration(barDuration + 0.5) }
                }
            }

            // â”€â”€ Strum â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            SectionLabel("STRUM")
            SettingsRow(label = "Strum Simulation") {
                Switch(
                    checked = strumEnabled,
                    onCheckedChange = viewModel::setStrumEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ACCENT
                    )
                )
            }

            if (strumEnabled) {
                // Strum speed dropdown
                SettingsRow(label = "Strum Speed") {
                    ExposedDropdownMenuBox(
                        expanded = strumDropdownExpanded,
                        onExpandedChange = { strumDropdownExpanded = it }
                    ) {
                        TextField(
                            value = currentPreset.label,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = strumDropdownExpanded)
                            },
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = PANEL,
                                focusedContainerColor   = PANEL,
                                unfocusedTextColor      = TEXT,
                                focusedTextColor        = TEXT,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedIndicatorColor   = Color.Transparent
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize   = 12.sp
                            ),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .widthIn(min = 140.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = strumDropdownExpanded,
                            onDismissRequest = { strumDropdownExpanded = false },
                            modifier = Modifier.background(PANEL)
                        ) {
                            STRUM_SPEED_PRESETS.forEach { preset ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = preset.label,
                                                color = if (preset.beatsPerString == strumSpeed) GREEN else TEXT,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = preset.description,
                                                color = TEXT_DIM,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.setStrumSpeed(preset.beatsPerString)
                                        strumDropdownExpanded = false
                                    },
                                    modifier = Modifier.background(
                                        if (preset.beatsPerString == strumSpeed)
                                            Color(0xFF1A2A1A) else Color.Transparent
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // â”€â”€ GM Guitar sound dropdown â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            SectionLabel("GUITAR SOUND (GM PROGRAM)")
            SettingsRow(label = "Instrument") {
                ExposedDropdownMenuBox(
                    expanded = gmDropdownExpanded,
                    onExpandedChange = { gmDropdownExpanded = it }
                ) {
                    TextField(
                        value = instrument.label,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = gmDropdownExpanded)
                        },
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = PANEL,
                            focusedContainerColor   = PANEL,
                            unfocusedTextColor      = TEXT,
                            focusedTextColor        = TEXT,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor   = Color.Transparent
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize   = 12.sp
                        ),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .widthIn(min = 160.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = gmDropdownExpanded,
                        onDismissRequest = { gmDropdownExpanded = false },
                        modifier = Modifier.background(PANEL)
                    ) {
                        GM_GUITARS.forEach { gm ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = gm.label,
                                            color = if (gm.program == instrument.program)
                                                Color(0xFF38BDF8) else TEXT,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "GM ${gm.program + 1}",
                                            color = TEXT_DIM,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.setInstrument(gm)
                                    gmDropdownExpanded = false
                                },
                                modifier = Modifier.background(
                                    if (gm.program == instrument.program)
                                        Color(0xFF1A2A3A) else Color.Transparent
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

// â”€â”€ Shared components â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text, color = TEXT_DIM, fontSize = 10.sp,
        fontFamily = FontFamily.Monospace, letterSpacing = 2.sp,
        modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 4.dp)
    )
}

@Composable
fun CategoryFilter(categories: List<String>, selected: String, onSelect: (String) -> Unit) {
    LazyRow(
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
                    text = cat,
                    color = if (isSelected) Color.White else TEXT_DIM,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChordGrid(chords: List<String>, selected: String, onSelect: (String) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        chords.forEach { chord ->
            val isSelected = chord == selected
            val bgColor = when {
                isSelected -> ACCENT
                chord.contains("m") && !chord.contains("maj") &&
                    !chord.contains("dim") -> Color(0xFF1A2A4A)
                chord.contains("7")   -> Color(0xFF2A1A2A)
                chord.contains("sus") -> Color(0xFF1A2A1A)
                chord.contains("dim") -> Color(0xFF2A1A1A)
                chord.contains("aug") -> Color(0xFF2A2A1A)
                chord.endsWith("5")   -> Color(0xFF1A1A2A)
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
                    text = chord,
                    color = if (isSelected) Color.White else TEXT,
                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun PositionSelector(positions: List<String>, selected: String, onSelect: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(positions) { pos ->
            val isSelected = pos == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isSelected) Color(0xFF1A3A5A) else PANEL)
                    .border(
                        1.dp,
                        if (isSelected) Color(0xFF38BDF8) else BORDER,
                        RoundedCornerShape(4.dp)
                    )
                    .clickable { onSelect(pos) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = pos,
                    color = if (isSelected) Color(0xFF38BDF8) else TEXT_DIM,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun PatternGrid(steps: List<StepState>, onCycleStep: (Int) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("B1","B2","B3","B4").forEach { label ->
                Text(
                    text = label, color = TEXT_DIM, fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Start
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            (0 until 4).forEach { beat ->
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    (0 until 4).forEach { sub ->
                        StepButton(
                            state   = steps[beat * 4 + sub],
                            onClick = { onCycleStep(beat * 4 + sub) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StepLegendItem(color = Color(0xFF38BDF8), label = "D = Down")
            StepLegendItem(color = Color(0xFFA78BFA), label = "U = Up")
            StepLegendItem(color = BORDER,            label = ". = Off")
        }
    }
}

@Composable
fun StepButton(state: StepState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val (bg, fg, label) = when (state) {
        StepState.DOWN -> Triple(DOWN_COL, Color(0xFF38BDF8), "D")
        StepState.UP   -> Triple(UP_COL,   Color(0xFFA78BFA), "U")
        StepState.OFF  -> Triple(RACK,     TEXT_DIM,          ".")
    }
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(bg)
            .border(1.dp, if (state == StepState.OFF) BORDER else fg, RoundedCornerShape(3.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label, color = fg, fontSize = 12.sp,
            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun StepLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, color = TEXT_DIM, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun PresetPatterns(onApply: (StepPattern) -> Unit) {
    StrumPatterns.groups.forEach { (groupName, patterns) ->
        Text(
            text = groupName.uppercase(), color = TEXT_DIM, fontSize = 9.sp,
            fontFamily = FontFamily.Monospace, letterSpacing = 2.sp,
            modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 3.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(patterns.entries.toList()) { (name, pattern) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(PANEL)
                        .border(1.dp, BORDER, RoundedCornerShape(4.dp))
                        .clickable { onApply(pattern) }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = name, color = TEXT, fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsRow(label: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(RACK)
            .border(1.dp, BORDER, RoundedCornerShape(4.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TEXT, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
        content()
    }
}

@Composable
fun StepperButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(PANEL)
            .border(1.dp, BORDER, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label, color = TEXT, fontSize = 14.sp,
            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SendToPianoRollDialog(
    tracks:    List<com.reasontouch.core.data.MidiTrack>,
    onConfirm: (trackIndex: Int, useStrum: Boolean, appendMode: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTrack by remember { mutableStateOf(0) }
    var useStrum      by remember { mutableStateOf(false) }
    var appendMode    by remember { mutableStateOf(false) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest  = onDismiss,
        containerColor    = Color(0xFF222228),
        titleContentColor = GREEN,
        textContentColor  = TEXT,
        title = {
            Text(
                text = "SEND TO PIANO ROLL",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp, fontSize = 14.sp
            )
        },
        text = {
            Column {
                Text(
                    text = "MODE", color = TEXT_DIM, fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace, letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(false to "BLOCK" to "All notes together",
                           true  to "STRUM" to "Apply pattern timing").forEach { (pair, desc) ->
                        val (mode, modeLabel) = pair
                        val active  = useStrum == mode
                        val col     = if (mode) GREEN else Color(0xFF38BDF8)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (active) Color(0xFF1A2A1A) else Color(0xFF222228))
                                .border(1.dp, if (active) col else BORDER, RoundedCornerShape(4.dp))
                                .clickable { useStrum = mode }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = modeLabel,
                                    color = if (active) col else TEXT_DIM,
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = desc, color = TEXT_DIM, fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "TARGET TRACK", color = TEXT_DIM, fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace, letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                tracks.forEachIndexed { i, track ->
                    val trackColor = Color(when (i % 8) {
                        0 -> 0xFFE84040L; 1 -> 0xFF3DDC84L; 2 -> 0xFF38BDF8L
                        3 -> 0xFFA78BFAL; 4 -> 0xFFFF6B35L; 5 -> 0xFFF5C518L
                        6 -> 0xFFF472B6L; else -> 0xFF94A3B8L
                    })
                    val isSelected = i == selectedTrack
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) Color(0xFF28283A) else BG)
                            .border(1.dp, if (isSelected) trackColor else BORDER, RoundedCornerShape(4.dp))
                            .clickable { selectedTrack = i }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp).height(20.dp)
                                .background(trackColor, RoundedCornerShape(2.dp))
                        )
                        Text(
                            text = track.name,
                            color = if (isSelected) trackColor else TEXT_DIM,
                            fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = { onConfirm(selectedTrack, useStrum, appendMode) },
                colors  = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = GREEN
                )
            ) {
                Text(
                    text = "SEND", color = BG,
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(text = "CANCEL", color = TEXT_DIM, fontFamily = FontFamily.Monospace)
            }
        }
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetPatternsDropdown(onApply: (StepPattern) -> Unit) {
    var expanded     by remember { mutableStateOf(false) }
    var selectedName by remember { mutableStateOf("Select preset...") }

    // Flatten groups into a list of (groupName, patternName, pattern)
    // groupName == "" signals a header row
    data class PresetItem(val group: String, val name: String, val pattern: StepPattern?)

    val items = StrumPatterns.groups.flatMap { (groupName, patterns) ->
        listOf(PresetItem(groupName, "", null)) +
        patterns.map { (name, pattern) -> PresetItem(groupName, name, pattern) }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.weight(1f)
        ) {
            TextField(
                value = selectedName,
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = PANEL,
                    focusedContainerColor   = PANEL,
                    unfocusedTextColor      = TEXT,
                    focusedTextColor        = TEXT,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor   = Color.Transparent
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize   = 12.sp
                ),
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(PANEL)
            ) {
                items.forEach { item ->
                    if (item.pattern == null) {
                        // Group header — not clickable
                        Text(
                            text = item.group.uppercase(),
                            color = ACCENT2,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp,
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    } else {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = item.name,
                                    color = if (item.name == selectedName) GREEN else TEXT,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            onClick = {
                                selectedName = item.name
                                onApply(item.pattern)
                                expanded = false
                            },
                            modifier = Modifier.background(
                                if (item.name == selectedName)
                                    Color(0xFF1A2A1A) else Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    }
}