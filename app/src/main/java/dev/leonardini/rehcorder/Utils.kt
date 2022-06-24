package dev.leonardini.rehcorder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import dev.leonardini.rehcorder.ui.dialogs.MaterialInfoDialogFragment
import java.io.File

object Utils {

    /**
     * Decides if external storage is the best choice
     * @return pair of <Bool, File> indicating whether it's external storage and giving the base file
     */
    fun getPreferredStorageLocation(context: Context): Pair<Boolean, File> {
        val externalStorage =
            Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED && context.getExternalFilesDir(
                null
            ) != null
        val baseDir =
            context.getExternalFilesDir(null) ?: context.filesDir
        return Pair(externalStorage, baseDir)
    }

    /**
     * Returns recording path given the fileName and the baseDir
     */
    fun getRecordingPath(baseDir: File, recordingName: String): String {
        return "${baseDir.absolutePath}/recordings/$recordingName"
    }

    /**
     * Returns song path given the fileName and the baseDir
     */
    fun getSongPath(baseDir: File, songName: String): String {
        return "${baseDir.absolutePath}/songs/$songName"
    }

    /**
     * Converts seconds count in HH:mm:ss string
     */
    fun secondsToTimeString(time: Long): String {
        val hours = time / 3600
        val minutes = (time / 60) % 60
        val seconds = time % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun createServiceNotificationChannelIfNotExists(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel("dev.leonardini.rehcorder") == null) {
                val channel = NotificationChannel(
                    "dev.leonardini.rehcorder",
                    "Rehcorder",
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    private fun getSongUri(activity: AppCompatActivity, baseDir: File, fileName: String): Uri? {
        val file = File(getSongPath(baseDir, fileName))
        if (!file.exists()) {
            MaterialInfoDialogFragment(
                R.string.dialog_not_found_title,
                R.string.dialog_not_found_message
            ).show(activity.supportFragmentManager, "FileNotFoundDialog")
            return null
        }

        return FileProvider.getUriForFile(
            activity,
            "${activity.applicationContext.packageName}.provider",
            file
        )
    }

    /**
     * Share a song
     */
    fun shareSong(activity: AppCompatActivity, baseDir: File, fileName: String) {
        getSongUri(activity, baseDir, fileName)?.let { uri ->
            val builder = ShareCompat.IntentBuilder(activity)
            builder.addStream(uri)
            builder.setType("audio/*")
            builder.startChooser()
        }
    }

    /**
     * Play a song with the system music player
     */
    fun playSongIntent(activity: AppCompatActivity, baseDir: File, fileName: String) {
        getSongUri(activity, baseDir, fileName)?.let { uri ->
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.setDataAndType(uri, "audio/*")
            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_GRANT_READ_URI_PERMISSION
            activity.startActivity(intent)
        }
    }
}