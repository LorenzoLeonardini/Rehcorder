package dev.leonardini.rehcorder.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import dev.leonardini.rehcorder.adapters.SongsHeader
import dev.leonardini.rehcorder.db.AppDatabase
import dev.leonardini.rehcorder.db.Database
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SongsViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase

    init {
        database = Database.getInstance(application)
    }

    val songs = Pager(PagingConfig(pageSize = 10)) {
        database.songDao().getAll()
    }.flow.map { pagingData ->
        pagingData.insertSeparators { before, _ ->
            when (before) {
                null -> SongsHeader()
                else -> null
            }
        }
    }.cachedIn(viewModelScope)

    fun updateSongName(id: Long, name: String) {
        viewModelScope.launch {
            database.songDao().updateName(id, name)
        }
    }

}
