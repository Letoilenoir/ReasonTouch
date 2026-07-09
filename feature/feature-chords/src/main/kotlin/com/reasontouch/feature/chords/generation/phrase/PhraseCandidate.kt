package com.reasontouch.feature.chords.generation.phrase

/**
 * A complete candidate phrase offered by the Composer.
 *
 * Unlike a single chord suggestion, a PhraseCandidate
 * represents an entire musical continuation together with
 * an explanation of its purpose.
 */
data class PhraseCandidate(

    /**
     * Display name shown to the user.
     *
     * Examples:
     *  • Resolve Naturally
     *  • Echo the Phrase
     *  • Extend the Journey
     */
    val title: String,

    /**
     * Human-readable explanation.
     *
     * This explanation should be adaptable to the user's
     * selected guidance level (Beginner → Expert).
     */
    val explanation: String,

    /**
     * Complete continuation phrase.
     *
     * Initially this is represented simply as chord names.
     * Later this may become a richer musical structure.
     */
    val chords: List<String>,

    /**
     * Relative confidence (0.0–1.0).
     *
     * Used for ordering rather than absolute correctness.
     */
    val confidence: Float
)