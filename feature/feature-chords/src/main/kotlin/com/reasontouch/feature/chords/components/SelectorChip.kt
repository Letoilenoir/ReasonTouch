package com.reasontouch.feature.chords.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reasontouch.core.ui.theme.ReasonTouchTheme
import androidx.compose.foundation.layout.padding

@Composable
fun SelectorChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val skin = ReasonTouchTheme.skin

    Box(
        modifier = Modifier
            .border(
                1.dp,
                if (selected) skin.selectedBorder else skin.border,
                RoundedCornerShape(8.dp)
            )
            .background(
                skin.panel,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(
                horizontal = 8.dp,
                vertical = 6.dp
            )
    ) {

        Text(
            text = text,
            color =
                if (selected)
                    skin.selectedBorder
                else
                    skin.textSecondary,
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}