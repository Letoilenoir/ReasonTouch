package com.reasontouch.feature.chords.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.reasontouch.core.ui.theme.ReasonTouchTheme
import com.reasontouch.feature.chords.PairingDecision

@Composable
fun SuggestNextDialog(
    decision: PairingDecision,
    options: List<String>,
    onOptionSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val skin = ReasonTouchTheme.skin

    Dialog(onDismissRequest = onDismiss) {

        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(
                    color = skin.panel,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(20.dp)
        ) {

            // Title
            Text(
                text = "SUGGEST NEXT",
                color = skin.accent,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Pairing type
            Text(
                text = decision.type.name,
                color = skin.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Explanation
            Text(
                text = decision.rationale,
                color = skin.textSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Confidence
            Text(
                text = "Suggested: ${decision.suggestedBars} bars (${(decision.confidence * 100).toInt()}% confident)",
                color = skin.textMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Section heading
            Text(
                text = "Suggested continuations",
                color = skin.textSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Phrase options
            options.forEachIndexed { index, option ->

                Button(
                    onClick = { onOptionSelected(index) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = skin.panelAlt,
                        contentColor = skin.textPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = option,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .padding(end = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = skin.panelAlt,
                        contentColor = skin.textSecondary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "CANCEL",
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = {
                        if (options.isNotEmpty()) {
                            onOptionSelected(0)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = skin.accent,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
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