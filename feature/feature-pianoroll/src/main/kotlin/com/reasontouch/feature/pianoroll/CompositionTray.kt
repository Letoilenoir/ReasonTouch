package com.reasontouch.feature.pianoroll

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.reasontouch.feature.drums.DrumPresets
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Spacer
import com.reasontouch.feature.chords.CompositionMode
import com.reasontouch.feature.chords.RankedSuggestion

private val OVERLAY_BG   = Color(0xEE1E1E24)
private val OVERLAY_EDGE = Color(0xFF3A3A45)
private val ACCENT_RED   = Color(0xFFE84040)
private val TEXT_DIM     = Color(0xFF88889A)

enum class TraySection { HOME, SUGGEST_NEXT, BASS, DRUMS, FULL_GROOVE }

data class GroovePreview(
    val bassStyle: BassStyle,
    val bassNotes: List<com.reasontouch.core.data.NoteEvent>,
    val drumPresetName: String,
    val drumNotes: List<com.reasontouch.core.data.NoteEvent>
)

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
    onFullGrooveRequested: () -> Unit,
    bassSuggestions: List<BassStyle> = emptyList(),
    drumSuggestions: List<String> = emptyList(),
    onApplyBass: (BassStyle, appendMode: Boolean) -> Unit,
    onApplyDrums: (presetName: String, appendMode: Boolean) -> Unit,
    compositionMode: CompositionMode = CompositionMode.ASSISTED,
    grooveBassSuggestions: List<RankedSuggestion<BassStyle>> = emptyList(),
    grooveDrumSuggestions: List<RankedSuggestion<String>> = emptyList(),
    groovePreview: GroovePreview? = null,
    onGenerateGroove: (BassStyle, String) -> Unit,
    onApplyGroove: () -> Unit,
    onDiscardGroove: () -> Unit,
    transitionFill: Boolean = true,
    onTransitionFillChange: (Boolean) -> Unit = {},
    endFill: Boolean = true,
    onEndFillChange: (Boolean) -> Unit = {},
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
                            onDrums = { onSectionChange(TraySection.DRUMS); onDrumsRequested() },
                            onFullGroove = { onSectionChange(TraySection.FULL_GROOVE); onFullGrooveRequested() }
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
                        transitionFill = transitionFill,
                        onTransitionFillChange = onTransitionFillChange,
                        endFill = endFill,
                        onEndFillChange = onEndFillChange,
                        onBack = { onSectionChange(TraySection.HOME) }
                    )
                    TraySection.FULL_GROOVE -> FullGroovePanel(
                        compositionMode = compositionMode,
                        bassSuggestions = grooveBassSuggestions,
                        drumSuggestions = grooveDrumSuggestions,
                        preview = groovePreview,
                        transitionFill = transitionFill,
                        onTransitionFillChange = onTransitionFillChange,
                        endFill = endFill,
                        onEndFillChange = onEndFillChange,
                        onGenerate = onGenerateGroove,
                        onApply = onApplyGroove,
                        onDiscard = { onDiscardGroove(); onSectionChange(TraySection.HOME) },
                        onBack = { onDiscardGroove(); onSectionChange(TraySection.HOME) }
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
    transitionFill: Boolean,
    onTransitionFillChange: (Boolean) -> Unit,
    endFill: Boolean,
    onEndFillChange: (Boolean) -> Unit,
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

        FillToggles(
            transitionFill = transitionFill,
            onTransitionChange = onTransitionFillChange,
            endFill = endFill,
            onEndChange = onEndFillChange
        )
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
    isSelected: Boolean = false,
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
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) skin.accent else Color.Transparent,
                shape = RoundedCornerShape(skin.cornerRadiusSmall)
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
private fun CompactSuggestableRow(
    label: String,
    isSuggested: Boolean,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    val skin = ReasonTouchTheme.skin
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
            .background(
                if (isSelected) skin.accent.copy(alpha = 0.2f) else if (isSuggested) skin.accent.copy(alpha = 0.1f) else skin.panelAlt,
                RoundedCornerShape(skin.cornerRadiusSmall)
            )
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) skin.accent else Color.Transparent,
                shape = RoundedCornerShape(skin.cornerRadiusSmall)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = skin.paddingMedium, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = skin.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            if (isSuggested) {
                Text(text = "SUGGESTED", color = skin.accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
    onDrums: () -> Unit,
    onFullGroove: () -> Unit
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

        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = skin.paddingMedium))

        TrayActionRow(label = "FULL GROOVE", onClick = onFullGroove)
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
private fun FullGroovePanel(
    compositionMode: CompositionMode,
    bassSuggestions: List<RankedSuggestion<BassStyle>>,
    drumSuggestions: List<RankedSuggestion<String>>,
    preview: GroovePreview?,
    transitionFill: Boolean,
    onTransitionFillChange: (Boolean) -> Unit,
    endFill: Boolean,
    onEndFillChange: (Boolean) -> Unit,
    onGenerate: (BassStyle, String) -> Unit,
    onApply: () -> Unit,
    onDiscard: () -> Unit,
    onBack: () -> Unit
) {
    val skin = ReasonTouchTheme.skin
    val isAssisted = compositionMode != CompositionMode.MANUAL

    var selectedBass by remember(bassSuggestions) {
        mutableStateOf(if (isAssisted) bassSuggestions.firstOrNull()?.value else null)
    }
    var selectedDrum by remember(drumSuggestions) {
        mutableStateOf(if (isAssisted) drumSuggestions.firstOrNull()?.value else null)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(text = "FULL GROOVE", color = skin.accent, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(skin.paddingMedium))

        if (preview != null) {
            // PREVIEW STATE
            Text(
                text = "Bass: ${preview.bassStyle.label} \u00b7 ${preview.bassNotes.size} notes",
                color = skin.textPrimary,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = skin.paddingSmall)
            )
            Text(
                text = "Drums: ${preview.drumPresetName} \u00b7 ${preview.drumNotes.size} notes",
                color = skin.textPrimary,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = skin.paddingLarge)
            )
            Text(
                text = "Not yet saved. Apply to write both to the Piano Roll, or Discard to cancel.",
                color = skin.textMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = skin.paddingLarge)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = skin.paddingMedium)
                    .background(skin.accent, RoundedCornerShape(skin.cornerRadiusSmall))
                    .clickable(onClick = onApply)
                    .padding(horizontal = skin.paddingLarge, vertical = skin.paddingMedium)
            ) {
                Text(text = "APPLY", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            TrayActionRow(label = "DISCARD", onClick = onDiscard)
        } else {
            // PICKER STATE -- two columns side-by-side (Bass left, Drums + Fills right)
            Text(
                text = if (isAssisted) "Suggestions shown below -- pick any to change."
                else "Choose a Bass style and a Drum pattern.",
                color = skin.textMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = skin.paddingMedium)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(skin.paddingMedium)
                ) {
                    // Left Column: Bass
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "BASS", color = skin.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = skin.paddingSmall))
                        BassStyle.values().forEach { style ->
                            CompactSuggestableRow(
                                label = style.label,
                                isSuggested = isAssisted && style == bassSuggestions.firstOrNull()?.value,
                                isSelected = style == selectedBass,
                                onClick = { selectedBass = style }
                            )
                        }
                    }

                    // Right Column: Drums + Fill Toggles
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "DRUMS", color = skin.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = skin.paddingSmall))
                        DrumPresets.all.keys.forEach { presetName ->
                            CompactSuggestableRow(
                                label = presetName,
                                isSuggested = isAssisted && presetName == drumSuggestions.firstOrNull()?.value,
                                isSelected = selectedDrum == presetName,
                                onClick = { selectedDrum = presetName }
                            )
                        }

                        Spacer(Modifier.height(4.dp))
                        FillToggles(
                            transitionFill = transitionFill,
                            onTransitionChange = onTransitionFillChange,
                            endFill = endFill,
                            onEndChange = onEndFillChange
                        )
                    }
                }
            }

            Spacer(Modifier.height(skin.paddingSmall))

            val canGenerate = selectedBass != null && selectedDrum != null
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = skin.paddingMedium),
                horizontalArrangement = Arrangement.spacedBy(skin.paddingMedium)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(skin.panelAlt, RoundedCornerShape(skin.cornerRadiusSmall))
                        .clickable(onClick = onBack)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "BACK", color = skin.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (canGenerate) skin.accent else skin.panelAlt,
                            RoundedCornerShape(skin.cornerRadiusSmall)
                        )
                        .clickable(enabled = canGenerate) {
                            onGenerate(selectedBass!!, selectedDrum!!)
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "GENERATE",
                        color = if (canGenerate) Color.Black else skin.textMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
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

@Composable
private fun CompactToggleCard(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val skin = ReasonTouchTheme.skin
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
            .background(
                if (checked) skin.accent.copy(alpha = 0.2f) else skin.panelAlt,
                RoundedCornerShape(skin.cornerRadiusSmall)
            )
            .border(
                width = if (checked) 1.5.dp else 0.dp,
                color = if (checked) skin.accent else Color.Transparent,
                shape = RoundedCornerShape(skin.cornerRadiusSmall)
            )
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = skin.paddingMedium, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = skin.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .height(16.dp)
                    .background(
                        if (checked) skin.accent else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (checked) skin.accent else skin.textMuted,
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (checked) {
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(6.dp)
                            .background(Color.Black, RoundedCornerShape(3.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun FillToggles(
    transitionFill: Boolean,
    onTransitionChange: (Boolean) -> Unit,
    endFill: Boolean,
    onEndChange: (Boolean) -> Unit
) {
    Column {
        CompactToggleCard(
            label = "Transition Fills",
            checked = transitionFill,
            onCheckedChange = onTransitionChange
        )
        CompactToggleCard(
            label = "End Fill",
            checked = endFill,
            onCheckedChange = onEndChange
        )
    }
}