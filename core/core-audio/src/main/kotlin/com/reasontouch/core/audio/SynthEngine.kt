package com.reasontouch.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Android AudioTrack-based synthesis engine.
 * Renders PCM samples in a coroutine and plays via AudioTrack static mode
 * for low-latency note preview (piano key audition, chord tap).
 *
 * For sequenced playback, notes are scheduled with delay offsets.
 */
class SynthEngine {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val SAMPLE_RATE = 44100

    /**
     * Play a single note immediately — used for key audition.
     */
    fun playNote(
        midiNote: Int,
        durationSec: Float = 0.4f,
        velocity: Int = 100,
        voice: SynthVoice = SynthVoice.SAW
    ) {
        scope.launch {
            try {
                val samples  = SampleRenderer.render(midiNote, durationSec, velocity, voice)
                val pcm      = floatToShort(samples)
                val minBuf   = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufSize  = maxOf(minBuf, pcm.size * 2)
                val track    = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                    .build()

                track.write(pcm, 0, pcm.size)
                track.play()

                // Release after playback completes
                val durationMs = ((durationSec + 0.5f) * 1000).toLong()
                kotlinx.coroutines.delay(durationMs)
                track.stop()
                track.release()
            } catch (e: Exception) {
                // Silently ignore audio errors — app should never crash on audio failure
            }
        }
    }

    /**
     * Schedule multiple notes with timing offsets — used for chord playback.
     */
    fun playChord(
        midiNotes: List<Int>,
        durationSec: Float = 0.5f,
        velocity: Int = 100,
        voice: SynthVoice = SynthVoice.SAW,
        strumDelayMs: Long = 0L
    ) {
        midiNotes.forEachIndexed { i, note ->
            scope.launch {
                if (strumDelayMs > 0) kotlinx.coroutines.delay(i * strumDelayMs)
                playNote(note, durationSec, velocity, voice)
            }
        }
    }

    /**
     * Convert Float PCM [-1,1] to Short PCM for AudioTrack.
     */
    private fun floatToShort(samples: FloatArray): ShortArray {
        return ShortArray(samples.size) { i ->
            (samples[i].coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
        }
    }

    fun release() {
        scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
    }
}