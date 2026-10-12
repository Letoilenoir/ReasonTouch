package com.reasontouch.feature.chords.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
fun PadInspectorDialog(
    barIndex: Int,
    currentChord: ChordEvent?,
    onApplyVoicing: (TheoryChord) -> Unit,
    onDismiss: () -> Unit
) {
    val skin = ReasonTouchTheme.skin
    val baseChord = currentChord?.let { MusicTheory.parseChordName(it.chordName) } ?: TheoryChord(NoteClass.C, ChordQuality.MAJ)

    val alternatives = listOf(
        TheoryChord(baseChord.root, ChordQuality.MAJ),
        TheoryChord(baseChord.root, ChordQuality.MIN),
        TheoryChord(baseChord.root, ChordQuality.MAJ7),
        TheoryChord(baseChord.root, ChordQuality.MIN7),
        TheoryChord(baseChord.root, ChordQuality.DOM7),
        TheoryChord(baseChord.root, ChordQuality.SUS4),
        TheoryChord(baseChord.root, ChordQuality.SUS2)
    )

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
                text = "PAD INSPECTOR — BAR ${barIndex + 1}",
                color = skin.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(skin.paddingMedium))

            Text(
                text = "Current Chord: ${currentChord?.chordName ?: "None"}",
                color = skin.textPrimary,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(skin.paddingLarge))

            Text(
                text = "ALTERNATIVE TRIADS & TETRADS",
                color = skin.textSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier.height(200.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(alternatives) { voicing ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(skin.cornerRadiusSmall))
                            .background(skin.panelAlt)
                            .border(1.dp, skin.border, RoundedCornerShape(skin.cornerRadiusSmall))
                            .clickable { onApplyVoicing(voicing); onDismiss() }
                            .padding(horizontal = skin.paddingMedium, vertical = 8.dp)
                    ) {
                        Column {
                            Text(text = voicing.label, color = skin.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(text = "MIDI Notes: ${voicing.midiNotes.joinToString(", ")}", color = skin.textSecondary, fontSize = 10.sp)
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
