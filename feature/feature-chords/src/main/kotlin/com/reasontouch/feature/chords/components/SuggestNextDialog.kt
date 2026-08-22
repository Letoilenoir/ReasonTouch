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

    @Composable
    fun SuggestNextDialog(
        suggestions: List<SuggestionOption>,
        onOptionSelected: (intentType: PairingType, phraseIndex: Int) -> Unit,
        onDismiss: () -> Unit
    ) {
        val skin = ReasonTouchTheme.skin

        Dialog(onDismissRequest = onDismiss) {
            SuggestNextFlow(
                suggestions = suggestions,
                onOptionSelected = onOptionSelected,
                onCancel = onDismiss,
                cancelLabel = "CANCEL",
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(
                        color = skin.panel,
                        shape = RoundedCornerShape(skin.cornerRadiusMedium)
                    )
                    .padding(skin.paddingXLarge)
            )
        }
    }
}