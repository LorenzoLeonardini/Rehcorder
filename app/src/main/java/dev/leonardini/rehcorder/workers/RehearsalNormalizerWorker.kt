package dev.leonardini.rehcorder.workers

import android.content.Context
import androidx.work.WorkerParameters
import dev.leonardini.rehcorder.db.Database
import dev.leonardini.rehcorder.db.Rehearsal
import java.io.File

class RehearsalNormalizerWorker(context: Context, parameters: WorkerParameters) :
    NormalizerWorker(context, parameters) {

    override val NOTIFICATION_ID: Int = 31337

    private val database = Database.getInstance(applicationContext)

    private var id: Long = -1
    private lateinit var fileName: String
    private lateinit var tempFileName: String

    override suspend fun prepare(): Result? {
        id = inputData.getLong("id", -1L)
        if (id == -1L) {
            return Result.failure()
        }
        fileName = inputData.getString("file") ?: return Result.failure()
        tempFileName =
            "${File(fileName).parentFile!!.parentFile!!.absolutePath}/tmp-${id}-${System.currentTimeMillis()}.m4a"

        if (database.rehearsalDao().getRehearsal(id).status != Rehearsal.RECORDED) {
            return Result.success()
        }

        return null
    }

    override suspend fun generateFileNames(): Pair<String, String> {
        return Pair(fileName, tempFileName)
    }

    override suspend fun onSuccess() {
        database.rehearsalDao()
            .updateStatus(id, Rehearsal.NORMALIZED)
    }

    override suspend fun onCancel() {
    }

    override suspend fun onFail() {
    }

}