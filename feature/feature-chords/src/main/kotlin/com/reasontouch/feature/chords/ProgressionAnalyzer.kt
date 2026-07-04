package com.reasontouch.feature.chords

import com.reasontouch.core.data.ChordEvent
import com.reasontouch.feature.chords.HarmonicFunction
import com.reasontouch.feature.chords.KeyCandidate
import com.reasontouch.feature.chords.MusicTheory

/**
 * ProgressionAnalyzer: Analyzes a single progression in isolation.
 */
object ProgressionAnalyzer {

    fun analyze(
        chords: List<ChordEvent>,
        detectedKey: KeyCandidate
    ): ProgressionAnalysis {
        if (chords.isEmpty()) {
            return emptyProgressionAnalysis(detectedKey)
        }

        val lastChord = chords.last()
        val lastTheoryChord = MusicTheory.parseChordName(lastChord.chordName)
            ?: return emptyProgressionAnalysis(detectedKey)

        val endingFunction =
            MusicTheory.degreeOf(lastTheoryChord, detectedKey.root, detectedKey.isMinor)
                ?.let { MusicTheory.function(it, detectedKey.isMinor) }
                ?: HarmonicFunction.TONIC

        val cadenceType = detectCadence(chords, detectedKey)
        val harmonicStability = calculateHarmonicStability(
            chords = chords,
            detectedKey = detectedKey,
            cadenceType = cadenceType,
            endingFunction = endingFunction
        )

        val energy = calculateEnergy(chords, detectedKey)
        val tension = calculateTension(chords, detectedKey)
        val rootMovement = calculateRootMovement(chords)

        return ProgressionAnalysis(
            key = detectedKey,
            cadenceType = cadenceType,
            harmonicStability = harmonicStability,
            energy = energy,
            tension = tension,
            endingFunction = endingFunction,
            rootMovementIntervals = rootMovement,
            barCount = chords.size,
            confidence = detectedKey.confidence
        )
    }

    private fun calculateHarmonicStability(
        chords: List<ChordEvent>,
        detectedKey: KeyCandidate,
        cadenceType: CadenceType,
        endingFunction: HarmonicFunction
    ): Float {
        val functionStability = when (endingFunction) {
            HarmonicFunction.TONIC -> 0.95f
            HarmonicFunction.PREDOMINANT -> 0.45f
            HarmonicFunction.DOMINANT -> 0.15f
        }

        val cadenceBonus = when (cadenceType) {
            CadenceType.AUTHENTIC -> 0.05f
            CadenceType.PLAGAL -> 0.03f
            CadenceType.DECEPTIVE -> -0.15f
            CadenceType.HALF -> -0.10f
            CadenceType.INTERRUPTED -> -0.05f
            CadenceType.OPEN -> -0.20f
        }

        val tonicCount = chords.count { chord ->
            val theory = MusicTheory.parseChordName(chord.chordName)
            theory?.let {
                MusicTheory.degreeOf(it, detectedKey.root, detectedKey.isMinor)
            } == 1
        }
        val functionDistributionBonus = (tonicCount.toFloat() / chords.size.toFloat()) * 0.15f

        val dominantCount = chords.count { chord ->
            val theory = MusicTheory.parseChordName(chord.chordName)
            theory?.let {
                MusicTheory.degreeOf(it, detectedKey.root, detectedKey.isMinor)
            } == 5
        }
        val dominantPenalty = if (dominantCount > 1) -0.1f else 0f

        val stability =
            (functionStability + cadenceBonus + functionDistributionBonus + dominantPenalty)
                .coerceIn(0f, 1f)

        return stability
    }

    private fun calculateEnergy(
        chords: List<ChordEvent>,
        detectedKey: KeyCandidate
    ): Float {
        if (chords.size <= 1) return 0.25f

        val density = (chords.size.toFloat() / chords.size.toFloat())
            .coerceIn(0f, 1f)

        val functionTransitions = chords.zipWithNext().count { (a, b) ->
            val funcA = getChordFunction(a, detectedKey)
            val funcB = getChordFunction(b, detectedKey)
            funcA != funcB
        }
        val transitionEnergy = (functionTransitions.toFloat() / chords.size.toFloat())
            .coerceIn(0f, 1f)

        val energy = (density * 0.4f + transitionEnergy * 0.6f)
            .coerceIn(0f, 1f)

        return energy
    }

