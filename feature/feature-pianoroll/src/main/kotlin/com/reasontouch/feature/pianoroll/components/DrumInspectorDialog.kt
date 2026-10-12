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

@Composable
fun DrumInspectorDialog(
    barIndex: Int,
    onApplyFill: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val skin = ReasonTouchTheme.skin

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

            Text(
                text = "Insert Drum Fill / Pattern for this bar",
                color = skin.textPrimary,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(skin.paddingLarge))

            val fillsList = DrumFillPatterns.FILLS.entries.toList()
            LazyColumn(
                modifier = Modifier.height(220.dp),
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
