package dev.leonardini.rehcorder.workers

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import dev.leonardini.rehcorder.Constants
import dev.leonardini.rehcorder.R
import dev.leonardini.rehcorder.Utils
import java.io.File

abstract class NormalizerWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    protected abstract val NOTIFICATION_ID: Int

    override suspend fun doWork(): Result {
        if (runAttemptCount > 10) {
            return Result.failure()
        }

        return try {
            val result = prepare()
            if (result != null) {
                result
            } else {
                val (fileName, tempFileName) = generateFileNames()
                setForeground(createForegroundInfo())
                normalize(fileName, tempFileName)
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun normalize(fileName: String, tempFileName: String): Result {
        Log.i("Normalizer", "Normalizing $fileName")

        val session =
            FFmpegKit.execute("-y -i $fileName -af loudnorm -ar 44100 $tempFileName")
        if (ReturnCode.isSuccess(session.returnCode)) {
            // SUCCESS
            val file = File(tempFileName)
            val destinationFile = File(fileName)
            val result = file.renameTo(destinationFile)
            Log.i("Normalizer", "Renaming result : $result")

            onSuccess()
            return Result.success()
        } else if (ReturnCode.isCancel(session.returnCode)) {
            onCancel()
            return Result.retry()
        } else {
            onFail()
            return Result.retry()
        }
    }

    protected abstract suspend fun prepare(): Result?
    protected abstract suspend fun generateFileNames(): Pair<String, String>
    protected abstract suspend fun onSuccess()
    protected abstract suspend fun onCancel()
    protected abstract suspend fun onFail()

    private fun createForegroundInfo(): ForegroundInfo {
        Utils.createServiceNotificationChannelIfNotExists(notificationManager)

        val notification =
            NotificationCompat.Builder(applicationContext, Constants.NOTIFICATION_CHANNEL)
                .setContentTitle(applicationContext.getString(R.string.notification_normalizer_title))
                .setSmallIcon(R.drawable.ic_mic)
                .setContentText(applicationContext.getString(R.string.notification_normalizer_text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOngoing(true)
                .setProgress(100, 0, true)
                .setSilent(true)
                .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

}