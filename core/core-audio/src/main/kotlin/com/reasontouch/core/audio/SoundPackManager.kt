package com.reasontouch.core.audio

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton


data class SoundPackInfo(
    val packId: String,
    val packName: String,
    val version: String,
    val author: String
)

@Singleton
class SoundPackManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()
    private val packsDir = File(context.filesDir, "soundpacks")
    private val manifestCache = mutableMapOf<String, SoundPackManifest>()

    private val _activePackId = MutableStateFlow("default")
    val activePackIdFlow: StateFlow<String> = _activePackId.asStateFlow()

    var activePackId: String
        get() = _activePackId.value
        set(value) { _activePackId.value = value }

    init {
        if (!packsDir.exists()) packsDir.mkdirs()
    }

    fun getInstalledPacks(): List<SoundPackInfo> {
        val list = mutableListOf(SoundPackInfo("default", "Default Sound Bank (Built-in)", "1.0", "ReasonTouch"))
        manifestCache.clear()
        val subDirs = packsDir.listFiles { file -> file.isDirectory } ?: return list

        subDirs.forEach { dir ->
            val manifestFile = File(dir, "pack.json")
            if (manifestFile.exists()) {
                try {
                    val manifest = InputStreamReader(manifestFile.inputStream()).use { reader ->
                        gson.fromJson(reader, SoundPackManifest::class.java)
                    }
                    if (manifest != null) {
                        manifestCache[manifest.packId] = manifest
                        list.add(
                            SoundPackInfo(
                                packId = manifest.packId,
                                packName = manifest.packName,
                                version = manifest.version,
                                author = manifest.author
                            )
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SoundPackManager", "Failed to parse pack.json in ${dir.name}", e)
                }
            }
        }
        return list
    }

    fun getManifest(packId: String): SoundPackManifest? {
        val effective = if (packId == "default") activePackId else packId
        if (manifestCache.isEmpty()) getInstalledPacks()
        return manifestCache[effective]
    }

    fun resolveSample(packId: String, trackName: String, midiNote: Int): SampleResolution {
        val effective = if (packId == "default") activePackId else packId
        if (effective == "default") return SampleResolution.NoActivePack

        val manifest = getManifest(effective) ?: return SampleResolution.NoActivePack
        val normalizedTrack = trackName.lowercase().trim()

        // Strict track presence check
        val trackConfig = manifest.tracks[normalizedTrack] ?: manifest.tracks["${normalizedTrack}s"]
        if (trackConfig == null) {
            android.util.Log.d("SoundPackManager", "[PerTrackIsolation] Track '$trackName' absent from pack '$effective' -> falling back to default sound")
            return SampleResolution.MissingMapping(midiNote)
        }

        // Strict note mapping check
        val relPath = trackConfig.mapping?.get("$midiNote") ?: trackConfig.rootKeys?.get("$midiNote")
        if (relPath == null) {
            android.util.Log.d("SoundPackManager", "[PerTrackIsolation] Track '$trackName' in pack '$effective' has no mapping for note $midiNote -> falling back to default sound")
            return SampleResolution.MissingMapping(midiNote)
        }

        val sampleFile = File(File(packsDir, effective), relPath)
        if (!sampleFile.exists()) {
            android.util.Log.e("SoundPackManager", "[PerTrackIsolation] File missing at ${sampleFile.absolutePath} for track '$trackName' note $midiNote")
            return SampleResolution.MissingFile(sampleFile.absolutePath)
        }

        return SampleResolution.Resolved(sampleFile)
    }

    fun importPack(sourceUri: Uri): Boolean {
        return try {
            val tempExtractDir = File(packsDir, "temp_${System.currentTimeMillis()}")
            if (!tempExtractDir.mkdirs()) return false

            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipInput ->
                    var entry = zipInput.nextEntry
                    while (entry != null) {
                        val newFile = File(tempExtractDir, entry.name)
                        if (!newFile.canonicalPath.startsWith(tempExtractDir.canonicalPath)) {
                            throw SecurityException("Zip entry is outside target directory: ${entry.name}")
                        }
                        if (entry.isDirectory) {
                            newFile.mkdirs()
                        } else {
                            newFile.parentFile?.mkdirs()
                            FileOutputStream(newFile).use { output ->
                                zipInput.copyTo(output)
                            }
                        }
                        zipInput.closeEntry()
                        entry = zipInput.nextEntry
                    }
                }
            } ?: return false

            val manifestFile = File(tempExtractDir, "pack.json")
            val actualManifest = if (manifestFile.exists()) manifestFile else {
                tempExtractDir.walkTopDown().firstOrNull { it.name == "pack.json" }
            }

            if (actualManifest == null || !actualManifest.exists()) {
                tempExtractDir.deleteRecursively()
                return false
            }

            val rootPackDir = actualManifest.parentFile ?: tempExtractDir
            val manifest = InputStreamReader(actualManifest.inputStream()).use { reader ->
                gson.fromJson(reader, SoundPackManifest::class.java)
            } ?: return false

            val finalDir = File(packsDir, manifest.packId)
            if (finalDir.exists()) finalDir.deleteRecursively()

            if (rootPackDir != tempExtractDir) {
                rootPackDir.copyRecursively(finalDir, overwrite = true)
                tempExtractDir.deleteRecursively()
            } else {
                tempExtractDir.renameTo(finalDir)
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("SoundPackManager", "Failed to import pack from uri: $sourceUri", e)
            false
        }
    }

    fun deletePack(packId: String): Boolean {
        if (packId == "default") return false
        val packDir = File(packsDir, packId)
        var deleted = false
        if (packDir.exists()) {
            deleted = packDir.deleteRecursively()
        }
        manifestCache.remove(packId)
        if (activePackId == packId) {
            activePackId = "default"
        }
        return deleted
    }

    fun getSampleFile(packId: String, relativePath: String): File {
        val effective = if (packId == "default") activePackId else packId
        return File(packsDir, "$effective/$relativePath")
    }
}
