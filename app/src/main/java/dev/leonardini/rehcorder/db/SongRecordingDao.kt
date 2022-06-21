package dev.leonardini.rehcorder.db

import androidx.room.Dao
import androidx.room.Insert

@Dao
interface SongRecordingDao {

    //    @Query("SELECT * FROM song")
//    fun getAll(): List<Song>
//
//    @Query("SELECT * FROM song ORDER BY name")
//    fun getAllSorted(): List<Song>
//
    @Insert
    fun insert(songRecording: SongRecording): Long

}