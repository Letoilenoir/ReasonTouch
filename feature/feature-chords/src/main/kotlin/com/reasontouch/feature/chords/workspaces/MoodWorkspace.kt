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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
private val PURPLE = Color(0xFFA78BFA)
private val GOLD = Color(0xFFF5C518)

data class Mood(
    val name: String,
    val description: String,
    val icon: String,
    val moodBias: Float  // -1.0 (dark/minor) to +1.0 (bright/major)
)

val AVAILABLE_MOODS = listOf(
    Mood("Dark", "Minor keys, melancholic", "🌙", -0.8f),
    Mood("Uplifting", "Major keys, hopeful", "☀️", 0.8f),
    Mood("Cinematic", "Epic, dramatic", "🎬", 0.3f),
    Mood("Ambient", "Atmospheric, sparse", "☁️", 0.0f),
    Mood("Energetic", "Fast, driving", "⚡", 0.6f),
    Mood("Melancholic", "Contemplative", "💔", -0.6f)
)

@Composable
fun MoodWorkspace(
    onMoodSelected: (Mood) -> Unit,
    onAudition: (String) -> Unit = {},
    onContinue: () -> Unit = {}
) {
    var selectedMood by remember { mutableStateOf<Mood?>(null) }
    var moodBiasFine by remember { mutableStateOf(0f) }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(BG)
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            MoodHeader()
        }

        item {
            MoodDescription()
        }

        item {
            Text(
                text = "SELECT A MOOD",
                color = TEXT_DIM,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(AVAILABLE_MOODS.chunked(2)) { moodRow ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                moodRow.forEach { mood ->
                    MoodCard(
                        mood = mood,
                        isSelected = selectedMood == mood,
                        onSelect = {
                            selectedMood = mood
                            moodBiasFine = mood.moodBias
                            onMoodSelected(mood)
                        },
                        onAudition = onAudition,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (moodRow.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (selectedMood != null) {
            item {
                MoodBiasTuner(
                    mood = selectedMood!!,
                    biasFine = moodBiasFine,
                    onBiasChange = { moodBiasFine = it }
                )
            }

            item {
                MoodNextSteps(
                    mood = selectedMood!!,
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
fun MoodHeader() {
    Column {
        Text(
            text = "🎵  MOOD-DRIVEN COMPOSITION",
            color = ACCENT,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Start with a feeling. The harmony engine will suggest compatible chords.",
            color = TEXT_DIM,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun MoodDescription() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1A1A28))
            .border(1.dp, PURPLE.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "How it works:",
            color = PURPLE,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "1. Choose a mood\n2. Refine the exact harmonic character\n3. Build your progression with AI-suggested chords\n4. Switch modes anytime",
            color = TEXT_DIM,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 14.sp
        )
    }
}

@Composable
fun MoodCard(
    mood: Mood,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onAudition: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) Color(0xFF2A1A2A) else PANEL
    val borderColor = if (isSelected) PURPLE else BORDER

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable {
                onAudition(mood.name)
                onSelect()
            }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = mood.icon,
                fontSize = 28.sp
            )
            Text(
                text = mood.name,
                color = if (isSelected) PURPLE else TEXT,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = mood.description,
                color = TEXT_DIM,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun MoodBiasTuner(
    mood: Mood,
    biasFine: Float,
    onBiasChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(PANEL)
            .border(1.dp, BORDER, RoundedCornerShape(6.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "HARMONIC CHARACTER",
            color = TEXT_DIM,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Dark", color = PURPLE, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Slider(
                value = biasFine,
                onValueChange = onBiasChange,
                valueRange = -1f..1f,
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp),
                colors = SliderDefaults.colors(
                    thumbColor = if (biasFine > 0) GOLD else PURPLE,
                    activeTrackColor = if (biasFine > 0) GOLD.copy(alpha = 0.6f) else PURPLE.copy(
                        alpha = 0.6f
                    )
                )
            )
            Text("Bright", color = GOLD, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = when {
                biasFine > 0.5f -> "Strongly major - very bright and hopeful"
                biasFine > 0.1f -> "Leaning major - predominantly bright"
                biasFine < -0.5f -> "Strongly minor - very dark and introspective"
                biasFine < -0.1f -> "Leaning minor - predominantly dark"
                else -> "Neutral - balanced major and minor possibilities"
            },
            color = TEXT_DIM,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun MoodNextSteps(
    mood: Mood,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1A2A1A))
            .border(1.dp, Color(0xFF3DDC84).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "✓ Ready to compose",
            color = Color(0xFF3DDC84),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "Your progression will suggest compatible chords based on '${mood.name}' in Assisted mode. You can override suggestions at any time.",
            color = TEXT_DIM,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 12.sp
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF3DDC84))
                .clickable(onClick = onContinue)
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "START COMPOSING",
                color = BG,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}