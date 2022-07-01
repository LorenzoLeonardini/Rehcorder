package dev.leonardini.rehcorder.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import dev.leonardini.rehcorder.adapters.RehearsalsHeader
import dev.leonardini.rehcorder.db.AppDatabase
import dev.leonardini.rehcorder.db.Database
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class RehearsalsViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase

    init {
        database = Database.getInstance(application)
    }

    val rehearsals = Pager(PagingConfig(pageSize = 10)) {
        database.rehearsalDao().getAll()
    }.flow.map { pagingData ->
        pagingData.insertSeparators { before, _ ->
            when (before) {
                null -> RehearsalsHeader()
                else -> null
            }
        }
    }.cachedIn(viewModelScope)

    val inNeedOfProcessRehearsal = database.rehearsalDao().getUnprocessedRehearsal()

    fun updateRehearsalName(id: Long, name: String?) {
        viewModelScope.launch {
            database.rehearsalDao().updateName(id, name)
        }
    }

    suspend fun getRehearsalStatus(id: Long): Int {
        return database.rehearsalDao().getRehearsal(id).status
    }

}
