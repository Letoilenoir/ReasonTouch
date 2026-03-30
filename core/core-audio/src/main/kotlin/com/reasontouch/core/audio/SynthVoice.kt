package com.reasontouch.core.audio

/**
 * Synthesis voice configurations.
 * Each voice defines oscillator type, filter and envelope parameters.
 * Mirrors the Web Audio voices from the browser prototype.
 */
enum class OscType { SINE, SQUARE, SAWTOOTH, TRIANGLE }

data class VoiceConfig(
    val oscType:     OscType,
    val filterFreq:  Float,   // Hz
    val filterQ:     Float,
    val attack:      Float,   // seconds
    val decay:       Float,   // seconds
    val fmRatio:     Float = 0f,   // 0 = no FM
    val fmDepth:     Float = 0f
)

enum class SynthVoice(val config: VoiceConfig) {
    SAW(VoiceConfig(
        oscType = OscType.SAWTOOTH, filterFreq = 1800f, filterQ = 0.8f,
        attack = 0.005f, decay = 0.3f
    )),
    SQUARE(VoiceConfig(
        oscType = OscType.SQUARE, filterFreq = 900f, filterQ = 1.2f,
        attack = 0.005f, decay = 0.4f
    )),
    SINE(VoiceConfig(
        oscType = OscType.SINE, filterFreq = 4000f, filterQ = 0.5f,
        attack = 0.01f, decay = 0.5f
    )),
    TRIANGLE(VoiceConfig(
        oscType = OscType.TRIANGLE, filterFreq = 3000f, filterQ = 0.6f,
        attack = 0.008f, decay = 0.6f
    )),
    PWM(VoiceConfig(
        oscType = OscType.SAWTOOTH, filterFreq = 600f, filterQ = 2.0f,
        attack = 0.05f, decay = 0.8f
    )),
    FM(VoiceConfig(
        oscType = OscType.SINE, filterFreq = 2500f, filterQ = 1.5f,
        attack = 0.01f, decay = 0.35f,
        fmRatio = 2.01f, fmDepth = 0.5f
    ));

    companion object {
        fun fromString(name: String): SynthVoice = entries.firstOrNull {
            it.name.equals(name, ignoreCase = true)
        } ?: SAW
    }
}