package com.reasontouch.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val ModeBarBackground = Color(0xFF202024)
val ManualModeColor   = Color(0xFF7A7A88)
val AssistedModeColor = Color(0xFFE84040)
val GuidedModeColor   = Color(0xFF8B5CF6)
val ModeChipBackground = Color(0xFF2A2A32)

val NeoDarkSkin = ReasonTouchSkin(
    // Backgrounds
    background = Color(0xFF1A1A1E),
    rack = Color(0xFF222228),
    panel = Color(0xFF2A2A32),
    panelAlt = Color(0xFF343440),
    
    // Borders
    border = Color(0xFF3A3A45),
    borderStrong = Color(0xFF555566),
    
    // Text
    textPrimary = Color(0xFFC8C8D4),
    textSecondary = Color(0xFF9999AA),
    textMuted = Color(0xFF666675),
    
    // Accents
    accent = Color(0xFFE84040),
    accentSecondary = Color(0xFFFF6B35),
    success = Color(0xFF3DDC84),
    warning = Color(0xFFFFB84D),
    danger = Color(0xFFE84040),
    
    // Musical colours
    chordTrack = Color(0xFFA78BFA),
    bassTrack = Color(0xFF3DDC84),
    melodyTrack = Color(0xFF60A5FA),
    drumTrack = Color(0xFFFFB84D),
    
    // Selection
    selectedBackground = Color(0xFFA78BFA).copy(alpha = 0.15f),
    selectedBorder = Color(0xFFA78BFA),
    selectedText = Color.White,
    
    // Dimensions
    cornerRadiusSmall = 8.dp,
    cornerRadiusMedium = 12.dp,
    cornerRadiusLarge = 16.dp,
    
    paddingXSmall = 4.dp,
    paddingSmall = 8.dp,
    paddingMedium = 12.dp,
    paddingLarge = 16.dp,
    paddingXLarge = 20.dp,
    
    spacingSmall = 8.dp,
    spacingMedium = 12.dp,
    spacingLarge = 16.dp,
    
    borderWidth = 1.dp,
    borderWidthStrong = 2.dp,
    
    buttonHeight = 44.dp,
    buttonCornerRadius = 8.dp
)