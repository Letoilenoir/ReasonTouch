package com.reasontouch.core.audio

import java.io.File

sealed interface SampleResolution {
    data class Resolved(val file: File) : SampleResolution
    data class MissingMapping(val midiNote: Int) : SampleResolution
    data class MissingFile(val path: String) : SampleResolution
    data class DecodeFailed(val path: String, val reason: String) : SampleResolution
    object NoActivePack : SampleResolution
}
