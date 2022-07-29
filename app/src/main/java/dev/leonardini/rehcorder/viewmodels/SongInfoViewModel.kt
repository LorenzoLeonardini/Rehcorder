package dev.leonardini.rehcorder.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import dev.leonardini.rehcorder.Utils
import dev.leonardini.rehcorder.adapters.SongsInfoHeader
import dev.leonardini.rehcorder.db.AppDatabase
import dev.leonardini.rehcorder.db.Database
import dev.leonardini.rehcorder.db.Song
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File

class SongInfoViewModel(application: Application, private val songId: Long) :
    AndroidViewModel(application) {

    private val database: AppDatabase

    init {
        database = Database.getInstance(application)
    }

    val song: LiveData<Song> = liveData {
        emit(database.songDao().getSong(songId)!!)
    }

    val songRehearsals = Pager(PagingConfig(pageSize = 10)) {
        database.songRecordingDao().getSongRehearsals(songId)
    }.flow.map { pagingData ->
        pagingData.insertSeparators { before, _ ->
            when (before) {
                null -> SongsInfoHeader()
                else -> null
            }
        }
    }.cachedIn(viewModelScope)

    fun deleteSong(applicationContext: Context) {
        viewModelScope.launch {
            val externalStorageBaseDir =
                applicationContext.getExternalFilesDir(null) ?: applicationContext.filesDir

            for (songRecording in database.songRecordingDao()
                .getRehearsalSongsRecordingsFromSong(songId)) {
                val fileName: String = songRecording.fileName
                val externalStorage: Boolean = songRecording.externalStorage

                File(
                    Utils.getSongPath(
                        if (externalStorage) externalStorageBaseDir else applicationContext.filesDir,
                        fileName
                    )
                ).delete()
            }

            database.songRecordingDao().deleteSong(songId)
            database.songDao().delete(songId)
        }
    }

}

class SongViewModelFactory(private val application: Application, private val songId: Long) :
    ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SongInfoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SongInfoViewModel(application, songId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}