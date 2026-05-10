package com.reasontouch.feature.pianoroll

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*

private val OVERLAY_BG   = Color(0xEE1E1E24)
private val OVERLAY_EDGE = Color(0xFF3A3A45)
private val ACCENT_RED   = Color(0xFFE84040)
private val TEXT_DIM     = Color(0xFF88889A)

@Composable
fun VelocityOverlayTray(
    state: PianoRollState,
    uiState: PianoRollUiState,
    viewModel: PianoRollViewModel,
    modifier: Modifier = Modifier
) {

    var expanded by remember { mutableStateOf(false) }

    val trayHeight by animateDpAsState(
        targetValue = if (expanded) 140.dp else 28.dp,
        label = "velocity_tray_height"
    )

    Column(
        modifier = modifier
            .padding(bottom = 8.dp)
            .then(
                if (expanded) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier.width(220.dp)
                }
            )
            .height(trayHeight)
            .clip(
                RoundedCornerShape(
                    topStart = 10.dp,
                    topEnd = 10.dp,
                    bottomStart = if (expanded) 0.dp else 10.dp,
                    bottomEnd = if (expanded) 0.dp else 10.dp
                )
            )
            .background(OVERLAY_BG)
    ) {

        // ── Floating pill header ─────────────────────────────

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(
                    if (expanded)
                        ACCENT_RED.copy(alpha = 0.18f)
                    else
                        OVERLAY_EDGE.copy(alpha = 0.65f)
                )
                .clickable {
                    expanded = !expanded
                },
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = if (expanded)
                    "VELOCITY ▼"
                else
                    "VELOCITY ▲",
                color = if (expanded) ACCENT_RED else TEXT_DIM,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // ── Expanded velocity editor ────────────────────────

        if (expanded) {

            VelocityStripCanvas(
                state     = state,
                uiState   = uiState,
                viewModel = viewModel,
                modifier  = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xAA18181C))
            )
        }
    }
}
