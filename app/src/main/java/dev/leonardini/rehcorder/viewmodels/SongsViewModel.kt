package dev.leonardini.rehcorder.viewmodels

import android.app.Application
import android.database.Cursor
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dev.leonardini.rehcorder.db.AppDatabase
import dev.leonardini.rehcorder.db.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SongsViewModel(application: Application) : AndroidViewModel(application) {

    private var fetchJob: Job? = null
    private val database: AppDatabase

    init {
        database = Database.getInstance(application)
    }

    val songs: LiveData<Cursor>
        get() = _songs

    private val _songs: MutableLiveData<Cursor> by lazy {
        MutableLiveData<Cursor>().also {
            fetchSongs()
        }
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
            _songs.postValue(database.songDao().getAllCursor())
        }
    }

}
