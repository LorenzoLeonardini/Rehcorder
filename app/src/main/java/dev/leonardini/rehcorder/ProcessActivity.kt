package dev.leonardini.rehcorder

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import dev.leonardini.rehcorder.databinding.ActivityProcessBinding
import dev.leonardini.rehcorder.db.AppDatabase
import dev.leonardini.rehcorder.db.Database
import dev.leonardini.rehcorder.db.Rehearsal
import dev.leonardini.rehcorder.db.Song
import dev.leonardini.rehcorder.services.SplitterService
import dev.leonardini.rehcorder.ui.dialogs.MaterialInfoDialogFragment
import dev.leonardini.rehcorder.ui.dialogs.SongPickerDialogFragment
import java.io.File
import kotlin.math.floor

class ProcessActivity : AppCompatActivity(), Runnable, SeekBar.OnSeekBarChangeListener,
    View.OnClickListener, MediaPlayer.OnCompletionListener {

    companion object {
        private const val PLAYING = "playing"
        private const val SEEK = "seek"
        private const val SONG_REGIONS = "songRegions"

        fun secondsToTimeString(time: Long): String {
            val hours = time / 3600
            val minutes = (time / 60) % 60
            val seconds = time % 60
            return String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }
    }

    private lateinit var database: AppDatabase

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityProcessBinding

    private var rehearsalId: Long = -1L
    private lateinit var fileName: String
    private var externalStorage: Boolean = false
    private lateinit var audioManager: AudioManager
    private lateinit var mediaPlayer: MediaPlayer
    private var stopped: Boolean = false
    private var savedCurrentPlayingStatus: Boolean = false

    // triplets of ints indicating start,end,songId
    private lateinit var songRegions: ArrayList<Long>

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityProcessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        if (!intent.hasExtra("rehearsalId") || !intent.hasExtra("fileName") || !intent.hasExtra("externalStorage")) {
            finish()
            return
        }

        binding.toolbar.title =
            intent.getStringExtra("rehearsalName") ?: intent.getStringExtra("fileName")!!

        database = Database.getInstance(applicationContext)

        rehearsalId = intent.getLongExtra("rehearsalId", -1L)
        fileName = intent.getStringExtra("fileName")!!
        externalStorage = intent.getBooleanExtra("externalStorage", false)

        audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_NORMAL

        val baseDir = if (externalStorage) getExternalFilesDir(null) ?: filesDir else filesDir
        if (!File("${baseDir.absolutePath}/recordings/$fileName").exists()) {
            MaterialInfoDialogFragment(
                R.string.dialog_not_found_title,
                R.string.dialog_not_found_message
            ).show(supportFragmentManager, "FileNotFound")
            finish()
        }

        mediaPlayer = MediaPlayer()
        mediaPlayer.setDataSource("${baseDir.absolutePath}/recordings/$fileName")
        mediaPlayer.setAudioAttributes(
            AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA).build()
        )
        mediaPlayer.setOnCompletionListener(this)
        mediaPlayer.prepare()

        binding.content.waveform.setAudioSession(mediaPlayer.audioSessionId)

        binding.content.audioLength.text = secondsToTimeString(mediaPlayer.duration / 1000L)
        binding.content.seekBar.max = mediaPlayer.duration

        binding.content.playPause.setOnClickListener(this)
        binding.content.seekBack.setOnClickListener(this)
        binding.content.seekForward.setOnClickListener(this)
        binding.content.toggleSong.setOnClickListener(this)
        binding.content.undo.setOnClickListener(this)
        binding.content.save.setOnClickListener(this)

        if (savedInstanceState != null) {
            if (savedInstanceState.getInt(SEEK, -1) > 0) {
                mediaPlayer.seekTo(savedInstanceState.getInt(SEEK))
            }
            if (savedInstanceState.getBoolean(PLAYING, false)) {
                mediaPlayer.start()
                binding.content.playPause.setIconResource(R.drawable.ic_pause)
            }
            val arr = savedInstanceState.getLongArray(SONG_REGIONS)?.toCollection(ArrayList())
            songRegions = arr ?: ArrayList()
        }
        if (!::songRegions.isInitialized) {
            songRegions = ArrayList()
        }
        binding.content.undo.isEnabled = songRegions.size > 0

        restoreSongRegionSelectionState(savedInstanceState == null)

        runOnUiThread(this)
        binding.content.seekBar.setOnSeekBarChangeListener(this)

        supportFragmentManager.setFragmentResultListener("SongPickerDialog", this) { _, bundle ->
            val id = bundle.getLong("id")
            songRegions.add(id)
            if (savedCurrentPlayingStatus) {
                mediaPlayer.start()
            }
        }
        supportFragmentManager.setFragmentResultListener("NewSongNameDialog", this) { _, bundle ->
            val which = bundle.getInt("which")
            if (which == AlertDialog.BUTTON_POSITIVE) {
                val name = bundle.getString("name")
                Thread {
                    val song = Song(name = name!!)
                    song.uid = database.songDao().insert(song)
                    runOnUiThread {
                        songRegions.add(song.uid)
                    }
                }.start()
            } else {
                runSongSelector()
            }
        }
    }

    private fun restoreSongRegionSelectionState(doRunSongSelector: Boolean) {
        when (songRegions.size % 3) {
            0 -> binding.content.toggleSong.setText(R.string.begin_song)
            1 -> binding.content.toggleSong.setText(R.string.end_song)
            2 -> if (doRunSongSelector) runSongSelector()
        }

        for (i in 0 until floor(songRegions.size / 3.0).toInt()) {
            binding.content.seekBar.highlightRegion(
                songRegions[i * 3].toInt(),
                songRegions[i * 3 + 1].toInt()
            )
        }
    }

    override fun run() {
        if (!stopped) {
            binding.content.seekBar.progress = mediaPlayer.currentPosition
            binding.content.currentTime.text =
                secondsToTimeString(mediaPlayer.currentPosition / 1000L)

            binding.content.seekBar.postDelayed(this, 1000)
        }
    }

    override fun onCompletion(mp: MediaPlayer?) {
        binding.content.playPause.setIconResource(R.drawable.ic_play)
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (fromUser) {
            mediaPlayer.seekTo(progress.coerceAtMost(mediaPlayer.duration))
            binding.content.currentTime.text = secondsToTimeString(progress / 1000L)
        }
    }

    override fun onClick(v: View?) {
        if (v == binding.content.playPause) {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                binding.content.playPause.setIconResource(R.drawable.ic_play)
            } else {
                mediaPlayer.start()
                binding.content.playPause.setIconResource(R.drawable.ic_pause)
            }
        } else if (v == binding.content.seekBack) {
            val position = (mediaPlayer.currentPosition - 10000).coerceAtLeast(0)
            binding.content.seekBar.progress = position
            mediaPlayer.seekTo(position)
        } else if (v == binding.content.seekForward) {
            val position =
                (mediaPlayer.currentPosition + 10000).coerceAtMost(mediaPlayer.duration)
            binding.content.seekBar.progress = position
            mediaPlayer.seekTo(position)
        } else if (v == binding.content.toggleSong) {
            if (songRegions.size % 3 == 0) {
                // New song
                binding.content.toggleSong.setText(R.string.end_song)
                songRegions.add(mediaPlayer.currentPosition.toLong())
            } else {
                if (mediaPlayer.currentPosition <= songRegions.last()) {
                    return
                }
                binding.content.toggleSong.setText(R.string.begin_song)
                binding.content.seekBar.highlightRegion(
                    songRegions.last().toInt(),
                    mediaPlayer.currentPosition
                )
                songRegions.add(mediaPlayer.currentPosition.toLong())
                runSongSelector()
            }
            binding.content.undo.isEnabled = true
        } else if (v == binding.content.undo) {
            if (songRegions.size > 0) {
                songRegions.removeLast()
                if (songRegions.size % 3 == 2) {
                    songRegions.removeLast()
                }
                if (songRegions.size == 0) {
                    binding.content.undo.isEnabled = false
                }
                binding.content.seekBar.clearRegions()
                restoreSongRegionSelectionState(true)
            }
        } else if (v == binding.content.save) {
            if (songRegions.size == 0 || songRegions.size % 3 != 0) {
                MaterialInfoDialogFragment(
                    R.string.dialog_save_error_title,
                    R.string.dialog_save_error_message
                ).show(supportFragmentManager, "IncompleteProcess")
            } else {
                val baseDir =
                    if (externalStorage) getExternalFilesDir(null) ?: filesDir else filesDir
                val intent = Intent(this, SplitterService::class.java)
                intent.putExtra("id", rehearsalId)
                intent.putExtra("file", "${baseDir.absolutePath}/recordings/$fileName")
                intent.putExtra("regions", songRegions)

                Thread {
                    Database.getInstance(applicationContext).rehearsalDao()
                        .updateStatus(rehearsalId, Rehearsal.PROCESSING)
                    runOnUiThread {
                        if (Build.VERSION.SDK_INT >= 26) {
                            startForegroundService(intent)
                        } else {
                            startService(intent)
                        }
                        finish()
                    }
                }.start()
            }
        }
    }

    private fun runSongSelector() {
        savedCurrentPlayingStatus = mediaPlayer.isPlaying
        if (savedCurrentPlayingStatus) {
            mediaPlayer.pause()
        }
        Thread {
            val songs = database.songDao().getAllSorted()
            runOnUiThread {
                SongPickerDialogFragment(ArrayList(songs)).show(
                    supportFragmentManager,
                    "SongPickerDialog"
                )
            }
        }.start()
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {}

    override fun onStopTrackingTouch(seekBar: SeekBar?) {}

    override fun onDestroy() {
        binding.content.waveform.cleanAudioVisualizer()
        stopped = true
        mediaPlayer.stop()
        mediaPlayer.release()
        audioManager.mode = AudioManager.MODE_NORMAL
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        return super.onCreateOptionsMenu(menu)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(PLAYING, mediaPlayer.isPlaying)
        outState.putInt(SEEK, mediaPlayer.currentPosition)

        outState.putLongArray(SONG_REGIONS, songRegions.toLongArray())

        super.onSaveInstanceState(outState)
    }
}