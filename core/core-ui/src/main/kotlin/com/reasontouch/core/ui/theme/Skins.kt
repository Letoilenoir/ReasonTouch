package com.reasontouch.core.ui.theme

import androidx.compose.ui.graphics.Color

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
    selectedText = Color.White
)

