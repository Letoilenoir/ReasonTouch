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
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Sf2Player @Inject constructor(
    assetManager: AssetManager,
    private val soundPackManager: SoundPackManager
) {

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
        gmProgram:   Int   = 0,
        packId:      String = "default"
    ) {
        scope.launch(Dispatchers.Default) {
            try {
                val pcm   = render(midiNote, durationSec, velocity, gmProgram, packId) ?: return@launch
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
        strumDelayMs: Long  = 0L,
        packId:       String = "default"
    ) {
        midiNotes.forEachIndexed { i, note ->
            scope.launch {
                if (strumDelayMs > 0) delay(i * strumDelayMs)
                playNote(note, durationSec, velocity, gmProgram, packId)
            }
        }
    }

    private fun render(
        midiNote:    Int,
        durationSec: Float,
        velocity:    Int,
        gmProgram:   Int,
        packId:      String = "default"
    ): ShortArray? {
        val effectivePackId = if (packId == "default") soundPackManager.activePackId else packId
        val resolution = soundPackManager.resolveSample(effectivePackId, "multisample", midiNote)

        val (header, rawSamples) = when (resolution) {
            is SampleResolution.Resolved -> {
                val pcm = loadWavFile(resolution.file)
                if (pcm != null) {
                    val mockHeader = Sf2Parser.SampleHeader(
                        name = "custom",
                        start = 0,
                        end = pcm.size,
                        loopStart = 0,
                        loopEnd = 0,
                        sampleRate = 44100,
                        originalPitch = midiNote,
                        pitchCorrection = 0,
                        sampleType = 1
                    )
                    Pair(mockHeader, pcm)
                } else {
                    android.util.Log.e("Sf2Player", "[SampleResolution] DecodeFailed for file: ${resolution.file.absolutePath}")
                    parser.findSample(gmProgram, midiNote)
                }
            }
            is SampleResolution.MissingMapping -> {
                android.util.Log.w("Sf2Player", "[SampleResolution] MissingMapping for MIDI note $midiNote in pack '$effectivePackId'. Falling back to default soundfont.")
                parser.findSample(gmProgram, midiNote)
            }
            is SampleResolution.MissingFile -> {
                android.util.Log.e("Sf2Player", "[SampleResolution] MissingFile at path '${resolution.path}'. Falling back to default soundfont.")
                parser.findSample(gmProgram, midiNote)
            }
            is SampleResolution.DecodeFailed -> {
                android.util.Log.e("Sf2Player", "[SampleResolution] DecodeFailed at '${resolution.path}': ${resolution.reason}")
                parser.findSample(gmProgram, midiNote)
            }
            is SampleResolution.NoActivePack -> {
                parser.findSample(gmProgram, midiNote)
            }
        } ?: return null

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

    private fun loadWavFile(file: File): ShortArray? {
        return try {
            val bytes = file.readBytes()
            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val riff = String(bytes, 0, 4)
            if (riff != "RIFF") return null

            buf.position(12)
            var numChannels = 1
            var sampleRate = 44100
            var bitsPerSample = 16
            var dataStart = 0
            var dataSize = 0

            while (buf.remaining() >= 8) {
                val chunkId = String(bytes, buf.position(), 4)
                buf.position(buf.position() + 4)
                val chunkSize = buf.int

                when (chunkId) {
                    "fmt " -> {
                        buf.short
                        numChannels = buf.short.toInt() and 0xFFFF
                        sampleRate = buf.int
                        buf.int
                        buf.short
                        bitsPerSample = buf.short.toInt() and 0xFFFF
                        val extra = chunkSize - 16
                        if (extra > 0) buf.position(buf.position() + extra)
                    }
                    "data" -> {
                        dataStart = buf.position()
                        dataSize = chunkSize
                        break
                    }
                    else -> {
                        buf.position((buf.position() + chunkSize).coerceAtMost(buf.limit()))
                    }
                }
            }

            if (dataStart == 0 || dataSize == 0) return null

            val rawSamples = when (bitsPerSample) {
                16 -> ShortArray(dataSize / 2) { buf.short }
                8 -> ShortArray(dataSize) { ((bytes[dataStart + it].toInt() and 0xFF) - 128).shl(8).toShort() }
                else -> return null
            }

            val monoSamples = if (numChannels == 2) {
                ShortArray(rawSamples.size / 2) { i ->
                    ((rawSamples[i * 2].toInt() + rawSamples[i * 2 + 1].toInt()) / 2).toShort()
                }
            } else rawSamples

            if (sampleRate == OUTPUT_SAMPLE_RATE) {
                monoSamples
            } else {
                val ratio = sampleRate.toDouble() / OUTPUT_SAMPLE_RATE
                val outLen = (monoSamples.size / ratio).toInt()
                ShortArray(outLen) { i ->
                    val srcPos = i * ratio
                    val idx0 = srcPos.toInt().coerceAtMost(monoSamples.size - 1)
                    val idx1 = (idx0 + 1).coerceAtMost(monoSamples.size - 1)
                    val frac = (srcPos - idx0).toFloat()
                    (monoSamples[idx0] * (1f - frac) + monoSamples[idx1] * frac).toInt().toShort()
                }
            }
        } catch (e: Exception) {
            null
        }
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
            .also { builder ->
                if (android.os.Build.VERSION.SDK_INT >= 26) {
                    builder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                }
            }
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
