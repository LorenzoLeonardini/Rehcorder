package dev.leonardini.rehcorder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PackageManagerCompat
import androidx.core.view.get
import androidx.navigation.fragment.findNavController
import com.google.android.material.navigation.NavigationBarView
import dev.leonardini.alarm.MaterialDialogFragment
import dev.leonardini.alarm.MaterialInfoDialogFragment
import dev.leonardini.rehcorder.databinding.ActivityMainBinding
import java.io.IOException

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 42
        private const val PERMISSION_DIALOG_TAG = "PermissionDialog"
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private var recording: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        val bottomNavigation = binding.bottomNavigation
        bottomNavigation.menu[1].isEnabled = false
        bottomNavigation.setOnItemReselectedListener { item ->
            when(item.itemId) {
                R.id.page_songs -> true
                R.id.page_rehearsals -> true
                else -> false
            }
        }
        bottomNavigation.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.page_songs -> {
                    findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.to_first_fragment)
                    true
                }
                R.id.page_rehearsals -> {
                    findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.to_second_fragment)
                    true
                }
                else -> {
                    false
                }
            }
        }

        binding.fab.setOnClickListener {
            if (recording) {
                binding.fab.setImageResource(R.drawable.ic_record)
                stopRecording()
            } else {
                binding.fab.setImageResource(R.drawable.ic_stop)

                if (recordingPermissionsGranted()) {
                    startRecording()
                } else {
                    ActivityCompat.requestPermissions(
                        this,
                        permissions,
                        REQUEST_RECORD_AUDIO_PERMISSION
                    )
                }
            }
            recording = !recording
        }
    }

    private var permissionToRecordAccepted: Boolean = false
    private var permissions = arrayOf(Manifest.permission.RECORD_AUDIO)
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null

    private fun recordingPermissionsGranted() = permissions.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionToRecordAccepted = if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
        if (!permissionToRecordAccepted) {
            MaterialInfoDialogFragment(
                R.string.permission_required_title,
                R.string.permission_required
            ) { _, _ ->
                run {
                    binding.fab.setImageResource(R.drawable.ic_record)
                    recording = false
                }
            }.show(supportFragmentManager, PERMISSION_DIALOG_TAG)
        } else {
            startRecording()
        }
    }

    private fun getBestAudioSource(): Int {
        val audioManager: AudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return if (audioManager.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) != null) {
            Log.i("Recorder", "Using unprocessed mic")
            MediaRecorder.AudioSource.UNPROCESSED
        } else {
            Log.i("Recorder", "Using voice recognition mic")
            MediaRecorder.AudioSource.VOICE_RECOGNITION
        }
    }

    private fun startRecording() {
        if (!this.permissionToRecordAccepted) return
        recorder = MediaRecorder(applicationContext).apply {
            setAudioSource(getBestAudioSource())
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile("${externalCacheDir?.absolutePath}/audiorecordtest.3gp")
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            try {
                prepare()
            } catch (e: IOException) {
                Log.e("Recorder", "prepare() failed")
            }

            start()
        }

        Snackbar.make(binding.root, "Started recording", Snackbar.LENGTH_LONG)
            .setAnchorView(R.id.fab).show()
    }

    private fun stopRecording() {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null

        Snackbar.make(binding.root, "Stopped recording", Snackbar.LENGTH_LONG)
            .setAnchorView(R.id.fab).show()

        startPlaying()
    }

    private fun startPlaying() {
        player = MediaPlayer().apply {
            try {
                setDataSource("${externalCacheDir?.absolutePath}/audiorecordtest.3gp")
                prepare()
                start()
                setOnCompletionListener { stopPlaying() }
            } catch (e: IOException) {
                Log.e("Player", "prepare() failed")
            }
        }
    }

    private fun stopPlaying() {
        player?.release()
        player = null
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}