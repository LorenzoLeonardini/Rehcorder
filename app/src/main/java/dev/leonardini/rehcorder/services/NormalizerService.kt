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
import com.arthenica.ffmpegkit.*
import dev.leonardini.rehcorder.R
import dev.leonardini.rehcorder.Utils
import dev.leonardini.rehcorder.db.Database
import dev.leonardini.rehcorder.db.Rehearsal
import java.io.File
import java.util.*

/**
 * Runs ffmpeg normalization on the provided audio file
 */
class NormalizerService : Service(), FFmpegSessionCompleteCallback, LogCallback,
    StatisticsCallback {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private var queue: Queue<Triple<Long, String, Int>> = LinkedList()
    private var running: Boolean = false
    private var currentId: Long = -1L
    private var currentFile: String = ""
    private var currentRequestId: Int = 0

    private fun requestForeground() {
        val nm: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        Utils.createServiceNotificationChannelIfNotExists(nm)

        var notificationBuilder = NotificationCompat.Builder(this, "dev.leonardini.rehcorder")
            .setContentTitle(resources.getString(R.string.notification_normalizer_title))
            .setSmallIcon(R.drawable.ic_mic)
            .setContentText(resources.getString(R.string.notification_normalizer_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
        if (Build.VERSION.SDK_INT >= 31) {
            notificationBuilder =
                notificationBuilder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
        val notification = notificationBuilder.build()

        startForeground(31337, notification)
    }

    private fun start() {
        val (id, fileName, requestId) = queue.poll() ?: return
        currentId = id
        currentFile = fileName
        currentRequestId = requestId

        running = true
        Log.i("Normalizer", "Normalizing $fileName")
        FFmpegKit.executeAsync(
            "-y -i $fileName -af loudnorm -ar 44100 ${File(fileName).parentFile!!.parentFile!!.absolutePath}/tmp.m4a",
            this,
            this,
            this
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        requestForeground()

        if (intent == null) {
            return START_NOT_STICKY
        }
        Log.i("Normalizer", "Started service")

        val id = intent.getLongExtra("id", -1L)
        if (id == -1L) {
            throw IllegalArgumentException("Missing id in start service intent")
        }
        val file = intent.getStringExtra("file")!!

        queue.add(Triple(id, file, startId))

        if (!running) {
            start()
        }

        return START_REDELIVER_INTENT
    }

    // End callback
    override fun apply(session: FFmpegSession?) {
        // TODO: should probably check exit code, but we've seen it's not really relevant
        // TODO: ffmpeg sometimes returns with code 0 and still displays an error in stdout

        val file = File("${File(currentFile).parentFile!!.parentFile!!.absolutePath}/tmp.m4a")
        val destinationFile = File(currentFile)
        val result = file.renameTo(destinationFile)
        Log.i("Normalizer", "Renaming result : $result")

        Database.getInstance(applicationContext).rehearsalDao()
            .updateStatus(currentId, Rehearsal.NORMALIZED)

        Handler(Looper.getMainLooper()).post {
            if (queue.size == 0) {
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