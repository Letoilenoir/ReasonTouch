package com.reasontouch.feature.chords

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.reasontouch.core.ui.theme.ReasonTouchTheme
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import com.reasontouch.feature.chords.components.SelectorChip

@Composable
fun MoodWorkspace() {
    var selectedMoodCategory by remember {
        mutableStateOf("DARK")
    }

    val skin = ReasonTouchTheme.skin

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = "Select the emotional direction for your progression",
            color = skin.textSecondary,
            fontFamily = FontFamily.Monospace
        )

        MoodCategorySection(
            selectedMoodCategory = selectedMoodCategory,
            onMoodSelected = {
                selectedMoodCategory = it
            }
        )

        MoodPresetSection()

        MoodPreviewSection()

        GenerateMoodButton()
    }
}

@Composable
fun MoodCategorySection(
    selectedMoodCategory: String,
    onMoodSelected: (String) -> Unit
) {

    SectionCard(
        title = "MOOD CATEGORY"
    ) {

        MoodCategoryChips(
            selected = selectedMoodCategory,
            onSelected = onMoodSelected
        )
    }
}
@Composable
fun MoodPresetSection() {

    SectionCard(
        title = "PRESETS"
    ) {

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            MoodPresetTile(
                title = "MELANCHOLIC PIANO",
                subtitle = "Minor reflective harmony"
            )

            MoodPresetTile(
                title = "EPIC TRAILER",
                subtitle = "Large cinematic movement"
            )

            MoodPresetTile(
                title = "DREAM SEQUENCE",
                subtitle = "Suspended atmospheric chords"
            )

            MoodPresetTile(
                title = "LATE NIGHT JAZZ",
                subtitle = "Extended jazz voicings"
            )
        }
    }
}@Composable
fun MoodPreviewSection() {

    SectionCard(
        title = "PREVIEW"
    ) {

        PlaceholderPanel(
            text = "Chord progression preview"
        )
    }
}
@Composable
fun GenerateMoodButton() {

    SectionCard(
        title = "GENERATE"
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFA78BFA))
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = "GENERATE PROGRESSION",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
@Composable
fun MoodPresetTile(
    title: String,
    subtitle: String
) {

    val skin = ReasonTouchTheme.skin

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(skin.panelAlt)
            .padding(14.dp)
    ) {

        Column {

            Text(
                text = title,
                color = skin.textPrimary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = subtitle,
                color = skin.textMuted,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
@Composable
fun MoodCategoryChips(
    selected: String,
    onSelected: (String) -> Unit
) {

    val skin = ReasonTouchTheme.skin

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        listOf(
            "DARK",
            "UPLIFTING",
            "CINEMATIC",
            "AMBIENT"
        ).forEach { mood ->

            val isSelected = mood == selected

            SelectorChip(
                text = mood,
                selected = isSelected,
                onClick = {
                    onSelected(mood)
                }
            )
        }
    }
}