package com.reasontouch.feature.pianoroll.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.reasontouch.feature.drums.DrumFillPatterns
import com.reasontouch.feature.drums.DrumPresets

@Composable
fun DrumInspectorDialog(
    barIndex: Int,
    onApplyFill: (String) -> Unit,
    onApplyPreset: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val skin = ReasonTouchTheme.skin
    var tab by remember { mutableStateOf(0) } // 0 = Fills, 1 = Presets

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
                text = "DRUM INSPECTOR — BAR ${barIndex + 1}",
                color = skin.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(skin.paddingMedium))

            // Tab Toggle: Fills vs Presets
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(skin.panelAlt, RoundedCornerShape(skin.cornerRadiusSmall))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(skin.cornerRadiusSmall))
                        .background(if (tab == 0) skin.accent else Color.Transparent)
                        .clickable { tab = 0 }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "FILLS", color = if (tab == 0) Color.Black else skin.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(skin.cornerRadiusSmall))
                        .background(if (tab == 1) skin.accent else Color.Transparent)
                        .clickable { tab = 1 }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "PRESETS", color = if (tab == 1) Color.Black else skin.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(skin.paddingMedium))

            if (tab == 0) {
                Text(text = "Insert Drum Fill", color = skin.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                val fillsList = DrumFillPatterns.FILLS.entries.toList()
                LazyColumn(
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(count = fillsList.size) { index ->
                        val entry = fillsList[index]
                        val key = entry.key
                        val fill = entry.value
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(skin.cornerRadiusSmall))
                                .background(skin.panelAlt)
                                .border(1.dp, skin.border, RoundedCornerShape(skin.cornerRadiusSmall))
                                .clickable { onApplyFill(key); onDismiss() }
                                .padding(horizontal = skin.paddingMedium, vertical = 8.dp)
                        ) {
                            Column {
                                Text(text = fill.name, color = skin.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(text = fill.description, color = skin.textSecondary, fontSize = 10.sp)
                            }
                        }
                    }
                }
            } else {
                Text(text = "Apply Drum Preset Pattern", color = skin.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                val presetsList = DrumPresets.all.keys.toList()
                LazyColumn(
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(count = presetsList.size) { index ->
                        val presetName = presetsList[index]
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(skin.cornerRadiusSmall))
                                .background(skin.panelAlt)
                                .border(1.dp, skin.border, RoundedCornerShape(skin.cornerRadiusSmall))
                                .clickable { onApplyPreset(presetName); onDismiss() }
                                .padding(horizontal = skin.paddingMedium, vertical = 10.dp)
                        ) {
                            Text(text = presetName, color = skin.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(skin.paddingLarge))
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
