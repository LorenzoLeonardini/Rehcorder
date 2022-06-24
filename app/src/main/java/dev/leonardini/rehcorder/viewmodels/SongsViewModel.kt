package dev.leonardini.rehcorder.viewmodels

import android.database.Cursor
import androidx.lifecycle.*
import dev.leonardini.rehcorder.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SongsViewModel(private val database: AppDatabase) : ViewModel() {

    private var fetchJob: Job? = null

    private val songs: MutableLiveData<Cursor> by lazy {
        MutableLiveData<Cursor>().also {
            fetchSongs()
        }
    }

    fun getSongs(): LiveData<Cursor> {
        return songs
    }

    fun updateSongName(id: Long, name: String) {
        viewModelScope.launch {
            database.songDao().updateName(id, name)
            fetchSongs()
        }
    }

    private fun fetchSongs() {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch(Dispatchers.IO) {
            songs.postValue(database.songDao().getAllCursor())
        }
    }

}

class SongsViewModelFactory(private val database: AppDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SongsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SongsViewModel(database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}