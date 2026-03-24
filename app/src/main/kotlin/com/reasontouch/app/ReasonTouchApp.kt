package com.reasontouch.app

import android.app.Application
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.reasontouch.core.data.SessionRepository
import com.reasontouch.feature.export.ExportDialog
import com.reasontouch.feature.export.ExportViewModel

@Composable
fun ReasonTouchApp(repository: SessionRepository) {
    val navController     = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute      = navBackStackEntry?.destination?.route ?: ""
    val application       = LocalContext.current.applicationContext as Application

    var bpm        by remember { mutableStateOf(120) }
    var isPlaying  by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }

    val showChrome       = currentRoute.startsWith("chords/") || currentRoute.startsWith("piano_roll/")
    val currentSessionId = navBackStackEntry?.arguments?.getString("sessionId") ?: ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        NavHost(
            navController    = navController,
            startDestination = Screen.SessionList.route,
            modifier         = Modifier.weight(1f)
        ) {
            composable(Screen.SessionList.route) {
                SessionListScreen(
                    onSessionSelected = { sessionId ->
                        navController.navigate(Screen.Chords.createRoute(sessionId))
                    }
                )
            }
            composable(Screen.Chords.route) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
                com.reasontouch.feature.chords.ChordScreen(sessionId = sessionId)
            }
            composable(Screen.PianoRoll.route) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
                com.reasontouch.feature.pianoroll.PianoRollScreen(sessionId = sessionId)
            }
        }

        if (showChrome) {
            TransportBar(
                bpm      = bpm,
                isPlaying = isPlaying,
                onPlay   = { isPlaying = !isPlaying },
                onStop   = { isPlaying = false },
                onRewind = { isPlaying = false },
                onExport = { showExport = true }
            )
            BottomNav(
                currentRoute     = currentRoute,
                onChordsClick    = {
                    val sessionId = navBackStackEntry?.arguments?.getString("sessionId") ?: return@BottomNav
                    navController.navigate(Screen.Chords.createRoute(sessionId)) {
                        launchSingleTop = true; restoreState = true
                    }
                },
                onPianoRollClick = {
                    val sessionId = navBackStackEntry?.arguments?.getString("sessionId") ?: return@BottomNav
                    navController.navigate(Screen.PianoRoll.createRoute(sessionId)) {
                        launchSingleTop = true; restoreState = true
                    }
                }
            )
        }
    }

    if (showExport && currentSessionId.isNotEmpty()) {
        val exportVm: ExportViewModel = viewModel(
            key     = "export_$currentSessionId",
            factory = ExportViewModel.Factory(
                sessionId   = currentSessionId,
                repository  = repository,
                application = application
            )
        )
        ExportDialog(
            sessionId   = currentSessionId,
            sessionName = "Session",
            onDismiss   = { showExport = false },
            viewModel   = exportVm
        )
    }
}