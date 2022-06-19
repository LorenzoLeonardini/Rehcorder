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
import dev.leonardini.rehcorder.db.Database
import dev.leonardini.rehcorder.db.Rehearsal
import java.io.File
import java.io.IOException

class RecorderService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private var recorder: MediaRecorder? = null
    private var id: Long = -1
    private var fileName: String? = null
    private var unprocessedMicrophone: Boolean = true

    private fun getBestAudioSource(): Int {
        val preference = PreferenceManager.getDefaultSharedPreferences(this)
        unprocessedMicrophone = preference.getBoolean("unprocessed_microphone", true)
        if (!unprocessedMicrophone) {
            Log.i("Recorder", "Using default mic")
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

    private fun startRecording() {
        val folder = File("${filesDir.absolutePath}/recordings/")
        if (!folder.exists())
            folder.mkdirs()

        recorder = MediaRecorder(applicationContext).apply {
            setAudioSource(getBestAudioSource())
            setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
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

    private fun stopRecording() {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null

        if (!unprocessedMicrophone) {
            Thread {
                Database.getInstance(applicationContext).rehearsalDao()
                    .updateStatus(id, Rehearsal.NORMALIZED)
            }.start()
        } else {
            Thread {
                Database.getInstance(applicationContext).rehearsalDao()
                    .updateStatus(id, Rehearsal.RECORDED)
            }.start()
            val intent = Intent(this, NormalizerService::class.java)
            intent.putExtra("id", id)
            intent.putExtra("file", "${filesDir.absolutePath}/recordings/$fileName")
            startForegroundService(intent)
        }
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        requestForeground()
        if (intent == null) {
            return START_NOT_STICKY
        }

        if (intent.action == "RECORD") {
            if (fileName != null) {
                return START_NOT_STICKY
            }
            id = intent.getLongExtra("id", -1L)
            if (id == -1L) {
                throw IllegalArgumentException("Missing id in start service intent")
            }
            fileName = intent.getStringExtra("file")!!
            startRecording()
        } else if (intent.action == "STOP") {
            stopRecording()
        }

        return START_NOT_STICKY
    }

}