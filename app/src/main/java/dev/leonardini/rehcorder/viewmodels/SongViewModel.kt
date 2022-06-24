package dev.leonardini.rehcorder.viewmodels

import android.database.Cursor
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import dev.leonardini.rehcorder.db.AppDatabase
import dev.leonardini.rehcorder.db.Song
import kotlinx.coroutines.Dispatchers

class SongViewModel(private val database: AppDatabase, private val songId: Long) :
    ViewModel() {

    private val song: LiveData<Song> = liveData(Dispatchers.IO) {
        emit(database.songDao().getSong(songId)!!)
    }

    private val songRehearsals: LiveData<Cursor> = liveData(Dispatchers.IO) {
        emit(database.songRecordingDao().getSongSortedCursor(songId))
    }

    fun getSong(): LiveData<Song> {
        return song
    }

    fun getSongRehearsals(): LiveData<Cursor> {
        return songRehearsals
    }
}

class SongViewModelFactory(private val database: AppDatabase, private val songId: Long) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SongViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SongViewModel(database, songId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}