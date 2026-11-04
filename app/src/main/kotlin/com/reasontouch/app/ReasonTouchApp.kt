package com.reasontouch.app

import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import com.reasontouch.core.playback.GridSubdivision
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import com.reasontouch.feature.pianoroll.TransportControls
import com.reasontouch.core.playback.PlaybackViewModel
import com.reasontouch.core.ui.theme.ReasonTouchTheme
import com.reasontouch.feature.chords.ChordViewModel


private val RACK   = Color(0xFF222228)
private val PANEL  = Color(0xFF2A2A32)
private val BORDER = Color(0xFF3A3A45)
private val TEXT_DIM = Color(0xFF666675)
private val GREEN  = Color(0xFF3DDC84)

@Composable
fun ReasonTouchApp(repository: SessionRepository) {
    ReasonTouchTheme {

        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route ?: ""
        val application = LocalContext.current.applicationContext as Application

        var showExport by remember { mutableStateOf(false) }

        val showChrome =
            currentRoute.startsWith("chords/") ||
                    currentRoute.startsWith("piano_roll/") ||
                    currentRoute.startsWith("drums/")

        val currentSessionId =
            navBackStackEntry?.arguments?.getString("sessionId") ?: ""
        val currentSession by remember(currentSessionId) {
            repository.getSession(currentSessionId)
        }.collectAsState(initial = null)
        var onBackAction by remember { mutableStateOf<(() -> Unit)?>(null) }

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

                composable(Screen.SessionList.route) {

                    SessionListScreen(
                        onSessionSelected = { sessionId ->

                            navController.navigate(
                                Screen.Chords.createRoute(sessionId)
                            )
                        }
                    )
                }

                composable(Screen.Chords.route) { backStackEntry ->

                    val sessionId =
                        backStackEntry.arguments?.getString("sessionId")
                            ?: return@composable

                    val chordVm: ChordViewModel = hiltViewModel()

                    com.reasontouch.feature.chords.ChordScreenV2(
                        viewModel = chordVm,
                        onSetBackAction = { action -> onBackAction = action }
                    )
                }

                composable(Screen.PianoRoll.route) { backStackEntry ->

                    val sessionId =
                        backStackEntry.arguments?.getString("sessionId")
                            ?: return@composable

                    com.reasontouch.feature.pianoroll.PianoRollScreen(
                        sessionId = sessionId,
                        onSetBackAction = { action -> onBackAction = action },
                        onNavigateToChords = {
                            navController.navigate(Screen.Chords.createRoute(sessionId))
                        }
                    )
                }

                composable(Screen.Drums.route) { backStackEntry ->

                    val sessionId =
                        backStackEntry.arguments?.getString("sessionId")
                            ?: return@composable

                    com.reasontouch.feature.drums.DrumScreen(
                        sessionId = sessionId,

                        onNavigateToRoll = {

                            navController.navigate(
                                Screen.PianoRoll.createRoute(sessionId)
                            ) {
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable(Screen.SessionSettings.route) {

                    SessionSettingsScreen(
                        onBack = {
                            navController.popBackStack()
                        },

                        onExportMidi = {
                            showExport = true
                        }
                    )
                }
            }

            if (showChrome) {
                val playbackVm: PlaybackViewModel = hiltViewModel()
                val transport = playbackVm.transport.collectAsState()
                val gridSubdivision by playbackVm.gridSubdivision.collectAsState()
                val isPianoRoll = currentRoute.startsWith("piano_roll/")
                val isDrums = currentRoute.startsWith("drums/")
                val bottomScrollState = rememberScrollState()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(RACK)
                        .horizontalScroll(bottomScrollState)
                        .padding(horizontal = 12.dp),

                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {

                    // HOME BUTTON
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(PANEL)
                            .border(1.dp, BORDER, RoundedCornerShape(4.dp))
                            .clickable {
                                navController.navigate(Screen.SessionList.route) {
                                    popUpTo(Screen.SessionList.route) { inclusive = true }
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "HOME",
                            color = TEXT_DIM,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // SESSION BUTTON
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(PANEL)
                            .border(
                                1.dp,
                                BORDER,
                                RoundedCornerShape(4.dp)
                            )
                            .clickable {
                                if (currentSessionId.isNotEmpty()) {
                                    navController.navigate(
                                        Screen.SessionSettings.createRoute(
                                            currentSessionId
                                        )
                                    )
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = "\u2699 ${currentSession?.name?.uppercase() ?: "SESSION"}",
                            color = TEXT_DIM,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 90.dp)
                        )
                    }

                    // MIDI EXPORT BUTTON
                    if (currentSessionId.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .height(28.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF1A3A2A))
                                .border(1.dp, GREEN, RoundedCornerShape(4.dp))
                                .clickable { showExport = true }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "↓ MIDI",
                                color = GREEN,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // GLOBAL TRANSPORT
                    TransportControls(
                        isPlaying = transport.value.isPlaying,
                        onPlay = { playbackVm.play(currentSessionId) },
                        onStop = playbackVm::stop,
                        onRewind = playbackVm::rewind,
                        onFastForward = playbackVm::fastForward,
                        onSkipToStart = playbackVm::skipToStart,
                        onSkipToEnd = playbackVm::skipToEnd
                    )

                    // CONTEXTUAL CONTROLS (Drums or Piano Roll)
                    if (isDrums) {
                        val drumOptions = listOf(
                            "16 Steps" to GridSubdivision(4, 4),
                            "32 Steps" to GridSubdivision(4, 8),
                            "16T" to GridSubdivision.TRIPLET_16TH,
                            "8T" to GridSubdivision.TRIPLET_8TH
                        )
                        drumOptions.forEach { (label, sub) ->
                            val isSelected = gridSubdivision.stepsPerBeat == sub.stepsPerBeat && gridSubdivision.totalBeats == sub.totalBeats
                            Box(
                                modifier = Modifier
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.2f) else PANEL)
                                    .border(1.dp, if (isSelected) Color(0xFF38BDF8) else BORDER, RoundedCornerShape(4.dp))
                                    .clickable { playbackVm.setGridSubdivision(sub) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color(0xFF38BDF8) else TEXT_DIM,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    } else if (isPianoRoll) {
                        // 1. Grid Subdivisions group
                        val gridOptions = listOf(
                            "16 Steps" to GridSubdivision(4, 4),
                            "32 Steps" to GridSubdivision(4, 8),
                            "16T" to GridSubdivision.TRIPLET_16TH,
                            "8T" to GridSubdivision.TRIPLET_8TH
                        )
                        gridOptions.forEach { (label, sub) ->
                            val isSelected = gridSubdivision.stepsPerBeat == sub.stepsPerBeat && gridSubdivision.totalBeats == sub.totalBeats
                            Box(
                                modifier = Modifier
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.2f) else PANEL)
                                    .border(1.dp, if (isSelected) Color(0xFF38BDF8) else BORDER, RoundedCornerShape(4.dp))
                                    .clickable { playbackVm.setGridSubdivision(sub) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color(0xFF38BDF8) else TEXT_DIM,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Box(modifier = Modifier.width(1.dp).height(20.dp).background(BORDER))

                        // 2. Note Duration options group
                        val drawDuration by playbackVm.drawDuration.collectAsState()
                        val snapSteps = listOf(0.0625f, 0.125f, 0.25f, 0.5f, 1f, 2f, 4f, 8f)
                        val labels = listOf("1/64", "1/32", "1/16", "1/8", "1/4", "1/2", "1 BAR", "2 BAR")
                        snapSteps.forEachIndexed { idx, dur ->
                            val isSelected = kotlin.math.abs(drawDuration - dur) < 0.001f
                            Box(
                                modifier = Modifier
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSelected) Color(0xFFE84040).copy(alpha = 0.2f) else PANEL)
                                    .border(1.dp, if (isSelected) Color(0xFFE84040) else BORDER, RoundedCornerShape(4.dp))
                                    .clickable { playbackVm.setDrawDuration(dur) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = labels[idx],
                                    color = if (isSelected) Color(0xFFE84040) else TEXT_DIM,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(BORDER)
            )

                BottomNav(
                    currentRoute = currentRoute,

                    onChordsClick = {

                        val sid =
                            navBackStackEntry?.arguments
                                ?.getString("sessionId")
                                ?: return@BottomNav

                        navController.navigate(
                            Screen.Chords.createRoute(sid)
                        ) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    },

                    onPianoRollClick = {

                        val sid =
                            navBackStackEntry?.arguments
                                ?.getString("sessionId")
                                ?: return@BottomNav

                        navController.navigate(
                            Screen.PianoRoll.createRoute(sid)
                        ) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    },

                    onDrumsClick = {

                        val sid =
                            navBackStackEntry?.arguments
                                ?.getString("sessionId")
                                ?: return@BottomNav

                        navController.navigate(
                            Screen.Drums.createRoute(sid)
                        ) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

        if (showExport && currentSessionId.isNotEmpty()) {

            val exportVm: ExportViewModel = viewModel(
                key = "export_$currentSessionId",

                factory = ExportViewModel.Factory(
                    sessionId = currentSessionId,
                    repository = repository,
                    application = application
                )
            )

            ExportDialog(
                sessionId = currentSessionId,
                sessionName = "Session",

                onDismiss = {
                    showExport = false
                },

                viewModel = exportVm
            )
        }
    }
}











