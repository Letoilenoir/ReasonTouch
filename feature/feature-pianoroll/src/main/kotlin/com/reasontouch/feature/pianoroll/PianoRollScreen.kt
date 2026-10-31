package com.reasontouch.feature.pianoroll

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.reasontouch.feature.chords.ChordViewModel
import com.reasontouch.feature.chords.PairingType
import com.reasontouch.feature.chords.BassStyle
import com.reasontouch.feature.chords.RankedSuggestion
import com.reasontouch.feature.chords.CompositionMode
import com.reasontouch.feature.chords.toCompositionMode
import com.reasontouch.feature.chords.CompositionContextBuilder
import com.reasontouch.feature.drums.DrumViewModel
import com.reasontouch.feature.drums.DrumPresets
import com.reasontouch.feature.drums.generateDrumNotes
import com.reasontouch.feature.chords.BassGenerator
import com.reasontouch.feature.chords.KeyDetector
import com.reasontouch.feature.chords.toStepPattern
import androidx.compose.foundation.rememberScrollState
// GmInstrument defined locally to avoid cross-module dependency
data class TrackInstrument(val label: String, val program: Int)

private val BG       = Color(0xFF1A1A1E)
private val RACK     = Color(0xFF222228)
private val PANEL    = Color(0xFF2A2A32)
private val BORDER   = Color(0xFF3A3A45)
private val ACCENT   = Color(0xFFE84040)
private val ACCENT2  = Color(0xFFFF6B35)
private val GREEN    = Color(0xFF3DDC84)
private val TEXT     = Color(0xFFC8C8D4)
private val TEXT_DIM = Color(0xFF666675)
private val PURPLE   = Color(0xFFA78BFA)
private val BLUE     = Color(0xFF38BDF8)
private val GOLD     = Color(0xFFF5C518)

// -- Per-track instrument lists ------------------------------------------------

private val BASS_INSTRUMENTS = listOf(
    TrackInstrument("Acoustic Bass",    32),
    TrackInstrument("Finger Bass",      33),
    TrackInstrument("Synth Bass 1",     38),
    TrackInstrument("Synth Bass 2",     39)
)

private val LEAD_INSTRUMENTS = listOf(
    TrackInstrument("Grand Piano",      0),
    TrackInstrument("Electric Piano",   4),
    TrackInstrument("Overdriven",       29),
    TrackInstrument("Square Lead",      80),
    TrackInstrument("Saw Lead",         81)
)

private val CHORD_INSTRUMENTS = listOf(
    TrackInstrument("Nylon Guitar",     24),
    TrackInstrument("Steel Guitar",     25),
    TrackInstrument("Jazz Guitar",      26),
    TrackInstrument("Clean Guitar",     27),
    TrackInstrument("Overdriven",       29),
    TrackInstrument("Distortion",       30)
)

private val PAD_INSTRUMENTS = listOf(
    TrackInstrument("Electric Piano",   4),
    TrackInstrument("Strings",          48),
    TrackInstrument("Strings 2",        49),
    TrackInstrument("Synth Strings",    51),
    TrackInstrument("Fantasia",         88),
    TrackInstrument("Warm Pad",         89)
)

private fun instrumentsForTrack(trackName: String): List<TrackInstrument>? = when (trackName.uppercase()) {
    "BASS"  -> BASS_INSTRUMENTS
    "LEAD"  -> LEAD_INSTRUMENTS
    "CHORD" -> CHORD_INSTRUMENTS
    "PAD"   -> PAD_INSTRUMENTS
    else    -> null  // DRUMS � no picker
}

private fun emojiForTrack(trackName: String): String = when (trackName.uppercase()) {
    "BASS"  -> "\uD83C\uDFB8"
    "LEAD"  -> "\uD83C\uDFB9"
    "CHORD" -> "\uD83C\uDFB8"
    "PAD"   -> "\uD83C\uDFB9"
    "DRUMS" -> "\uD83E\uDD41"
    else    -> "\uD83C\uDFB5"
}

