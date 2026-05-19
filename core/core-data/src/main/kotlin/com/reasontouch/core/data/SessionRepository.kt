package com.reasontouch.core.data

import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val trackDao: MidiTrackDao,
    private val noteDao: NoteEventDao,
    private val chordDao: ChordEventDao,
    private val strumPatternDao: StrumPatternDao,
    private val strumStepDao: StrumStepDao
) {
    // -- Sessions ------------------------------------------
    fun getAllSessions(): Flow<List<Session>> = sessionDao.getAllSessions()
    fun getSession(id: String): Flow<Session?> = sessionDao.getSessionById(id)
    suspend fun saveSession(session: Session) = sessionDao.insertSession(session)
    suspend fun updateSession(session: Session) = sessionDao.updateSession(session)
    suspend fun deleteSession(session: Session) = sessionDao.deleteSession(session)

    // -- Tracks --------------------------------------------
    fun getTracksForSession(sessionId: String): Flow<List<MidiTrack>> =
        trackDao.getTracksForSession(sessionId)
    suspend fun saveTrack(track: MidiTrack) = trackDao.insertTrack(track)
    suspend fun saveTracks(tracks: List<MidiTrack>) = trackDao.insertTracks(tracks)
    suspend fun updateTrack(track: MidiTrack) = trackDao.updateTrack(track)
    suspend fun deleteTrack(track: MidiTrack) = trackDao.deleteTrack(track)

    // -- Notes ---------------------------------------------
    fun getNotesForTrack(trackId: String): Flow<List<NoteEvent>> =
        noteDao.getNotesForTrack(trackId)
    suspend fun getNotesForTrackOnce(trackId: String): List<NoteEvent> =
        noteDao.getNotesForTrackOnce(trackId)
    suspend fun saveNote(note: NoteEvent) = noteDao.insertNote(note)
    suspend fun saveNotes(notes: List<NoteEvent>) = noteDao.insertNotes(notes)
    suspend fun updateNote(note: NoteEvent) = noteDao.updateNote(note)
    suspend fun deleteNote(note: NoteEvent) = noteDao.deleteNote(note)
    suspend fun deleteNoteById(id: String) = noteDao.deleteNoteById(id)
    suspend fun deleteNotesForTrack(trackId: String) = noteDao.deleteNotesForTrack(trackId)

    // -- Chords --------------------------------------------
    fun getChordsForSession(sessionId: String): Flow<List<ChordEvent>> =
        chordDao.getChordsForSession(sessionId)
    suspend fun saveChord(chord: ChordEvent) = chordDao.insertChord(chord)
    suspend fun saveChords(chords: List<ChordEvent>) = chordDao.insertChords(chords)
    suspend fun updateChord(chord: ChordEvent) = chordDao.updateChord(chord)
    suspend fun deleteChord(chord: ChordEvent) = chordDao.deleteChord(chord)
    suspend fun deleteChordsForSession(sessionId: String) =
        chordDao.deleteChordsForSession(sessionId)

    // -- Strum Patterns ------------------------------------
    fun getAllStrumPatterns(): Flow<List<StrumPattern>> =
        strumPatternDao.getAllPatterns()
    fun getPresetPatterns(): Flow<List<StrumPattern>> =
        strumPatternDao.getPresetPatterns()
    fun getUserPatterns(): Flow<List<StrumPattern>> =
        strumPatternDao.getUserPatterns()
    suspend fun saveStrumPattern(pattern: StrumPattern) =
        strumPatternDao.insertPattern(pattern)
    suspend fun updateStrumPattern(pattern: StrumPattern) =
        strumPatternDao.updatePattern(pattern)
    suspend fun deleteStrumPattern(pattern: StrumPattern) =
        strumPatternDao.deletePattern(pattern)

    // -- Strum Steps ---------------------------------------
    fun getStepsForPattern(patternId: String): Flow<List<StrumStep>> =
        strumStepDao.getStepsForPattern(patternId)
    suspend fun getStepsForPatternOnce(patternId: String): List<StrumStep> =
        strumStepDao.getStepsForPatternOnce(patternId)
    suspend fun saveStrumStep(step: StrumStep) = strumStepDao.insertStep(step)
    suspend fun saveStrumSteps(steps: List<StrumStep>) = strumStepDao.insertSteps(steps)
    suspend fun updateStrumStep(step: StrumStep) = strumStepDao.updateStep(step)
    suspend fun deleteStepsForPattern(patternId: String) =
        strumStepDao.deleteStepsForPattern(patternId)

    // -- Session creation ----------------------------------
    suspend fun createNewSession(name: String): Session {
        val session = Session(name = name)
        sessionDao.insertSession(session)

        val defaultTracks = listOf(
            MidiTrack(sessionId = session.id, index = 0, name = "BASS",
                voice = "saw",    color = 0xFFE84040, midiChannel = 0, volume = 0.90f, gmProgram = 33),
            MidiTrack(sessionId = session.id, index = 1, name = "LEAD",
                voice = "sine",   color = 0xFF3DDC84, midiChannel = 1, volume = 0.85f, gmProgram = 81),
            MidiTrack(sessionId = session.id, index = 2, name = "CHORD",
                voice = "square", color = 0xFF38BDF8, midiChannel = 2, volume = 0.65f, gmProgram = 25),
            MidiTrack(sessionId = session.id, index = 3, name = "PAD",
                voice = "pwm",    color = 0xFFA78BFA, midiChannel = 3,  volume = 0.55f, gmProgram = 89),
            MidiTrack(sessionId = session.id, index = 4, name = "DRUMS",
                voice = "saw",    color = 0xFFF5C518, midiChannel = 9,  volume = 0.90f, gmProgram = 0,
                drumPack = "default")
        )
        trackDao.insertTracks(defaultTracks)
        return session
    }

    // -- Preset pattern seeding ----------------------------
    suspend fun seedPresetsIfEmpty() {
        val existing = strumPatternDao.getPresetPatterns()
        // Only seed once — checked via one-shot query
        val presets = buildPresetPatterns()
        presets.forEach { (pattern, steps) ->
            strumPatternDao.insertPattern(pattern)
            strumStepDao.insertSteps(steps)
        }
    }

    private fun buildPresetPatterns(): List<Pair<StrumPattern, List<StrumStep>>> {
        val result = mutableListOf<Pair<StrumPattern, List<StrumStep>>>()

        // All Down — every beat, all down strums
        val allDown = StrumPattern(
            name = "All Down",
            beats = 4, subdivisions = 4,
            strumSpeed = "NATURAL",
            isPreset = true
        )
        val allDownSteps = (0 until 16).map { i ->
            StrumStep(
                patternId = allDown.id,
                stepIndex = i,
                active = (i % 4 == 0),   // beats 1 2 3 4
                direction = "DOWN"
            )
        }
        result.add(Pair(allDown, allDownSteps))

        // Down/Up 8ths — alternating every 2 steps
        val downUp8 = StrumPattern(
            name = "Down/Up 8ths",
            beats = 4, subdivisions = 4,
            strumSpeed = "NATURAL",
            isPreset = true
        )
        val downUp8Steps = (0 until 16).map { i ->
            StrumStep(
                patternId = downUp8.id,
                stepIndex = i,
                active = (i % 2 == 0),
                direction = if (i % 4 == 0) "DOWN" else "UP"
            )
        }
        result.add(Pair(downUp8, downUp8Steps))

        // Folk — D DU UDU
        val folk = StrumPattern(
            name = "Folk",
            beats = 4, subdivisions = 4,
            strumSpeed = "NATURAL",
            isPreset = true
        )
        // Steps 0,2,4,6,7,8,10,11 active — D.DU.UDU
        val folkActive = setOf(0, 2, 4, 6, 7, 8, 10, 11)
        val folkDir = mapOf(0 to "DOWN", 2 to "DOWN", 4 to "DOWN",
            6 to "UP", 7 to "UP", 8 to "DOWN", 10 to "UP", 11 to "DOWN")
        val folkSteps = (0 until 16).map { i ->
            StrumStep(
                patternId = folk.id,
                stepIndex = i,
                active = folkActive.contains(i),
                direction = folkDir[i] ?: "DOWN"
            )
        }
        result.add(Pair(folk, folkSteps))

        // Rock D-DU-UDU
        val rock = StrumPattern(
            name = "Rock D-DU-UDU",
            beats = 4, subdivisions = 4,
            strumSpeed = "FAST",
            isPreset = true
        )
        val rockActive = setOf(0, 2, 4, 6, 8, 10, 11)
        val rockDir = mapOf(0 to "DOWN", 2 to "DOWN", 4 to "UP",
            6 to "DOWN", 8 to "UP", 10 to "DOWN", 11 to "UP")
        val rockSteps = (0 until 16).map { i ->
            StrumStep(
                patternId = rock.id,
                stepIndex = i,
                active = rockActive.contains(i),
                direction = rockDir[i] ?: "DOWN"
            )
        }
        result.add(Pair(rock, rockSteps))

        // Reggae — upstrokes on offbeats
        val reggae = StrumPattern(
            name = "Reggae",
            beats = 4, subdivisions = 4,
            strumSpeed = "FLICK",
            isPreset = true
        )
        val reggaeActive = setOf(2, 6, 10, 14)
        val reggaeSteps = (0 until 16).map { i ->
            StrumStep(
                patternId = reggae.id,
                stepIndex = i,
                active = reggaeActive.contains(i),
                direction = "UP"
            )
        }
        result.add(Pair(reggae, reggaeSteps))

        return result
    }
}
