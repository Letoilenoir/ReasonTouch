package com.reasontouch.core.ui.components

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
import com.reasontouch.core.midi.StepState
import androidx.compose.foundation.layout.width
import com.reasontouch.core.ui.components.StepSequencerEditor
import com.reasontouch.core.ui.components.SequencerMode


enum class SequencerMode {
    CHORD_STRUM,  // States: OFF, DOWN, UP
    DRUM          // States: OFF, ON
}

@Composable
fun StepSequencerEditor(
    steps: List<StepState>,
    onStepChanged: (Int, StepState) -> Unit,
    mode: SequencerMode = SequencerMode.CHORD_STRUM,
    label: String = "PATTERN",
    showLegend: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 12.dp)) {
        // Label
        if (label.isNotEmpty()) {
            Text(
                label,
                color = TEXT_DIM,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Beat labels (B1, B2, B3, B4)
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("B1", "B2", "B3", "B4").forEach { beatLabel ->
                Text(
                    beatLabel,
                    color = TEXT_DIM,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))

        // 16-step grid (4 beats × 4 steps per beat)
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
                        val stepIndex = beat * 4 + sub
                        StepButton(
                            state = steps[stepIndex],
                            mode = mode,
                            onClick = {
                                val newState = cycleStep(steps[stepIndex], mode)
                                onStepChanged(stepIndex, newState)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Legend
        if (showLegend) {
            StepLegend(mode)
        }
    }
}

@Composable
private fun StepButton(
    state: StepState,
    mode: SequencerMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (bg, fg, label) = when (mode) {
        SequencerMode.CHORD_STRUM -> when (state) {
            StepState.DOWN -> Triple(DOWN_COL, BLUE, "D")
            StepState.UP -> Triple(UP_COL, Color(0xFFA78BFA), "U")
            StepState.OFF -> Triple(RACK, TEXT_DIM, ".")
        }
        SequencerMode.DRUM -> when (state) {
            StepState.OFF -> Triple(RACK, TEXT_DIM, ".")
            else -> Triple(BLUE.copy(alpha = 0.3f), BLUE, "●")  // ON state
        }
    }

    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(bg)
            .border(
                1.dp,
                if (state == StepState.OFF) BORDER else fg,
                RoundedCornerShape(3.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = fg,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun StepLegend(mode: SequencerMode) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        when (mode) {
            SequencerMode.CHORD_STRUM -> {
                StepLegendItem(color = BLUE, label = "D = Down")
                StepLegendItem(color = Color(0xFFA78BFA), label = "U = Up")
                StepLegendItem(color = BORDER, label = ". = Off")
            }
            SequencerMode.DRUM -> {
                StepLegendItem(color = BLUE, label = "● = On")
                StepLegendItem(color = BORDER, label = ". = Off")
            }
        }
    }
}

@Composable
private fun StepLegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .height(8.dp)
                .width(8.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(
            label,
            color = TEXT_DIM,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

// Cycle step state based on mode
private fun cycleStep(state: StepState, mode: SequencerMode): StepState {
    return when (mode) {
        SequencerMode.CHORD_STRUM -> when (state) {
            StepState.OFF -> StepState.DOWN
            StepState.DOWN -> StepState.UP
            StepState.UP -> StepState.OFF
        }
        SequencerMode.DRUM -> when (state) {
            StepState.OFF -> StepState.DOWN  // Treat DOWN = for drums
            else -> StepState.OFF
        }
    }
}

// Color palette (copy from MoodWorkspace or import from theme)
private val RACK = Color(0xFF1A1A1E)
private val PANEL = Color(0xFF2A2A32)
private val BORDER = Color(0xFF3A3A45)
private val ACCENT = Color(0xFFE84040)
private val TEXT_DIM = Color(0xFF666675)
private val BLUE = Color(0xFF6B9FDB)
private val DOWN_COL = Color(0xFF2A1A1A)
private val UP_COL = Color(0xFF1A2A2A)