@Composable
fun PianoRollScreen(
    sessionId: String,
    viewModel: PianoRollViewModel = hiltViewModel(),
    onSetBackAction: (((() -> Unit)?) -> Unit)? = null,
    onNavigateToChords: () -> Unit = {}
) {
    val state = remember { PianoRollState() }

    // Register back action immediately
    LaunchedEffect(Unit) {
        onSetBackAction?.invoke {
            onNavigateToChords()
        }
    }

    val playheadBeat by viewModel.playheadBeat.collectAsState()
    val isPlaying    by viewModel.isPlaying.collectAsState()
    val hasClipboard by viewModel.hasClipboard.collectAsState()
    val drawDuration by viewModel.drawDuration.collectAsState()
    var showInstrumentPanel by remember { mutableStateOf(false) }
    var trayExpanded by remember { mutableStateOf(false) }
    var traySection by remember { mutableStateOf(TraySection.HOME) }
    var activeBarToInspect by remember { mutableStateOf<Int?>(null) }
    var currentSuggestions by remember {
        mutableStateOf<List<com.reasontouch.feature.chords.SuggestionWorkflow.SuggestionOption>>(emptyList())
    }

    val session      by viewModel.session.collectAsState()
    val compositionMode = session?.compositionMode.toCompositionMode()
    val tracks       by viewModel.tracks.collectAsState()
    val chords       by viewModel.chords.collectAsState()
    val activeIndex  by viewModel.activeTrackIndex.collectAsState()
    val chordViewModel: ChordViewModel = hiltViewModel()
    val chordUiState by chordViewModel.ui.collectAsState()
    val drumViewModel: DrumViewModel = hiltViewModel()
    var bassSuggestions by remember { mutableStateOf<List<BassStyle>>(emptyList()) }
    var drumSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var groovePreview by remember { mutableStateOf<GroovePreview?>(null) }
    var grooveBassSuggestions by remember { mutableStateOf<List<RankedSuggestion<BassStyle>>>(emptyList()) }
    var grooveDrumSuggestions by remember { mutableStateOf<List<RankedSuggestion<String>>>(emptyList()) }
    val activeNotes  by viewModel.activeNotes.collectAsState()
    val allNotes     by viewModel.allNotes.collectAsState()
    val currentTool  by viewModel.currentTool.collectAsState()
    val snapIndex    by viewModel.snapIndex.collectAsState()
    val selectedIds  by viewModel.selectedNoteIds.collectAsState()
    val loopEnabled  by viewModel.loopEnabled.collectAsState()
    val loopStart    by viewModel.loopStart.collectAsState()
    val loopEnd      by viewModel.loopEnd.collectAsState()
    val bpm       = session?.bpm ?: 120
    val totalBars = session?.totalBars ?: 4
    val sessionVal = session
    val bpmVal = sessionVal?.bpm ?: 120
    val keyLabelVal = "${sessionVal?.keyRoot ?: "C"} ${sessionVal?.keyQuality?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Major"}"

    val uiState = PianoRollUiState(
        tracks       = tracks,
        activeIndex  = activeIndex,
        activeNotes  = activeNotes,
        allNotes     = allNotes,
        currentTool  = currentTool,
        snapIndex    = snapIndex,
        selectedIds  = selectedIds,
        loopEnabled  = loopEnabled,
        loopStart    = loopStart,
        loopEnd      = loopEnd,
        playheadBeat = playheadBeat,
        isPlaying    = isPlaying,
        totalBars    = totalBars,
        hasClipboard = hasClipboard,
        drawDuration = drawDuration,
        bpm          = bpmVal,
        keyLabel     = keyLabelVal
    )

    LaunchedEffect(playheadBeat, isPlaying) {
        if (isPlaying && state.gridWidth > 0f) {
            val playheadX = state.beatToX(playheadBeat)
            val threshold = state.gridWidth * 0.7f
            if (playheadX > threshold) {
                state.scrollX = (playheadBeat * state.pixelsPerBeat - state.gridWidth * 0.3f)
                    .coerceAtLeast(0f)
            }
        }
    }

    LaunchedEffect(isPlaying, playheadBeat) {
        if (!isPlaying && playheadBeat == 0f) state.scrollX = 0f
    }

    val scope = rememberCoroutineScope()

    suspend fun generateGroovePreview(
        bassStyle: BassStyle,
        drumPresetName: String,
        motown: Boolean,
        jaco: Boolean,
        dart: Boolean,
        variation: Boolean
    ): GroovePreview? {
        val bassTrack = tracks.firstOrNull { it.name.uppercase() == "BASS" } ?: return null
        val drumTrack = tracks.firstOrNull { it.name.uppercase() == "DRUMS" } ?: return null

        val context = com.reasontouch.feature.chords.CompositionContextBuilder.buildContext(session, chords, chordUiState.barDuration.toFloat())
        val bassNotes = BassGenerator.generate(
            chords = chords,
            style = bassStyle,
            targetTrackId = bassTrack.id,
            beatsPerBar = chordUiState.barDuration.toFloat(),
            appendOffset = 0f,
            snapValue = 0.25f,
            context = context,
            useMotownWalk = motown,
            useJacoExtensions = jaco,
            useDartGhostNotes = dart,
            useMotifVariation = variation
        )
        val segments = drumViewModel.generateSegmentsFromContext(context, drumPresetName)
        val drumNotes = mutableListOf<com.reasontouch.core.data.NoteEvent>()
        segments.forEach { segment ->
            val patternBeats = segment.pattern.steps * (1f / 4f)
            drumNotes.addAll(
                generateDrumNotes(
                    pattern = segment.pattern,
                    barCount = segment.barCount,
                    trackId = drumTrack.id,
                    baseOffset = segment.startBar * patternBeats
                )
            )
        }

        return GroovePreview(bassStyle, bassNotes, drumPresetName, drumNotes)
    }
    Column(modifier = Modifier.fillMaxSize().background(BG)) {

        PianoRollToolbar(
            uiState           = uiState,
            onTool            = viewModel::setTool,
            onSnapCycle       = { viewModel.setSnapIndex((snapIndex + 1) % viewModel.snapValues.size) },
            onLoopToggle      = viewModel::toggleLoop,
            onInstrumentClick = { showInstrumentPanel = !showInstrumentPanel },
            onUndo            = { viewModel.undo() }
        )

        if (uiState.hasSelection) {
            SelectionActionBar(
                selectedCount = selectedIds.size,
                hasClipboard  = hasClipboard,
                pasteAtBeat   = playheadBeat,
                onCopy        = { viewModel.copySelectedNotes() },
                onPaste       = { viewModel.pasteNotes(playheadBeat, uiState.snapValue) },
                onDelete      = { viewModel.deleteSelectedNotes() },
                onClear       = { viewModel.clearSelection() },
                onTranspose   = { delta -> viewModel.transposeSelectedNotes(delta) },
                onInvert      = { viewModel.invertSelectedNotes() }
            )
        }

        // Floating instrument panel - appears above track row when open
        if (showInstrumentPanel) {
            val activeTrack = tracks.getOrNull(activeIndex)
            if (activeTrack != null) {
                InstrumentPanel(
                    trackName      = activeTrack.name,
                    currentProgram = activeTrack.gmProgram,
                    onSelect       = { program ->
                        viewModel.setTrackGmProgram(activeTrack.id, program)
                        showInstrumentPanel = false
                    },
                    onDismiss = { showInstrumentPanel = false }
                )
            }
        } else {
            val scrollState = rememberScrollState()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                NeoTrackCardRow(
                    tracks         = tracks,
                    activeIndex    = activeIndex,
                    onSelect       = viewModel::setActiveTrack,
                    onMuteToggle   = viewModel::muteTrack,
                    onVolumeChange = viewModel::setTrackVolume
                )
            }
        }

        val activeTrackName = tracks.getOrNull(activeIndex)?.name?.uppercase() ?: ""
        if (activeTrackName.isNotEmpty()) {
            val listState = androidx.compose.foundation.lazy.rememberLazyListState()
            val currentBar = (playheadBeat / 4f).toInt().coerceIn(0, totalBars - 1)
            androidx.compose.runtime.LaunchedEffect(currentBar) {
                listState.animateScrollToItem(currentBar)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF222228))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("$activeTrackName INSPECT:", color = Color(0xFF38BDF8), fontSize = 9.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
                androidx.compose.foundation.lazy.LazyRow(
                    state = listState,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(totalBars) { barIdx ->
                        val isCurrent = barIdx == currentBar
                        val isEditing = barIdx == activeBarToInspect
                        val label = when (activeTrackName) {
                            "CHORD", "PAD" -> chords.firstOrNull { it.barIndex == barIdx }?.chordName ?: "—"
                            "BASS" -> "Style"
                            "DRUMS" -> "Groove"
                            "LEAD" -> "Melody"
                            else -> "Bar"
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    when {
                                        isEditing -> Color(0xFF3DDC84).copy(alpha = 0.3f) // Green for editing
                                        isCurrent -> Color(0xFFE84040).copy(alpha = 0.3f) // Red for playback
                                        else -> Color(0xFF2A2A32)
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    color = when {
                                        isEditing -> Color(0xFF3DDC84)
                                        isCurrent -> Color(0xFFE84040)
                                        else -> Color(0xFF3A3A45)
                                    },
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clickable {
                                    activeBarToInspect = barIdx
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Bar ${barIdx + 1}: $label",
                                color = when {
                                    isEditing || isCurrent -> Color.White
                                    else -> Color(0xFFC8C8D4)
                                },
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            PianoRollCanvas(
                state     = state,
                uiState   = uiState,
                viewModel = viewModel,
                modifier  = Modifier.fillMaxSize()
            )

            if (activeBarToInspect != null) {
                val barIdx = activeBarToInspect!!
                val chordEvent = chords.firstOrNull { it.barIndex == barIdx }
                val chordNames = chords.map { it.chordName.substringBefore(" ") }
                val key = KeyDetector.detect(chordNames).firstOrNull()

                when (activeTrackName) {
                    "CHORD" -> com.reasontouch.feature.chords.components.ChordInspectorDialog(
                        barIndex = barIdx,
                        currentChord = chordEvent,
                        key = key,
                        currentStrumPattern = chordEvent?.strumPatternId?.toStepPattern(),
                        onSubstitute = { newTheoryChord, strumPattern ->
                            viewModel.recordSnapshot()
                            chordViewModel.substituteChord(barIdx, newTheoryChord, strumPattern) { viewModel.refreshNotes() }
                            activeBarToInspect = null
                        },
                        onTranspose = { delta ->
                            viewModel.recordSnapshot()
                            chordViewModel.transposeBar(barIdx, delta) { viewModel.refreshNotes() }
                            activeBarToInspect = null
                        },
                        onInvert = {
                            viewModel.recordSnapshot()
                            chordViewModel.invertBar(barIdx) { viewModel.refreshNotes() }
                            activeBarToInspect = null
                        },
                        onDismiss = { activeBarToInspect = null }
                    )
                    "BASS" -> com.reasontouch.feature.chords.components.BassInspectorDialog(
                        barIndex = barIdx,
                        currentStyle = null,
                        onApply = { style, motown, jaco, dart, variation ->
                            viewModel.recordSnapshot()
                            chordViewModel.substituteBass(barIdx, style, motown, jaco, dart, variation) { viewModel.refreshNotes() }
                            activeBarToInspect = null
                        },
                        onDismiss = { activeBarToInspect = null }
                    )
                    "DRUMS" -> com.reasontouch.feature.pianoroll.components.DrumInspectorDialog(
                        barIndex = barIdx,
                        onApplyFill = { fillType ->
                            viewModel.recordSnapshot()
                            drumViewModel.writeFillToPianoRoll(fillType, barIdx, appendMode = true) { viewModel.refreshNotes() }
                            activeBarToInspect = null
                        },
                        onApplyPreset = { presetName ->
                            viewModel.recordSnapshot()
                            drumViewModel.writePresetToPianoRoll(presetName, barIdx, appendMode = true) { viewModel.refreshNotes() }
                            activeBarToInspect = null
                        },
                        onDismiss = { activeBarToInspect = null }
                    )
                    "LEAD" -> com.reasontouch.feature.chords.components.LeadInspectorDialog(
                        barIndex = barIdx,
                        onApplyPreset = { presetName ->
                            viewModel.recordSnapshot()
                            chordViewModel.substituteLead(barIdx, presetName) { viewModel.refreshNotes() }
                            activeBarToInspect = null
                        },
                        onDismiss = { activeBarToInspect = null }
                    )
                    "PAD" -> com.reasontouch.feature.chords.components.PadInspectorDialog(
                        barIndex = barIdx,
                        currentChord = chordEvent,
                        onApplyVoicing = { voicing ->
                            viewModel.recordSnapshot()
                            chordViewModel.substitutePad(barIdx, voicing) { viewModel.refreshNotes() }
                            activeBarToInspect = null
                        },
                        onDismiss = { activeBarToInspect = null }
                    )
                }
            }

            CompositionTray(
                expanded             = trayExpanded,
                onExpandedChange     = { trayExpanded = it },
                section              = traySection,
                onSectionChange      = { traySection = it },
                suggestions          = currentSuggestions,
                onRequestSuggestions = {
                    scope.launch { currentSuggestions = viewModel.suggestNextOptions() }
                },
                onOptionSelected     = { intentType, phraseIndex, customPattern, customSpeed ->
                    currentSuggestions
                        .firstOrNull { it.option.type == intentType }
                        ?.phrases
                        ?.getOrNull(phraseIndex)
                        ?.let { phrase ->
                            viewModel.recordSnapshot()
                            chordViewModel.addPhrase(
                                generatedProgression = phrase,
                                pairingType = intentType,
                                customStrumPattern = customPattern,
                                customStrumSpeed = customSpeed
                            ) {
                                viewModel.refreshNotes()
                            }
                        }
                    currentSuggestions = emptyList()
                },
                onBassRequested      = {
                    val context = CompositionContextBuilder.buildContext(session, chords, chordUiState.barDuration.toFloat())
                    val density = context.bars.lastOrNull()?.attackDensity ?: 0f
                    bassSuggestions = BassStyle.suggestForDensity(density)
                },
                onDrumsRequested     = {
                    val context = CompositionContextBuilder.buildContext(session, chords, chordUiState.barDuration.toFloat())
                    val density = context.bars.lastOrNull()?.attackDensity ?: 0f
                    drumSuggestions = DrumPresets.suggestForDensity(density)
                },
                bassSuggestions      = bassSuggestions,
                drumSuggestions      = drumSuggestions,
                transitionFill       = drumViewModel.transitionFill.value,
                onTransitionFillChange = drumViewModel::setTransitionFill,
                endFill              = drumViewModel.endFill.value,
                onEndFillChange      = drumViewModel::setEndFill,
                onLead               = { traySection = TraySection.LEAD },
                onApplyLead          = { presetName, append ->
                    viewModel.recordSnapshot()
                    chordViewModel.generateLead(presetName, append) {
                        viewModel.refreshNotes()
                    }
                    trayExpanded = false
                },
                onHarmonizeFromLead  = {
                    viewModel.recordSnapshot()
                    chordViewModel.harmonizeFromLeadTrack { viewModel.refreshNotes() }
                    trayExpanded = false
                },
                hasLead              = tracks.firstOrNull { it.name.uppercase() == "LEAD" }
                    ?.let { track -> allNotes[track.id]?.isNotEmpty() } ?: false,
                onPad                = { traySection = TraySection.PAD },
                onApplyPad           = { append ->
                    viewModel.recordSnapshot()
                    chordViewModel.generatePad(append) {
                        viewModel.refreshNotes()
                    }
                    trayExpanded = false
                },
                onHarmonizeFromPad   = {
                    viewModel.recordSnapshot()
                    chordViewModel.harmonizeFromPadTrack { viewModel.refreshNotes() }
                    trayExpanded = false
                },
                hasPad               = tracks.firstOrNull { it.name.uppercase() == "PAD" }
                    ?.let { track -> allNotes[track.id]?.isNotEmpty() } ?: false,
                onFills              = { traySection = TraySection.FILLS },
                onApplyFill          = { fillType, barIndex, append ->
                    viewModel.recordSnapshot()
                    drumViewModel.writeFillToPianoRoll(fillType, barIndex, append) {
                        viewModel.refreshNotes()
                    }
                    trayExpanded = false
                },
                hasFills             = tracks.firstOrNull { it.name.uppercase() == "DRUMS" }
                    ?.let { track -> allNotes[track.id]?.any { note -> note.duration == 0.25f && note.velocity >= 95 } } ?: false,
                onApplyBass          = { style, append, motown, jaco, dart, variation ->
                    viewModel.recordSnapshot()
                    chordViewModel.generateBass(style, append, motown, jaco, dart, variation) {
                        viewModel.refreshNotes()
                    }
                    trayExpanded = false
                },
                onApplyDrums         = { presetName, append ->
                    viewModel.recordSnapshot()
                    DrumPresets.all[presetName]?.let { drumViewModel.applyPreset(it) }
                    drumViewModel.writeToPianoRoll(append) { viewModel.refreshNotes() }
                    trayExpanded = false
                },
                onFullGrooveRequested = {
                    groovePreview = null
                    val context = CompositionContextBuilder.buildContext(session, chords, chordUiState.barDuration.toFloat())
                    val density = context.bars.lastOrNull()?.attackDensity ?: 0f
                    grooveBassSuggestions = BassStyle.suggestForDensityWithRationale(density)
                    grooveDrumSuggestions = DrumPresets.suggestForDensityWithRationale(density)
                },
                compositionMode       = compositionMode,
                grooveBassSuggestions = grooveBassSuggestions,
                grooveDrumSuggestions = grooveDrumSuggestions,
                groovePreview         = groovePreview,
                onGenerateGroove      = { bassStyle, drumPresetName, motown, jaco, dart, variation ->
                    scope.launch {
                        groovePreview = generateGroovePreview(bassStyle, drumPresetName, motown, jaco, dart, variation)
                    }
                },
                onApplyGroove         = {
                    viewModel.recordSnapshot()
                    groovePreview?.let { preview ->
                        viewModel.saveGeneratedNotes(preview.bassNotes + preview.drumNotes)
                    }
                    groovePreview = null
                    trayExpanded = false
                },
                onDiscardGroove       = { groovePreview = null },
                hasHarmony           = chords.isNotEmpty(),
                hasBass              = tracks.firstOrNull { it.name.uppercase() == "BASS" }
                    ?.let { track -> allNotes[track.id]?.isNotEmpty() } ?: false,
                hasDrums             = tracks.firstOrNull { it.name.uppercase() == "DRUMS" }
                    ?.let { track -> allNotes[track.id]?.isNotEmpty() } ?: false,
                onHarmonizeFromBass  = {
                    viewModel.recordSnapshot()
                    chordViewModel.harmonizeFromBassTrack { viewModel.refreshNotes() }
                    trayExpanded = false
                },
                modifier             = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

// -- Instrument panel ----------------------------------------------------------

@Composable
fun InstrumentPanel(
    trackName:      String,
    currentProgram: Int,
    onSelect:       (Int) -> Unit,
    onDismiss:      () -> Unit
) {
    val instruments = instrumentsForTrack(trackName)
    val emoji       = emojiForTrack(trackName)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E2A))
            .border(1.dp, BLUE.copy(alpha = 0.4f), RoundedCornerShape(0.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$emoji  ${trackName.uppercase()} SOUND",
                color = BLUE, fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace, letterSpacing = 1.sp
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("X", color = TEXT_DIM, fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (instruments == null) {
            // DRUMS � WAV engine, no picker
            Text(
                text = "WAV Kit � sound managed by Drum Machine",
                color = TEXT_DIM, fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        } else {
            // Instrument chips in a wrapping row
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement   = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                instruments.forEach { inst ->
                    val isSelected = inst.program == currentProgram
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isSelected) BLUE.copy(alpha = 0.2f)
                                else Color(0xFF16161A)
                            )
                            .border(
                                1.dp,
                                if (isSelected) BLUE else BORDER,
                                RoundedCornerShape(4.dp)
                            )
                            .clickable { onSelect(inst.program) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = inst.label,
                                color = if (isSelected) BLUE else TEXT,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "GM ${inst.program}",
                                color = TEXT_DIM, fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

// -- Selection action bar ------------------------------------------------------

@Composable
fun SelectionActionBar(
    selectedCount: Int,
    hasClipboard:  Boolean,
    pasteAtBeat:   Float = 0f,
    onCopy:        () -> Unit,
    onPaste:       () -> Unit,
    onDelete:      () -> Unit,
    onClear:       () -> Unit,
    onTranspose:   (Int) -> Unit,
    onInvert:      () -> Unit
) {
    val scrollState = androidx.compose.foundation.rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(Color(0xFF1E1A2E))
            .horizontalScroll(scrollState)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SelectionChip("DELETE", ACCENT,   onDelete)
        SelectionChip("COPY",   PURPLE, onCopy)
        if (hasClipboard) {
            SelectionChip(
                "PASTE",
                GREEN, onPaste
            )
        }
        SelectionChip("-12", BLUE, { onTranspose(-12) })
        SelectionChip("-1", BLUE, { onTranspose(-1) })
        SelectionChip("+1", BLUE, { onTranspose(1) })
        SelectionChip("+12", BLUE, { onTranspose(12) })
        SelectionChip("INVERT", GOLD, onInvert)
        SelectionChip("X",      TEXT_DIM, onClear)
    }
    Box(modifier = Modifier.fillMaxWidth().height(1.dp)
        .background(PURPLE.copy(alpha = 0.3f)))
}

@Composable
fun SelectionChip(label: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(color.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = color, fontSize = 11.sp,
            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

// -- Toolbar -------------------------------------------------------------------

@Composable
fun PianoRollToolbar(
    uiState:           PianoRollUiState,
    onTool:            (PianoRollViewModel.Tool) -> Unit,
    onSnapCycle:       () -> Unit,
    onLoopToggle:      () -> Unit,
    onInstrumentClick: () -> Unit,
    onUndo:            () -> Unit
) {
    val activeTrack = uiState.tracks.getOrNull(uiState.activeIndex)
    val emoji       = if (activeTrack != null) emojiForTrack(activeTrack.name) else "\uD83C\uDFB5"
    val instLabel   = if (activeTrack != null) {
        val instruments = instrumentsForTrack(activeTrack.name)
        instruments?.firstOrNull { it.program == activeTrack.gmProgram }?.label
            ?: if (activeTrack.name.uppercase() == "DRUMS") "WAV Kit" else "GM ${activeTrack.gmProgram}"
    } else ""

    Row(
        modifier = Modifier
            .fillMaxWidth().height(40.dp)
            .background(PANEL).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        PianoRollViewModel.Tool.entries.forEach { tool ->
            ToolChip(
                label    = tool.name,
                selected = tool == uiState.currentTool,
                onClick  = { onTool(tool) }
            )
        }

        Divider()
        ToolChip(
            label          = "LOOP",
            selected       = uiState.loopEnabled,
            selectedColor  = Color(0xFF1A4A2E),
            selectedBorder = GREEN,
            selectedText   = GREEN,
            onClick        = onLoopToggle
        )
        Divider()
        ToolChip(
            label          = "UNDO",
            selected       = false,
            selectedColor  = Color(0xFF3A2A1A),
            selectedBorder = Color(0xFFF5C518),
            selectedText   = Color(0xFFF5C518),
            onClick        = onUndo
        )
        Divider()
        // Instrument selector button - right side, weight fills remaining space
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF16161A))
                .border(1.dp, BLUE.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                .clickable(onClick = onInstrumentClick)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$emoji $instLabel \u25BE",
                color = BLUE, fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
        }
    }
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BORDER))
}

@Composable
private fun Divider() {
    Box(modifier = Modifier.width(1.dp).height(24.dp).background(BORDER))
}

@Composable
fun ToolChip(
    label:          String,
    selected:       Boolean,
    onClick:        () -> Unit,
    monospace:      Boolean = false,
    selectedColor:  Color   = ACCENT,
    selectedBorder: Color   = ACCENT,
    selectedText:   Color   = Color.White
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(if (selected) selectedColor else RACK)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) selectedText else TEXT_DIM,
            fontSize = 12.sp, fontWeight = FontWeight.Bold,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default
        )
    }
}