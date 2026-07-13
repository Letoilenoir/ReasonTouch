package com.reasontouch.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class ReasonTouchSkin(
    // Backgrounds
    val background: Color,
    val rack: Color,
    val panel: Color,
    val panelAlt: Color,
    
    // Borders
    val border: Color,
    val borderStrong: Color,
    
    // Text
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    
    // Accents
    val accent: Color,
    val accentSecondary: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    
    // Track / Musical colours
    val chordTrack: Color,
    val bassTrack: Color,
    val melodyTrack: Color,
    val drumTrack: Color,
    
    // Selection states
    val selectedBackground: Color,
    val selectedBorder: Color,
    val selectedText: Color,
    
    // Dimensions
    val cornerRadiusSmall: Dp,      // For small elements (buttons, chips)
    val cornerRadiusMedium: Dp,     // For dialogs, cards
    val cornerRadiusLarge: Dp,      // For major containers
    
    val paddingXSmall: Dp,          // 4dp
    val paddingSmall: Dp,           // 8dp
    val paddingMedium: Dp,          // 12dp
    val paddingLarge: Dp,           // 16dp
    val paddingXLarge: Dp,          // 20dp
    
    val spacingSmall: Dp,           // 8dp (between elements)
    val spacingMedium: Dp,          // 12dp
    val spacingLarge: Dp,           // 16dp
    
    val borderWidth: Dp,            // 1dp (standard border)
    val borderWidthStrong: Dp,      // 2dp (emphasized border)
    
    val buttonHeight: Dp,           // 44dp
    val buttonCornerRadius: Dp      // 8dp
)