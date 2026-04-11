package com.reasontouch.core.audio

import android.content.res.AssetManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.pow

class Sf2Player(assetManager: AssetManager) {

    private val scope  = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val parser = Sf2Parser(assetManager)
    private val OUTPUT_SAMPLE_RATE = 44100
    private val RELEASE_SEC = 0.08f

    private val activeTracks = mutableListOf<AudioTrack>()
    private val trackLock    = Any()

    fun playNote(
        midiNote:    Int,
        durationSec: Float = 0.4f,
        velocity:    Int   = 100,
        gmProgram:   Int   = 0
    ) {
        scope.launch(Dispatchers.Default) {
            try {
                val pcm   = render(midiNote, durationSec, velocity, gmProgram) ?: return@launch
                val track = buildTrack(pcm.size)
                track.write(pcm, 0, pcm.size)
                synchronized(trackLock) { activeTracks.add(track) }
                track.play()
                val durationMs = (pcm.size.toLong() * 1000L / OUTPUT_SAMPLE_RATE) + 50L
                delay(durationMs)
                track.stop()
                track.release()
                synchronized(trackLock) { activeTracks.remove(track) }
            } catch (e: Exception) { }
        }
    }

    fun playChord(
        midiNotes:    List<Int>,
        durationSec:  Float = 0.5f,
        velocity:     Int   = 100,
        gmProgram:    Int   = 0,
        strumDelayMs: Long  = 0L
    ) {
        midiNotes.forEachIndexed { i, note ->
            scope.launch {
                if (strumDelayMs > 0) delay(i * strumDelayMs)
                playNote(note, durationSec, velocity, gmProgram)
            }
        }
    }

    private fun render(
        midiNote:    Int,
        durationSec: Float,
        velocity:    Int,
        gmProgram:   Int
    ): ShortArray? {
        val (header, rawSamples) = parser.findSample(gmProgram, midiNote) ?: return null

        android.util.Log.d("SF2DEBUG", "render: dur=${"%.3f".format(durationSec)}s " +
            "hasLoop=${header.hasLoop} loopStart=${header.relLoopStart} " +
            "loopEnd=${header.relLoopEnd} rawSize=${rawSamples.size}")

        val semitones  = midiNote - header.originalPitch
        val pitchRatio = 2.0.pow(semitones / 12.0).toFloat() *
                         (header.sampleRate.toFloat() / OUTPUT_SAMPLE_RATE)

        val totalSec       = durationSec + RELEASE_SEC
        val outputSamples  = (totalSec * OUTPUT_SAMPLE_RATE).toInt()
        val sustainSamples = (durationSec * OUTPUT_SAMPLE_RATE).toInt()

        val out      = ShortArray(outputSamples)
        val velScale = (velocity / 127f).coerceIn(0f, 1f)

        val hasLoop      = header.hasLoop
        val loopStartRel = header.relLoopStart
        val loopEndRel   = header.relLoopEnd
        val loopLen      = (loopEndRel - loopStartRel).coerceAtLeast(1)

        var srcPos = 0.0

        for (i in 0 until outputSamples) {
            val idx0 = srcPos.toInt()
            val frac = (srcPos - idx0).toFloat()

            val actualIdx = if (hasLoop && idx0 >= loopEndRel) {
                loopStartRel + ((idx0 - loopStartRel) % loopLen)
            } else {
                idx0
            }

            if (actualIdx >= rawSamples.size) break

            val s0     = rawSamples[actualIdx].toFloat()
            val s1     = if (actualIdx + 1 < rawSamples.size)
                             rawSamples[actualIdx + 1].toFloat() else s0
            val sample = s0 + frac * (s1 - s0)
            val env    = envelope(i, sustainSamples, outputSamples)

            out[i] = (sample * env * velScale)
                .toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

            srcPos += pitchRatio
        }

        return out
    }

    private fun envelope(sampleIndex: Int, sustainEnd: Int, totalSamples: Int): Float {
        val attackSamples = (0.005f * OUTPUT_SAMPLE_RATE).toInt()
        val decaySamples  = (0.010f * OUTPUT_SAMPLE_RATE).toInt()
        val sustainLevel  = 0.85f

        return when {
            sampleIndex < attackSamples ->
                sampleIndex.toFloat() / attackSamples
            sampleIndex < attackSamples + decaySamples -> {
                val t = (sampleIndex - attackSamples).toFloat() / decaySamples
                1f - t * (1f - sustainLevel)
            }
            sampleIndex < sustainEnd -> sustainLevel
            else -> {
                val releaseSamples = (totalSamples - sustainEnd).coerceAtLeast(1)
                val t = (sampleIndex - sustainEnd).toFloat() / releaseSamples
                sustainLevel * (1f - t)
            }
        }
    }

    private fun buildTrack(sampleCount: Int): AudioTrack {
        val minBuf  = AudioTrack.getMinBufferSize(
            OUTPUT_SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufSize = maxOf(minBuf, sampleCount * 2)
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(OUTPUT_SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()
    }

    fun release() {
        synchronized(trackLock) {
            activeTracks.forEach { runCatching { it.stop(); it.release() } }
            activeTracks.clear()
        }
        scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
    }
}