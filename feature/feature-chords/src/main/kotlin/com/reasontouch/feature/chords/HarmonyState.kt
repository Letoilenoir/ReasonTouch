package com.reasontouch.feature.chords

/**
 * UI state for the harmony suggestion panel.
 * Held in ChordViewModel, consumed by ChordScreen.
 */
data class HarmonyState(
    val isAnalysing:    Boolean               = false,
    val keyCandidates:  List<KeyCandidate>    = emptyList(),
    val selectedKey:    KeyCandidate?         = null,
    val suggestions:    List<ChordSuggestion> = emptyList(),
    val showPanel:      Boolean               = false
) {
    val hasResults    get() = keyCandidates.isNotEmpty()
    val hasSuggestions get() = suggestions.isNotEmpty()
}