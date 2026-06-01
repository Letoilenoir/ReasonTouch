package com.reasontouch.feature.chords.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reasontouch.core.ui.theme.ReasonTouchTheme
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment

@Composable
fun StartingPointCard(
    title: String,
    subtitle: String,
    icon: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    val skin = ReasonTouchTheme.skin

    Column(
        modifier = Modifier
            .width(82.dp)
            .border(
                width = 1.dp,
                color =
                    if (selected)
                        skin.selectedBorder
                    else
                        skin.border,
                shape = RoundedCornerShape(14.dp)
            )
            .background(
                color = skin.panel,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp),

                verticalArrangement = Arrangement.Top
    ) {

        Text(
            text = title,
            color = skin.textPrimary,
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Text(
                text = subtitle,
                color = skin.textSecondary,
                fontSize = 5.sp,
                lineHeight = 6.sp,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .size(18.dp)
                    .border(
                        1.dp,
                        skin.border,
                        RoundedCornerShape(6.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = icon,
                    fontSize = 8.sp
                )
            }
        }
    }
}