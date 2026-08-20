package com.reasontouch.feature.chords.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.reasontouch.core.ui.theme.ReasonTouchTheme
import com.reasontouch.feature.chords.PairingType
import com.reasontouch.feature.chords.SuggestionWorkflow.SuggestionOption

/**
 * Multi-option SUGGEST NEXT dialog. Presents ranked intents (RESOLVE/CONTRAST/CONTINUE/LIFT/
 * SURPRISE, per PairingEngine.suggestNextMulti()) as tabs, with that intent's generated phrase
 * candidates listed below. Replaces the single-PairingDecision version -- see the 2026-08-03
 * design note, Section 3 ("intent-level choice").
 *
 * onOptionSelected reports both which intent tab was active and which phrase within it was
 * picked, since the caller needs the actual GeneratedProgression to commit, not just an index.
 */
@Composable
fun SuggestNextDialog(
    suggestions: List<SuggestionOption>,
    onOptionSelected: (intentType: PairingType, phraseIndex: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val skin = ReasonTouchTheme.skin

    var selectedTabIndex by remember { mutableStateOf(0) }
    var selectedPhraseIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(selectedTabIndex) {
        selectedPhraseIndex = null
    }

    val current = suggestions.getOrNull(selectedTabIndex) ?: suggestions.first()

    Dialog(onDismissRequest = onDismiss) {

        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(
                    color = skin.panel,
                    shape = RoundedCornerShape(skin.cornerRadiusMedium)
                )
                .padding(skin.paddingXLarge)
        ) {

            Text(
                text = "SUGGEST NEXT",
                color = skin.accent,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = skin.paddingXLarge)
            )

            if (suggestions.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = skin.paddingLarge)
                ) {
                    suggestions.forEachIndexed { index, suggestion ->
                        val isSelected = index == selectedTabIndex

                        Box(
                            modifier = Modifier
                                .padding(end = skin.paddingSmall)
                                .background(
                                    color = if (isSelected) skin.accent else skin.panelAlt,
                                    shape = RoundedCornerShape(skin.cornerRadiusSmall)
                                )
                                .clickable { selectedTabIndex = index }
                                .padding(
                                    horizontal = skin.paddingMedium,
                                    vertical = skin.paddingSmall
                                )
                        ) {
                            Text(
                                text = suggestion.option.type.name,
                                color = if (isSelected) Color.White else skin.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Text(
                text = current.option.rationale,
                color = skin.textSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = skin.paddingMedium)
            )

            Text(
                text = "Suggested: ${current.option.suggestedBars} bars " +
                        "(${(current.option.confidence * 100).toInt()}% confident)",
                color = skin.textMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = skin.paddingXLarge)
            )

            Text(
                text = "Suggested continuations",
                color = skin.textSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = skin.paddingMedium)
            )

            if (current.phrases.isEmpty()) {
                Text(
                    text = "${current.option.type.name} pathway not yet implemented for this context",
                    color = skin.textMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = skin.paddingMedium)
                )
            } else {
                current.phrases.forEachIndexed { index, phrase ->
                    val isSelected = index == selectedPhraseIndex

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = skin.paddingSmall)
                            .background(
                                color = if (isSelected) skin.accent.copy(alpha = 0.15f) else skin.panelAlt,
                                shape = RoundedCornerShape(skin.cornerRadiusSmall)
                            )
                            .border(
                                width = skin.borderWidth,
                                color = if (isSelected) skin.accent else skin.border,
                                shape = RoundedCornerShape(skin.cornerRadiusSmall)
                            )
                            .clickable { selectedPhraseIndex = index }
                            .padding(
                                horizontal = skin.paddingLarge,
                                vertical = skin.paddingMedium
                            )
                    ) {
                        Column {
                            val lines = phrase.explanation.split("\n")

                            Text(
                                text = lines.first(),
                                color = skin.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            if (lines.size > 1) {
                                Spacer(modifier = Modifier.height(skin.paddingXSmall))
                                Text(
                                    text = lines[1],
                                    color = skin.textSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(skin.spacingMedium))

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(skin.buttonHeight)
                        .padding(end = skin.paddingSmall),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = skin.panelAlt,
                        contentColor = skin.textSecondary
                    ),
                    shape = RoundedCornerShape(skin.buttonCornerRadius)
                ) {
                    Text(text = "CANCEL", fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        selectedPhraseIndex?.let { onOptionSelected(current.option.type, it) }
                    },
                    enabled = selectedPhraseIndex != null,
                    modifier = Modifier
                        .weight(1f)
                        .height(skin.buttonHeight),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = skin.accent,
                        contentColor = Color.White,
                        disabledContainerColor = skin.panelAlt,
                        disabledContentColor = skin.textMuted
                    ),
                    shape = RoundedCornerShape(skin.buttonCornerRadius)
                ) {
                    Text(text = "SELECT", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}