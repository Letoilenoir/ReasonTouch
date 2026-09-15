package com.reasontouch.feature.chords

/**
 * A ranked suggestion paired with a one-line explanation of why it's
 * ranked where it is. Added 2026-09-07 for Assisted mode's rationale
 * display (Bass/Drum suggestions) -- see docs/design/Groove_Relationship_Model.md
 * and the 2026-09-07 memory notes on GUIDED's retirement as a persistent mode.
 */
data class RankedSuggestion<T>(
    val value: T,
    val rationale: String
)