package com.reasontouch.core.`data`

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class ChordEventDao_Impl(
  __db: RoomDatabase,
) : ChordEventDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfChordEvent: EntityInsertAdapter<ChordEvent>

  private val __deleteAdapterOfChordEvent: EntityDeleteOrUpdateAdapter<ChordEvent>

  private val __updateAdapterOfChordEvent: EntityDeleteOrUpdateAdapter<ChordEvent>
  init {
    this.__db = __db
    this.__insertAdapterOfChordEvent = object : EntityInsertAdapter<ChordEvent>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `chord_events` (`id`,`sessionId`,`barIndex`,`chordName`,`rootMidi`,`midiNotes`,`voicing`,`strumPatternId`) VALUES (?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ChordEvent) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.sessionId)
        statement.bindLong(3, entity.barIndex.toLong())
        statement.bindText(4, entity.chordName)
        statement.bindLong(5, entity.rootMidi.toLong())
        statement.bindText(6, entity.midiNotes)
        statement.bindText(7, entity.voicing)
        val _tmpStrumPatternId: String? = entity.strumPatternId
        if (_tmpStrumPatternId == null) {
          statement.bindNull(8)
        } else {
          statement.bindText(8, _tmpStrumPatternId)
        }
      }
    }
    this.__deleteAdapterOfChordEvent = object : EntityDeleteOrUpdateAdapter<ChordEvent>() {
      protected override fun createQuery(): String = "DELETE FROM `chord_events` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: ChordEvent) {
        statement.bindText(1, entity.id)
      }
    }
    this.__updateAdapterOfChordEvent = object : EntityDeleteOrUpdateAdapter<ChordEvent>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `chord_events` SET `id` = ?,`sessionId` = ?,`barIndex` = ?,`chordName` = ?,`rootMidi` = ?,`midiNotes` = ?,`voicing` = ?,`strumPatternId` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: ChordEvent) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.sessionId)
        statement.bindLong(3, entity.barIndex.toLong())
        statement.bindText(4, entity.chordName)
        statement.bindLong(5, entity.rootMidi.toLong())
        statement.bindText(6, entity.midiNotes)
        statement.bindText(7, entity.voicing)
        val _tmpStrumPatternId: String? = entity.strumPatternId
        if (_tmpStrumPatternId == null) {
          statement.bindNull(8)
        } else {
          statement.bindText(8, _tmpStrumPatternId)
        }
        statement.bindText(9, entity.id)
      }
    }
  }

  public override suspend fun insertChord(chord: ChordEvent): Unit = performSuspending(__db, false,
      true) { _connection ->
    __insertAdapterOfChordEvent.insert(_connection, chord)
  }

  public override suspend fun insertChords(chords: List<ChordEvent>): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfChordEvent.insert(_connection, chords)
  }

  public override suspend fun deleteChord(chord: ChordEvent): Unit = performSuspending(__db, false,
      true) { _connection ->
    __deleteAdapterOfChordEvent.handle(_connection, chord)
  }

  public override suspend fun updateChord(chord: ChordEvent): Unit = performSuspending(__db, false,
      true) { _connection ->
    __updateAdapterOfChordEvent.handle(_connection, chord)
  }

  public override fun getChordsForSession(sessionId: String): Flow<List<ChordEvent>> {
    val _sql: String = "SELECT * FROM chord_events WHERE sessionId = ? ORDER BY barIndex ASC"
    return createFlow(__db, false, arrayOf("chord_events")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, sessionId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSessionId: Int = getColumnIndexOrThrow(_stmt, "sessionId")
        val _columnIndexOfBarIndex: Int = getColumnIndexOrThrow(_stmt, "barIndex")
        val _columnIndexOfChordName: Int = getColumnIndexOrThrow(_stmt, "chordName")
        val _columnIndexOfRootMidi: Int = getColumnIndexOrThrow(_stmt, "rootMidi")
        val _columnIndexOfMidiNotes: Int = getColumnIndexOrThrow(_stmt, "midiNotes")
        val _columnIndexOfVoicing: Int = getColumnIndexOrThrow(_stmt, "voicing")
        val _columnIndexOfStrumPatternId: Int = getColumnIndexOrThrow(_stmt, "strumPatternId")
        val _result: MutableList<ChordEvent> = mutableListOf()
        while (_stmt.step()) {
          val _item: ChordEvent
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpSessionId: String
          _tmpSessionId = _stmt.getText(_columnIndexOfSessionId)
          val _tmpBarIndex: Int
          _tmpBarIndex = _stmt.getLong(_columnIndexOfBarIndex).toInt()
          val _tmpChordName: String
          _tmpChordName = _stmt.getText(_columnIndexOfChordName)
          val _tmpRootMidi: Int
          _tmpRootMidi = _stmt.getLong(_columnIndexOfRootMidi).toInt()
          val _tmpMidiNotes: String
          _tmpMidiNotes = _stmt.getText(_columnIndexOfMidiNotes)
          val _tmpVoicing: String
          _tmpVoicing = _stmt.getText(_columnIndexOfVoicing)
          val _tmpStrumPatternId: String?
          if (_stmt.isNull(_columnIndexOfStrumPatternId)) {
            _tmpStrumPatternId = null
          } else {
            _tmpStrumPatternId = _stmt.getText(_columnIndexOfStrumPatternId)
          }
          _item =
              ChordEvent(_tmpId,_tmpSessionId,_tmpBarIndex,_tmpChordName,_tmpRootMidi,_tmpMidiNotes,_tmpVoicing,_tmpStrumPatternId)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteChordsForSession(sessionId: String) {
    val _sql: String = "DELETE FROM chord_events WHERE sessionId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, sessionId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
