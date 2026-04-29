package com.reasontouch.app

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.reasontouch.core.data.SessionRepository
import com.reasontouch.feature.export.ExportDialog
import com.reasontouch.feature.export.ExportViewModel

private val RACK   = Color(0xFF222228)
private val PANEL  = Color(0xFF2A2A32)
private val BORDER = Color(0xFF3A3A45)
private val ACCENT = Color(0xFFE84040)
private val GREEN  = Color(0xFF3DDC84)
private val TEXT_DIM = Color(0xFF666675)

@Composable
fun ReasonTouchApp(repository: SessionRepository) {
    val navController     = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute      = navBackStackEntry?.destination?.route ?: ""
    val application       = LocalContext.current.applicationContext as Application

    var showExport by remember { mutableStateOf(false) }

    val showChrome       = currentRoute.startsWith("chords/") ||
                           currentRoute.startsWith("piano_roll/") ||
                           currentRoute.startsWith("drums/")
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
                val sessionId = backStackEntry.arguments
                    ?.getString("sessionId") ?: return@composable
                com.reasontouch.feature.chords.ChordScreen(sessionId = sessionId)
            }
            composable(Screen.PianoRoll.route) { backStackEntry ->
                val sessionId = backStackEntry.arguments
                    ?.getString("sessionId") ?: return@composable
                com.reasontouch.feature.pianoroll.PianoRollScreen(sessionId = sessionId)
            }
            composable(Screen.Drums.route) { backStackEntry ->
                val sessionId = backStackEntry.arguments
                    ?.getString("sessionId") ?: return@composable
                com.reasontouch.feature.drums.DrumScreen(
                    sessionId        = sessionId,
                    onNavigateToRoll = {
                        navController.navigate(Screen.PianoRoll.createRoute(sessionId)) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.SessionSettings.route) {
                SessionSettingsScreen(onBack = { navController.popBackStack() })
            }
        }

        if (showChrome) {
            // Chrome bar
            Row(
                modifier = Modifier
                    .fillMaxWidth().height(40.dp)
                    .background(RACK).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(PANEL)
                        .border(1.dp, BORDER, RoundedCornerShape(4.dp))
                        .clickable {
                            if (currentSessionId.isNotEmpty()) {
                                navController.navigate(
                                    Screen.SessionSettings.createRoute(currentSessionId)
                                )
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("\u2699 SESSION", color = TEXT_DIM, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Box(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF1A3A2A))
                        .border(1.dp, GREEN, RoundedCornerShape(4.dp))
                        .clickable { showExport = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("\u2193 MIDI", color = GREEN, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BORDER))

            BottomNav(
                currentRoute     = currentRoute,
                onChordsClick    = {
                    val sid = navBackStackEntry
                        ?.arguments?.getString("sessionId") ?: return@BottomNav
                    navController.navigate(Screen.Chords.createRoute(sid)) {
                        launchSingleTop = true; restoreState = true
                    }
                },
                onPianoRollClick = {
                    val sid = navBackStackEntry
                        ?.arguments?.getString("sessionId") ?: return@BottomNav
                    navController.navigate(Screen.PianoRoll.createRoute(sid)) {
                        launchSingleTop = true; restoreState = true
                    }
                },
                onDrumsClick     = {
                    val sid = navBackStackEntry
                        ?.arguments?.getString("sessionId") ?: return@BottomNav
                    navController.navigate(Screen.Drums.createRoute(sid)) {
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