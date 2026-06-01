package com.reasontouch.core.ui.theme

import androidx.compose.ui.graphics.Color

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
    val selectedText: Color
)

