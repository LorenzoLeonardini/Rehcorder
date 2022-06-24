package dev.leonardini.rehcorder.viewmodels

import android.database.Cursor
import androidx.lifecycle.*
import dev.leonardini.rehcorder.db.AppDatabase
import dev.leonardini.rehcorder.db.Rehearsal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class RehearsalsViewModel(private val database: AppDatabase) : ViewModel() {

    private var fetchJob: Job? = null
    private var rehearsalFetchJob: Job? = null

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

class RehearsalsViewModelFactory(private val database: AppDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RehearsalsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RehearsalsViewModel(database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}