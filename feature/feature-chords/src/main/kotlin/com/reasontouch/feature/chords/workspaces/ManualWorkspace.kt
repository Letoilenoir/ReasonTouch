package com.reasontouch.feature.chords.workspaces

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BG = Color(0xFF1A1A1E)
private val TEXT = Color(0xFFC8C8D4)
private val TEXT_DIM = Color(0xFF666675)
private val GREEN = Color(0xFF3DDC84)

/**
 * ManualWorkspace - Returns to the original ChordPanel behavior
 * User can freely select any chord and build their own progression
 * with no AI assistance or suggestions.
 * 
 * This delegates to ChordPanel which is already fully implemented
 * in the original ChordScreen.
 */
@Composable
fun ManualWorkspace(
    onChordSelected: (String) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(BG),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp)
    ) {
        item {
            ManualHeader()
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            Text(
                text = "Use the chord grid below to build your progression freely.",
                color = TEXT_DIM,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BG)
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Text(
                text = "NOTE: Manual workspace uses the full ChordPanel from below.",
                color = TEXT_DIM,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun ManualHeader() {
    Column {
        Text(
            text = "✏️  MANUAL COMPOSITION",
            color = GREEN,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "No suggestions. Complete creative freedom. Build exactly what you hear.",
            color = TEXT_DIM,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
