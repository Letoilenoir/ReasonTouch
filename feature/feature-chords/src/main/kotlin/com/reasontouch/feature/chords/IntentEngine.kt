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

            intents += IntentRanking(intent = CompositionIntent.CONTRAST, confidence = 0.85f, rationale = "A stable ending leaves room for a contrasting idea.")

            intents += IntentRanking(intent = CompositionIntent.LIFT, confidence = 0.70f, rationale = "The current harmony could support increased energy.")

        } else if (stability >= 0.4f) {

            intents += IntentRanking(intent = CompositionIntent.EXPAND, confidence = 0.75f, rationale = "The harmony can naturally be developed further.")

            intents += IntentRanking(intent = CompositionIntent.CONTINUE, confidence = 0.70f, rationale = "The musical flow remains open.")

        } else {

            intents += IntentRanking(intent = CompositionIntent.RESOLVE, confidence = 0.95f, rationale = "The harmony suggests a strong sense of resolution.")

            intents += IntentRanking(intent = CompositionIntent.CONTINUE, confidence = 0.80f, rationale = "Continuation is another natural possibility.")
        }

        return intents.sortedByDescending { it.confidence }
    }
}