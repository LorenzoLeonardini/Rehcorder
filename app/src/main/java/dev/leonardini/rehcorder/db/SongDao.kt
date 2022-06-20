package dev.leonardini.rehcorder.db

import android.database.Cursor
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SongDao {

    @Query("SELECT * FROM song")
    fun getAll(): List<Song>

    @Query("SELECT * FROM song ORDER BY name")
    fun getAllCursor(): Cursor

    @Query("SELECT * FROM song ORDER BY name")
    fun getAllSorted(): List<Song>

    @Insert
    fun insert(songs: Song): Long

}