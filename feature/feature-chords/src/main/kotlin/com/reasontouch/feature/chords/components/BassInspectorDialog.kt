package com.reasontouch.feature.chords.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reasontouch.core.ui.theme.ReasonTouchTheme
import com.reasontouch.feature.chords.BassStyle

@Composable
fun BassInspectorDialog(
    barIndex: Int,
    currentStyle: BassStyle?,
    onApply: (BassStyle, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val skin = ReasonTouchTheme.skin
    var useMotown by remember { mutableStateOf(true) }
    var useJaco by remember { mutableStateOf(true) }
    var useDart by remember { mutableStateOf(true) }
    var useVariation by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC111115))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(360.dp)
                .heightIn(max = 520.dp)
                .clip(RoundedCornerShape(skin.cornerRadiusMedium))
                .background(skin.panel)
                .border(1.dp, skin.border, RoundedCornerShape(skin.cornerRadiusMedium))
                .clickable(enabled = false) {}
                .padding(skin.paddingLarge)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "BASS INSPECTOR — BAR ${barIndex + 1}",
                color = skin.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(skin.paddingMedium))

            Text(text = "STYLIST MODIFIERS", color = skin.textSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(4.dp))
            StylistRadioRow("Motown Chromatic Walk", useMotown) { useMotown = it }
            StylistRadioRow("Fusion 7ths/9ths (Jaco)", useJaco) { useJaco = it }
            StylistRadioRow("Ghost Note Pocket (Dart)", useDart) { useDart = it }
            StylistRadioRow("Phrase Motif Variation", useVariation) { useVariation = it }

            Spacer(Modifier.height(skin.paddingMedium))
            Text(text = "SELECT BASS STYLE", color = skin.textSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(4.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BassStyle.entries.toList().forEach { style ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(skin.cornerRadiusSmall))
                            .background(skin.panelAlt)
                            .border(1.dp, skin.border, RoundedCornerShape(skin.cornerRadiusSmall))
                            .clickable { onApply(style, useMotown, useJaco, useDart, useVariation); onDismiss() }
                            .padding(horizontal = skin.paddingMedium, vertical = 8.dp)
                    ) {
                        Column {
                            Text(text = style.label, color = skin.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = style.description, color = skin.textSecondary, fontSize = 9.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(skin.paddingMedium))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(skin.panelAlt, RoundedCornerShape(skin.cornerRadiusSmall))
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "CANCEL", color = skin.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StylistRadioRow(label: String, selected: Boolean, onSelect: (Boolean) -> Unit) {
    val skin = ReasonTouchTheme.skin
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 3.dp)
            .background(if (selected) skin.accent.copy(alpha = 0.15f) else skin.panelAlt, RoundedCornerShape(skin.cornerRadiusSmall))
            .border(width = if (selected) 1.dp else 0.dp, color = if (selected) skin.accent else Color.Transparent, shape = RoundedCornerShape(skin.cornerRadiusSmall))
            .clickable { onSelect(!selected) }
            .padding(horizontal = skin.paddingMedium, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = skin.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .height(16.dp)
                    .background(if (selected) skin.accent else Color.Transparent, RoundedCornerShape(8.dp))
                    .border(1.5.dp, if (selected) skin.accent else skin.textMuted, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Box(modifier = Modifier.width(6.dp).height(6.dp).background(Color.Black, RoundedCornerShape(3.dp)))
                }
            }
        }
    }
}
