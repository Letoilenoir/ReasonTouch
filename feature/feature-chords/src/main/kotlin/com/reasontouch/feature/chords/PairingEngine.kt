package com.reasontouch.feature.chords



/**
 * PairingEngine: Suggests what section type should follow a progression.
 * Input: ONE ProgressionAnalysis
 * Output: PairingDecision (Continue, Lift, Contrast, Resolve, etc.)
 */
object PairingEngine {

    fun suggestNext(
        currentAnalysis: ProgressionAnalysis,
        history: List<PairingType> = emptyList()
    ): PairingDecision {
        val stability = currentAnalysis.harmonicStability
        val cadenceType = currentAnalysis.cadenceType
        val endingFunction = currentAnalysis.endingFunction
        val tension = currentAnalysis.tension
        val barCount = currentAnalysis.barCount

        return when {
            stability >= 0.8f -> {
                suggestAfterHighStability(
                    cadenceType = cadenceType,
                    history = history,
                    tension = tension,
                    barCount = barCount
                )
            }

            stability in 0.4f..0.7f -> {
                suggestAfterMediumStability(
                    cadenceType = cadenceType,
                    tension = tension,
                    barCount = barCount,
                    history = history
                )
            }

            else -> {
                suggestAfterLowStability(
                    endingFunction = endingFunction,
                    tension = tension,
                    barCount = barCount
                )
            }
        }
    }

    private fun suggestAfterHighStability(
        cadenceType: CadenceType,
        history: List<PairingType>,
        tension: Float,
        barCount: Int
    ): PairingDecision {
        val primaryCount = history.count { it in listOf(PairingType.CONTINUE, PairingType.EXPAND) }
        
        return when {
            cadenceType == CadenceType.AUTHENTIC && primaryCount >= 2 -> {
                if (history.count { it == PairingType.CONTINUE } > 2) {
                    PairingDecision(
                        type = PairingType.SIMPLIFY,
                        suggestedBars = 8,
                        confidence = 0.85f,
                        rationale = "High stability + multiple verses → pull back energy"
                    )
                } else {
                    PairingDecision(
                        type = PairingType.CONTRAST,
                        suggestedBars = 8,
                        confidence = 0.90f,
                        rationale = "Authentic cadence → natural point for variation"
                    )
                }
            }

            tension > 0.6f -> {
                PairingDecision(
                    type = PairingType.LIFT,
                    suggestedBars = 8,
                    confidence = 0.80f,
                    rationale = "High stability + tension → energetic lift"
                )
            }

            else -> {
                PairingDecision(
                    type = PairingType.CONTRAST,
                    suggestedBars = 8,
                    confidence = 0.85f,
                    rationale = "High stability → ready for variation"
                )
            }
        }
    }

    private fun suggestAfterMediumStability(
        cadenceType: CadenceType,
        tension: Float,
        barCount: Int,
        history: List<PairingType>
    ): PairingDecision {
        return when {
            cadenceType == CadenceType.DECEPTIVE -> {
                PairingDecision(
                    type = PairingType.SURPRISE,
                    suggestedBars = 8,
                    confidence = 0.75f,
                    rationale = "Deceptive cadence → unexpected turn"
                )
            }

            cadenceType == CadenceType.HALF -> {
                PairingDecision(
                    type = PairingType.EXPAND,
                    suggestedBars = 8,
                    confidence = 0.80f,
                    rationale = "Ends on V → explore harmonically"
                )
            }

            barCount <= 4 -> {
                PairingDecision(
                    type = PairingType.CONTINUE,
                    suggestedBars = 8,
                    confidence = 0.70f,
                    rationale = "Short phrase + medium stability → continue"
                )
            }

            else -> {
                PairingDecision(
                    type = PairingType.EXPAND,
                    suggestedBars = 8,
                    confidence = 0.75f,
                    rationale = "Medium stability → harmonic exploration"
                )
            }
        }
    }

    private fun suggestAfterLowStability(
        endingFunction: HarmonicFunction,
        tension: Float,
        barCount: Int
    ): PairingDecision {
        return when {
            endingFunction == HarmonicFunction.DOMINANT -> {
                PairingDecision(
                    type = PairingType.RESOLVE,
                    suggestedBars = 4,
                    confidence = 0.95f,
                    rationale = "Ends on V → resolution needed"
                )
            }

            endingFunction == HarmonicFunction.PREDOMINANT -> {
                PairingDecision(
                    type = PairingType.RESOLVE,
                    suggestedBars = 4,
                    confidence = 0.75f,
                    rationale = "Unresolved and off the tonic → resolution needed"
                )
            }

            tension > 0.7f -> {
                PairingDecision(
                    type = PairingType.CONTINUE,
                    suggestedBars = 8,
                    confidence = 0.85f,
                    rationale = "Low stability + high tension → maintain momentum"
                )
            }

            else -> {
                PairingDecision(
                    type = PairingType.CONTINUE,
                    suggestedBars = 8,
                    confidence = 0.80f,
                    rationale = "Low stability → continuation needed"
                )
            }
        }
    }
}