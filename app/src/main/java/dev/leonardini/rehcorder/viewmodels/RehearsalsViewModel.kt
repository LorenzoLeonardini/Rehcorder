package dev.leonardini.rehcorder.viewmodels

import android.app.Application
import android.database.Cursor
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dev.leonardini.rehcorder.db.AppDatabase
import dev.leonardini.rehcorder.db.Database
import dev.leonardini.rehcorder.db.Rehearsal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class RehearsalsViewModel(application: Application) : AndroidViewModel(application) {

    private var fetchJob: Job? = null
    private var rehearsalFetchJob: Job? = null
    private val database: AppDatabase

    init {
        database = Database.getInstance(application)
    }

    val rehearsals: LiveData<Cursor>
        get() = _rehearsals
    private val _rehearsals: MutableLiveData<Cursor> by lazy {
        MutableLiveData<Cursor>().also {
            fetchRehearsals()
        }
    }

    val inNeedOfProcessRehearsal: LiveData<Rehearsal?>
        get() = _inNeedOfProcessRehearsal
    private val _inNeedOfProcessRehearsal: MutableLiveData<Rehearsal?> by lazy {
        MutableLiveData<Rehearsal?>().also {
            fetchInNeedOfProcess()
        }
    }

    fun updateRehearsalName(id: Long, name: String?) {
        viewModelScope.launch {
            database.rehearsalDao().updateName(id, name)
            fetchRehearsals()
        }
    }

    fun getRehearsalStatus(id: Long): Int {
        return database.rehearsalDao().getRehearsal(id).status
    }

    fun update() {
        fetchRehearsals()
        fetchInNeedOfProcess()
    }

    private fun fetchRehearsals() {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch(Dispatchers.IO) {
            _rehearsals.postValue(database.rehearsalDao().getAllCursor())
        }
    }

    private fun fetchInNeedOfProcess() {
        rehearsalFetchJob?.cancel()
        rehearsalFetchJob = viewModelScope.launch(Dispatchers.IO) {
            _inNeedOfProcessRehearsal.postValue(database.rehearsalDao().getUnprocessedRehearsal())
        }
    }
}
