package com.reasontouch.core.`data`

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Float
import kotlin.Int
import kotlin.Long
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
public class MidiTrackDao_Impl(
  __db: RoomDatabase,
) : MidiTrackDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfMidiTrack: EntityInsertAdapter<MidiTrack>

  private val __deleteAdapterOfMidiTrack: EntityDeleteOrUpdateAdapter<MidiTrack>

  private val __updateAdapterOfMidiTrack: EntityDeleteOrUpdateAdapter<MidiTrack>
  init {
    this.__db = __db
    this.__insertAdapterOfMidiTrack = object : EntityInsertAdapter<MidiTrack>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `midi_tracks` (`id`,`sessionId`,`index`,`name`,`voice`,`color`,`midiChannel`,`muted`,`solo`,`volume`) VALUES (?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: MidiTrack) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.sessionId)
        statement.bindLong(3, entity.index.toLong())
        statement.bindText(4, entity.name)
        statement.bindText(5, entity.voice)
        statement.bindLong(6, entity.color)
        statement.bindLong(7, entity.midiChannel.toLong())
        val _tmp: Int = if (entity.muted) 1 else 0
        statement.bindLong(8, _tmp.toLong())
        val _tmp_1: Int = if (entity.solo) 1 else 0
        statement.bindLong(9, _tmp_1.toLong())
        statement.bindDouble(10, entity.volume.toDouble())
      }
    }
    this.__deleteAdapterOfMidiTrack = object : EntityDeleteOrUpdateAdapter<MidiTrack>() {
      protected override fun createQuery(): String = "DELETE FROM `midi_tracks` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: MidiTrack) {
        statement.bindText(1, entity.id)
      }
    }
    this.__updateAdapterOfMidiTrack = object : EntityDeleteOrUpdateAdapter<MidiTrack>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `midi_tracks` SET `id` = ?,`sessionId` = ?,`index` = ?,`name` = ?,`voice` = ?,`color` = ?,`midiChannel` = ?,`muted` = ?,`solo` = ?,`volume` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: MidiTrack) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.sessionId)
        statement.bindLong(3, entity.index.toLong())
        statement.bindText(4, entity.name)
        statement.bindText(5, entity.voice)
        statement.bindLong(6, entity.color)
        statement.bindLong(7, entity.midiChannel.toLong())
        val _tmp: Int = if (entity.muted) 1 else 0
        statement.bindLong(8, _tmp.toLong())
        val _tmp_1: Int = if (entity.solo) 1 else 0
        statement.bindLong(9, _tmp_1.toLong())
        statement.bindDouble(10, entity.volume.toDouble())
        statement.bindText(11, entity.id)
      }
    }
  }

  public override suspend fun insertTrack(track: MidiTrack): Unit = performSuspending(__db, false,
      true) { _connection ->
    __insertAdapterOfMidiTrack.insert(_connection, track)
  }

  public override suspend fun insertTracks(tracks: List<MidiTrack>): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfMidiTrack.insert(_connection, tracks)
  }

  public override suspend fun deleteTrack(track: MidiTrack): Unit = performSuspending(__db, false,
      true) { _connection ->
    __deleteAdapterOfMidiTrack.handle(_connection, track)
  }

  public override suspend fun updateTrack(track: MidiTrack): Unit = performSuspending(__db, false,
      true) { _connection ->
    __updateAdapterOfMidiTrack.handle(_connection, track)
  }

  public override fun getTracksForSession(sessionId: String): Flow<List<MidiTrack>> {
    val _sql: String = "SELECT * FROM midi_tracks WHERE sessionId = ? ORDER BY `index` ASC"
    return createFlow(__db, false, arrayOf("midi_tracks")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, sessionId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSessionId: Int = getColumnIndexOrThrow(_stmt, "sessionId")
        val _columnIndexOfIndex: Int = getColumnIndexOrThrow(_stmt, "index")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfVoice: Int = getColumnIndexOrThrow(_stmt, "voice")
        val _columnIndexOfColor: Int = getColumnIndexOrThrow(_stmt, "color")
        val _columnIndexOfMidiChannel: Int = getColumnIndexOrThrow(_stmt, "midiChannel")
        val _columnIndexOfMuted: Int = getColumnIndexOrThrow(_stmt, "muted")
        val _columnIndexOfSolo: Int = getColumnIndexOrThrow(_stmt, "solo")
        val _columnIndexOfVolume: Int = getColumnIndexOrThrow(_stmt, "volume")
        val _result: MutableList<MidiTrack> = mutableListOf()
        while (_stmt.step()) {
          val _item: MidiTrack
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpSessionId: String
          _tmpSessionId = _stmt.getText(_columnIndexOfSessionId)
          val _tmpIndex: Int
          _tmpIndex = _stmt.getLong(_columnIndexOfIndex).toInt()
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpVoice: String
          _tmpVoice = _stmt.getText(_columnIndexOfVoice)
          val _tmpColor: Long
          _tmpColor = _stmt.getLong(_columnIndexOfColor)
          val _tmpMidiChannel: Int
          _tmpMidiChannel = _stmt.getLong(_columnIndexOfMidiChannel).toInt()
          val _tmpMuted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfMuted).toInt()
          _tmpMuted = _tmp != 0
          val _tmpSolo: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfSolo).toInt()
          _tmpSolo = _tmp_1 != 0
          val _tmpVolume: Float
          _tmpVolume = _stmt.getDouble(_columnIndexOfVolume).toFloat()
          _item =
              MidiTrack(_tmpId,_tmpSessionId,_tmpIndex,_tmpName,_tmpVoice,_tmpColor,_tmpMidiChannel,_tmpMuted,_tmpSolo,_tmpVolume)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getTrackById(id: String): MidiTrack? {
    val _sql: String = "SELECT * FROM midi_tracks WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSessionId: Int = getColumnIndexOrThrow(_stmt, "sessionId")
        val _columnIndexOfIndex: Int = getColumnIndexOrThrow(_stmt, "index")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfVoice: Int = getColumnIndexOrThrow(_stmt, "voice")
        val _columnIndexOfColor: Int = getColumnIndexOrThrow(_stmt, "color")
        val _columnIndexOfMidiChannel: Int = getColumnIndexOrThrow(_stmt, "midiChannel")
        val _columnIndexOfMuted: Int = getColumnIndexOrThrow(_stmt, "muted")
        val _columnIndexOfSolo: Int = getColumnIndexOrThrow(_stmt, "solo")
        val _columnIndexOfVolume: Int = getColumnIndexOrThrow(_stmt, "volume")
        val _result: MidiTrack?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpSessionId: String
          _tmpSessionId = _stmt.getText(_columnIndexOfSessionId)
          val _tmpIndex: Int
          _tmpIndex = _stmt.getLong(_columnIndexOfIndex).toInt()
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpVoice: String
          _tmpVoice = _stmt.getText(_columnIndexOfVoice)
          val _tmpColor: Long
          _tmpColor = _stmt.getLong(_columnIndexOfColor)
          val _tmpMidiChannel: Int
          _tmpMidiChannel = _stmt.getLong(_columnIndexOfMidiChannel).toInt()
          val _tmpMuted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfMuted).toInt()
          _tmpMuted = _tmp != 0
          val _tmpSolo: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfSolo).toInt()
          _tmpSolo = _tmp_1 != 0
          val _tmpVolume: Float
          _tmpVolume = _stmt.getDouble(_columnIndexOfVolume).toFloat()
          _result =
              MidiTrack(_tmpId,_tmpSessionId,_tmpIndex,_tmpName,_tmpVoice,_tmpColor,_tmpMidiChannel,_tmpMuted,_tmpSolo,_tmpVolume)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteTracksForSession(sessionId: String) {
    val _sql: String = "DELETE FROM midi_tracks WHERE sessionId = ?"
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
