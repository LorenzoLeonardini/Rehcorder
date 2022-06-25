package dev.leonardini.rehcorder.services

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.arthenica.ffmpegkit.*
import dev.leonardini.rehcorder.R
import dev.leonardini.rehcorder.Utils
import dev.leonardini.rehcorder.db.Database
import dev.leonardini.rehcorder.db.Rehearsal
import java.io.File
import java.util.*

/**
 * Foreground service to split audio recordings using ffmpeg
 */
class SplitterService : Service(), FFmpegSessionCompleteCallback, LogCallback,
    StatisticsCallback {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private var queue: Queue<Pair<Triple<Long, String, ArrayList<Long>>, Int>> = LinkedList()
    private var running: Boolean = false
    private var currentId: Long = -1L
    private var currentRehearsalFile: String = ""
    private var currentRegions: Queue<Triple<Long, Long, Long>> = LinkedList()
    private var currentMaxProgress: Int = 0
    private var currentRequestId: Int = 0

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

    private fun requestForeground() {
        var notificationBuilder = createNotificationBuilder()
        if (Build.VERSION.SDK_INT >= 31) {
            notificationBuilder =
                notificationBuilder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
        val notification = notificationBuilder.build()

        startForeground(42, notification)
    }

    private fun updateProgress() {
        val nm: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = createNotificationBuilder(nm)
            .setProgress(currentMaxProgress, currentMaxProgress - currentRegions.size, false)
            .build()

        nm.notify(42, notification)
    }

    private fun start() {
        // No current region, means new file
        if (currentRegions.size == 0) {
            val (data, requestId) = queue.poll() ?: return
            currentRequestId = requestId
            val (id, fileName, regions) = data
            currentId = id
            currentRehearsalFile = fileName
            for (i in 0 until regions.size / 3) {
                currentRegions.add(Triple(regions[i * 3], regions[i * 3 + 1], regions[i * 3 + 2]))
            }

            currentMaxProgress = currentRegions.size
            updateProgress()
        }

        val (start, end, id) = currentRegions.poll() ?: return
        val startSeek = start / 1000f
        val endSeek = end / 1000f

        running = true

        Thread {
            Log.i("Splitter", "Splitting $currentRehearsalFile for song id $id")

            val (externalStorage, baseDir) = Utils.getPreferredStorageLocation(this)

            // Check folder or create
            val folder = File(Utils.getSongPath(baseDir, ""))
            if (!folder.exists())
                folder.mkdirs()

            // Insert into database and generate file name
            val database = Database.getInstance(applicationContext)
            val song = database.songDao().getSong(id)!!
            var songFileFriendlyName = song.name.replace(" ", "_").replace(Regex("\\W+"), "")
            val songRecordingUid =
                database.songRecordingDao()
                    .insert(id, currentId, song.name.replace(" ", "_"), externalStorage)
            val songRecording = database.songRecordingDao().get(songRecordingUid)!!
            songFileFriendlyName =
                "${song.uid}_${songFileFriendlyName}_${songRecording.version}.m4a"
            database.songRecordingDao().updateFileName(songRecordingUid, songFileFriendlyName)

            val songPath = Utils.getSongPath(baseDir, songFileFriendlyName)

            Log.i("Splitter", "Splitting into $songPath")

            FFmpegKit.executeAsync(
                "-y -ss $startSeek -to $endSeek -i $currentRehearsalFile $songPath",
                this,
                this,
                this
            )
        }.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        requestForeground()

        if (intent == null) {
            return START_NOT_STICKY
        }
        Log.i("Splitter", "Started service")

        val id = intent.getLongExtra("id", -1L)
        if (id == -1L) {
            throw IllegalArgumentException("Missing id in start service intent")
        }
        val file = intent.getStringExtra("file")!!

        @Suppress("UNCHECKED_CAST")
        val regions = intent.getSerializableExtra("regions") as ArrayList<Long>

        queue.add(Pair(Triple(id, file, regions), startId))
        if (!running) {
            start()
        }

        return START_REDELIVER_INTENT
    }

    // End callback
    override fun apply(session: FFmpegSession?) {
        // TODO: should probably check exit code, but we've seen it's not really relevant
        // TODO: ffmpeg sometimes returns with code 0 and still displays an error in stdout

        // If no more regions to split, the current audio processing is finished
        if (currentRegions.size == 0) {
            Database.getInstance(applicationContext).rehearsalDao()
                .updateStatus(currentId, Rehearsal.PROCESSED)

            val preference = PreferenceManager.getDefaultSharedPreferences(this)
            val deleteRecording = preference.getBoolean("delete_recording", false)
            if (deleteRecording) {
                File(currentRehearsalFile).delete()
            }
        }

        Handler(Looper.getMainLooper()).post {
            updateProgress()
            if (queue.size == 0 && currentRegions.size == 0) {
                running = false
                stopSelf(currentRequestId)
            } else {
                start()
            }
        }
    }

    // Log callback
    override fun apply(log: com.arthenica.ffmpegkit.Log?) {
    }

    // Statistics callback
    override fun apply(statistics: Statistics?) {
    }

}