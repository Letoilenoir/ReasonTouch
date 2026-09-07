package com.reasontouch.feature.chords

import com.reasontouch.core.data.ChordEvent
import com.reasontouch.core.data.Session
import com.reasontouch.core.midi.StepState

/**
 * CompositionContext — Phase 6a of the Bass/Drum Arrangement Roadmap.
 *
 * A minimal, derived read-model over an existing progression, assembled
 * fresh from Session + ChordEvent data already in the database. Not a
 * persisted entity itself -- everything here is either a direct field
 * pass-through or cheaply recomputed at call time.
 *
 * Deliberately minimal per Phase 6a's scope: only the fields Phases 6b/6c/
 * 7a/7b actually need (phrase boundaries, attack density, matched preset
 * name). See docs/design/Groove_Relationship_Model.md Section 6 for the
 * full phase ordering this builds toward.
 */
data class CompositionContext(
    val bpm: Int,
    val beatsPerBar: Float,
    val bars: List<BarContext>
)

data class BarContext(
    val chordEvent: ChordEvent,
    val attackDensity: Float,
    val matchedPatternName: String?,
    val isFirstInPhrase: Boolean,
    val isLastInPhrase: Boolean
)

object CompositionContextBuilder {

    /**
     * Assembles a CompositionContext from a Session and its chord
     * progression. Both are already fetched by ChordViewModel/
     * PianoRollViewModel today -- this performs no repository access
     * itself, keeping it a pure function over data the caller already has.
     */
    fun buildContext(session: Session?, chords: List<ChordEvent>, barDuration: Float = 4f): CompositionContext {
        val bpm = session?.bpm ?: 120
        val beatsPerBar = barDuration

        // Group bars sharing a phraseId to determine first/last-in-phrase,
        // per Phase 6a's Option A decision (phrase membership is persisted
        // on ChordEvent, not recomputed).
        val byPhrase: Map<String, List<ChordEvent>> = chords
            .filter { it.phraseId != null }
            .groupBy { it.phraseId!! }
            .mapValues { (_, bars) -> bars.sortedBy { it.barIndex } }

        val bars = chords.map { chord ->
            val phraseBars = chord.phraseId?.let { byPhrase[it] }
            BarContext(
                chordEvent = chord,
                attackDensity = attackDensity(chord),
                matchedPatternName = matchPatternName(chord),
                isFirstInPhrase = phraseBars?.firstOrNull()?.id == chord.id,
                isLastInPhrase = phraseBars?.lastOrNull()?.id == chord.id
            )
        }

        return CompositionContext(bpm = bpm, beatsPerBar = beatsPerBar, bars = bars)
    }

    /**
     * Derived Musical Properties, Phase 2 Section 2.1 -- canonical formula.
     * Proportion of a bar's 16 steps carrying an active strum attack.
     */
    private fun attackDensity(chord: ChordEvent): Float {
        val pattern = chord.strumPatternId.toStepPattern()
        return pattern.steps.count { it != StepState.OFF } / 16f
    }

    /**
     * Reverse-lookup against every known preset (Core + Guitar groups) by
     * STRUCTURAL step comparison, not StepPattern.equals() -- StepPattern's
     * equals() compares by `id`, and every pattern decoded via
     * StrumEncoding.toStepPattern() gets a fresh random UUID, so it would
     * never match StepPattern.EMPTY (or any preset) via `==` even when the
     * steps are identical. Comparing `.steps` directly avoids that latent
     * bug (also present, unexercised, in StrumPatternTray's getPatternName()).
     *
     * Returns "Clear" for an all-off pattern, matching the existing preset
     * list's own naming (StrumPatterns.groups["Core"]["Clear"] == EMPTY),
     * per this phase's decision that empty patterns are a valid, named
     * state, not an absence of one.
     */
    private fun matchPatternName(chord: ChordEvent): String? {
        val steps = chord.strumPatternId.toStepPattern().steps
        return StrumPatterns.all.entries
            .find { it.value.steps == steps }
            ?.key
    }
}