package dev.leonardini.rehcorder.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

object Utils {
    fun createServiceNotificationChannelIfNotExists(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if(notificationManager.getNotificationChannel("dev.leonardini.rehcorder") == null) {
                val channel = NotificationChannel(
                    "dev.leonardini.rehcorder",
                    "Rehcorder",
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}