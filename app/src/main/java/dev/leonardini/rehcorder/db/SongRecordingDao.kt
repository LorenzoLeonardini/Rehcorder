package dev.leonardini.rehcorder.db

import android.database.Cursor
import androidx.room.Dao
import androidx.room.Query

@Dao
interface SongRecordingDao {

    @Query("SELECT * FROM song_recording where uid=:id")
    fun get(id: Long): SongRecording?

    @Query(
        "SELECT song_recording.*, song.name FROM song_recording " +
                "INNER JOIN song ON song.uid=song_recording.song_id " +
                "WHERE song_recording.recording_id=:id " +
                "ORDER BY song_recording.uid"
    )
    fun getRehearsalSortedCursor(id: Long): Cursor

    @Query(
        "SELECT song_recording.*, rehearsal.name, rehearsal.date FROM song_recording " +
                "INNER JOIN rehearsal ON rehearsal.uid=song_recording.recording_id " +
                "WHERE song_recording.song_id=:id " +
                "ORDER BY song_recording.uid DESC"
    )
    fun getSongSortedCursor(id: Long): Cursor

    @Query(
        "INSERT INTO song_recording " +
                "(song_id, recording_id, file_name, external_storage, version) VALUES " +
                "(:songId, :recordingId, :fileName, :externalStorage, (" +
                "SELECT IFNULL(MAX(version), 0) + 1 FROM song_recording WHERE song_id=:songId)" +
                ")"
    )
    fun insert(songId: Long, recordingId: Long, fileName: String, externalStorage: Boolean): Long

    @Query("DELETE FROM song_recording WHERE recording_id=:id")
    fun deleteRehearsal(id: Long)
}