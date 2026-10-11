package com.reasontouch.feature.chords.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.reasontouch.core.data.ChordEvent
import com.reasontouch.core.ui.theme.ReasonTouchTheme
import com.reasontouch.feature.chords.*

@Composable
fun ChordInspectorDialog(
    barIndex: Int,
    currentChord: ChordEvent?,
    key: KeyCandidate?,
    onSubstitute: (TheoryChord) -> Unit,
    onDismiss: () -> Unit
) {
    val skin = ReasonTouchTheme.skin
    var selectedRoot by remember { mutableStateOf(key?.root ?: NoteClass.C) }
    var selectedQuality by remember { mutableStateOf(ChordQuality.MAJ) }

    val suggestions = remember(currentChord, key) {
        val root = key?.root ?: NoteClass.C
        val isMinor = key?.isMinor ?: false
        MusicTheory.diatonicChords(root, isMinor).take(4)
    }

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
                text = "CHORD INSPECTOR — BAR ${barIndex + 1}",
                color = skin.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(skin.paddingMedium))

            Text(
                text = "Current: ${currentChord?.chordName ?: "None"}",
                color = skin.textPrimary,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(skin.paddingLarge))

            Text(
                text = "SUGGESTED SUBSTITUTIONS",
                color = skin.textSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier.height(130.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(suggestions) { chord ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(skin.cornerRadiusSmall))
                            .background(skin.panelAlt)
                            .border(1.dp, skin.border, RoundedCornerShape(skin.cornerRadiusSmall))
                            .clickable { onSubstitute(chord); onDismiss() }
                            .padding(horizontal = skin.paddingMedium, vertical = 8.dp)
                    ) {
                        Text(
                            text = chord.label,
                            color = skin.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(skin.paddingLarge))
            Text(
                text = "MANUAL PICKER",
                color = skin.textSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(6.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(NoteClass.entries.toList()) { note ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                if (selectedRoot == note) skin.accent else skin.panelAlt,
                                RoundedCornerShape(4.dp)
                            )
                            .clickable { selectedRoot = note },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = note.label, color = if (selectedRoot == note) Color.Black else skin.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(ChordQuality.entries.toList()) { quality ->
                    Box(
                        modifier = Modifier
                            .padding(vertical = 2.dp)
                            .background(
                                if (selectedQuality == quality) skin.accent else skin.panelAlt,
                                RoundedCornerShape(4.dp)
                            )
                            .clickable { selectedQuality = quality }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = quality.label, color = if (selectedQuality == quality) Color.Black else skin.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(skin.paddingLarge))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(skin.paddingMedium)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(skin.panelAlt, RoundedCornerShape(skin.cornerRadiusSmall))
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "CANCEL", color = skin.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(skin.accent, RoundedCornerShape(skin.cornerRadiusSmall))
                        .clickable {
                            onSubstitute(TheoryChord(selectedRoot, selectedQuality))
                            onDismiss()
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "APPLY", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
