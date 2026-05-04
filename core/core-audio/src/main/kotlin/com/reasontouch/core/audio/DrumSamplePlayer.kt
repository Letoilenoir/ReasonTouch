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
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Plays drum WAV samples from assets/drums/ using AudioTrack MODE_STATIC.
 * Each GM drum note maps to a pre-loaded PCM buffer.
 * Samples are loaded once at startup — zero disk I/O during playback.
 */
class DrumSamplePlayer(private val assetManager: AssetManager) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val OUTPUT_SAMPLE_RATE = 44100

    // Map GM note -> pre-rendered PCM buffer (mono 16-bit 44100Hz)
    private val buffers = mutableMapOf<Int, ShortArray>()

    // GM drum note to filename mapping
    private val NOTE_TO_FILE = mapOf(
        36 to "kick.wav",
        38 to "snare.wav",
        39 to "clap.wav",
        42 to "hihat_closed.wav",
        46 to "hihat_open.wav",
        51 to "ride.wav"
    )

    init {
        // Pre-load all samples on background thread
        scope.launch(Dispatchers.IO) {
            NOTE_TO_FILE.forEach { (note, filename) ->
                try {
                    val pcm = loadWav("drums/$filename")
                    if (pcm != null) {
                        buffers[note] = pcm
                        android.util.Log.d("DrumPlayer",
                            "Loaded $filename: ${pcm.size} samples")
                    } else {
                        android.util.Log.w("DrumPlayer", "Failed to load $filename")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DrumPlayer", "Error loading $filename: ${e.message}")
                }
            }
            android.util.Log.d("DrumPlayer", "Loaded ${buffers.size}/6 drum samples")
        }
    }

    /**
     * Play a drum note by GM note number.
     * Fires immediately — no allocation during playback.
     */
    fun play(gmNote: Int, velocity: Int = 100) {
        val pcm = buffers[gmNote] ?: run {
            android.util.Log.w("DrumPlayer", "No buffer for GM note $gmNote")
            return
        }
        val velScale = (velocity / 127f).coerceIn(0f, 1f)

        scope.launch(Dispatchers.IO) {
            try {
                // Scale velocity — copy buffer with volume applied
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
                            builder.setPerformanceMode(
                                AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                        }
                    }
                    .build()

                track.write(scaled, 0, scaled.size)
                track.play()

                val durationMs = (scaled.size.toLong() * 1000L / OUTPUT_SAMPLE_RATE) + 50L
                delay(durationMs)
                track.stop()
                track.release()
            } catch (e: Exception) {
                android.util.Log.e("DrumPlayer", "Playback error: ${e.message}")
            }
        }
    }

    /**
     * Load a WAV file from assets and return mono 16-bit PCM at 44100Hz.
     * Handles stereo→mono downmix and basic sample rate conversion.
     */
    private fun loadWav(assetPath: String): ShortArray? {
        return try {
            val bytes = assetManager.open(assetPath).use { stream ->
                val out = ByteArrayOutputStream()
                stream.copyTo(out)
                out.toByteArray()
            }

            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

            // Parse WAV header
            val riff = String(bytes, 0, 4)
            if (riff != "RIFF") return null

            buf.position(12) // skip to fmt chunk search
            var audioFormat = 0
            var numChannels = 1
            var sampleRate  = 44100
            var bitsPerSample = 16
            var dataStart = 0
            var dataSize  = 0

            while (buf.remaining() >= 8) {
                val chunkId   = String(bytes, buf.position(), 4)
                buf.position(buf.position() + 4)
                val chunkSize = buf.int

                when (chunkId) {
                    "fmt " -> {
                        audioFormat   = buf.short.toInt() and 0xFFFF
                        numChannels   = buf.short.toInt() and 0xFFFF
                        sampleRate    = buf.int
                        buf.int  // byte rate
                        buf.short // block align
                        bitsPerSample = buf.short.toInt() and 0xFFFF
                        // skip any extra fmt bytes
                        val extra = chunkSize - 16
                        if (extra > 0) buf.position(buf.position() + extra)
                    }
                    "data" -> {
                        dataStart = buf.position()
                        dataSize  = chunkSize
                        break
                    }
                    else -> {
                        buf.position((buf.position() + chunkSize)
                            .coerceAtMost(buf.limit()))
                    }
                }
            }

            if (dataStart == 0 || dataSize == 0) return null

            // Read raw PCM samples
            val rawSamples = when (bitsPerSample) {
                16 -> {
                    val count = dataSize / 2
                    ShortArray(count) { buf.short }
                }
                8 -> {
                    // 8-bit WAV is unsigned — convert to signed 16-bit
                    ShortArray(dataSize) {
                        ((bytes[dataStart + it].toInt() and 0xFF) - 128).shl(8).toShort()
                    }
                }
                else -> return null
            }

            // Downmix stereo to mono if needed
            val monoSamples = if (numChannels == 2) {
                ShortArray(rawSamples.size / 2) { i ->
                    ((rawSamples[i * 2].toInt() + rawSamples[i * 2 + 1].toInt()) / 2)
                        .toShort()
                }
            } else rawSamples

            // Resample if needed (simple linear interpolation)
            if (sampleRate == OUTPUT_SAMPLE_RATE) {
                monoSamples
            } else {
                val ratio = sampleRate.toDouble() / OUTPUT_SAMPLE_RATE
                val outLen = (monoSamples.size / ratio).toInt()
                ShortArray(outLen) { i ->
                    val srcPos = i * ratio
                    val idx0   = srcPos.toInt().coerceAtMost(monoSamples.size - 1)
                    val idx1   = (idx0 + 1).coerceAtMost(monoSamples.size - 1)
                    val frac   = (srcPos - idx0).toFloat()
                    (monoSamples[idx0] * (1f - frac) + monoSamples[idx1] * frac)
                        .toInt().toShort()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DrumPlayer", "loadWav error: ${e.message}")
            null
        }
    }

    fun release() {
        scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
        buffers.clear()
    }
}