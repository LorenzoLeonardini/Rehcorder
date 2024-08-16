package dev.leonardini.rehcorder.workers

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.leonardini.rehcorder.db.Database
import dev.leonardini.rehcorder.db.Rehearsal
import dev.leonardini.rehcorder.ui.SettingsFragment
import java.io.File

class FinishSplittingWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    private val database = Database.getInstance(applicationContext)

    override suspend fun doWork(): Result {
        if (runAttemptCount > 10) {
            return Result.failure()
        }

        val rehearsalId = inputData.getLong("id", -1L)
        if (rehearsalId == -1L) {
            return Result.failure()
        }
        val fileName = inputData.getString("file") ?: return Result.failure()

        return try {
            database.rehearsalDao()
                .updateStatus(rehearsalId, Rehearsal.PROCESSED)

            val preference = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            val deleteRecording = preference.getBoolean(SettingsFragment.DELETE_RECORDING, false)
            if (deleteRecording) {
                File(fileName).delete()
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

}