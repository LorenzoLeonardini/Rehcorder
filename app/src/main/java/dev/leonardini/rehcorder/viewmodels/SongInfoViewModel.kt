package dev.leonardini.rehcorder.viewmodels

import android.database.Cursor
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import dev.leonardini.rehcorder.db.AppDatabase
import dev.leonardini.rehcorder.db.Song
import kotlinx.coroutines.Dispatchers

class SongInfoViewModel(private val database: AppDatabase, private val songId: Long) :
    ViewModel() {

    val song: LiveData<Song> = liveData(Dispatchers.IO) {
        emit(database.songDao().getSong(songId)!!)
    }

    val songRehearsals: LiveData<Cursor> = liveData(Dispatchers.IO) {
        emit(database.songRecordingDao().getSongSortedCursor(songId))
    }
}

class SongViewModelFactory(private val database: AppDatabase, private val songId: Long) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SongInfoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SongInfoViewModel(database, songId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}