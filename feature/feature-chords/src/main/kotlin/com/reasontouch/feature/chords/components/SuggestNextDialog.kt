package com.reasontouch.feature.chords.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
                    skin.panel,
                    RoundedCornerShape(12.dp)
                )
                .padding(20.dp)
        ) {
            // Title (in red/accent color)
            Text(
                text = "SUGGEST NEXT",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = skin.accent,
                modifier = Modifier.padding(bottom = 20.dp)
            )
            
            // Intent type
            Text(
                text = decision.type.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = skin.textPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Rationale
            Text(
                text = decision.rationale,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = skin.textSecondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Confidence/bars info
            Text(
                text = "Suggested: ${decision.suggestedBars} bars (${(decision.confidence * 100).toInt()}% confident)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = skin.textMuted,
                modifier = Modifier.padding(bottom = 20.dp)
            )
            
            // Section header
            Text(
                text = "Suggested continuations",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = skin.textSecondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Option buttons with red outline
            options.forEachIndexed { index, option ->
                Button(
                    onClick = { onOptionSelected(index) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .padding(bottom = 8.dp)
                        .border(1.dp, skin.accent, RoundedCornerShape(8.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = skin.panel,
                        contentColor = skin.accent
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = option,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Bottom action buttons
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Cancel button (left)
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
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
                
                // Confirm button (right, in accent red)
                Button(
                    onClick = { if (options.isNotEmpty()) onOptionSelected(0) },
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