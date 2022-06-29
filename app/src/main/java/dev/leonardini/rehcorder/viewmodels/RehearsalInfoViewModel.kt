package dev.leonardini.rehcorder.viewmodels

import android.app.Application
import android.content.Context
import android.database.Cursor
import androidx.lifecycle.*
import dev.leonardini.rehcorder.Utils
import dev.leonardini.rehcorder.db.AppDatabase
import dev.leonardini.rehcorder.db.Database
import dev.leonardini.rehcorder.db.Rehearsal
import kotlinx.coroutines.Dispatchers
import java.io.File

class RehearsalInfoViewModel(application: Application, private val rehearsalId: Long) :
    AndroidViewModel(application) {

    private val database: AppDatabase

    init {
        database = Database.getInstance(application)
    }

    val rehearsal: LiveData<Rehearsal> = liveData(Dispatchers.IO) {
        emit(database.rehearsalDao().getRehearsal(rehearsalId))
    }

    val rehearsalSongs: LiveData<Cursor> = liveData(Dispatchers.IO) {
        emit(database.songRecordingDao().getRehearsalSortedCursor(rehearsalId))
    }

    fun deleteRehearsal(applicationContext: Context) {
        rehearsal.value!!.let { rehearsal ->
            val externalStorageBaseDir =
                applicationContext.getExternalFilesDir(null) ?: applicationContext.filesDir
            File(
                Utils.getRecordingPath(
                    if (rehearsal.externalStorage) externalStorageBaseDir else applicationContext.filesDir,
                    rehearsal.fileName
                )
            ).delete()

            rehearsalSongs.value!!.let { songs ->
                for (i in 0 until songs.count) {
                    songs.moveToPosition(i)
                    val fileName: String =
                        songs.getString(songs.getColumnIndexOrThrow("file_name"))
                    val externalStorage: Boolean =
                        songs.getInt(songs.getColumnIndexOrThrow("external_storage")) == 1

                    File(
                        Utils.getSongPath(
                            if (externalStorage) externalStorageBaseDir else applicationContext.filesDir,
                            fileName
                        )
                    ).delete()
                }
            }

            database.songRecordingDao().deleteRehearsal(rehearsalId)
            database.rehearsalDao().delete(rehearsalId)
        }

    }
}

class RehearsalViewModelFactory(
    private val application: Application,
    private val rehearsalId: Long
) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RehearsalInfoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RehearsalInfoViewModel(application, rehearsalId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}