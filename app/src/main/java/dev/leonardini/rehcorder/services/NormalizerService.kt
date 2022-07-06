package dev.leonardini.rehcorder.services

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import dev.leonardini.rehcorder.R
import dev.leonardini.rehcorder.Utils
import dev.leonardini.rehcorder.db.Database
import dev.leonardini.rehcorder.db.Rehearsal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class NormalizerService : ForegroundIntentService("NormalizerService") {
    override fun onHandleIntent(intent: Intent) {
        val id = intent.getLongExtra("id", -1L)
        if (id == -1L) {
            throw IllegalArgumentException("Missing id in start service intent")
        }
        val fileName = intent.getStringExtra("file")!!

        Log.i("Normalizer", "Normalizing $fileName")
        val session =
            FFmpegKit.execute("-y -i $fileName -af loudnorm -ar 44100 ${File(fileName).parentFile!!.parentFile!!.absolutePath}/tmp.m4a")
        if (ReturnCode.isSuccess(session.returnCode)) {
            // SUCCESS
            val file = File("${File(fileName).parentFile!!.parentFile!!.absolutePath}/tmp.m4a")
            val destinationFile = File(fileName)
            val result = file.renameTo(destinationFile)
            Log.i("Normalizer", "Renaming result : $result")

            CoroutineScope(Dispatchers.Main).launch {
                Database.getInstance(applicationContext).rehearsalDao()
                    .updateStatus(id, Rehearsal.NORMALIZED)
            }
        } else if (ReturnCode.isCancel(session.returnCode)) {
            // CANCEL
        } else {
            // FAILURE
        }
    }

    override fun startForegroundNotification() {
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
}