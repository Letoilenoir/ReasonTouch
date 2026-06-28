package com.reasontouch.feature.chords.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.reasontouch.feature.chords.PairingDecision
import com.reasontouch.feature.chords.PairingType

@Composable
fun SuggestNextDialog(
    decision: PairingDecision,
    onAccept: (barCount: Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
                .padding(24.dp)
        ) {
            // Title
            Text(
                text = "🧠 SUGGEST NEXT",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Pairing type + description
            Text(
                text = decision.type.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = decision.rationale,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Suggested bars + confidence
            Text(
                text = "Suggested: ${decision.suggestedBars} bars (${(decision.confidence * 100).toInt()}% confident)",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(bottom = 24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Buttons
            Button(
                onClick = { onAccept(decision.suggestedBars) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("ADD ${decision.suggestedBars} BARS")
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors()
            ) {
                Text("CANCEL")
            }
        }
    }
}