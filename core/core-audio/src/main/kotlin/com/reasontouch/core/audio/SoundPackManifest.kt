package com.reasontouch.core.audio

import com.google.gson.annotations.SerializedName

/**
 * Represents the structure of pack.json inside any .rpack expansion bundle.
 */
data class SoundPackManifest(
    @SerializedName("packId") val packId: String,
    @SerializedName("packName") val packName: String,
    @SerializedName("author") val author: String = "Unknown",
    @SerializedName("version") val version: String = "1.0.0",
    @SerializedName("description") val description: String = "",
    @SerializedName("tracks") val tracks: Map<String, TrackConfig> = emptyMap()
)

/**
 * Configuration for individual tracks (e.g., DRUMS, CHORDS, BASS, LEAD, PAD)
 */
data class TrackConfig(
    @SerializedName("type") val type: String, // "sample_kit" or "multisample"
    @SerializedName("format") val format: String? = "wav",
    @SerializedName("gmProgram") val gmProgram: Int? = null,
    @SerializedName("mapping") val mapping: Map<String, String>? = null, // MIDI Note number (e.g., "36") -> relative file path
    @SerializedName("rootKeys") val rootKeys: Map<String, String>? = null // MIDI Note number (e.g., "48") -> relative file path
)
