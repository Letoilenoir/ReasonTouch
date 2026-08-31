package com.reasontouch.feature.chords.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.reasontouch.core.midi.StepState
import com.reasontouch.feature.chords.StepPattern
import com.reasontouch.feature.chords.StrumPatterns
import com.reasontouch.feature.chords.STRUM_SPEED_PRESETS
import com.reasontouch.core.ui.components.StepSequencerEditor
import com.reasontouch.core.ui.components.SequencerMode
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as columnItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.grid.GridItemSpan

// Theme colors
private val PANEL = Color(0xFF2A2A32)
private val BORDER = Color(0xFF3A3A45)
private val ACCENT = Color(0xFFE84040)
private val TEXT_DIM = Color(0xFF666675)
private val TEXT = Color(0xFFC8C8D4)

@Composable
fun StrumPatternTray(
    selectedPattern: StepPattern?,
    onPatternSelected: (StepPattern) -> Unit,
    patterns: Map<String, Map<String, StepPattern>>,
    selectedSpeed: Double,
    onSpeedSelected: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }
    var customSteps by remember { mutableStateOf(selectedPattern?.steps ?: List(16) { StepState.OFF }) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PANEL)
            .border(1.dp, BORDER, RoundedCornerShape(8.dp))
            .clickable(enabled = !isEditMode) { isExpanded = !isExpanded }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val speedLabel = STRUM_SPEED_PRESETS.firstOrNull { it.beatsPerString == selectedSpeed }
                ?.label ?: "Custom"
            Text(
                text = "STRUM PATTERN${selectedPattern?.let { " — ${getPatternName(it)}" } ?: ""} · $speedLabel",
                color = TEXT_DIM,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (isEditMode) Icons.Default.Clear else (if (isExpanded) Icons.Default.Clear else Icons.Default.Add),
                contentDescription = "Toggle",
                tint = TEXT_DIM,
                modifier = Modifier
                    .size(20.dp)
                    .clickable {
                        if (isEditMode) {
                            isEditMode = false
                        } else {
                            isExpanded = !isExpanded
                        }
                    }
            )
        }

        // EDIT MODE: Custom pattern editor
        if (isEditMode) {
            Spacer(modifier = Modifier.height(8.dp))

            StepSequencerEditor(
                steps = customSteps,
                onStepChanged = { index, newState ->
                    customSteps = customSteps.toMutableList().also {
                        it[index] = newState
                    }
                },
                mode = SequencerMode.CHORD_STRUM,
                label = "CUSTOM PATTERN",
                showLegend = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Confirm / Cancel buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(BORDER.copy(alpha = 0.5f))
                        .clickable {
                            isEditMode = false
                            customSteps = selectedPattern?.steps ?: List(16) { StepState.OFF }
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "CANCEL",
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
                        .background(ACCENT)
                        .clickable {
                            val customPattern = StepPattern(
                                steps = customSteps
                            )
                            onPatternSelected(customPattern)
                            isEditMode = false
                            isExpanded = false
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "APPLY",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
        // PRESET MODE: pattern grid (left, 60%) + speed list (right, 40%), independently scrollable
        else if (isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // PATTERNS — 2-column grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(0.6f).fillMaxHeight(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    patterns.forEach { (groupName, groupPatterns) ->
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = groupName.uppercase(),
                                color = ACCENT,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                            )
                        }
                        gridItems(groupPatterns.entries.toList()) { (name, pattern) ->
                            val isSelected = selectedPattern == pattern
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (isSelected) ACCENT.copy(alpha = 0.15f) else Color.Transparent
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) ACCENT else BORDER,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable {
                                        onPatternSelected(pattern)
                                        isExpanded = false
                                    }
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = name,
                                    color = if (isSelected) ACCENT else TEXT_DIM,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }

                // SPEED — single column list
                LazyColumn(
                    modifier = Modifier.weight(0.4f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    columnItems(STRUM_SPEED_PRESETS) { preset ->
                        val isSelected = preset.beatsPerString == selectedSpeed
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isSelected) ACCENT.copy(alpha = 0.15f) else Color.Transparent
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) ACCENT else BORDER,
                                    RoundedCornerShape(4.dp)
                                )
                                .clickable { onSpeedSelected(preset.beatsPerString) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = preset.label,
                                color = if (isSelected) ACCENT else TEXT,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = preset.description,
                                color = TEXT_DIM,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }

        // CUSTOMIZE button (visible when pattern selected and not in edit/expand mode)
        if (!isEditMode && selectedPattern != null && !isExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(BORDER.copy(alpha = 0.3f))
                    .border(1.dp, BORDER, RoundedCornerShape(4.dp))
                    .clickable {
                        customSteps = selectedPattern.steps
                        isEditMode = true
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "✎ CUSTOMIZE",
                    color = TEXT,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// Helper to find pattern name from StrumPatterns
private fun getPatternName(pattern: StepPattern): String {
    return StrumPatterns.all.entries
        .find { it.value == pattern }?.key ?: "Custom"
}