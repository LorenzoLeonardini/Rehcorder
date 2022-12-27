package dev.leonardini.rehcorder.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import dev.leonardini.rehcorder.adapters.UiModel
import kotlinx.coroutines.flow.Flow

@Dao
interface RehearsalDao {

    @Query(
        "SELECT rehearsal.*, COUNT(song_recording.uid) AS songs_count FROM rehearsal " +
                "LEFT JOIN song_recording ON rehearsal.uid=song_recording.recording_id " +
                "GROUP BY rehearsal.uid, name, status, date, rehearsal.file_name, rehearsal.external_storage " +
                "ORDER BY date DESC"
    )
    fun getAll(): PagingSource<Int, RehearsalWithSongsCount>

    @Query("SELECT * FROM rehearsal WHERE status = ${Rehearsal.NORMALIZED} LIMIT 1")
    fun getUnprocessedRehearsal(): Flow<Rehearsal?>

    // Used while migrating to Works, to recover crashed normalizations
    @Query("SELECT * FROM rehearsal WHERE status = ${Rehearsal.RECORDED} AND worker = FALSE")
    suspend fun getStuckRehearsals(): List<Rehearsal>

    @Query("SELECT * FROM rehearsal WHERE uid=:id")
    suspend fun getRehearsal(id: Long): Rehearsal

    @Insert
    suspend fun insert(rehearsal: Rehearsal): Long

    @Update
    suspend fun update(rehearsal: Rehearsal)

    @Query("UPDATE rehearsal SET name=:name WHERE uid=:id")
    suspend fun updateName(id: Long, name: String?)

    @Query("UPDATE rehearsal SET status=:status WHERE uid=:id")
    suspend fun updateStatus(id: Long, status: Int)

    @Query("DELETE FROM rehearsal WHERE uid=:id")
    suspend fun delete(id: Long)
}

data class RehearsalWithSongsCount(
    val uid: Long,
    val name: String?,
    val status: Int,
    val date: Long,
    val file_name: String,
    val external_storage: Boolean,
    val songs_count: Int
) : UiModel()