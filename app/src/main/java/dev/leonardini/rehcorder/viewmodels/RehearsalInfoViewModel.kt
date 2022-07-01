package dev.leonardini.rehcorder.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import dev.leonardini.rehcorder.Utils
import dev.leonardini.rehcorder.adapters.RehearsalsInfoHeader
import dev.leonardini.rehcorder.db.AppDatabase
import dev.leonardini.rehcorder.db.Database
import dev.leonardini.rehcorder.db.Rehearsal
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File

class RehearsalInfoViewModel(application: Application, private val rehearsalId: Long) :
    AndroidViewModel(application) {

    private val database: AppDatabase

    init {
        database = Database.getInstance(application)
    }

    val rehearsal: LiveData<Rehearsal> = liveData {
        emit(database.rehearsalDao().getRehearsal(rehearsalId))
    }

    val rehearsalSongs = Pager(PagingConfig(pageSize = 10)) {
        database.songRecordingDao().getRehearsalSongs(rehearsalId)
    }.flow.map { pagingData ->
        pagingData.insertSeparators { before, _ ->
            when (before) {
                null -> RehearsalsInfoHeader()
                else -> null
            }
        }
    }.cachedIn(viewModelScope)

    fun deleteRehearsal(applicationContext: Context) {
        viewModelScope.launch {
            rehearsal.value!!.let { rehearsal ->
                val externalStorageBaseDir =
                    applicationContext.getExternalFilesDir(null) ?: applicationContext.filesDir
                File(
                    Utils.getRecordingPath(
                        if (rehearsal.externalStorage) externalStorageBaseDir else applicationContext.filesDir,
                        rehearsal.fileName
                    )
                ).delete()

                for (song in database.songRecordingDao().getRehearsalSongsRecordings(rehearsalId)) {
                    val fileName: String = song.fileName
                    val externalStorage: Boolean = song.externalStorage

                    File(
                        Utils.getSongPath(
                            if (externalStorage) externalStorageBaseDir else applicationContext.filesDir,
                            fileName
                        )
                    ).delete()
                }

                database.songRecordingDao().deleteRehearsal(rehearsalId)
                database.rehearsalDao().delete(rehearsalId)
            }
        }
    }
}

class RehearsalViewModelFactory(
    private val application: Application,
    private val rehearsalId: Long
) :
    ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RehearsalInfoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RehearsalInfoViewModel(application, rehearsalId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}