package com.reasontouch.feature.chords.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reasontouch.core.ui.theme.ReasonTouchTheme
import com.reasontouch.feature.chords.PairingType
import com.reasontouch.feature.chords.SuggestionWorkflow.SuggestionOption

private val SELECT_GREEN = Color(0xFF3DDC84)

/**
 * The Suggest Next interaction, extracted from SuggestNextDialog.kt so it can be hosted either
 * inside a Dialog (legacy, pinHeader=false, sizes to content) or inside the Composition Tray
 * (pinHeader=true, fills a bounded height and scrolls only the body). Without pinHeader, the
 * whole thing -- header, breadcrumb, phrase cards -- lived in one scrollable Column, so BACK/
 * SELECT scrolled out of reach once a strategy had enough phrases (2026-08-22 tray testing).
 * pinHeader keeps the header row fixed and confines scrolling to the body beneath it.
 *
 * onCancel's meaning is host-dependent: the Dialog host treats it as dismiss; the Composition
 * Tray host treats it as "collapse back to the tray's compact home state."
 */
@Composable
fun SuggestNextFlow(
    suggestions: List<SuggestionOption>,
    onOptionSelected: (intentType: PairingType, phraseIndex: Int) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    cancelLabel: String = "CANCEL",
    pinHeader: Boolean = false
) {
    val skin = ReasonTouchTheme.skin

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var selectedPhraseIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(selectedIndex) {
        selectedPhraseIndex = null
    }

    if (suggestions.isEmpty()) return
    val current = selectedIndex?.let { suggestions.getOrNull(it) }

    val header: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (current == null) "SUGGEST NEXT" else "SUGGESTED CONTINUATIONS",
                color = skin.accent,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (current != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButtonSquare(
                        symbol = "\u2039",
                        onClick = { selectedIndex = null },
                        containerColor = skin.panelAlt,
                        contentColor = skin.textSecondary
                    )
                    Spacer(modifier = Modifier.width(skin.paddingSmall))
                    IconButtonSquare(
                        symbol = "\u2713",
                        onClick = {
                            selectedPhraseIndex?.let { onOptionSelected(current.option.type, it) }
                        },
                        enabled = selectedPhraseIndex != null,
                        containerColor = SELECT_GREEN,
                        contentColor = Color.Black,
                        disabledContainerColor = skin.panelAlt,
                        disabledContentColor = skin.textMuted
                    )
                }
            }
        }
    }

    val body: @Composable () -> Unit = {
        if (current == null) {
            suggestions.forEachIndexed { index, suggestion ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = skin.paddingSmall)
                        .background(skin.panelAlt, RoundedCornerShape(skin.cornerRadiusSmall))
                        .clickable { selectedIndex = index }
                        .padding(horizontal = skin.paddingLarge, vertical = skin.paddingMedium)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = suggestion.option.type.name,
                            color = skin.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${(suggestion.option.confidence * 100).toInt()}%",
                            color = skin.textMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(skin.spacingMedium))

            Button(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth().height(skin.buttonHeight),
                colors = ButtonDefaults.buttonColors(
                    containerColor = skin.panelAlt,
                    contentColor = skin.textSecondary
                ),
                shape = RoundedCornerShape(skin.buttonCornerRadius)
            ) {
                Text(text = cancelLabel, fontSize = 13.sp)
            }

        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = skin.paddingMedium)
                    .clickable { selectedIndex = null },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "\u2039 ${current.option.type.name}",
                    color = skin.accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(skin.paddingMedium))
                Text(
                    text = current.option.rationale,
                    color = skin.textSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = "Suggested: ${current.option.suggestedBars} bars " +
                        "(${(current.option.confidence * 100).toInt()}% confident)",
                color = skin.textMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = skin.paddingLarge)
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
                            .padding(horizontal = skin.paddingLarge, vertical = skin.paddingMedium)
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
        }
    }

    if (pinHeader) {
        Column(modifier = modifier.fillMaxHeight()) {
            header()
            Spacer(modifier = Modifier.height(skin.paddingXLarge))
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    body()
                }
            }
        }
    } else {
        Column(modifier = modifier) {
            header()
            Spacer(modifier = Modifier.height(skin.paddingXLarge))
            body()
        }
    }
}

@Composable
private fun IconButtonSquare(
    symbol: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    enabled: Boolean = true,
    disabledContainerColor: Color = containerColor,
    disabledContentColor: Color = contentColor
) {
    val skin = ReasonTouchTheme.skin
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(
                color = if (enabled) containerColor else disabledContainerColor,
                shape = RoundedCornerShape(skin.buttonCornerRadius)
            )
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            color = if (enabled) contentColor else disabledContentColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}