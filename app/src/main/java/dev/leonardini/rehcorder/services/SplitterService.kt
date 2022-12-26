package dev.leonardini.rehcorder.services

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import dev.leonardini.rehcorder.R
import dev.leonardini.rehcorder.Utils
import dev.leonardini.rehcorder.db.AppDatabase
import dev.leonardini.rehcorder.db.Database
import dev.leonardini.rehcorder.db.Rehearsal
import dev.leonardini.rehcorder.ui.SettingsFragment
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.*

class SplitterService : ForegroundIntentService("SplitterService") {

    data class SplittingRegion(val start: Long, val end: Long, val id: Long)

    override fun onHandleIntent(intent: Intent) {
        runBlocking {
            val rehearsalId = intent.getLongExtra("id", -1L)
            if (rehearsalId == -1L) {
                throw IllegalArgumentException("Missing id in start service intent")
            }
            val fileName = intent.getStringExtra("file")!!

            @Suppress("UNCHECKED_CAST")
            val regions = intent.getSerializableExtra("regions") as ArrayList<Long>

            val regionsQueue: Queue<SplittingRegion> = LinkedList()
            for (i in 0 until regions.size / 3) {
                regionsQueue.add(
                    SplittingRegion(
                        regions[i * 3],
                        regions[i * 3 + 1],
                        regions[i * 3 + 2]
                    )
                )
            }

            val currentMaxProgress = regionsQueue.size
            updateProgress(regionsQueue.size, currentMaxProgress)

            val database = Database.getInstance(applicationContext)

            while (regionsQueue.size > 0) {
                val region = regionsQueue.poll() ?: return@runBlocking
                val startSeek = region.start / 1000f
                val endSeek = region.end / 1000f
                Log.i("Splitter", "Splitting $fileName for song id ${region.id}")

                val (externalStorage, baseDir) = Utils.getPreferredStorageLocation(this@SplitterService)

                // Check folder or create
                val folder = File(Utils.getSongPath(baseDir, ""))
                if (!folder.exists())
                    folder.mkdirs()

                // Insert into database and generate file name
                val songFileFriendlyName = insertIntoDBAndGenerateFileName(
                    database,
                    region.id,
                    rehearsalId,
                    externalStorage
                )
                val songPath = Utils.getSongPath(baseDir, songFileFriendlyName)
                Log.i("Splitter", "Splitting into $songPath")

                val session =
                    FFmpegKit.execute("-y -ss $startSeek -to $endSeek -i $fileName $songPath")

                if (ReturnCode.isSuccess(session.returnCode)) {
                    // SUCCESS
                    updateProgress(regionsQueue.size, currentMaxProgress)
                } else if (ReturnCode.isCancel(session.returnCode)) {
                    // CANCEL
                } else {
                    // FAILURE
                }
            }
            Database.getInstance(applicationContext).rehearsalDao()
                .updateStatus(rehearsalId, Rehearsal.PROCESSED)

            val preference = PreferenceManager.getDefaultSharedPreferences(this@SplitterService)
            val deleteRecording = preference.getBoolean(SettingsFragment.DELETE_RECORDING, false)
            if (deleteRecording) {
                File(fileName).delete()
            }
        }
    }

    private suspend fun insertIntoDBAndGenerateFileName(
        database: AppDatabase,
        regionId: Long,
        rehearsalId: Long,
        externalStorage: Boolean
    ): String {
        val song = database.songDao().getSongAsync(regionId)!!
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

    private fun createNotificationBuilder(notificationManager: NotificationManager? = null): NotificationCompat.Builder {
        val nm = notificationManager
            ?: getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        Utils.createServiceNotificationChannelIfNotExists(nm)

        return NotificationCompat.Builder(this, "dev.leonardini.rehcorder")
            .setContentTitle(resources.getString(R.string.notification_splitter_title))
            .setSmallIcon(R.drawable.ic_mic)
            .setContentText(resources.getString(R.string.notification_splitter_text))
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
    }

    private fun updateProgress(progress: Int, maxProgress: Int) {
        val nm: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = createNotificationBuilder(nm)
            .setProgress(maxProgress, maxProgress - progress, false)
            .build()

        nm.notify(42, notification)
    }

    override fun startForegroundNotification() {
        var notificationBuilder = createNotificationBuilder()
        if (Build.VERSION.SDK_INT >= 31) {
            notificationBuilder =
                notificationBuilder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
        val notification = notificationBuilder.build()

        startForeground(42, notification)
    }
}