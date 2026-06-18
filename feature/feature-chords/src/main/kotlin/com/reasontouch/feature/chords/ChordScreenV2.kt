package com.reasontouch.feature.chords

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.reasontouch.feature.chords.components.CompositionModeBar
import com.reasontouch.feature.chords.workspaces.*
import androidx.compose.foundation.layout.Box

@Composable
fun ChordScreenV2(
    viewModel: ChordViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsState()

    val compositionMode = remember { mutableStateOf(CompositionMode.ASSISTED) }
    val selectedStartingPoint = remember { mutableStateOf<StartingPoint?>(null) }
    var showAssistedDescriptor by remember { mutableStateOf(true) }  // ← ADD

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1a1a1a))
    ) {
        // COMPOSITION MODE BAR
        CompositionModeBar(
            mode = compositionMode.value,
            onModeSelected = { newMode ->
                compositionMode.value = newMode
                selectedStartingPoint.value = null
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // WORKSPACE - Use Column for simple layouts, LazyColumn only for scrollable content
        when (compositionMode.value) {
            CompositionMode.MANUAL -> {
                ManualWorkspace(viewModel = viewModel)
            }

            CompositionMode.ASSISTED -> {
                if (selectedStartingPoint.value == null) {
                    if (showAssistedDescriptor) {
                        AssistedModeHeader(onDismiss = { showAssistedDescriptor = false })
                        Spacer(modifier = Modifier.height(12.dp))
                    } else {
                        // Show a clickable button to reveal header again
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF2A2A32))
                                .border(1.dp, Color(0xFF3A3A45), RoundedCornerShape(4.dp))
                                .clickable { showAssistedDescriptor = true }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "ℹ SHOW ASSISTED INFO",
                                color = Color(0xFF666675),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    StartingPointSelector(
                        onStartingPointSelected = { startingPoint ->
                            selectedStartingPoint.value = startingPoint
                        }
                    )
                } else {
                    when (selectedStartingPoint.value?.name) {
                        "Mood" -> MoodWorkspace(
                            viewModel = viewModel,
                            onMoodSelected = { mood -> },
                            onAudition = { moodName ->
                                val chordForMood = when(moodName) {
                                    "Dark" -> "Am"
                                    "Uplifting" -> "Cmaj7"
                                    "Cinematic" -> "Gmaj7"
                                    "Ambient" -> "Gmaj7"
                                    "Energetic" -> "G7"
                                    "Melancholic" -> "Em7"
                                    else -> "C"
                                }
                                viewModel.auditionChord(chordForMood, "Open")
                            },
                            onContinue = { }
                        )
                        "Inspire" -> InspireWorkspace(
                            viewModel = viewModel,
                            onGenerateProgression = { },
                            onContinue = { }
                        )
                        "Progression" -> ProgressionWorkspace(
                            viewModel = viewModel,
                            onProgressionSelected = { template -> },
                            onContinue = { }
                        )
                    }
                }
            }

            CompositionMode.GUIDED -> {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Guided Workspace (TBD)", color = Color(0xFFE84040), fontSize = 16.sp)
                }
            }
        }
    }
}
@Composable
fun AssistedModeHeader(onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1E))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "🤖  ASSISTED MODE",
                color = Color(0xFFE84040),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF3A3A45))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    "✕ HIDE",
                    color = Color(0xFF666675),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Text(
            "Music theory available as suggestions. You're in control.",
            color = Color(0xFF666675),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF16161A))
                .border(1.dp, Color(0xFFE84040).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                .padding(12.dp)
        ) {
            Text(
                "• Accept suggestions to learn harmonic progression\n• Reject suggestions to explore unconventional ideas\n• Switch to Manual anytime for complete freedom",
                color = Color(0xFF666675),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 14.sp
            )
        }
    }
}