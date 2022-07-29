package dev.leonardini.rehcorder.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import dev.leonardini.rehcorder.adapters.UiModel

@Dao
interface SongRecordingDao {

    @Query("SELECT * FROM song_recording where uid=:id")
    suspend fun get(id: Long): SongRecording?

    @Query(
        "SELECT song_recording.*, song.name FROM song_recording " +
                "INNER JOIN song ON song.uid=song_recording.song_id " +
                "WHERE song_recording.recording_id=:id " +
                "ORDER BY song_recording.uid"
    )
    fun getRehearsalSongs(id: Long): PagingSource<Int, RehearsalSongs>

    @Query(
        "SELECT * FROM song_recording WHERE song_recording.recording_id=:id"
    )
    suspend fun getRehearsalSongsRecordingsFromRehearsal(id: Long): List<SongRecording>

    @Query(
        "SELECT * FROM song_recording WHERE song_recording.song_id=:id"
    )
    suspend fun getRehearsalSongsRecordingsFromSong(id: Long): List<SongRecording>

    @Query(
        "SELECT song_recording.*, rehearsal.name, rehearsal.date FROM song_recording " +
                "INNER JOIN rehearsal ON rehearsal.uid=song_recording.recording_id " +
                "WHERE song_recording.song_id=:id " +
                "ORDER BY song_recording.uid DESC"
    )
    fun getSongRehearsals(id: Long): PagingSource<Int, SongRehearsals>

    @Query(
        "INSERT INTO song_recording " +
                "(song_id, recording_id, file_name, external_storage, version) VALUES " +
                "(:songId, :recordingId, :fileName, :externalStorage, (" +
                "SELECT IFNULL(MAX(version), 0) + 1 FROM song_recording WHERE song_id=:songId)" +
                ")"
    )
    suspend fun insert(
        songId: Long,
        recordingId: Long,
        fileName: String,
        externalStorage: Boolean
    ): Long

    @Query("UPDATE song_recording SET file_name=:fileName WHERE uid=:id")
    suspend fun updateFileName(id: Long, fileName: String)

    @Query("DELETE FROM song_recording WHERE recording_id=:id")
    suspend fun deleteRehearsal(id: Long)

    @Query("DELETE FROM song_recording WHERE song_id=:id")
    suspend fun deleteSong(id: Long)
}

data class RehearsalSongs(
    val uid: Long,
    val version: Int,
    val song_id: Long,
    val recording_id: Long,
    val file_name: String,
    val external_storage: Boolean,
    val name: String
) : UiModel()

data class SongRehearsals(
    val uid: Long,
    val version: Int,
    val song_id: Long,
    val recording_id: Long,
    val file_name: String,
    val external_storage: Boolean,
    val name: String?,
    val date: Long
) : UiModel()