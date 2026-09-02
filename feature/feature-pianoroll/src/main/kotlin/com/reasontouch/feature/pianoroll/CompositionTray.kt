package com.reasontouch.feature.pianoroll

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reasontouch.core.ui.theme.ReasonTouchTheme
import com.reasontouch.feature.chords.PairingType
import com.reasontouch.feature.chords.BassStyle
import com.reasontouch.feature.chords.SuggestionWorkflow.SuggestionOption
import com.reasontouch.feature.chords.components.SuggestNextFlow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Spacer

private val OVERLAY_BG   = Color(0xEE1E1E24)
private val OVERLAY_EDGE = Color(0xFF3A3A45)
private val ACCENT_RED   = Color(0xFFE84040)
private val TEXT_DIM     = Color(0xFF88889A)

enum class TraySection { HOME, SUGGEST_NEXT, BASS, DRUMS }

/**
 * Persistent Composition Tray handle -- replaces VelocityOverlayTray at the same anchor point,
 * keeping its pill/expand-collapse interaction pattern (see VelocityOverlayTray.kt, now
 * redundant) but hosting HOME/SUGGEST_NEXT/BASS/DRUMS content instead of the velocity strip.
 *
 * expanded/section are hoisted so the toolbar SUGGEST chip (PianoRollToolbar's onSuggestNext)
 * can jump straight into SUGGEST_NEXT without this composable needing to know about the
 * toolbar. Collapsing and re-expanding always resets to HOME, matching the "tray reflects
 * current composition state" principle in UX_Direction Section 12.
 */
@Composable
fun CompositionTray(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    section: TraySection,
    onSectionChange: (TraySection) -> Unit,
    suggestions: List<SuggestionOption>,
    onRequestSuggestions: () -> Unit,
    onOptionSelected: (intentType: PairingType, phraseIndex: Int) -> Unit,
    onBassRequested: () -> Unit,
    onDrumsRequested: () -> Unit,
    bassSuggestions: List<BassStyle> = emptyList(),
    drumSuggestions: List<String> = emptyList(),
    onApplyBass: (BassStyle, appendMode: Boolean) -> Unit,
    onApplyDrums: (presetName: String, appendMode: Boolean) -> Unit,
    hasHarmony: Boolean = false,
    hasBass: Boolean = false,
    hasDrums: Boolean = false,
    modifier: Modifier = Modifier
) {
    val skin = ReasonTouchTheme.skin

    val trayHeight by animateDpAsState(
        targetValue = if (expanded) 340.dp else 28.dp,
        label = "compose_tray_height"
    )

    LaunchedEffect(expanded, section) {
        if (expanded && section == TraySection.SUGGEST_NEXT && suggestions.isEmpty()) {
            onRequestSuggestions()
        }
    }

    Column(
        modifier = modifier
            .padding(bottom = 8.dp)
            .then(if (expanded) Modifier.fillMaxWidth() else Modifier.width(220.dp))
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

        // Pill header -- identical interaction to the old VelocityOverlayTray
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(
                    if (expanded) ACCENT_RED.copy(alpha = 0.18f) else OVERLAY_EDGE.copy(alpha = 0.65f)
                )
                .clickable {
                    val next = !expanded
                    onExpandedChange(next)
                    if (next) onSectionChange(TraySection.HOME)
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (expanded) "COMPOSE \u25BC" else "COMPOSE \u25B2",
                color = if (expanded) ACCENT_RED else TEXT_DIM,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        if (expanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xAA18181C))
                    .padding(skin.paddingLarge)
            ) {
                when (section) {
                    TraySection.HOME -> Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    ) {
                        TrayHome(
                            hasHarmony = hasHarmony,
                            hasBass = hasBass,
                            hasDrums = hasDrums,
                            onSuggestNext = { onSectionChange(TraySection.SUGGEST_NEXT) },
                            onBass = { onSectionChange(TraySection.BASS); onBassRequested() },
                            onDrums = { onSectionChange(TraySection.DRUMS); onDrumsRequested() }
                        )
                    }
                    TraySection.SUGGEST_NEXT -> {
                        if (suggestions.isNotEmpty()) {
                            SuggestNextFlow(
                                suggestions = suggestions,
                                onOptionSelected = { type, phraseIndex ->
                                    onOptionSelected(type, phraseIndex)
                                    onSectionChange(TraySection.HOME)
                                },
                                onCancel = { onSectionChange(TraySection.HOME) },
                                cancelLabel = "BACK",
                                pinHeader = true,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = "Analyzing progression...",
                                color = skin.textMuted,
                                fontSize = 13.sp
                            )
                        }
                    }
                    TraySection.BASS -> BassPanel(
                        suggestions = bassSuggestions,
                        onApply = onApplyBass,
                        onBack = { onSectionChange(TraySection.HOME) }
                    )
                    TraySection.DRUMS -> DrumsPanel(
                        suggestions = drumSuggestions,
                        onApply = onApplyDrums,
                        onBack = { onSectionChange(TraySection.HOME) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BassPanel(
    suggestions: List<BassStyle>,
    onApply: (BassStyle, Boolean) -> Unit,
    onBack: () -> Unit
) {
    val skin = ReasonTouchTheme.skin
    var appendMode by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(text = "BASS", color = skin.accent, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(skin.paddingMedium))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(BassStyle.values().toList()) { style ->
                SuggestableRow(
                    label = style.label,
                    description = style.description,
                    isSuggested = style in suggestions,
                    onClick = { onApply(style, appendMode) }
                )
            }
        }

        AppendModeRow(appendMode = appendMode, onToggle = { appendMode = it })
        Spacer(Modifier.height(skin.paddingMedium))
        TrayActionRow(label = "BACK", onClick = onBack)
    }
}

@Composable
private fun DrumsPanel(
    suggestions: List<String>,
    onApply: (String, Boolean) -> Unit,
    onBack: () -> Unit
) {
    val skin = ReasonTouchTheme.skin
    var appendMode by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(text = "DRUMS", color = skin.accent, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(skin.paddingMedium))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(com.reasontouch.feature.drums.DrumPresets.all.keys.toList()) { presetName ->
                SuggestableRow(
                    label = presetName,
                    description = null,
                    isSuggested = presetName in suggestions,
                    onClick = { onApply(presetName, appendMode) }
                )
            }
        }

        AppendModeRow(appendMode = appendMode, onToggle = { appendMode = it })
        Spacer(Modifier.height(skin.paddingMedium))
        TrayActionRow(label = "BACK", onClick = onBack)
    }
}

