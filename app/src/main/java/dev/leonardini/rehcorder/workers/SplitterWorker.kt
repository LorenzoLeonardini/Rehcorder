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
import dev.leonardini.rehcorder.db.AppDatabase
import dev.leonardini.rehcorder.db.Database
import java.io.File

class SplitterWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    companion object {
        private const val NOTIFICATION_ID: Int = 42
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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
        val songId = inputData.getLong("songId", -1L)
        if (songId == -1L) {
            return Result.failure()
        }
        val regionStart = inputData.getLong("regionStart", -1L)
        if (regionStart == -1L) {
            return Result.failure()
        }
        val regionEnd = inputData.getLong("regionEnd", -1L)
        if (regionEnd == -1L) {
            return Result.failure()
        }

        setForeground(createForegroundInfo())

        try {
            val startSeek = regionStart / 1000f
            val endSeek = regionEnd / 1000f
            Log.i("Splitter", "Splitting $fileName for song id $songId")

            val (externalStorage, baseDir) = Utils.getPreferredStorageLocation(applicationContext)

            // Check folder or create
            val folder = File(Utils.getSongPath(baseDir, ""))
            if (!folder.exists())
                folder.mkdirs()

            // Insert into database and generate file name
            val songFileFriendlyName = insertIntoDBAndGenerateFileName(
                database,
                songId,
                rehearsalId,
                externalStorage
            )
            val songPath = Utils.getSongPath(baseDir, songFileFriendlyName)
            Log.i("Splitter", "Splitting into $songPath")

            val session =
                FFmpegKit.execute("-y -ss $startSeek -to $endSeek -i $fileName $songPath")

            return if (ReturnCode.isSuccess(session.returnCode)) {
                Result.success()
            } else if (ReturnCode.isCancel(session.returnCode)) {
                Result.retry()
            } else {
                Result.retry()
            }
        } catch (_: Exception) {
            return Result.retry()
        }
    }

    private suspend fun insertIntoDBAndGenerateFileName(
        database: AppDatabase,
        regionId: Long,
        rehearsalId: Long,
        externalStorage: Boolean
    ): String {
        val song = database.songDao().getSong(regionId)!!
        var songFileFriendlyName = song.name.replace(" ", "_").replace(Regex("\\W+"), "")
        val songRecordingUid =
            database.songRecordingDao()
                .insert(
                    regionId,
                    rehearsalId,
                    song.name.replace(" ", "_"),
                    externalStorage
                )
        val songRecording = database.songRecordingDao().get(songRecordingUid)!!
        songFileFriendlyName =
            "${song.uid}_${songFileFriendlyName}_${songRecording.version}.m4a"
        database.songRecordingDao().updateFileName(songRecordingUid, songFileFriendlyName)
        return songFileFriendlyName
    }

    private fun createForegroundInfo(): ForegroundInfo {
        Utils.createServiceNotificationChannelIfNotExists(notificationManager)

        val notification =
            NotificationCompat.Builder(applicationContext, Constants.NOTIFICATION_CHANNEL)
                .setContentTitle(applicationContext.getString(R.string.notification_splitter_title))
                .setSmallIcon(R.drawable.ic_mic)
                .setContentText(applicationContext.getString(R.string.notification_splitter_text))
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOngoing(true)
                .setProgress(100, 0, true)
                .setSilent(true)
                .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

}