# Technical Investigation Report: .rpack Sound Expansion Import & Playback Pipeline

**Date:** October 2026  
**Status:** Import & Management: **Working** | Playback Integration: **Undergoing Final Tuning**

---

## 1. Executive Summary & Current State

ReasonTouch has successfully established the foundational architecture for the extensible **`.rpack` Sound Expansion System**. 
* **What is fully working:**
  1. **Storage Access Framework (SAF) Integration:** Users can select and import `.rpack` or `.zip` expansion archives directly from the Session Settings screen (`SessionSettingsScreen.kt`).
  2. **Zip Extraction & Manifest Parsing:** `SoundPackManager.kt` securely unzips archives (with Zip-Slip traversal protection), detects `pack.json` manifests, and caches pack metadata.
  3. **UI Management:** Installed packs dynamically populate the Session Settings dropdown selector, persist per-session via `packId`, and support pack deletion (`DELETE CURRENT PACK`) with automatic fallback to the default built-in sound bank.
  4. **Global Active Routing:** `SoundPackManager.activePackId` synchronizes active session configurations globally across view models and audio players.

* **The Current "Last Mile" Challenge:**
  - While custom packs extract correctly into `context.filesDir/soundpacks/<packId>/` and are detected by `SoundPackManager`, audio playback (`Sf2Player` and `DrumSamplePlayer`) occasionally falls back to default built-in soundfont/asset samples rather than successfully decoding and streaming the custom WAV expansion files.

---

## 2. Technical Architecture of the Implementation

### A. Lifecycle & Management (`SoundPackManager.kt`)
* **Import Flow:** 
  1. `SessionSettingsViewModel.importPack(uri)` invokes `SoundPackManager.importPack(uri)`.
  2. Archives are unzipped into a temporary sandbox folder, `pack.json` is located (handling both root-level and nested folders), and extracted into `context.filesDir/soundpacks/<packId>/`.
* **Manifest Parsing:** 
  * Uses Gson to deserialize `pack.json` into `SoundPackManifest` and `TrackConfig` data structures, exposing `mapping` (MIDI Note number → relative WAV path) and `rootKeys`.

### B. Playback Routing (`Sf2Player.kt` & `DrumSamplePlayer.kt`)
* **Global Active Pack:** 
  - `SoundPackManager.activePackId` is automatically synchronized when a session loads or updates in `SessionSettingsViewModel`.
* **Sample Resolution Attempt:**
  - When playing notes or drums, `Sf2Player` and `DrumSamplePlayer` query `soundPackManager.getManifest(packId)`.
  - They look up note mappings from `pack.json` (`trackCfg.mapping["$midiNote"]`) and check fallback relative paths (`instruments/prog_$gmProgram/note_$midiNote.wav`, `drums/kick.wav`, etc.).
  - If a custom file exists on disk, it is decoded via `loadWavFile()` into PCM short arrays.

---

## 3. Analysis of the "Last Mile" Playback Gap

1. **Manifest Mapping Discrepancy:**
   - Third-party or test `.rpack` archives may use varying folder structures (e.g., omitting `pack.json` track configurations or using different relative paths like `samples/C3.wav` vs `instruments/prog_26/note_60.wav`). If `pack.json` specifies a mapping format that differs from the player's fallback search heuristics, file lookup returns `null`, triggering the default built-in sound bank.
2. **Audio Format Compatibility:**
   - Custom WAV files in expansion packs may occasionally use uncompressed PCM formats, sample rates other than 44,100Hz, or 24-bit PCM depth (whereas `loadWavFile()` currently parses standard 8-bit and 16-bit PCM).

---

## 4. Next Steps & Action Plan for Final Tuning

1. **Enhanced Diagnostic Logging:**
   - Instrument `SoundPackManager.getSampleFile()` and `Sf2Player.render()` with explicit logging to print exact target paths being searched and whether `sampleFile.exists()` evaluates to true on device.
2. **Flexible Path Resolution:**
   - Expand `Sf2Player` and `DrumSamplePlayer` fallback heuristics to perform a recursive search within `soundpacks/<packId>/` for any WAV file matching the requested note number if direct relative path resolution fails.
3. **24-Bit / Alternate PCM Support:**
   - Ensure `loadWavFile()` supports 24-bit integer PCM and sample rate conversion if user-imported expansion packs contain non-16-bit assets.
