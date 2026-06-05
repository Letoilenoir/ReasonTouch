package com.reasontouch.feature.chords.workspaces

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BG = Color(0xFF1A1A1E)
private val PANEL = Color(0xFF2A2A32)
private val BORDER = Color(0xFF3A3A45)
private val ACCENT = Color(0xFFE84040)
private val TEXT = Color(0xFFC8C8D4)
private val TEXT_DIM = Color(0xFF666675)
private val BLUE = Color(0xFF38BDF8)

@Composable
fun InspireWorkspace(
    onGenerateProgression: () -> Unit,
    onContinue: () -> Unit = {}
) {
    var isGenerating by remember { mutableStateOf(false) }
    var suggestedProgression by remember { mutableStateOf<List<String>?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(BG)
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            InspireHeader()
        }

        item {
            InspireDescription()
        }

        item {
            GenerateButton(
                isGenerating = isGenerating,
                onClick = {
                    isGenerating = true
                    onGenerateProgression()
                    // Simulate generation delay
                    suggestedProgression = listOf("Cmaj7", "Am7", "Dmaj7", "G7")
                    isGenerating = false
                }
            )
        }

        if (isGenerating) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = BLUE,
                            strokeWidth = 2.dp
                        )
                        Text(
                            "Generating inspiration...",
                            color = TEXT_DIM,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        if (suggestedProgression != null && !isGenerating) {
            item {
                ProgressionPreview(
                    progression = suggestedProgression!!,
                    onAccept = onContinue,
                    onRegenerate = {
                        isGenerating = true
                        suggestedProgression = null
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun InspireHeader() {
    Column {
        Text(
            text = "✨  INSPIRATION MODE",
            color = BLUE,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Let the system suggest a complete progression. Perfect for creative block.",
            color = TEXT_DIM,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun InspireDescription() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1A1A28))
            .border(1.dp, BLUE.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "How it works:",
            color = BLUE,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "1. Generate a random progression\n2. Review the suggested chords\n3. Accept and start composing\n4. Modify or regenerate anytime",
            color = TEXT_DIM,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 14.sp
        )
    }
}

@Composable
fun GenerateButton(
    isGenerating: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isGenerating) BLUE.copy(alpha = 0.5f) else BLUE)
            .clickable(enabled = !isGenerating, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isGenerating) "GENERATING..." else "GENERATE PROGRESSION",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun ProgressionPreview(
    progression: List<String>,
    onAccept: () -> Unit,
    onRegenerate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(PANEL)
            .border(1.dp, BORDER, RoundedCornerShape(6.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "SUGGESTED PROGRESSION",
            color = TEXT_DIM,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF1A1A1E))
                .border(1.dp, BORDER, RoundedCornerShape(4.dp))
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            progression.forEachIndexed { index, chord ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF2A2A32))
                        .border(1.dp, BLUE.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${index + 1}",
                            color = TEXT_DIM,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = chord,
                            color = BLUE,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1A2A3A))
                    .border(1.dp, BLUE, RoundedCornerShape(4.dp))
                    .clickable(onClick = onRegenerate)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "REGENERATE",
                    color = BLUE,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF3DDC84))
                    .clickable(onClick = onAccept)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "ACCEPT",
                    color = Color.Black,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
