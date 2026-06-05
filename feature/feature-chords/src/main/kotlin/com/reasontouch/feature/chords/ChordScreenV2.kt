package com.reasontouch.feature.chords

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.reasontouch.feature.chords.components.CompositionModeBar
import com.reasontouch.feature.chords.workspaces.*

@Composable
fun ChordScreenV2(
    viewModel: ChordViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsState()
    
    val compositionMode = remember { mutableStateOf(CompositionMode.ASSISTED) }
    val selectedStartingPoint = remember { mutableStateOf<StartingPoint?>(null) }

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
                ManualWorkspace()
            }

            CompositionMode.ASSISTED -> {
                if (selectedStartingPoint.value == null) {
                    StartingPointSelector(
                        onStartingPointSelected = { startingPoint ->
                            selectedStartingPoint.value = startingPoint
                        }
                    )
                } else {
                    when (selectedStartingPoint.value?.name) {
                        "Mood" -> MoodWorkspace(
                            onMoodSelected = { mood -> },
                            onContinue = { }
                        )
                        "Inspire" -> InspireWorkspace(
                            onGenerateProgression = { }
                        )
                        "Progression" -> ProgressionWorkspace(
                            onProgressionSelected = { template -> }
                        )
                        else -> ManualWorkspace()
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
