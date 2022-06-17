package dev.leonardini.rehcorder

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.arthenica.ffmpegkit.*
import java.io.File
import java.io.IOException


class RecorderService : Service(), FFmpegSessionCompleteCallback, LogCallback, StatisticsCallback {
    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    private var recorder: MediaRecorder? = null
    private var fileName: String? = null

    private fun getBestAudioSource(): Int {
        val preference = PreferenceManager.getDefaultSharedPreferences(this)
        if (!preference.getBoolean("unprocessed_microphone", true)) {
            return MediaRecorder.AudioSource.MIC
        }

        val audioManager: AudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return if (audioManager.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) != null) {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "dev.leonardini.rehcorder",
                "Rehcorder",
                NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(channel)
        }

        val int = Intent(this, MainActivity::class.java)
        int.putExtra("Recording", true)
        val pendingIntent = PendingIntent.getActivity(this, 0, int, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, "dev.leonardini.rehcorder")
            .setContentTitle("Rehearsal is being recorded")
            .setSmallIcon(R.drawable.ic_mic)
            .setContentText("Tap to return to the application")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

        startForeground(1337, notification)
    }

    private fun startRecording(fileName: String) {
        this.fileName = fileName

        val folder = File("${filesDir.absolutePath}/recordings/")
        if (!folder.exists())
            folder.mkdirs()

        recorder = MediaRecorder(applicationContext).apply {
            setAudioSource(getBestAudioSource())
            setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
            setOutputFile(fileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

            try {
                prepare()
            } catch (e: IOException) {
                Log.e("Recorder", "prepare() failed")
            }

            start()
        }
    }

    private fun stopRecording() {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null

        val preference = PreferenceManager.getDefaultSharedPreferences(this)
        if (!preference.getBoolean("unprocessed_microphone", true)) {
            stopSelf()
            return
        }

        val notification = NotificationCompat.Builder(this, "dev.leonardini.rehcorder")
            .setContentTitle("Normalizing audio...")
            .setSmallIcon(R.drawable.ic_mic)
            .setContentText("Audio is being normalized in the background")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

        startForeground(1337, notification)

        FFmpegKit.executeAsync(
            "-y -i $fileName -af loudnorm ${filesDir.absolutePath}/tmp.aac",
            this,
            this,
            this
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            return START_NOT_STICKY
        }

        if (intent.action == "RECORD") {
            if (fileName != null) {
                return START_NOT_STICKY
            }
            requestForeground()
            startRecording(intent.getStringExtra("file")!!)
        } else if (intent.action == "STOP") {
            stopRecording()
        }

        return START_NOT_STICKY
    }

    // End callback
    override fun apply(session: FFmpegSession?) {
        // TODO: should probably check exit code, but we've seen it's not really relevant
        val file = File("${filesDir.absolutePath}/tmp.aac")
        file.renameTo(File(fileName!!))

        stopSelf()
    }

    // Log callback
    override fun apply(log: com.arthenica.ffmpegkit.Log?) {
    }

    // Statistics callback
    override fun apply(statistics: Statistics?) {
    }

}