package dev.leonardini.rehcorder.services

import android.app.Notification
import android.app.NotificationChannel
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
import com.arthenica.ffmpegkit.*
import dev.leonardini.rehcorder.R
import dev.leonardini.rehcorder.db.Database
import dev.leonardini.rehcorder.db.Rehearsal
import dev.leonardini.rehcorder.db.SongRecording
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

    private fun requestForeground() {
        val nm: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "dev.leonardini.rehcorder",
                "Rehcorder",
                NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, "dev.leonardini.rehcorder")
            .setContentTitle("Splitting audio...")
            .setSmallIcon(R.drawable.ic_mic)
            .setContentText("Audio is being split into tracks in the background")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

        startForeground(42, notification)
    }

    private fun start() {
        val folder = File("${filesDir.absolutePath}/songs/")
        if (!folder.exists())
            folder.mkdirs()

        if (currentRegions.size == 0) {
            val (id, fileName, regions) = queue.poll() ?: return
            currentId = id
            currentRehearsalFile = fileName
            for (i in 0 until regions.size / 3) {
                currentRegions.add(Triple(regions[i * 3], regions[i * 3 + 1], regions[i * 3 + 2]))
            }
        }
        val (start, end, id) = currentRegions.poll() ?: return
        val startSeek = start / 1000f
        val endSeek = end / 1000f

        running = true

        Thread {
            Log.i("Splitter", "Splitting $currentRehearsalFile for song id $id")

            val song = Database.getInstance(applicationContext).songDao().getSong(id)!!

            val songRecording = SongRecording(id, currentId, song.name.replace(" ", "_"))
            songRecording.uid =
                Database.getInstance(applicationContext).songRecordingDao().insert(songRecording)

            currentSongFile = "${songRecording.fileName}_${songRecording.uid}.aac"
            Log.i("Splitter", "Splitting into ${filesDir.absolutePath}/songs/$currentSongFile")

            FFmpegKit.executeAsync(
                "-y -ss $startSeek -to $endSeek -i $currentRehearsalFile ${filesDir.absolutePath}/songs/$currentSongFile",
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
        }

        Handler(Looper.getMainLooper()).post {
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