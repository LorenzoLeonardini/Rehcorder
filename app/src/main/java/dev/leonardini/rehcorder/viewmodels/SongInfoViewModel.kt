package dev.leonardini.rehcorder.viewmodels

import android.app.Application
import android.database.Cursor
import androidx.lifecycle.*
import dev.leonardini.rehcorder.db.AppDatabase
import dev.leonardini.rehcorder.db.Database
import dev.leonardini.rehcorder.db.Song
import kotlinx.coroutines.Dispatchers

class SongInfoViewModel(application: Application, private val songId: Long) :
    AndroidViewModel(application) {

    private val database: AppDatabase

    init {
        database = Database.getInstance(application)
    }

    val song: LiveData<Song> = liveData(Dispatchers.IO) {
        emit(database.songDao().getSong(songId)!!)
    }

    val songRehearsals: LiveData<Cursor> = liveData(Dispatchers.IO) {
        emit(database.songRecordingDao().getSongSortedCursor(songId))
    }
}

class SongViewModelFactory(private val application: Application, private val songId: Long) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SongInfoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SongInfoViewModel(application, songId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}