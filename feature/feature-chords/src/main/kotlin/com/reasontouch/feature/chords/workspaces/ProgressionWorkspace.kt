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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
private val GOLD = Color(0xFFF5C518)

data class ProgressionTemplate(
    val name: String,
    val chords: List<String>,
    val style: String,
    val mood: String,
    val difficulty: String
)

val PROGRESSION_TEMPLATES = listOf(
    ProgressionTemplate(
        "I-vi-IV-V",
        listOf("Cmaj", "Am", "F", "G"),
        "Pop",
        "Balanced",
        "Beginner"
    ),
    ProgressionTemplate(
        "Jazz Turnaround",
        listOf("Cmaj7", "Bm7b5", "E7", "Am7"),
        "Jazz",
        "Sophisticated",
        "Advanced"
    ),
    ProgressionTemplate(
        "Blues 12-Bar",
        listOf("C7", "F7", "C7", "G7"),
        "Blues",
        "Gritty",
        "Intermediate"
    ),
    ProgressionTemplate(
        "Minor Odyssey",
        listOf("Am", "F", "C", "G"),
        "Dark",
        "Melancholic",
        "Beginner"
    ),
    ProgressionTemplate(
        "Modal Vamp",
        listOf("Dm7", "G", "Dm7", "G"),
        "Contemporary",
        "Contemplative",
        "Intermediate"
    ),
    ProgressionTemplate(
        "Diminished Tension",
        listOf("Cmaj7", "Bdim7", "Cmaj7", "Bdim7"),
        "Modern",
        "Dramatic",
        "Advanced"
    )
)

@Composable
fun ProgressionWorkspace(
    onProgressionSelected: (ProgressionTemplate) -> Unit,
    onContinue: () -> Unit = {}
) {
    var selectedProgression by remember { mutableStateOf<ProgressionTemplate?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(BG)
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ProgressionHeader()
        }

        item {
            ProgressionDescription()
        }

        item {
            Text(
                text = "CHOOSE A TEMPLATE",
                color = TEXT_DIM,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        items(PROGRESSION_TEMPLATES) { template ->
            ProgressionTemplateCard(
                template = template,
                isSelected = selectedProgression == template,
                onSelect = {
                    selectedProgression = template
                    onProgressionSelected(template)
                }
            )
        }

        if (selectedProgression != null) {
            item {
                ProgressionAcceptBar(
                    template = selectedProgression!!,
                    onContinue = onContinue
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun ProgressionHeader() {
    Column {
        Text(
            text = "🎵  PROGRESSION TEMPLATES",
            color = GOLD,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Start with a classic progression. Great for learning harmonic structure.",
            color = TEXT_DIM,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun ProgressionDescription() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1A1A28))
            .border(1.dp, GOLD.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "How it works:",
            color = GOLD,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "1. Select a progression template\n2. Customize voicings and patterns\n3. Extend or simplify as needed\n4. Learn the harmonic principles",
            color = TEXT_DIM,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 14.sp
        )
    }
}

@Composable
fun ProgressionTemplateCard(
    template: ProgressionTemplate,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val bgColor = if (isSelected) Color(0xFF2A2A1A) else PANEL
    val borderColor = if (isSelected) GOLD else BORDER

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .clickable(onClick = onSelect)
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.name,
                        color = if (isSelected) GOLD else TEXT,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = template.style,
                        color = TEXT_DIM,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF1A1A1E))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = template.difficulty,
                        color = if (template.difficulty == "Advanced") Color(0xFFE84040)
                        else if (template.difficulty == "Intermediate") GOLD
                        else Color(0xFF3DDC84),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                template.chords.forEach { chord ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF1A1A1E))
                            .border(1.dp, BORDER, RoundedCornerShape(3.dp))
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chord,
                            color = GOLD,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Text(
                text = "Mood: ${template.mood}",
                color = TEXT_DIM,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun ProgressionAcceptBar(
    template: ProgressionTemplate,
    onContinue: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1A2A1A))
            .border(1.dp, Color(0xFF3DDC84).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .clickable(onClick = onContinue)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "✓ START WITH THIS PROGRESSION",
            color = Color(0xFF3DDC84),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
