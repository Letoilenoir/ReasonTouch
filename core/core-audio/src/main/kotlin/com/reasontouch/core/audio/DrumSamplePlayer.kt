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
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DrumSamplePlayer @Inject constructor(
    private val assetManager: AssetManager,
    private val soundPackManager: SoundPackManager
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val OUTPUT_SAMPLE_RATE = 44100

    private val buffers = mutableMapOf<Int, ShortArray>()

    private val NOTE_TO_FILE = mapOf(
        36 to "kick.wav",
        38 to "snare.wav",
        39 to "clap.wav",
        42 to "hihat_closed.wav",
        46 to "hihat_open.wav",
        51 to "ride.wav"
    )

    init {
        scope.launch(Dispatchers.IO) {
            NOTE_TO_FILE.forEach { (note, filename) ->
                try {
                    val pcm = loadWavAsset("drums/$filename")
                    if (pcm != null) {
                        buffers[note] = pcm
                    }
                } catch (e: Exception) { }
            }
        }
    }

    fun play(gmNote: Int, velocity: Int = 100, packId: String = "default") {
        val resolvedNote = when {
            buffers.containsKey(gmNote) -> gmNote
            gmNote in listOf(41, 43, 45, 47, 48, 50) -> 38
            gmNote == 49 -> 51
            else -> 38
        }

        scope.launch(Dispatchers.IO) {
            try {
                val pcm = if (packId != "default") {
                    val manifest = soundPackManager.getManifest(packId)
                    var drumPath: String? = null
                    manifest?.tracks?.values?.forEach { trackCfg ->
                        val path = trackCfg.mapping?.get("$resolvedNote") ?: trackCfg.rootKeys?.get("$resolvedNote")
                        if (path != null) drumPath = path
                    }

                    val candidateFiles = mutableListOf<File>()
                    if (drumPath != null) {
                        candidateFiles.add(soundPackManager.getSampleFile(packId, drumPath))
                    }
                    val filename = NOTE_TO_FILE[resolvedNote] ?: "snare.wav"
                    candidateFiles.add(soundPackManager.getSampleFile(packId, "drums/$filename"))
                    candidateFiles.add(soundPackManager.getSampleFile(packId, "samples/$filename"))
                    candidateFiles.add(soundPackManager.getSampleFile(packId, filename))

                    val customFile = candidateFiles.firstOrNull { it.exists() }
                    if (customFile != null) loadWavFile(customFile) else buffers[resolvedNote]
                } else {
                    buffers[resolvedNote]
                } ?: return@launch

                val velScale = (velocity / 127f).coerceIn(0f, 1f)
                val scaled = ShortArray(pcm.size) { i ->
                    (pcm[i] * velScale).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        .toShort()
                }

                val minBuf = AudioTrack.getMinBufferSize(
                    OUTPUT_SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufSize = maxOf(minBuf, scaled.size * 2)

                val track = AudioTrack.Builder()
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

                track.write(scaled, 0, scaled.size)
                track.play()

                val durationMs = (scaled.size.toLong() * 1000L / OUTPUT_SAMPLE_RATE) + 50L
                delay(durationMs)
                track.stop()
                track.release()
            } catch (e: Exception) { }
        }
    }

    private fun loadWavAsset(assetPath: String): ShortArray? {
        return try {
            val bytes = assetManager.open(assetPath).use { stream ->
                val out = ByteArrayOutputStream()
                stream.copyTo(out)
                out.toByteArray()
            }
            parseWavBytes(bytes)
        } catch (e: Exception) {
            null
        }
    }

    private fun loadWavFile(file: File): ShortArray? {
        return try {
            val bytes = file.readBytes()
            parseWavBytes(bytes)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseWavBytes(bytes: ByteArray): ShortArray? {
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

        return if (sampleRate == OUTPUT_SAMPLE_RATE) {
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
    }

    fun release() {
        scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
        buffers.clear()
    }
}
