package com.reasontouch.core.audio

import android.content.res.AssetManager
import android.util.Log
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Sf2Parser(assetManager: AssetManager, fileName: String = "TimGM6mb.sf2") {

    data class SampleHeader(
        val name: String,
        val start: Int,
        val end: Int,
        val loopStart: Int,
        val loopEnd: Int,
        val sampleRate: Int,
        val originalPitch: Int,
        val pitchCorrection: Int,
        val sampleType: Int
    ) {
        val relLoopStart get() = (loopStart - start).coerceAtLeast(0)
        val relLoopEnd   get() = (loopEnd   - start).coerceAtLeast(0)
        val hasLoop      get() = loopEnd > loopStart && relLoopEnd > relLoopStart
    }

    data class InstBag(val genIndex: Int, val modIndex: Int)
    data class PBag(val genIndex: Int, val modIndex: Int)
    data class Generator(val oper: Int, val value: Short)
    data class Instrument(val name: String, val bagIndex: Int)
    data class Preset(val name: String, val program: Int, val bank: Int, val bagIndex: Int)

    val sampleHeaders = mutableListOf<SampleHeader>()
    val instruments   = mutableListOf<Instrument>()
    val instBags      = mutableListOf<InstBag>()
    val instGens      = mutableListOf<Generator>()
    val presets       = mutableListOf<Preset>()
    val presetBags    = mutableListOf<PBag>()
    val presetGens    = mutableListOf<Generator>()

    private var sampleData: ShortArray = ShortArray(0)

    private val GEN_KEY_RANGE     = 43
    private val GEN_VEL_RANGE     = 44
    private val GEN_SAMPLE_ID     = 53
    private val GEN_OVERRIDE_ROOT = 58

    init {
        assetManager.open(fileName).use { parse(it) }
    }

    fun findDrumSample(midiNote: Int): Pair<SampleHeader, ShortArray>? {

        Log.d("SF2", "---- Drum preset scan for midiNote=$midiNote ----")

        presets.forEach {
            Log.d(
                "SF2",
                "Preset name=${it.name}, bank=${it.bank}, program=${it.program}"
            )
        }

        // Broader percussion search
        val preset =
            presets.firstOrNull { it.bank == 128 && it.program == 0 }
                ?: presets.firstOrNull { it.bank == 128 }
                ?: presets.firstOrNull { it.bank == 127 }
                ?: presets.firstOrNull { it.bank >= 120 }
                ?: presets.firstOrNull {
                    it.name.contains("drum", ignoreCase = true) ||
                            it.name.contains("perc", ignoreCase = true) ||
                            it.name.contains("kit", ignoreCase = true)
                }
                ?: run {
                    Log.e("SF2", "No drum preset found")
                    return null
                }

        Log.d(
            "SF2",
            "Using drum preset: ${preset.name}, bank=${preset.bank}, program=${preset.program}"
        )

        val pBagStart = preset.bagIndex
        val pBagEnd = presets.getOrNull(presets.indexOf(preset) + 1)?.bagIndex
            ?: presetBags.size

        var instIndex = -1
        for (bi in pBagStart until pBagEnd) {
            val bag = presetBags.getOrNull(bi) ?: break
            val genEnd = presetBags.getOrNull(bi + 1)?.genIndex ?: presetGens.size

            for (gi in bag.genIndex until genEnd) {
                val gen = presetGens.getOrNull(gi) ?: break
                if (gen.oper == 41) instIndex = gen.value.toInt() and 0xFFFF
            }
        }

        if (instIndex < 0) {
            Log.e("SF2", "No instrument found for drum preset")
            return null
        }

        val inst = instruments.getOrNull(instIndex) ?: return null
        val iBagStart = inst.bagIndex
        val iBagEnd = instruments.getOrNull(instIndex + 1)?.bagIndex ?: instBags.size

        var bestSampleIndex = -1
        var bestRootOverride = -1

        for (bi in iBagStart until iBagEnd) {
            val bag = instBags.getOrNull(bi) ?: break
            val genEnd = instBags.getOrNull(bi + 1)?.genIndex ?: instGens.size

            var loKey = 0
            var hiKey = 127
            var sampleId = -1
            var rootOverride = -1

            for (gi in bag.genIndex until genEnd) {
                val gen = instGens.getOrNull(gi) ?: break

                when (gen.oper) {
                    GEN_KEY_RANGE -> {
                        val raw = gen.value.toInt() and 0xFFFF
                        loKey = raw and 0xFF
                        hiKey = (raw shr 8) and 0xFF
                        if (hiKey < loKey) {
                            val tmp = loKey
                            loKey = hiKey
                            hiKey = tmp
                        }
                    }

                    GEN_SAMPLE_ID -> sampleId = gen.value.toInt() and 0xFFFF
                    GEN_OVERRIDE_ROOT -> rootOverride = gen.value.toInt() and 0xFF
                }
            }

            if (sampleId >= 0 && midiNote in loKey..hiKey) {
                Log.d(
                    "SF2",
                    "Matched note=$midiNote sampleId=$sampleId range=$loKey-$hiKey"
                )
                bestSampleIndex = sampleId
                bestRootOverride = rootOverride
                break
            }
        }

        if (bestSampleIndex < 0) {
            Log.w("SF2", "No exact drum match for note=$midiNote, using fallback")

            for (bi in iBagStart until iBagEnd) {
                val bag = instBags.getOrNull(bi) ?: break
                val genEnd = instBags.getOrNull(bi + 1)?.genIndex ?: instGens.size

                for (gi in bag.genIndex until genEnd) {
                    val gen = instGens.getOrNull(gi) ?: break
                    if (gen.oper == GEN_SAMPLE_ID) {
                        bestSampleIndex = gen.value.toInt() and 0xFFFF
                        break
                    }
                }

                if (bestSampleIndex >= 0) break
            }
        }

        val header = sampleHeaders.getOrNull(bestSampleIndex) ?: run {
            Log.e("SF2", "Still no drum sample found")
            return null
        }

        if (header.sampleType == 0 || header.end <= header.start) {
            Log.e("SF2", "Invalid sample header for ${header.name}")
            return null
        }

        val finalHeader =
            if (bestRootOverride >= 0)
                header.copy(originalPitch = bestRootOverride)
            else header

        val len = (header.end - header.start).coerceAtLeast(0)
        val pcm = ShortArray(len)

        for (i in 0 until len) {
            pcm[i] = sampleData.getOrElse(header.start + i) { 0 }
        }

        Log.d("SF2", "Loaded drum sample: ${header.name}, length=$len")

        return Pair(finalHeader, pcm)
    }
    fun findSample(gmProgram: Int, midiNote: Int): Pair<SampleHeader, ShortArray>? {
        val preset = presets.firstOrNull { it.program == gmProgram && it.bank == 0 }
            ?: presets.firstOrNull { it.bank == 0 }
            ?: return null

        val pBagStart = preset.bagIndex
        val pBagEnd = presets.getOrNull(presets.indexOf(preset) + 1)?.bagIndex
            ?: presetBags.size

        var instIndex = -1

        for (bi in pBagStart until pBagEnd) {
            val bag = presetBags.getOrNull(bi) ?: break
            val genEnd = presetBags.getOrNull(bi + 1)?.genIndex ?: presetGens.size

            for (gi in bag.genIndex until genEnd) {
                val gen = presetGens.getOrNull(gi) ?: break
                if (gen.oper == 41) instIndex = gen.value.toInt() and 0xFFFF
            }
        }

        if (instIndex < 0) return null

        val inst = instruments.getOrNull(instIndex) ?: return null
        val iBagStart = inst.bagIndex
        val iBagEnd = instruments.getOrNull(instIndex + 1)?.bagIndex ?: instBags.size

        var bestSampleIndex = -1
        var bestRootOverride = -1

        for (bi in iBagStart until iBagEnd) {
            val bag = instBags.getOrNull(bi) ?: break
            val genEnd = instBags.getOrNull(bi + 1)?.genIndex ?: instGens.size

            var loKey = 0
            var hiKey = 127
            var sampleId = -1
            var rootOverride = -1

            for (gi in bag.genIndex until genEnd) {
                val gen = instGens.getOrNull(gi) ?: break

                when (gen.oper) {
                    GEN_KEY_RANGE -> {
                        val raw = gen.value.toInt() and 0xFFFF
                        loKey = raw and 0xFF
                        hiKey = (raw shr 8) and 0xFF
                        if (hiKey < loKey) {
                            val t = loKey
                            loKey = hiKey
                            hiKey = t
                        }
                    }

                    GEN_SAMPLE_ID -> sampleId = gen.value.toInt() and 0xFFFF
                    GEN_OVERRIDE_ROOT -> rootOverride = gen.value.toInt() and 0xFF
                }
            }

            if (sampleId >= 0 && midiNote in loKey..hiKey) {
                bestSampleIndex = sampleId
                bestRootOverride = rootOverride
                break
            }
        }

        val header = sampleHeaders.getOrNull(bestSampleIndex) ?: return null

        val finalHeader =
            if (bestRootOverride >= 0)
                header.copy(originalPitch = bestRootOverride)
            else header

        val len = (header.end - header.start).coerceAtLeast(0)
        val pcm = ShortArray(len)

        for (i in 0 until len) {
            pcm[i] = sampleData.getOrElse(header.start + i) { 0 }
        }

        return Pair(finalHeader, pcm)
    }
    private fun parse(stream: InputStream) {
        val bytes = stream.readBytes()
        val buf   = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val riff  = readFourCC(buf); if (riff != "RIFF") return
        buf.int
        val sfbk  = readFourCC(buf); if (sfbk != "sfbk") return

        while (buf.remaining() >= 8) {
            val chunkId   = readFourCC(buf)
            val chunkSize = buf.int
            val chunkEnd  = buf.position() + chunkSize
            if (chunkId == "LIST") {
                val listType = readFourCC(buf)
                when (listType) {
                    "pdta" -> parsePdta(buf, chunkEnd)
                    "sdta" -> parseSdta(buf, chunkEnd)
                }
            }
            buf.position(chunkEnd.coerceAtMost(buf.limit()))
        }
    }

    private fun parseSdta(buf: ByteBuffer, end: Int) {
        while (buf.position() < end - 8) {
            val id   = readFourCC(buf)
            val size = buf.int
            val next = buf.position() + size
            if (id == "smpl") {
                sampleData = ShortArray(size / 2) { buf.short }
            } else {
                buf.position(next.coerceAtMost(buf.limit()))
            }
        }
    }

    private fun parsePdta(buf: ByteBuffer, end: Int) {
        while (buf.position() < end - 8) {
            val id   = readFourCC(buf)
            val size = buf.int
            val next = buf.position() + size
            when (id) {
                "phdr" -> parsePhdr(buf, size)
                "pbag" -> parsePbag(buf, size)
                "pgen" -> parsePgen(buf, size)
                "inst" -> parseInst(buf, size)
                "ibag" -> parseIbag(buf, size)
                "igen" -> parseIgen(buf, size)
                "shdr" -> parseShdr(buf, size)
                else   -> buf.position(next.coerceAtMost(buf.limit()))
            }
            buf.position(next.coerceAtMost(buf.limit()))
        }
    }

    private fun parsePhdr(buf: ByteBuffer, size: Int) {
        repeat(size / 38) {
            val name     = readString(buf, 20)
            val program  = buf.short.toInt() and 0xFFFF
            val bank     = buf.short.toInt() and 0xFFFF
            val bagIndex = buf.short.toInt() and 0xFFFF
            buf.int; buf.int; buf.int
            if (name != "EOP") presets.add(Preset(name, program, bank, bagIndex))
        }
    }

    private fun parsePbag(buf: ByteBuffer, size: Int) {
        repeat(size / 4) {
            presetBags.add(PBag(buf.short.toInt() and 0xFFFF, buf.short.toInt() and 0xFFFF))
        }
    }

    private fun parsePgen(buf: ByteBuffer, size: Int) {
        repeat(size / 4) {
            presetGens.add(Generator(buf.short.toInt() and 0xFFFF, buf.short))
        }
    }

    private fun parseInst(buf: ByteBuffer, size: Int) {
        repeat(size / 22) {
            val name     = readString(buf, 20)
            val bagIndex = buf.short.toInt() and 0xFFFF
            if (name != "EOI") instruments.add(Instrument(name, bagIndex))
        }
    }

    private fun parseIbag(buf: ByteBuffer, size: Int) {
        repeat(size / 4) {
            instBags.add(InstBag(buf.short.toInt() and 0xFFFF, buf.short.toInt() and 0xFFFF))
        }
    }

    private fun parseIgen(buf: ByteBuffer, size: Int) {
        repeat(size / 4) {
            instGens.add(Generator(buf.short.toInt() and 0xFFFF, buf.short))
        }
    }

    private fun parseShdr(buf: ByteBuffer, size: Int) {
        repeat(size / 46) {
            val name          = readString(buf, 20)
            val start         = buf.int
            val end           = buf.int
            val loopStart     = buf.int
            val loopEnd       = buf.int
            val sampleRate    = buf.int
            val originalPitch = buf.get().toInt() and 0xFF
            val pitchCorrect  = buf.get().toInt()
            buf.short
            val sampleType    = buf.short.toInt() and 0xFFFF
            if (name != "EOS") {
                sampleHeaders.add(SampleHeader(name, start, end, loopStart, loopEnd,
                    sampleRate, originalPitch, pitchCorrect, sampleType))
            }
        }
    }

    private fun readFourCC(buf: ByteBuffer): String {
        val b = ByteArray(4); buf.get(b)
        return String(b, Charsets.US_ASCII)
    }

    private fun readString(buf: ByteBuffer, len: Int): String {
        val b = ByteArray(len); buf.get(b)
        return String(b, Charsets.US_ASCII).trimEnd('\u0000')
    }
}