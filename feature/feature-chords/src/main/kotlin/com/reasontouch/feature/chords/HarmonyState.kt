package com.reasontouch.feature.chords

data class HarmonyState(
    val isAnalysing:    Boolean               = false,
    val keyCandidates:  List<KeyCandidate>    = emptyList(),
    val selectedKey:    KeyCandidate?         = null,
    val suggestions:    List<ChordSuggestion> = emptyList(),
    val borrowedChords: List<BorrowedChord>   = emptyList(),
    val showPanel:      Boolean               = false,
    val moodBias:       Float                 = 0f   // -1 dark .. 0 neutral .. +1 bright
) {
    val hasResults     get() = keyCandidates.isNotEmpty()
    val hasSuggestions get() = suggestions.isNotEmpty()
    val hasBorrowed    get() = borrowedChords.isNotEmpty()
}