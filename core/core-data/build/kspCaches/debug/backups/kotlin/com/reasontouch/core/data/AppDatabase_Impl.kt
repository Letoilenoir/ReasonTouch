package com.reasontouch.core.`data`

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AppDatabase_Impl : AppDatabase() {
  private val _sessionDao: Lazy<SessionDao> = lazy {
    SessionDao_Impl(this)
  }

  private val _midiTrackDao: Lazy<MidiTrackDao> = lazy {
    MidiTrackDao_Impl(this)
  }

  private val _noteEventDao: Lazy<NoteEventDao> = lazy {
    NoteEventDao_Impl(this)
  }

  private val _chordEventDao: Lazy<ChordEventDao> = lazy {
    ChordEventDao_Impl(this)
  }

  private val _strumPatternDao: Lazy<StrumPatternDao> = lazy {
    StrumPatternDao_Impl(this)
  }

  private val _strumStepDao: Lazy<StrumStepDao> = lazy {
    StrumStepDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(2,
        "69f49bf6d9e21ccb41816f4379204e23", "f9e24a42a24dd486f2d7da97d8b0f472") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `sessions` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `bpm` INTEGER NOT NULL, `keyRoot` TEXT NOT NULL, `keyQuality` TEXT NOT NULL, `timeSignatureNumerator` INTEGER NOT NULL, `timeSignatureDenominator` INTEGER NOT NULL, `totalBars` INTEGER NOT NULL, `defaultStrumSpeed` TEXT NOT NULL, `strumSimulationEnabled` INTEGER NOT NULL, `gmProgram` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `midi_tracks` (`id` TEXT NOT NULL, `sessionId` TEXT NOT NULL, `index` INTEGER NOT NULL, `name` TEXT NOT NULL, `voice` TEXT NOT NULL, `color` INTEGER NOT NULL, `midiChannel` INTEGER NOT NULL, `muted` INTEGER NOT NULL, `solo` INTEGER NOT NULL, `volume` REAL NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_midi_tracks_sessionId` ON `midi_tracks` (`sessionId`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `note_events` (`id` TEXT NOT NULL, `trackId` TEXT NOT NULL, `pitch` INTEGER NOT NULL, `beat` REAL NOT NULL, `duration` REAL NOT NULL, `velocity` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`trackId`) REFERENCES `midi_tracks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_note_events_trackId` ON `note_events` (`trackId`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `chord_events` (`id` TEXT NOT NULL, `sessionId` TEXT NOT NULL, `barIndex` INTEGER NOT NULL, `chordName` TEXT NOT NULL, `rootMidi` INTEGER NOT NULL, `midiNotes` TEXT NOT NULL, `voicing` TEXT NOT NULL, `strumPatternId` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_chord_events_sessionId` ON `chord_events` (`sessionId`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `strum_patterns` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `beats` INTEGER NOT NULL, `subdivisions` INTEGER NOT NULL, `strumSpeed` TEXT NOT NULL, `isPreset` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `strum_steps` (`id` TEXT NOT NULL, `patternId` TEXT NOT NULL, `stepIndex` INTEGER NOT NULL, `active` INTEGER NOT NULL, `direction` TEXT NOT NULL, `accented` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`patternId`) REFERENCES `strum_patterns`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_strum_steps_patternId` ON `strum_steps` (`patternId`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '69f49bf6d9e21ccb41816f4379204e23')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `sessions`")
        connection.execSQL("DROP TABLE IF EXISTS `midi_tracks`")
        connection.execSQL("DROP TABLE IF EXISTS `note_events`")
        connection.execSQL("DROP TABLE IF EXISTS `chord_events`")
        connection.execSQL("DROP TABLE IF EXISTS `strum_patterns`")
        connection.execSQL("DROP TABLE IF EXISTS `strum_steps`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        connection.execSQL("PRAGMA foreign_keys = ON")
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection):
          RoomOpenDelegate.ValidationResult {
        val _columnsSessions: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsSessions.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSessions.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSessions.put("bpm", TableInfo.Column("bpm", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSessions.put("keyRoot", TableInfo.Column("keyRoot", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSessions.put("keyQuality", TableInfo.Column("keyQuality", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSessions.put("timeSignatureNumerator", TableInfo.Column("timeSignatureNumerator",
            "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSessions.put("timeSignatureDenominator",
            TableInfo.Column("timeSignatureDenominator", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSessions.put("totalBars", TableInfo.Column("totalBars", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSessions.put("defaultStrumSpeed", TableInfo.Column("defaultStrumSpeed", "TEXT",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSessions.put("strumSimulationEnabled", TableInfo.Column("strumSimulationEnabled",
            "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSessions.put("gmProgram", TableInfo.Column("gmProgram", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSessions.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSessions.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysSessions: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesSessions: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoSessions: TableInfo = TableInfo("sessions", _columnsSessions, _foreignKeysSessions,
            _indicesSessions)
        val _existingSessions: TableInfo = read(connection, "sessions")
        if (!_infoSessions.equals(_existingSessions)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |sessions(com.reasontouch.core.data.Session).
              | Expected:
              |""".trimMargin() + _infoSessions + """
              |
              | Found:
              |""".trimMargin() + _existingSessions)
        }
        val _columnsMidiTracks: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsMidiTracks.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMidiTracks.put("sessionId", TableInfo.Column("sessionId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMidiTracks.put("index", TableInfo.Column("index", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMidiTracks.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMidiTracks.put("voice", TableInfo.Column("voice", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMidiTracks.put("color", TableInfo.Column("color", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMidiTracks.put("midiChannel", TableInfo.Column("midiChannel", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMidiTracks.put("muted", TableInfo.Column("muted", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMidiTracks.put("solo", TableInfo.Column("solo", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMidiTracks.put("volume", TableInfo.Column("volume", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysMidiTracks: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysMidiTracks.add(TableInfo.ForeignKey("sessions", "CASCADE", "NO ACTION",
            listOf("sessionId"), listOf("id")))
        val _indicesMidiTracks: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesMidiTracks.add(TableInfo.Index("index_midi_tracks_sessionId", false,
            listOf("sessionId"), listOf("ASC")))
        val _infoMidiTracks: TableInfo = TableInfo("midi_tracks", _columnsMidiTracks,
            _foreignKeysMidiTracks, _indicesMidiTracks)
        val _existingMidiTracks: TableInfo = read(connection, "midi_tracks")
        if (!_infoMidiTracks.equals(_existingMidiTracks)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |midi_tracks(com.reasontouch.core.data.MidiTrack).
              | Expected:
              |""".trimMargin() + _infoMidiTracks + """
              |
              | Found:
              |""".trimMargin() + _existingMidiTracks)
        }
        val _columnsNoteEvents: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsNoteEvents.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsNoteEvents.put("trackId", TableInfo.Column("trackId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsNoteEvents.put("pitch", TableInfo.Column("pitch", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsNoteEvents.put("beat", TableInfo.Column("beat", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsNoteEvents.put("duration", TableInfo.Column("duration", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsNoteEvents.put("velocity", TableInfo.Column("velocity", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysNoteEvents: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysNoteEvents.add(TableInfo.ForeignKey("midi_tracks", "CASCADE", "NO ACTION",
            listOf("trackId"), listOf("id")))
        val _indicesNoteEvents: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesNoteEvents.add(TableInfo.Index("index_note_events_trackId", false,
            listOf("trackId"), listOf("ASC")))
        val _infoNoteEvents: TableInfo = TableInfo("note_events", _columnsNoteEvents,
            _foreignKeysNoteEvents, _indicesNoteEvents)
        val _existingNoteEvents: TableInfo = read(connection, "note_events")
        if (!_infoNoteEvents.equals(_existingNoteEvents)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |note_events(com.reasontouch.core.data.NoteEvent).
              | Expected:
              |""".trimMargin() + _infoNoteEvents + """
              |
              | Found:
              |""".trimMargin() + _existingNoteEvents)
        }
        val _columnsChordEvents: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsChordEvents.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsChordEvents.put("sessionId", TableInfo.Column("sessionId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsChordEvents.put("barIndex", TableInfo.Column("barIndex", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsChordEvents.put("chordName", TableInfo.Column("chordName", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsChordEvents.put("rootMidi", TableInfo.Column("rootMidi", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsChordEvents.put("midiNotes", TableInfo.Column("midiNotes", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsChordEvents.put("voicing", TableInfo.Column("voicing", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsChordEvents.put("strumPatternId", TableInfo.Column("strumPatternId", "TEXT", false,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysChordEvents: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysChordEvents.add(TableInfo.ForeignKey("sessions", "CASCADE", "NO ACTION",
            listOf("sessionId"), listOf("id")))
        val _indicesChordEvents: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesChordEvents.add(TableInfo.Index("index_chord_events_sessionId", false,
            listOf("sessionId"), listOf("ASC")))
        val _infoChordEvents: TableInfo = TableInfo("chord_events", _columnsChordEvents,
            _foreignKeysChordEvents, _indicesChordEvents)
        val _existingChordEvents: TableInfo = read(connection, "chord_events")
        if (!_infoChordEvents.equals(_existingChordEvents)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |chord_events(com.reasontouch.core.data.ChordEvent).
              | Expected:
              |""".trimMargin() + _infoChordEvents + """
              |
              | Found:
              |""".trimMargin() + _existingChordEvents)
        }
        val _columnsStrumPatterns: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsStrumPatterns.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsStrumPatterns.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsStrumPatterns.put("beats", TableInfo.Column("beats", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsStrumPatterns.put("subdivisions", TableInfo.Column("subdivisions", "INTEGER", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStrumPatterns.put("strumSpeed", TableInfo.Column("strumSpeed", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStrumPatterns.put("isPreset", TableInfo.Column("isPreset", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysStrumPatterns: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesStrumPatterns: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoStrumPatterns: TableInfo = TableInfo("strum_patterns", _columnsStrumPatterns,
            _foreignKeysStrumPatterns, _indicesStrumPatterns)
        val _existingStrumPatterns: TableInfo = read(connection, "strum_patterns")
        if (!_infoStrumPatterns.equals(_existingStrumPatterns)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |strum_patterns(com.reasontouch.core.data.StrumPattern).
              | Expected:
              |""".trimMargin() + _infoStrumPatterns + """
              |
              | Found:
              |""".trimMargin() + _existingStrumPatterns)
        }
        val _columnsStrumSteps: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsStrumSteps.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsStrumSteps.put("patternId", TableInfo.Column("patternId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsStrumSteps.put("stepIndex", TableInfo.Column("stepIndex", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsStrumSteps.put("active", TableInfo.Column("active", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsStrumSteps.put("direction", TableInfo.Column("direction", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsStrumSteps.put("accented", TableInfo.Column("accented", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysStrumSteps: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysStrumSteps.add(TableInfo.ForeignKey("strum_patterns", "CASCADE", "NO ACTION",
            listOf("patternId"), listOf("id")))
        val _indicesStrumSteps: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesStrumSteps.add(TableInfo.Index("index_strum_steps_patternId", false,
            listOf("patternId"), listOf("ASC")))
        val _infoStrumSteps: TableInfo = TableInfo("strum_steps", _columnsStrumSteps,
            _foreignKeysStrumSteps, _indicesStrumSteps)
        val _existingStrumSteps: TableInfo = read(connection, "strum_steps")
        if (!_infoStrumSteps.equals(_existingStrumSteps)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |strum_steps(com.reasontouch.core.data.StrumStep).
              | Expected:
              |""".trimMargin() + _infoStrumSteps + """
              |
              | Found:
              |""".trimMargin() + _existingStrumSteps)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "sessions", "midi_tracks",
        "note_events", "chord_events", "strum_patterns", "strum_steps")
  }

  public override fun clearAllTables() {
    super.performClear(true, "sessions", "midi_tracks", "note_events", "chord_events",
        "strum_patterns", "strum_steps")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(SessionDao::class, SessionDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(MidiTrackDao::class, MidiTrackDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(NoteEventDao::class, NoteEventDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(ChordEventDao::class, ChordEventDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(StrumPatternDao::class, StrumPatternDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(StrumStepDao::class, StrumStepDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override
      fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun sessionDao(): SessionDao = _sessionDao.value

  public override fun midiTrackDao(): MidiTrackDao = _midiTrackDao.value

  public override fun noteEventDao(): NoteEventDao = _noteEventDao.value

  public override fun chordEventDao(): ChordEventDao = _chordEventDao.value

  public override fun strumPatternDao(): StrumPatternDao = _strumPatternDao.value

  public override fun strumStepDao(): StrumStepDao = _strumStepDao.value
}
