package com.reasontouch.core.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        Session::class,
        MidiTrack::class,
        NoteEvent::class,
        ChordEvent::class,
        StrumPattern::class,
        StrumStep::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun midiTrackDao(): MidiTrackDao
    abstract fun noteEventDao(): NoteEventDao
    abstract fun chordEventDao(): ChordEventDao
    abstract fun strumPatternDao(): StrumPatternDao
    abstract fun strumStepDao(): StrumStepDao
}
