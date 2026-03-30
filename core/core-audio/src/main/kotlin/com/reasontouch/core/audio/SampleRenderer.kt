package com.reasontouch.core.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

/**
 * Renders PCM float samples for a given voice, pitch, duration and velocity.
 * Single-threaded, allocation-minimal rendering suitable for real-time use.
 */
object SampleRenderer {

    private const val SAMPLE_RATE = 44100
    private const val TWO_PI      = (2.0 * PI).toFloat()

    fun noteToFreq(midiNote: Int): Float {
        return (440.0 * Math.pow(2.0, (midiNote - 69) / 12.0)).toFloat()
    }

    fun render(
        midiNote: Int,
        durationSec: Float,
        velocity: Int,
        voice: SynthVoice
    ): FloatArray {
        val cfg         = voice.config
        val freq        = noteToFreq(midiNote)
        val totalFrames = ((durationSec + cfg.decay) * SAMPLE_RATE)
            .toInt().coerceAtMost(SAMPLE_RATE * 4)
        val samples     = FloatArray(totalFrames)
        val volume      = (velocity / 127f) * 0.28f

        val attackFrames = (cfg.attack * SAMPLE_RATE).toInt().coerceAtLeast(1)
        val decayFrames  = (cfg.decay  * SAMPLE_RATE).toInt().coerceAtLeast(1)
        val sustainEnd   = (durationSec * SAMPLE_RATE).toInt().coerceAtMost(totalFrames)

        // FM modulator state
        val fmFreq    = freq * cfg.fmRatio
        var fmPhase   = 0f
        val fmInc     = if (cfg.fmRatio > 0f) TWO_PI * fmFreq / SAMPLE_RATE else 0f

        var phase = 0f
        val phaseInc = TWO_PI * freq / SAMPLE_RATE

        // Simple one-pole low-pass filter state
        var filterState = 0f
        val filterCoeff = run {
            val rc = 1f / (TWO_PI * cfg.filterFreq)
            val dt = 1f / SAMPLE_RATE
            dt / (rc + dt)
        }

        for (i in 0 until totalFrames) {
            // Envelope
            val env = when {
                i < attackFrames  -> i.toFloat() / attackFrames
                i < sustainEnd    -> 1f
                else -> {
                    val decayPos = (i - sustainEnd).toFloat() / decayFrames
                    exp(-decayPos * 5f).coerceAtLeast(0f)
                }
            }

            // FM modulation
            val fmMod = if (cfg.fmRatio > 0f) {
                sin(fmPhase) * freq * cfg.fmDepth
                    .also { fmPhase = (fmPhase + fmInc) % TWO_PI }
            } else 0f

            val instPhaseInc = TWO_PI * (freq + fmMod) / SAMPLE_RATE

            // Oscillator
            val osc = when (cfg.oscType) {
                OscType.SINE     -> sin(phase)
                OscType.SQUARE   -> if (sin(phase) >= 0f) 1f else -1f
                OscType.SAWTOOTH -> (phase / PI.toFloat()) - 1f
                OscType.TRIANGLE -> (2f / PI.toFloat()) * (abs((phase % TWO_PI) - PI.toFloat()) - PI.toFloat() / 2f)
            }

            // Low-pass filter
            filterState += filterCoeff * (osc - filterState)

            samples[i] = filterState * env * volume
            phase = (phase + instPhaseInc) % TWO_PI
        }

        return samples
    }
}