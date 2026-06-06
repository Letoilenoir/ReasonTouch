package com.reasontouch.feature.chords.workspaces

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reasontouch.feature.chords.ChordViewModel

private val BG = Color(0xFF1A1A1E)
private val TEXT = Color(0xFFC8C8D4)
private val TEXT_DIM = Color(0xFF666675)
private val GREEN = Color(0xFF3DDC84)

/**
 * ManualWorkspace - Full chord grid composition with no AI assistance
 * Integrates ChordPanel from original ChordScreen for complete freedom
 */
@Composable
fun ManualWorkspace(
    viewModel: ChordViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(BG)
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
                    .padding(horizontal = 12.dp)
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            // TODO: Wire ChordPanel from original ChordScreen here
            Text(
                text = "[ChordPanel to be integrated]",
                color = TEXT_DIM,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(12.dp)
            )
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun ManualHeader() {
    Column(modifier = Modifier.padding(12.dp)) {
        Text(
            text = "??  MANUAL COMPOSITION",
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
