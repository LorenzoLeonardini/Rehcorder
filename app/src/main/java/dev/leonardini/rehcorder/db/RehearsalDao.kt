package dev.leonardini.rehcorder.db

import android.database.Cursor
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface RehearsalDao {

    @Query("SELECT * FROM rehearsal ORDER BY date DESC")
    fun getAll(): List<Rehearsal>

    @Query(
        "SELECT rehearsal.*, COUNT(song_recording.uid) AS songs_count FROM rehearsal " +
                "LEFT JOIN song_recording ON rehearsal.uid=song_recording.recording_id " +
                "GROUP BY rehearsal.uid, name, status, date, rehearsal.file_name, external_storage " +
                "ORDER BY date DESC"
    )
    fun getAllCursor(): Cursor

    @Query("SELECT * FROM rehearsal WHERE status = ${Rehearsal.NORMALIZED} LIMIT 1")
    fun getUnprocessedRehearsal(): Rehearsal?

    @Query("SELECT * FROM rehearsal WHERE uid=:id")
    fun getRehearsal(id: Long): Rehearsal?

    @Insert
    fun insert(rehearsal: Rehearsal): Long

    @Update
    fun update(rehearsal: Rehearsal)

    @Query("UPDATE rehearsal SET name=:name WHERE uid=:id")
    fun updateName(id: Long, name: String?)

    @Query("UPDATE rehearsal SET status=:status WHERE uid=:id")
    fun updateStatus(id: Long, status: Int)
}