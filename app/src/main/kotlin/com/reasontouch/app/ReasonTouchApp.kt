package com.reasontouch.app

import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
                        sessionId = sessionId
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(RACK)
                        .padding(horizontal = 12.dp),

                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // HOME BUTTON
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(PANEL)
                            .border(1.dp, BORDER, RoundedCornerShape(4.dp))
                            .clickable {
                                navController.navigate(Screen.SessionList.route) {
                                    popUpTo(Screen.SessionList.route) { inclusive = true }
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "HOME",
                            color = TEXT_DIM,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    // SESSION BUTTON

                    Box(
                        modifier = Modifier
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
                            .padding(
                                horizontal = 12.dp,
                                vertical = 6.dp
                            )
                    ) {

                        Text(
                            text = "\u2699 SESSION",
                            color = TEXT_DIM,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))

                    // BACK BUTTON
                    if (onBackAction != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE84040).copy(alpha = 0.8f))
                                .clickable { onBackAction?.invoke() }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "← BACK",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // SPACE BETWEEN SESSION + TRANSPORT
                    Spacer(modifier = Modifier.width(12.dp))
                    // GLOBAL TRANSPORT
                    val playbackVm: PlaybackViewModel = hiltViewModel()
                    val transport = playbackVm.transport.collectAsState()
                    TransportControls(
                        isPlaying = transport.value.isPlaying,
                        onPlay = { playbackVm.play(currentSessionId) },
                        onStop = playbackVm::stop,
                        onRewind = playbackVm::rewind,
                        onFastForward = playbackVm::fastForward,
                        onSkipToStart = playbackVm::skipToStart,
                        onSkipToEnd = playbackVm::skipToEnd
                    )
                    // DURATION STEPPER (Arrange only)
                    val isArrange = currentRoute.startsWith("piano_roll/")
                    if (isArrange) {
                        val drawDuration by playbackVm.drawDuration.collectAsState()
                        val snapValue = 0.0625f // 1/64 minimum step
                        val label = when (drawDuration) {
                            0.0625f -> "1/64"
                            0.125f -> "1/32"
                            0.25f -> "1/16"
                            0.5f -> "1/8"
                            1f -> "1/4"
                            2f -> "1/2"
                            4f -> "1 BAR"
                            8f -> "2 BAR"
                            else -> "%.2f".format(drawDuration)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.CenterVertically) {

                            androidx.compose.material3.IconButton(
                                onClick = { playbackVm.stepStepper(-1) },
                                modifier = Modifier.width(28.dp)
                            ) {
                                Text(
                                    "-",
                                    color = Color(0xFFC8C8D4),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Text(
                                text = label,
                                color = Color(0xFFE84040),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.width(36.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            androidx.compose.material3.IconButton(
                                onClick = { playbackVm.stepStepper(+1) },
                                modifier = Modifier.width(28.dp)
                            ) {
                                Text(
                                    "+",
                                    color = Color(0xFFC8C8D4),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
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











