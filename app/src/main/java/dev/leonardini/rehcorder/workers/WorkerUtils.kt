package dev.leonardini.rehcorder.workers

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object WorkerUtils {

    fun enqueueNormalization(id: Long, file: String, context: Context) {
        val workRequest: OneTimeWorkRequest = OneTimeWorkRequestBuilder<RehearsalNormalizerWorker>()
            .setInputData(
                workDataOf(
                    "id" to id,
                    "file" to file
                )
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 2, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("NORMALIZER_WORK", ExistingWorkPolicy.APPEND_OR_REPLACE, workRequest)
    }

    fun enqueueSplitting(
        id: Long,
        file: String,
        regions: List<Triple<Long, Long, Long>>,
        context: Context
    ) {
        val splitRequests: ArrayList<OneTimeWorkRequest> = ArrayList(20)
        for (region in regions) {
            splitRequests.add(
                OneTimeWorkRequestBuilder<SplitterWorker>()
                    .setInputData(
                        workDataOf(
                            "id" to id,
                            "file" to file,
                            "songId" to region.first,
                            "regionStart" to region.second,
                            "regionEnd" to region.third
                        )
                    )
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 2, TimeUnit.SECONDS)
                    .build()
            )
        }

        val finishRequest: OneTimeWorkRequest = OneTimeWorkRequestBuilder<FinishSplittingWorker>()
            .setInputData(
                workDataOf(
                    "id" to id,
                    "file" to file
                )
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 2, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context)
            .beginWith(splitRequests)
            .then(finishRequest)
            .enqueue()
    }

}