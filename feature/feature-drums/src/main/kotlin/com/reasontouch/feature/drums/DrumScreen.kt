package com.reasontouch.feature.drums

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.hilt.navigation.compose.hiltViewModel

private val BG       = Color(0xFF1A1A1E)
private val RACK     = Color(0xFF222228)
private val PANEL    = Color(0xFF2A2A32)
private val BORDER   = Color(0xFF3A3A45)
private val ACCENT   = Color(0xFFE84040)
private val ACCENT2  = Color(0xFFFF6B35)
private val GREEN    = Color(0xFF3DDC84)
private val TEXT     = Color(0xFFC8C8D4)
private val TEXT_DIM = Color(0xFF666675)
private val BLUE     = Color(0xFF38BDF8)

@Composable
fun DrumScreen(
    sessionId:        String,
    onNavigateToRoll: () -> Unit = {},
    viewModel:        DrumViewModel = hiltViewModel()
) {
    val pattern     by viewModel.pattern.collectAsState()
    val isPlaying   by viewModel.isPlaying.collectAsState()
    val currentStep by viewModel.currentStep.collectAsState()
    val session     by viewModel.session.collectAsState()
    var statusMsg   by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(BG)) {

        // ── Toolbar ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth().height(48.dp)
                .background(RACK).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "DRUM MACHINE", color = ACCENT, fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace, letterSpacing = 2.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DrumChip(
                    label    = if (pattern.steps == 16) "16 STEPS" else "32 STEPS",
                    selected = false, color = ACCENT2,
                    onClick  = {
                        viewModel.setStepCount(if (pattern.steps == 16) 32 else 16)
                    }
                )
                DrumChip("CLR", false, ACCENT) { viewModel.clearPattern() }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(ACCENT))

        // ── Beat markers ──────────────────────────────────────────────────
        BeatMarkers(steps = pattern.steps, currentStep = currentStep)

        // ── Step grid ─────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            DrumKit.lanes.forEachIndexed { laneIdx, lane ->
                DrumLaneRow(
                    lane        = lane,
                    pattern     = pattern,
                    laneIdx     = laneIdx,
                    currentStep = currentStep,
                    onToggle    = { stepIdx -> viewModel.toggleStep(laneIdx, stepIdx) },
                    onPadTap    = { viewModel.auditionLane(laneIdx) }
                )
            }
        }

        // ── Drum Fills Grid (utilizing lower estate) ──────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                "DRUM FILLS (Tap to insert)", color = TEXT_DIM, fontSize = 9.sp,
                fontFamily = FontFamily.Monospace, letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val keys = DrumFillPatterns.FILLS.keys.toList()
                items(count = keys.size) { index ->
                    val key = keys[index]
                    val fill = DrumFillPatterns.FILLS[key]!!
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(PANEL)
                            .border(1.dp, BORDER, RoundedCornerShape(4.dp))
                            .clickable {
                                viewModel.writeFillToPianoRoll(key, targetBarIndex = 0, appendMode = true) { msg ->
                                    statusMsg = msg
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Column {
                            Text(
                                text = fill.name,
                                color = TEXT,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = fill.description,
                                color = TEXT_DIM,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // ── Status ────────────────────────────────────────────────────────
        if (statusMsg.isNotEmpty()) {
            Text(
                statusMsg, color = GREEN, fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
            )
        }

        // ── Transport ─────────────────────────────────────────────────────
        DrumTransport(
            isPlaying = isPlaying,
            bpm       = session?.bpm ?: 120,
            onPlay    = viewModel::play,
            onStop    = viewModel::stop,
            onWrite   = { append ->
                viewModel.stop()
                viewModel.writeToPianoRoll(append) { msg ->
                    statusMsg = msg
                    onNavigateToRoll()
                }
            }
        )

        // ── Preset strip — always visible below transport ─────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF16161A))
                .padding(vertical = 6.dp)
        ) {
            Text(
                "PRESETS", color = TEXT_DIM, fontSize = 9.sp,
                fontFamily = FontFamily.Monospace, letterSpacing = 2.sp,
                modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(DrumPresets.all.entries.toList()) { (name, preset) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(PANEL)
                            .border(1.dp, BORDER, RoundedCornerShape(4.dp))
                            .clickable { viewModel.applyPreset(preset) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            name, color = TEXT, fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                item {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF2A1A1A))
                            .border(1.dp, ACCENT, RoundedCornerShape(4.dp))
                            .clickable { viewModel.clearPattern() }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "CLEAR", color = ACCENT, fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ── Beat markers ──────────────────────────────────────────────────────────────

@Composable
fun BeatMarkers(steps: Int, currentStep: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth().height(20.dp)
            .padding(start = 52.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        (0 until steps).forEach { step ->
            val isBeat   = step % 4 == 0
            val isActive = step == currentStep
            Box(
                modifier = Modifier
                    .weight(1f).height(20.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(when {
                        isActive -> GREEN.copy(alpha = 0.8f)
                        isBeat   -> PANEL
                        else     -> Color.Transparent
                    }),
                contentAlignment = Alignment.Center
            ) {
                if (isBeat) Text(
                    "${step / 4 + 1}",
                    color = if (isActive) Color.Black else TEXT_DIM,
                    fontSize = 9.sp, fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── Lane row ──────────────────────────────────────────────────────────────────

@Composable
fun DrumLaneRow(
    lane:        DrumLane,
    pattern:     DrumPattern,
    laneIdx:     Int,
    currentStep: Int,
    onToggle:    (Int) -> Unit,
    onPadTap:    () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .width(48.dp).height(36.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(lane.color.copy(alpha = 0.25f))
                .border(1.dp, lane.color.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                .clickable(onClick = onPadTap),
            contentAlignment = Alignment.Center
        ) {
            Text(
                lane.shortName, color = lane.color, fontSize = 11.sp,
                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace
            )
        }

        (0 until pattern.steps).forEach { stepIdx ->
            val isActive   = pattern.isActive(laneIdx, stepIdx)
            val isCurrent  = stepIdx == currentStep
            val isDownbeat = stepIdx % 4 == 0
            Box(
                modifier = Modifier
                    .weight(1f).height(36.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(when {
                        isActive && isCurrent -> lane.color
                        isActive              -> lane.color.copy(alpha = 0.8f)
                        isCurrent             -> Color(0xFF303040)
                        isDownbeat            -> Color(0xFF252530)
                        else                  -> Color(0xFF1E1E28)
                    })
                    .border(
                        width = if (isCurrent) 1.5.dp else 0.5.dp,
                        color = when {
                            isCurrent  -> GREEN
                            isActive   -> lane.color.copy(alpha = 0.4f)
                            isDownbeat -> BORDER
                            else       -> Color(0xFF2A2A38)
                        },
                        shape = RoundedCornerShape(3.dp)
                    )
                    .clickable { onToggle(stepIdx) }
            )
        }
    }
}

// ── Transport ─────────────────────────────────────────────────────────────────

@Composable
fun DrumTransport(
    isPlaying: Boolean,
    bpm:       Int,
    onPlay:    () -> Unit,
    onStop:    () -> Unit,
    onWrite:   (Boolean) -> Unit
) {
    var appendMode by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth().background(RACK)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DrumChip(
            label    = if (isPlaying) "|| STOP" else "> PLAY",
            selected = isPlaying,
            color    = if (isPlaying) ACCENT else GREEN,
            onClick  = { if (isPlaying) onStop() else onPlay() }
        )
        Text(
            "$bpm BPM", color = ACCENT2, fontSize = 13.sp,
            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (appendMode) Color(0xFF1A2A3A) else PANEL)
                .border(1.dp,
                    if (appendMode) BLUE else BORDER,
                    RoundedCornerShape(4.dp))
                .clickable { appendMode = !appendMode }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                if (appendMode) "APPEND" else "REPLACE",
                color = if (appendMode) BLUE else TEXT_DIM,
                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF1A3A2A))
                .border(1.dp, GREEN, RoundedCornerShape(4.dp))
                .clickable { onWrite(appendMode) }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                "\u2192 ROLL", color = GREEN, fontSize = 11.sp,
                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace
            )
        }
    }
}

// ── Shared chip ───────────────────────────────────────────────────────────────

@Composable
fun DrumChip(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (selected) color.copy(alpha = 0.2f) else PANEL)
            .border(1.dp, if (selected) color else BORDER, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            label, color = if (selected) color else TEXT_DIM,
            fontSize = 12.sp, fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}