package dev.leonardini.rehcorder.services

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.arthenica.ffmpegkit.*
import dev.leonardini.rehcorder.R
import dev.leonardini.rehcorder.db.Database
import dev.leonardini.rehcorder.db.Rehearsal
import java.io.File
import java.util.*

class SplitterService : Service(), FFmpegSessionCompleteCallback, LogCallback,
    StatisticsCallback {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private var queue: Queue<Triple<Long, String, ArrayList<Long>>> = LinkedList()
    private var running: Boolean = false
    private var currentId: Long = -1L
    private var currentRehearsalFile: String = ""
    private var currentRegions: Queue<Triple<Long, Long, Long>> = LinkedList()
    private var currentSongFile: String = ""
    private var currentMaxProgress: Int = 0

    private fun createNotificationBuilder(notificationManager: NotificationManager? = null): NotificationCompat.Builder {
        var nm = notificationManager
            ?: getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        Utils.createServiceNotificationChannelIfNotExists(nm)

        return NotificationCompat.Builder(this, "dev.leonardini.rehcorder")
            .setContentTitle(resources.getString(R.string.notification_splitter_title))
            .setSmallIcon(R.drawable.ic_mic)
            .setContentText(resources.getString(R.string.notification_splitter_text))
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

        var notification = createNotificationBuilder(nm)
            .setProgress(currentMaxProgress, currentMaxProgress - currentRegions.size, false)
            .build()

        nm.notify(42, notification)
    }

    private fun start() {
        if (currentRegions.size == 0) {
            val (id, fileName, regions) = queue.poll() ?: return
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

            val externalStorage =
                Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED && getExternalFilesDir(
                    null
                ) != null
            val baseDir = getExternalFilesDir(null) ?: filesDir

            val folder = File("${baseDir.absolutePath}/songs/")
            if (!folder.exists())
                folder.mkdirs()

            val database = Database.getInstance(applicationContext)

            val song = database.songDao().getSong(id)!!

            val songRecordingUid =
                database.songRecordingDao()
                    .insert(id, currentId, song.name.replace(" ", "_"), externalStorage)
            val songRecording = database.songRecordingDao().get(songRecordingUid)!!

            currentSongFile = "${songRecording.fileName}_${songRecording.version}.m4a"
            Log.i("Splitter", "Splitting into ${baseDir.absolutePath}/songs/$currentSongFile")

            FFmpegKit.executeAsync(
                "-y -ss $startSeek -to $endSeek -i $currentRehearsalFile ${baseDir.absolutePath}/songs/$currentSongFile",
                this,
                this,
                this
            )
        }.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            return START_NOT_STICKY
        }
        Log.i("Splitter", "Started service")

        requestForeground()
        val id = intent.getLongExtra("id", -1L)
        if (id == -1L) {
            throw IllegalArgumentException("Missing id in start service intent")
        }
        val file = intent.getStringExtra("file")!!
        val regions = intent.getSerializableExtra("regions") as ArrayList<Long>
        queue.add(Triple(id, file, regions))
        if (!running) {
            start()
        }

        return START_NOT_STICKY
    }

    // End callback
    override fun apply(session: FFmpegSession?) {
        // TODO: should probably check exit code, but we've seen it's not really relevant
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
                stopSelf()
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