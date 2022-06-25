package dev.leonardini.rehcorder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.location.LocationManagerCompat
import androidx.core.util.Consumer
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

    fun getLocation(
        applicationContext: Context,
        fine: Boolean,
        coarse: Boolean
    ): Pair<Pair<Boolean, Boolean>, Triple<Double, Double, String?>> {
        var hasLocationData = false
        var latitude = -1.0
        var longitude = -1.0
        var willComputeNewLocation = false
        var provider: String? = null
        val locationManager =
            applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        try {
            if (coarse || fine) {
                // Provider criteria
                val criteria = Criteria()
                criteria.accuracy = if (fine) Criteria.ACCURACY_FINE else Criteria.ACCURACY_COARSE
                criteria.isCostAllowed = false
                criteria.isAltitudeRequired = false
                criteria.isSpeedRequired = false

                // Pick best provider
                provider = locationManager.getBestProvider(criteria, true)
                if (provider != null) {
                    val location = locationManager.getLastKnownLocation(provider)
                    // If null or older than 30 minutes
                    if (location == null || location.time < System.currentTimeMillis() - 30 * 60 * 1000) {
                        // request new location
                        willComputeNewLocation = true
                    }
                    if (location != null) {
                        // meanwhile set last known location
                        hasLocationData = true
                        latitude = location.latitude
                        longitude = location.longitude
                    }
                }
            }
        } catch (exception: SecurityException) {
        }

        return Pair(
            Pair(hasLocationData, willComputeNewLocation),
            Triple(latitude, longitude, provider)
        )
    }

    fun getUpdatedLocation(
        applicationContext: Context,
        provider: String,
        consumer: Consumer<Location>
    ) {
        val locationManager =
            applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            LocationManagerCompat.getCurrentLocation(
                locationManager,
                provider,
                null,
                ContextCompat.getMainExecutor(applicationContext),
                consumer
            )
        } catch (exception: SecurityException) {
        }
    }
}