    private fun calculateTension(
        chords: List<ChordEvent>,
        detectedKey: KeyCandidate
    ): Float {
        var tensionSum = 0f

        chords.forEach { chord ->
            val theory = MusicTheory.parseChordName(chord.chordName) ?: return@forEach

            val qualityTension = when (theory.quality) {
                com.reasontouch.feature.chords.ChordQuality.DIM -> 0.9f
                com.reasontouch.feature.chords.ChordQuality.AUG -> 0.8f
                com.reasontouch.feature.chords.ChordQuality.DOM7 -> 0.7f
                com.reasontouch.feature.chords.ChordQuality.MAJ7 -> 0.5f
                com.reasontouch.feature.chords.ChordQuality.MIN7 -> 0.5f
                else -> 0.3f
            }

            val function = MusicTheory.degreeOf(theory, detectedKey.root, detectedKey.isMinor)
                ?.let { MusicTheory.function(it, detectedKey.isMinor) }
                ?: HarmonicFunction.TONIC

            val functionTension = when (function) {
                HarmonicFunction.DOMINANT -> 0.8f
                HarmonicFunction.PREDOMINANT -> 0.4f
                HarmonicFunction.TONIC -> 0.2f
            }

            tensionSum += (qualityTension + functionTension) / 2f
        }

        return (tensionSum / chords.size).coerceIn(0f, 1f)
    }

    private fun calculateRootMovement(chords: List<ChordEvent>): List<Int> {
        if (chords.size <= 1) return emptyList()

        return chords.zipWithNext().map { (a, b) ->
            val rootA = MusicTheory.parseChordName(a.chordName)?.root?.semitone ?: return@map 0
            val rootB = MusicTheory.parseChordName(b.chordName)?.root?.semitone ?: return@map 0

            val interval = rootB - rootA
            when {
                interval > 6 -> interval - 12
                interval < -6 -> interval + 12
                else -> interval
            }
        }
    }

    private fun detectCadence(
        chords: List<ChordEvent>,
        detectedKey: KeyCandidate
    ): CadenceType {
        if (chords.size < 2) return CadenceType.OPEN

        val lastTwo = chords.takeLast(2)
        val lastChord = lastTwo.last()
        val secondLast = lastTwo.first()

        val lastTheory = MusicTheory.parseChordName(lastChord.chordName) ?: return CadenceType.OPEN
        val secondLastTheory =
            MusicTheory.parseChordName(secondLast.chordName) ?: return CadenceType.OPEN

        val lastDegree = MusicTheory.degreeOf(lastTheory, detectedKey.root, detectedKey.isMinor)
            ?: return CadenceType.OPEN
        val secondLastDegree =
            MusicTheory.degreeOf(secondLastTheory, detectedKey.root, detectedKey.isMinor)
                ?: return CadenceType.OPEN

        return when {
            secondLastDegree == 5 && lastDegree == 1 -> CadenceType.AUTHENTIC
            secondLastDegree == 4 && lastDegree == 1 -> CadenceType.PLAGAL
            secondLastDegree == 5 && lastDegree == 6 -> CadenceType.DECEPTIVE
            lastDegree == 5 -> CadenceType.HALF
            else -> CadenceType.INTERRUPTED
        }
    }

    private fun getChordFunction(
        chord: ChordEvent,
        detectedKey: KeyCandidate
    ): HarmonicFunction {
        val theory = MusicTheory.parseChordName(chord.chordName) ?: return HarmonicFunction.TONIC
        val degree = MusicTheory.degreeOf(theory, detectedKey.root, detectedKey.isMinor)
            ?: return HarmonicFunction.TONIC
        return MusicTheory.function(degree, detectedKey.isMinor)
    }

    private fun emptyProgressionAnalysis(key: KeyCandidate): ProgressionAnalysis {
        return ProgressionAnalysis(
            key = key,
            cadenceType = CadenceType.OPEN,
            harmonicStability = 0.5f,
            energy = 0.3f,
            tension = 0.3f,
            endingFunction = HarmonicFunction.TONIC,
            rootMovementIntervals = emptyList(),
            barCount = 0,
            confidence = 0.3f
        )
    }

}