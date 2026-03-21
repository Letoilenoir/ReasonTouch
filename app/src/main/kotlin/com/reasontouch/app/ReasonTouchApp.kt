package com.reasontouch.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@Composable
fun ReasonTouchApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""

    var bpm by remember { mutableStateOf(120) }
    var isPlaying by remember { mutableStateOf(false) }

    // Determine if we should show transport and bottom nav
    val showChrome = currentRoute.startsWith("chords/") ||
                     currentRoute.startsWith("piano_roll/")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        NavHost(
            navController = navController,
            startDestination = Screen.SessionList.route,
            modifier = Modifier.weight(1f)
        ) {
            // Session list � no transport bar
            composable(Screen.SessionList.route) {
                SessionListScreen(
                    onSessionSelected = { sessionId ->
                        navController.navigate(Screen.Chords.createRoute(sessionId))
                    }
                )
            }

            // Chords screen
            composable(Screen.Chords.route) { backStackEntry ->
                val sessionId = backStackEntry.arguments
                    ?.getString("sessionId") ?: return@composable
                com.reasontouch.feature.chords.ChordScreen(sessionId = sessionId)
            }

            // Piano Roll screen
            composable(Screen.PianoRoll.route) { backStackEntry ->
                val sessionId = backStackEntry.arguments
                    ?.getString("sessionId") ?: return@composable
                com.reasontouch.feature.pianoroll.PianoRollScreen(sessionId = sessionId)
            }
        }

        // Transport and bottom nav only show when inside a session
        if (showChrome) {
            TransportBar(
                bpm = bpm,
                isPlaying = isPlaying,
                onPlay = { isPlaying = !isPlaying },
                onStop = { isPlaying = false },
                onRewind = { isPlaying = false }
            )
            BottomNav(
                currentRoute = currentRoute,
                onChordsClick = {
                    val sessionId = navBackStackEntry?.arguments?.getString("sessionId")
                        ?: return@BottomNav
                    navController.navigate(Screen.Chords.createRoute(sessionId)) {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onPianoRollClick = {
                    val sessionId = navBackStackEntry?.arguments?.getString("sessionId")
                        ?: return@BottomNav
                    navController.navigate(Screen.PianoRoll.createRoute(sessionId)) {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

