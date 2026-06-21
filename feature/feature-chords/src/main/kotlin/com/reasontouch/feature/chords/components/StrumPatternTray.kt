package com.reasontouch.feature.chords.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.reasontouch.feature.chords.StepPattern
import com.reasontouch.feature.chords.StrumPatterns

private val PANEL = Color(0xFF2A2A32)
private val BORDER = Color(0xFF3A3A45)
private val ACCENT = Color(0xFFE84040)
private val TEXT_DIM = Color(0xFF666675)

@Composable
fun StrumPatternTray(
    selectedPattern: StepPattern?,
    onPatternSelected: (StepPattern) -> Unit,
    patterns: Map<String, Map<String, StepPattern>>,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PANEL)
            .border(1.dp, BORDER, RoundedCornerShape(8.dp))
            .clickable { isExpanded = !isExpanded }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "STRUM PATTERN$${selectedPattern?.let { " — $${getPatternName(it)}" } ?: ""}",
                color = TEXT_DIM,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.Clear else Icons.Default.Add,
                contentDescription = "Toggle",
                tint = TEXT_DIM,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { isExpanded = !isExpanded }
            )
        }

        // Pattern list (shown when expanded)
        if (isExpanded) {
            patterns.forEach { (groupName, groupPatterns) ->
                Text(
                    text = groupName.uppercase(),
                    color = ACCENT,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    groupPatterns.forEach { (name, pattern) ->
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
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = name,
                                color = if (isSelected) ACCENT else TEXT_DIM,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getPatternName(pattern: StepPattern): String {
    return StrumPatterns.all.entries
        .find { it.value == pattern }?.key ?: "Custom"
}