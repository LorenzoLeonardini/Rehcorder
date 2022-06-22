package dev.leonardini.rehcorder.db

import android.database.Cursor
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SongDao {

    @Query("SELECT * FROM song")
    fun getAll(): List<Song>

    @Query(
        "SELECT song.*, COUNT(song_recording.uid) AS versions_count FROM song " +
                "LEFT JOIN song_recording ON song.uid=song_recording.song_id " +
                "GROUP BY song.uid, name " +
                "ORDER BY name"
    )
    fun getAllCursor(): Cursor

    @Query("SELECT * FROM song ORDER BY name")
    fun getAllSorted(): List<Song>

    @Query("SELECT * FROM song WHERE uid=:id LIMIT 1")
    fun getSong(id: Long): Song?

    @Insert
    fun insert(songs: Song): Long

    @Query("UPDATE song SET name=:name WHERE uid=:id")
    fun updateName(id: Long, name: String?)

}