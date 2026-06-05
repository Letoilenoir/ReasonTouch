package com.reasontouch.feature.chords.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reasontouch.core.ui.theme.*
import androidx.compose.ui.graphics.Color
import com.reasontouch.feature.chords.CompositionMode

@Composable
fun CompositionModeBar(
    mode: CompositionMode,
    onModeSelected: (CompositionMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(ModeBarBackground)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        CompositionModeChip(
            label = "MANUAL",
            selected = mode == CompositionMode.MANUAL,
            accent = ManualModeColor,
            onClick = { onModeSelected(CompositionMode.MANUAL) },
            modifier = Modifier.weight(1f)
        )

        CompositionModeChip(
            label = "ASSISTED",
            selected = mode == CompositionMode.ASSISTED,
            accent = AssistedModeColor,
            onClick = { onModeSelected(CompositionMode.ASSISTED) },
            modifier = Modifier.weight(1f)
        )

        CompositionModeChip(
            label = "GUIDED",
            selected = mode == CompositionMode.GUIDED,
            accent = GuidedModeColor,
            onClick = { onModeSelected(CompositionMode.GUIDED) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CompositionModeChip(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(44.dp)
            .background(
                color = if (selected)
                    accent.copy(alpha = 0.15f)
                else
                    ModeChipBackground,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                color = if (selected)
                    accent
                else
                    Color(0xFF3A3A45),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            color = if (selected) accent else Color(0xFF9999AA)
        )
    }
}