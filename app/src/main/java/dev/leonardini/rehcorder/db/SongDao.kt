package dev.leonardini.rehcorder.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SongDao {

    @Query("SELECT * FROM song")
    fun getAll(): List<Song>

    @Query("SELECT * FROM song ORDER BY name")
    fun getAllSorted(): List<Song>

    @Insert
    fun insert(songs: Song): Long

}