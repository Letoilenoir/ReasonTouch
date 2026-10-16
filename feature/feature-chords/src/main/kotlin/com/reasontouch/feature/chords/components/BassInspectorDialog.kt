package com.reasontouch.feature.chords.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
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
                .clip(RoundedCornerShape(skin.cornerRadiusMedium))
                .background(skin.panel)
                .border(1.dp, skin.border, RoundedCornerShape(skin.cornerRadiusMedium))
                .clickable(enabled = false) {}
                .padding(skin.paddingLarge)
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
            StylistToggleRow("Motown Chromatic Walk", useMotown) { useMotown = it }
            StylistToggleRow("Fusion 7ths/9ths (Jaco)", useJaco) { useJaco = it }
            StylistToggleRow("Ghost Note Pocket (Dart)", useDart) { useDart = it }
            StylistToggleRow("Phrase Motif Variation", useVariation) { useVariation = it }

            Spacer(Modifier.height(skin.paddingMedium))
            Text(text = "SELECT BASS STYLE", color = skin.textSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(4.dp))

            LazyColumn(
                modifier = Modifier.height(140.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(BassStyle.entries.toList()) { style ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(skin.cornerRadiusSmall))
                            .background(skin.panelAlt)
                            .border(1.dp, skin.border, RoundedCornerShape(skin.cornerRadiusSmall))
                            .clickable { onApply(style, useMotown, useJaco, useDart, useVariation); onDismiss() }
                            .padding(horizontal = skin.paddingMedium, vertical = 6.dp)
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
private fun StylistToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val skin = ReasonTouchTheme.skin
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = skin.textPrimary, fontSize = 12.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
