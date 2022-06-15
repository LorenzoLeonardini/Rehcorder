package dev.leonardini.rehcorder

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.*
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.get
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.snackbar.Snackbar
import dev.leonardini.rehcorder.databinding.ActivityMainBinding
import dev.leonardini.rehcorder.db.Database
import dev.leonardini.rehcorder.utils.MaterialInfoDialogFragment
import java.io.File
import java.io.IOException
import java.time.Instant


class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 42
        private const val PERMISSION_DIALOG_TAG = "PermissionDialog"
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private var recording: Boolean = false
    private var currentlyRecording :String = ""

    private lateinit var database: Database

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val topLevelDestinations = HashSet<Int>()
        topLevelDestinations.add(R.id.SongsFragment)
        topLevelDestinations.add(R.id.RehearsalsFragment)
        topLevelDestinations.add(R.id.RecordingFragment)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration.Builder(topLevelDestinations).build()
        setupActionBarWithNavController(navController, appBarConfiguration)

        val bottomNavigation = binding.bottomNavigation
        bottomNavigation.menu[1].isEnabled = false
        bottomNavigation.setOnItemReselectedListener { item ->
            when (item.itemId) {
                R.id.page_songs -> true
                R.id.page_rehearsals -> true
                else -> false
            }
        }
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.page_songs -> {
                    if (!recording)
                        findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.to_songs_fragment)
                    true
                }
                R.id.page_rehearsals -> {
                    if (!recording)
                        findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.to_rehearsals_fragment)
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
                    permissionToRecordAccepted = true
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

        database = Database(this)
    }

    private var permissionToRecordAccepted: Boolean = false
    private var permissions = arrayOf(Manifest.permission.RECORD_AUDIO)
    private var recorder: MediaRecorder? = null

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
        if (!permissionToRecordAccepted) return
        binding.bottomNavigation.selectedItemId = R.id.page_record
        binding.bottomNavigation.menu[0].isEnabled = false
        binding.bottomNavigation.menu[2].isEnabled = false
        findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.to_recording_fragment)

        val contentValues = ContentValues()
        val fileName = Instant.now().epochSecond
        contentValues.put("date", fileName)
        database.writableDatabase.insert("Rehearsals", null, contentValues)
        database.close()

        val folder = File("${filesDir.absolutePath}/recordings/")
        if (!folder.exists())
            folder.mkdirs()

        currentlyRecording = "$fileName.3gp"
        recorder = MediaRecorder(applicationContext).apply {
            setAudioSource(getBestAudioSource())
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile("${filesDir.absolutePath}/recordings/$fileName.3gp")
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
        binding.bottomNavigation.menu[0].isEnabled = true
        binding.bottomNavigation.menu[2].isEnabled = true

        startPlaying()
    }

    private fun startPlaying() {
        val uri = FileProvider.getUriForFile(this, "${this.packageName}.provider", File("${filesDir.absolutePath}/recordings/$currentlyRecording"))

        val viewMediaIntent = Intent()
        viewMediaIntent.action = Intent.ACTION_VIEW
        viewMediaIntent.setDataAndType(uri, "audio/*")
        viewMediaIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(viewMediaIntent)
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
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}