@Composable
private fun SuggestableRow(
    label: String,
    description: String?,
    isSuggested: Boolean,
    onClick: () -> Unit
) {
    val skin = ReasonTouchTheme.skin
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = skin.paddingMedium)
            .background(
                if (isSuggested) skin.accent.copy(alpha = 0.12f) else skin.panelAlt,
                RoundedCornerShape(skin.cornerRadiusSmall)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = skin.paddingLarge, vertical = skin.paddingMedium)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = label, color = skin.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                if (isSuggested) {
                    Text(text = "SUGGESTED", color = skin.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (description != null) {
                Text(text = description, color = skin.textMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun AppendModeRow(appendMode: Boolean, onToggle: (Boolean) -> Unit) {
    val skin = ReasonTouchTheme.skin
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (appendMode) "APPEND TO EXISTING" else "REPLACE EXISTING",
            color = skin.textSecondary,
            fontSize = 11.sp
        )
        Switch(checked = appendMode, onCheckedChange = onToggle)
    }
}

@Composable
private fun TrayHome(
    hasHarmony: Boolean,
    hasBass: Boolean,
    hasDrums: Boolean,
    onSuggestNext: () -> Unit,
    onBass: () -> Unit,
    onDrums: () -> Unit
) {
    val skin = ReasonTouchTheme.skin

    Column {
        Text(
            text = "COMPOSE",
            color = skin.accent,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = skin.paddingXLarge)
        )

        TraySectionLabel("HARMONY", complete = hasHarmony)
        TrayActionRow(label = "SUGGEST NEXT", onClick = onSuggestNext)

        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = skin.paddingLarge))

        TraySectionLabel("ARRANGEMENT", complete = hasBass && hasDrums)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(skin.paddingMedium)
        ) {
            TrayActionRow(label = if (hasBass) "BASS \u2713" else "BASS", onClick = onBass, modifier = Modifier.weight(1f))
            TrayActionRow(label = if (hasDrums) "DRUMS \u2713" else "DRUMS", onClick = onDrums, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun TraySectionLabel(text: String, complete: Boolean) {
    val skin = ReasonTouchTheme.skin
    Text(
        text = if (complete) "$text \u2713" else text,
        color = skin.textSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = skin.paddingSmall)
    )
}

@Composable
private fun TrayActionRow(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val skin = ReasonTouchTheme.skin
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = skin.paddingMedium)
            .background(skin.panelAlt, RoundedCornerShape(skin.cornerRadiusSmall))
            .clickable(onClick = onClick)
            .padding(horizontal = skin.paddingLarge, vertical = skin.paddingMedium)
    ) {
        Text(text = label, color = skin.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TrayStubPanel(title: String, onBack: () -> Unit) {
    val skin = ReasonTouchTheme.skin
    Column {
        Text(text = title, color = skin.accent, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text(
            text = "Full $title workflow not yet built into the tray -- see UX_Direction Section ${if (title == "BASS") 8 else 9}.",
            color = skin.textMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = skin.paddingSmall, bottom = skin.paddingLarge)
        )
        TrayActionRow(label = "BACK", onClick = onBack)
    }
}