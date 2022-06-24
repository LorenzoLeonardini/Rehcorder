package dev.leonardini.rehcorder.viewmodels

import android.content.Context
import android.database.Cursor
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import dev.leonardini.rehcorder.db.AppDatabase
import dev.leonardini.rehcorder.db.Rehearsal
import kotlinx.coroutines.Dispatchers
import java.io.File

class RehearsalViewModel(private val database: AppDatabase, private val rehearsalId: Long) :
    ViewModel() {

    private val rehearsal: LiveData<Rehearsal> = liveData(Dispatchers.IO) {
        emit(database.rehearsalDao().getRehearsal(rehearsalId))
    }

    private val rehearsalSongs: LiveData<Cursor> = liveData(Dispatchers.IO) {
        emit(database.songRecordingDao().getRehearsalSortedCursor(rehearsalId))
    }

    fun getRehearsal(): LiveData<Rehearsal> {
        return rehearsal
    }

    fun getRehearsalSongs(): LiveData<Cursor> {
        return rehearsalSongs
    }

    fun deleteRehearsal(applicationContext: Context) {
        rehearsal.value!!.let { rehearsal ->
            val externalStorageBaseDir =
                applicationContext.getExternalFilesDir(null) ?: applicationContext.filesDir
            File("${(if (rehearsal.externalStorage) externalStorageBaseDir else applicationContext.filesDir).absolutePath}/recordings/${rehearsal.fileName}").delete()

            rehearsalSongs.value!!.let { songs ->
                for (i in 1 until songs.count) {
                    songs.moveToPosition(i)
                    val version: Int = songs.getInt(songs.getColumnIndexOrThrow("version"))
                    val fileName: String =
                        songs.getString(songs.getColumnIndexOrThrow("file_name"))
                    val externalStorage: Boolean =
                        songs.getInt(songs.getColumnIndexOrThrow("external_storage")) == 1

                    File("${(if (externalStorage) externalStorageBaseDir else applicationContext.filesDir).absolutePath}/songs/${fileName}_$version.m4a").delete()
                }
            }

            database.songRecordingDao().deleteRehearsal(rehearsalId)
            database.rehearsalDao().delete(rehearsalId)
        }

    }
}

class RehearsalViewModelFactory(private val database: AppDatabase, private val rehearsalId: Long) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RehearsalViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RehearsalViewModel(database, rehearsalId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}