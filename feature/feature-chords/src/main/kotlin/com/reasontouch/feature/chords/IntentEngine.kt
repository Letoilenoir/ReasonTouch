package com.reasontouch.feature.chords

/**
 * Converts a harmonic analysis into ranked compositional intentions.
 *
 * This engine never generates music.
 * It only recommends possible creative directions.
 */
object IntentEngine {

    fun rank(
        analysis: ProgressionAnalysis
    ): List<IntentRanking> {

        val intents = mutableListOf<IntentRanking>()

        val stability = analysis.harmonicStability

        if (stability >= 0.8f) {

            intents += IntentRanking(
                CompositionIntent.CONTRAST,
                0.85f,
                "A stable ending leaves room for a contrasting idea."
            )

            intents += IntentRanking(
                CompositionIntent.LIFT,
                0.70f,
                "The current harmony could support increased energy."
            )

        } else if (stability >= 0.4f) {

            intents += IntentRanking(
                CompositionIntent.EXPAND,
                0.75f,
                "The harmony can naturally be developed further."
            )

            intents += IntentRanking(
                CompositionIntent.CONTINUE,
                0.70f,
                "The musical flow remains open."
            )

        } else {

            intents += IntentRanking(
                CompositionIntent.RESOLVE,
                0.95f,
                "The harmony suggests a strong sense of resolution."
            )

            intents += IntentRanking(
                CompositionIntent.CONTINUE,
                0.80f,
                "Continuation is another natural possibility."
            )
        }

        return intents.sortedByDescending { it.confidence }
    }
}