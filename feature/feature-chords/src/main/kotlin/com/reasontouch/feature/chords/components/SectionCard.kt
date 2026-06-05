package com.reasontouch.feature.chords.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .background(Color(0xFF2a2a2a), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        // Section title
        Text(
            text = title,
            fontSize = 12.sp,
            color = Color(0xFFaaaaaa),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Content
        content()
    }
}