package com.reasontouch.feature.chords.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.reasontouch.core.ui.theme.ReasonTouchTheme
import com.reasontouch.feature.chords.PairingDecision
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun SuggestNextDialog(
    decision: PairingDecision,
    options: List<String>,
    onOptionSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val skin = ReasonTouchTheme.skin
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

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

            // Title
            Text(
                text = "SUGGEST NEXT",
                color = skin.accent,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = skin.paddingXLarge)
            )

            // Pairing type
            Text(
                text = decision.type.name,
                color = skin.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = skin.paddingSmall)
            )

            // Explanation
            Text(
                text = decision.rationale,
                color = skin.textSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = skin.paddingMedium)
            )

            // Confidence
            Text(
                text = "Suggested: ${decision.suggestedBars} bars (${(decision.confidence * 100).toInt()}% confident)",
                color = skin.textMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = skin.paddingXLarge)
            )

            // Section heading
            Text(
                text = "Suggested continuations",
                color = skin.textSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = skin.paddingMedium)
            )

            // Phrase options
            options.forEachIndexed { index, option ->

                val isSelected = index == selectedIndex

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
                        .clickable {
                            selectedIndex = index
                        }
                        .padding(
                            horizontal = skin.paddingLarge,
                            vertical = skin.paddingMedium
                        )
                ) {

                    Column {

                        val lines = option.split("\n")

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

            Spacer(modifier = Modifier.height(skin.spacingMedium))

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {

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
                    Text(
                        text = "CANCEL",
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = {
                        selectedIndex?.let { onOptionSelected(it) }
                    },
                    enabled = selectedIndex != null,
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
                    Text(
                        text = "SELECT",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}