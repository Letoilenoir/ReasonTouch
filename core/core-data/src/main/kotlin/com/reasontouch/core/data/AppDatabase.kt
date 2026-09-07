package com.reasontouch.core.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.reasontouch.core.composition.SectionMarker
import com.reasontouch.core.composition.SectionMarkerDao

@Database(
    entities = [
        Session::class,
        MidiTrack::class,
        NoteEvent::class,
        ChordEvent::class,
        StrumPattern::class,
        StrumStep::class,
        SectionMarker::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun midiTrackDao(): MidiTrackDao
    abstract fun noteEventDao(): NoteEventDao
    abstract fun chordEventDao(): ChordEventDao
    abstract fun strumPatternDao(): StrumPatternDao
    abstract fun strumStepDao(): StrumStepDao
    abstract fun sectionMarkerDao(): SectionMarkerDao
}