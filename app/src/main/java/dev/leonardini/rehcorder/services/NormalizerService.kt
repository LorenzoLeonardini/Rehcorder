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
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*

class NormalizerService : Service(), FFmpegSessionCompleteCallback, LogCallback,
    StatisticsCallback {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private var queue: Queue<Pair<Long, String>> = LinkedList()
    private var running: Boolean = false
    private var currentId: Long = -1L
    private var currentFile: String = ""

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
            .setContentTitle("Normalizing audio...")
            .setSmallIcon(R.drawable.ic_mic)
            .setContentText("Audio is being normalized in the background")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

        startForeground(31337, notification)
    }

    private fun start() {
        val (id, fileName) = queue.poll() ?: return
        currentId = id
        currentFile = fileName

        running = true
        Log.i("Normalizer", "Normalizing $fileName")
        FFmpegKit.executeAsync(
            "-y -i $fileName -af loudnorm -ar 44100 ${filesDir.absolutePath}/tmp.aac",
            this,
            this,
            this
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            return START_NOT_STICKY
        }
        Log.i("Normalizer", "Started service")

        requestForeground()
        val id = intent.getLongExtra("id", -1L)
        if (id == -1L) {
            throw IllegalArgumentException("Missing id in start service intent")
        }
        val file = intent.getStringExtra("file")!!
        queue.add(Pair(id, file))
        if (!running) {
            start()
        }

        return START_NOT_STICKY
    }

    // End callback
    override fun apply(session: FFmpegSession?) {
        // TODO: should probably check exit code, but we've seen it's not really relevant
        val file = File("${filesDir.absolutePath}/tmp.aac")
        val destinationFile = File(currentFile)
        Files.move(file.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

        Database.getInstance(applicationContext).rehearsalDao()
            .updateStatus(currentId, Rehearsal.NORMALIZED)

        Handler(Looper.getMainLooper()).post {
            if (queue.size == 0) {
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