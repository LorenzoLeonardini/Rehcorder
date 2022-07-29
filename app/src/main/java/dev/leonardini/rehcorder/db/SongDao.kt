package dev.leonardini.rehcorder.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import dev.leonardini.rehcorder.adapters.UiModel

@Dao
interface SongDao {

    @Query(
        "SELECT song.*, COUNT(song_recording.uid) AS versions_count FROM song " +
                "LEFT JOIN song_recording ON song.uid=song_recording.song_id " +
                "GROUP BY song.uid, name " +
                "HAVING versions_count>0 " +
                "ORDER BY name"
    )
    fun getAll(): PagingSource<Int, SongWithVersionCount>

    @Query("SELECT * FROM song ORDER BY name")
    suspend fun getAllSorted(): List<Song>

    @Query("SELECT * FROM song WHERE uid=:id LIMIT 1")
    suspend fun getSong(id: Long): Song?

    @Insert
    suspend fun insert(songs: Song): Long

    @Query("UPDATE song SET name=:name WHERE uid=:id")
    suspend fun updateName(id: Long, name: String?)

    @Query("DELETE FROM song WHERE uid=:id")
    suspend fun delete(id: Long)

}

data class SongWithVersionCount(val uid: Long, val name: String, val versions_count: Int) :
    UiModel()