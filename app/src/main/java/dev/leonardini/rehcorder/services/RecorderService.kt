package dev.leonardini.rehcorder.services

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import dev.leonardini.rehcorder.MainActivity
import dev.leonardini.rehcorder.R
import dev.leonardini.rehcorder.Utils
import dev.leonardini.rehcorder.db.Database
import dev.leonardini.rehcorder.db.Rehearsal
import java.io.File
import java.io.IOException

/**
 * Foreground service to record audio
 */
class RecorderService : Service() {

    // https://stackoverflow.com/a/608600/4840510
    // https://groups.google.com/g/android-developers/c/jEvXMWgbgzE
    // Solution by hackbod, an Android engineer @ Google
    companion object {
        private var id: Long = -1
        private var _startTimestamp: Long = -1L
        val startTimestamp: Long
            get() = _startTimestamp
        private var fileName: String? = null
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private var recorder: MediaRecorder? = null

    /**
     * Select best audio source for the recording. If the user wants it and the device supports it,
     * it will use the UNPROCESSED source (or VOICE_RECOGNITION) so that the audio will be as clean
     * as possible. Meant for music.
     */
    private fun getBestAudioSource(): Int {
        val preference = PreferenceManager.getDefaultSharedPreferences(this)
        if (!preference.getBoolean("unprocessed_microphone", true)) {
            Log.i("Recorder", "Using default mic")
            return MediaRecorder.AudioSource.MIC
        }

        val audioManager: AudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return if (Build.VERSION.SDK_INT >= 24 && audioManager.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) != null) {
            Log.i("Recorder", "Using unprocessed mic")
            MediaRecorder.AudioSource.UNPROCESSED
        } else {
            Log.i("Recorder", "Using voice recognition mic")
            MediaRecorder.AudioSource.VOICE_RECOGNITION
        }
    }

    private fun requestForeground() {
        val nm: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        Utils.createServiceNotificationChannelIfNotExists(nm)

        val int = Intent(this, MainActivity::class.java)
        int.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            int,
            (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0) or PendingIntent.FLAG_UPDATE_CURRENT
        )

        var notificationBuilder = NotificationCompat.Builder(this, "dev.leonardini.rehcorder")
            .setContentTitle(resources.getString(R.string.notification_recorder_title))
            .setSmallIcon(R.drawable.ic_mic)
            .setContentText(resources.getString(R.string.notification_recorder_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
        if (Build.VERSION.SDK_INT >= 31) {
            notificationBuilder =
                notificationBuilder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
        val notification = notificationBuilder.build()

        startForeground(1337, notification)
    }

    private fun startRecording() {
        val folder = File(File(fileName!!).parentFile!!.absolutePath)
        if (!folder.exists())
            folder.mkdirs()

        // This constructor is deprecated, but the new one is only for API >= 31, makes no sense to check
        recorder =
            MediaRecorder().apply {
                setAudioSource(getBestAudioSource())
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(fileName)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(192000)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

                try {
                    prepare()
                } catch (e: IOException) {
                    Log.e("Recorder", "prepare() failed")
                }

                start()
            }
    }

    private fun stopRecording(currentRequestId: Int) {
        recorder?.apply {
            try {
                stop()
                release()
            } catch (e :Exception) {
                Log.e("Recorder", "Exception while stopping recorder")
            }
            recorder = null

            // Normalizing everything, even processed microphone in order to
            // compress file size (ffmpeg chooses the best sample rate)

            // Update rehearsal status
            Thread {
                Database.getInstance(applicationContext).rehearsalDao()
                    .updateStatus(id, Rehearsal.RECORDED)
            }.start()

            // Start normalizer service
            val intent = Intent(this@RecorderService, NormalizerService::class.java)
            intent.putExtra("id", id)
            intent.putExtra("file", fileName)
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }

        fileName = null
        _startTimestamp = -1L
        stopSelf(currentRequestId)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        _startTimestamp = intent?.getLongExtra("startTimestamp", -1L) ?: -1L

        if (intent == null) {
            return START_NOT_STICKY
        }

        if (intent.action == "RECORD") {
            requestForeground()
            if (fileName != null) {
                Log.e("Recorder", "Can only do one recording at a time")
                return START_NOT_STICKY
            }

            id = intent.getLongExtra("id", -1L)
            if (id == -1L) {
                throw IllegalArgumentException("Missing id in start service intent")
            }
            fileName = intent.getStringExtra("file")!!

            startRecording()
        } else if (intent.action == "STOP") {
            stopRecording(startId)
        }

        return START_NOT_STICKY
    }

}