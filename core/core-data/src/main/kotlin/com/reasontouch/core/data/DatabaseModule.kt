package com.reasontouch.core.data

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "reasontouch.db"
        )
        .fallbackToDestructiveMigration(true)
        .build()
    }

    @Provides
    fun provideSessionDao(db: AppDatabase): SessionDao = db.sessionDao()

    @Provides
    fun provideMidiTrackDao(db: AppDatabase): MidiTrackDao = db.midiTrackDao()

    @Provides
    fun provideNoteEventDao(db: AppDatabase): NoteEventDao = db.noteEventDao()

    @Provides
    fun provideChordEventDao(db: AppDatabase): ChordEventDao = db.chordEventDao()

    @Provides
    fun provideStrumPatternDao(db: AppDatabase): StrumPatternDao = db.strumPatternDao()

    @Provides
    fun provideStrumStepDao(db: AppDatabase): StrumStepDao = db.strumStepDao()
}
