package com.reasontouch.feature.pianoroll

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reasontouch.core.data.MidiTrack
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow

private val CARD_BG = Color(0xFF111118)
private val CARD_BORDER = Color(0xFF343444)
private val TEXT_DIM    = Color(0xFF77778A)

@Composable
fun NeoTrackCardRow(
    tracks: List<MidiTrack>,
    activeIndex: Int,
    onSelect: (Int) -> Unit,
    onMuteToggle: (Int) -> Unit,
    onVolumeChange: (Int, Float) -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF18181D))
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        tracks.forEachIndexed { index, track ->

            val trackColor = trackColor(index)

            NeoTrackCard(
                track = track,
                color = trackColor,
                active = index == activeIndex,
                onSelect = { onSelect(index) },
                onMute = { onMuteToggle(index) },
                onVolumeChange = { onVolumeChange(index, it) }
            )
        }
    }
}

@Composable
fun NeoTrackCard(
    track: MidiTrack,
    color: Color,
    active: Boolean,
    onSelect: () -> Unit,
    onMute: () -> Unit,
    onVolumeChange: (Float) -> Unit
) {

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            color.copy(alpha = 0.10f),
            CARD_BG
        )
    )

    Column(
        modifier = Modifier
            .width(68.dp)
            .height(60.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundBrush)
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.45f),
                shape = RoundedCornerShape(14.dp)
            )
            .shadow(
                elevation = if (active) 10.dp else 0.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = color.copy(alpha = 0.35f),
                spotColor = color.copy(alpha = 0.35f)
            )
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (active)
                            color.copy(alpha = 0.18f)
                        else
                            Color.Transparent
                    )
                    .clickable(onClick = onSelect)
                    .padding(horizontal = 4.dp, vertical = 3.dp)
            ) {

                Text(
                    text = track.name.uppercase(),
                    color = if (active) color else Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (track.muted)
                            Color(0x44FF4444)
                        else
                            Color(0x22101010)
                    )
                    .clickable(onClick = onMute),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "M",
                    color =
                        if (track.muted)
                            Color.Red
                        else
                            TEXT_DIM,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        NeonFader(
            value = track.volume,
            color = color,
            onValueChange = onVolumeChange
        )


    }
}
@Composable
fun NeonFader(
    value: Float,
    color: Color,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {

    androidx.compose.foundation.Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->

                    val newValue =
                        (change.position.x / size.width)
                            .coerceIn(0f, 1f)

                    onValueChange(newValue)
                }
            }
    ) {

        val centerY = size.height / 2f
        val railHeight = 6.dp.toPx()
        val frameHeight = 14.dp.toPx()
        // neon boundary frame
        drawRoundRect(
            color = color.copy(alpha = 0.22f),
            topLeft = androidx.compose.ui.geometry.Offset(
                0f,
                centerY - frameHeight / 2
            ),
            size = androidx.compose.ui.geometry.Size(
                size.width,
                frameHeight
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(100f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 1.5.dp.toPx()
            )
        )

        // recessed base rail
        drawRoundRect(
            color = Color(0xFF09090D),
            topLeft = androidx.compose.ui.geometry.Offset(
                0f,
                centerY - railHeight / 2
            ),
            size = androidx.compose.ui.geometry.Size(
                size.width,
                railHeight
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(100f)
        )

        // dim outer glow
        drawRoundRect(
            color = color.copy(alpha = 0.12f),
            topLeft = androidx.compose.ui.geometry.Offset(
                0f,
                centerY - 8.dp.toPx() / 2
            ),
            size = androidx.compose.ui.geometry.Size(
                size.width * value,
                8.dp.toPx()
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(100f)
        )

        // bright neon line
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(
                0f,
                centerY - railHeight / 2
            ),
            size = androidx.compose.ui.geometry.Size(
                size.width * value,
                railHeight
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(100f)
        )
        // LED-style segment dividers
        val segmentCount = 6

        repeat(segmentCount) { i ->

            val segmentX =
                (size.width * value / segmentCount) * i

            drawLine(
                color = Color.White.copy(alpha = 0.16f),
                start = androidx.compose.ui.geometry.Offset(
                    segmentX,
                    centerY - 5.dp.toPx()
                ),
                end = androidx.compose.ui.geometry.Offset(
                    segmentX,
                    centerY + 5.dp.toPx()
                ),
                strokeWidth = 1.dp.toPx()
            )
        }



    }
}
private fun trackColor(index: Int): Color {

    return Color(
        when (index % 8) {
            0 -> 0xFFE84040
            1 -> 0xFF3DDC84
            2 -> 0xFF38BDF8
            3 -> 0xFFA78BFA
            4 -> 0xFFFF8A00
            5 -> 0xFFF5C518
            6 -> 0xFFF472B6
            else -> 0xFF94A3B8
        }
    )
}