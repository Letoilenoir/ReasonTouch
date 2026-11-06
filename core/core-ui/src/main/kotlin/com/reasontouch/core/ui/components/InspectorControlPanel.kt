package com.reasontouch.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.reasontouch.core.playback.GridSubdivisionEnum
import com.reasontouch.core.playback.GrooveModifierState
import com.reasontouch.core.playback.InspectorState
import com.reasontouch.core.playback.ParadiddleModeEnum
import com.reasontouch.core.ui.theme.ReasonTouchTheme

@Composable
fun InspectorControlPanel(
    inspectorState: InspectorState,
    onTransposeChange: (Int) -> Unit,
    onGrooveChange: (GrooveModifierState) -> Unit,
    modifier: Modifier = Modifier
) {
    val skin = ReasonTouchTheme.skin

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Transpose Row (Pitched tracks only)
        if (inspectorState.isPitched) {
            Text(
                text = "BAR TRANSPOSE",
                color = skin.textSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TransposeChip("-12", skin) { onTransposeChange(-12) }
                TransposeChip("-1", skin) { onTransposeChange(-1) }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                        .background(skin.panelAlt, RoundedCornerShape(3.dp))
                        .border(1.dp, skin.border, RoundedCornerShape(3.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val display = if (inspectorState.transposeSemitones > 0) "+${inspectorState.transposeSemitones} ST" else "${inspectorState.transposeSemitones} ST"
                    Text(
                        text = display,
                        color = skin.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                TransposeChip("+1", skin) { onTransposeChange(1) }
                TransposeChip("+12", skin) { onTransposeChange(12) }
            }
            Spacer(Modifier.height(4.dp))
        }

        // Grid Subdivision Selector
        Text(
            text = "RHYTHM / SUBDIVISION",
            color = skin.textSecondary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.5.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val subOptions = listOf(
                "1/16" to GridSubdivisionEnum.STRAIGHT_16,
                "8T" to GridSubdivisionEnum.TRIPLET_8T,
                "16T" to GridSubdivisionEnum.TRIPLET_16T
            )
            subOptions.forEach { (label, sub) ->
                val isSelected = inspectorState.grooveState.gridSubdivision == sub
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isSelected) skin.accent.copy(alpha = 0.2f) else skin.panelAlt)
                        .border(1.dp, if (isSelected) skin.accent else skin.border, RoundedCornerShape(3.dp))
                        .clickable {
                            onGrooveChange(inspectorState.grooveState.copy(gridSubdivision = sub))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) skin.accent else skin.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Paradiddle / Modifier Selector
        Text(
            text = "PARADIDDLE / MODIFIER",
            color = skin.textSecondary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.5.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val modOptions = listOf(
                "Off" to ParadiddleModeEnum.OFF,
                "Accent" to ParadiddleModeEnum.ACCENT_VELOCITY,
                "Octave" to ParadiddleModeEnum.OCTAVE_BOUNCE,
                "Root/5th" to ParadiddleModeEnum.ROOT_FIFTH
            )
            modOptions.forEach { (label, mode) ->
                val isSelected = inspectorState.grooveState.paradiddleMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isSelected) skin.accent.copy(alpha = 0.2f) else skin.panelAlt)
                        .border(1.dp, if (isSelected) skin.accent else skin.border, RoundedCornerShape(3.dp))
                        .clickable {
                            onGrooveChange(inspectorState.grooveState.copy(paradiddleMode = mode))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) skin.accent else skin.textPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun TransposeChip(label: String, skin: com.reasontouch.core.ui.theme.ReasonTouchSkin, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(36.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(skin.panelAlt)
            .border(1.dp, skin.border, RoundedCornerShape(3.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = skin.textPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
