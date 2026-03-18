package com.reasontouch.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding

// Temporary session ID until session management is built
private const val TEMP_SESSION_ID = "temp_session"

@Composable
fun ReasonTouchApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "chords/$TEMP_SESSION_ID"

    var bpm by remember { mutableStateOf(120) }
    var isPlaying by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        // Main content area
        NavHost(
            navController = navController,
            startDestination = "chords/$TEMP_SESSION_ID",
            modifier = Modifier.weight(1f)
        ) {
            composable("chords/{sessionId}") { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString("sessionId") ?: TEMP_SESSION_ID
                ChordsPlaceholderScreen(sessionId = sessionId)
            }
            composable("piano_roll/{sessionId}") { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString("sessionId") ?: TEMP_SESSION_ID
                PianoRollPlaceholderScreen(sessionId = sessionId)
            }
        }

        // Persistent transport bar
        TransportBar(
            bpm = bpm,
            isPlaying = isPlaying,
            onPlay = { isPlaying = !isPlaying },
            onStop = { isPlaying = false },
            onRewind = { isPlaying = false }
        )

        // Bottom navigation
        BottomNav(
            currentRoute = currentRoute,
            onChordsClick = {
                navController.navigate("chords/$TEMP_SESSION_ID") {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            onPianoRollClick = {
                navController.navigate("piano_roll/$TEMP_SESSION_ID") {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }
}
