package dev.leonardini.rehcorder

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import dev.leonardini.rehcorder.databinding.ActivitySplitterBinding
import dev.leonardini.rehcorder.db.AppDatabase
import dev.leonardini.rehcorder.db.Database
import dev.leonardini.rehcorder.db.Rehearsal
import dev.leonardini.rehcorder.db.Song
import dev.leonardini.rehcorder.ui.dialogs.MaterialDialogFragment
import dev.leonardini.rehcorder.ui.dialogs.MaterialInfoDialogFragment
import dev.leonardini.rehcorder.ui.dialogs.SongPickerDialogFragment
import dev.leonardini.rehcorder.workers.WorkerUtils
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.floor

class SplitterActivity : AppCompatActivity(), Runnable, SeekBar.OnSeekBarChangeListener,
    View.OnClickListener, MediaPlayer.OnCompletionListener {

    companion object {
        private const val PLAYING = "playing"
        private const val SEEK = "seek"
        private const val SONG_REGIONS = "songRegions"

        private const val FILE_NOT_FOUND_DIALOG = "FileNotFound"
        private const val SONG_PICKER_DIALOG = "SongPickerDialog"
        const val NEW_SONG_NAME_DIALOG = "NewSongNameDialog"
        private const val EXIT_DIALOG = "ExitDialog"
        private const val INCOMPLETE_PROCESS_DIALOG = "IncompleteProcessDialog"
    }

    private lateinit var database: AppDatabase

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivitySplitterBinding

    private var rehearsalId: Long = -1L
    private lateinit var fileName: String
    private var externalStorage: Boolean = false
    private lateinit var audioManager: AudioManager
    private lateinit var mediaPlayer: MediaPlayer
    private var stopped: Boolean = false
    private var savedCurrentPlayingStatus: Boolean = false
    private var savedCurrentPlayingStatusOnStop: Boolean = false

    // triplets of ints indicating start,end,songId
    private lateinit var songRegions: ArrayList<Long>

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivitySplitterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        if (!intent.hasExtra("rehearsalId") || !intent.hasExtra("fileName") || !intent.hasExtra("externalStorage")) {
            finish()
            return
        }

        // Setup local variables based on rehearsal
        binding.toolbar.title =
            intent.getStringExtra("rehearsalName") ?: intent.getStringExtra("fileName")!!

        database = Database.getInstance(applicationContext)

        rehearsalId = intent.getLongExtra("rehearsalId", -1L)
        fileName = intent.getStringExtra("fileName")!!
        externalStorage = intent.getBooleanExtra("externalStorage", false)

        // Setup media player
        audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_NORMAL

        val baseDir = if (externalStorage) getExternalFilesDir(null) ?: filesDir else filesDir
        if (!File(Utils.getRecordingPath(baseDir, fileName)).exists()) {
            MaterialInfoDialogFragment(
                R.string.dialog_not_found_title,
                R.string.dialog_not_found_message
            ).show(supportFragmentManager, FILE_NOT_FOUND_DIALOG)
            finish()
        }

        mediaPlayer = MediaPlayer()
        mediaPlayer.setDataSource(Utils.getRecordingPath(baseDir, fileName))
        mediaPlayer.setAudioAttributes(
            AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA).build()
        )
        mediaPlayer.setOnCompletionListener(this)
        mediaPlayer.prepare()

        binding.content.waveform.setAudioSession(mediaPlayer.audioSessionId)

        binding.content.audioLength.text = Utils.secondsToTimeString(mediaPlayer.duration / 1000L)
        binding.content.seekBar.max = mediaPlayer.duration

        // Register listeners
        binding.content.playPause.setOnClickListener(this)
        binding.content.seekBack.setOnClickListener(this)
        binding.content.seekForward.setOnClickListener(this)
        binding.content.toggleSong.setOnClickListener(this)
        binding.content.undo.setOnClickListener(this)
        binding.content.save.setOnClickListener(this)

        // Recover state
        if (savedInstanceState != null) {
            if (savedInstanceState.getInt(SEEK, -1) > 0) {
                mediaPlayer.seekTo(savedInstanceState.getInt(SEEK))
            }
            if (savedInstanceState.getBoolean(PLAYING, false)) {
                mediaPlayer.start()
                binding.content.playPause.setIconResource(R.drawable.ic_pause)
                binding.content.playPause.contentDescription = resources.getString(R.string.pause)
            }
            val arr = savedInstanceState.getLongArray(SONG_REGIONS)?.toCollection(ArrayList())
            songRegions = arr ?: ArrayList()
        }
        if (!::songRegions.isInitialized) {
            songRegions = ArrayList()
        }
        binding.content.undo.isEnabled = songRegions.size > 0

        restoreSongRegionSelectionState(savedInstanceState == null)

        // Setup seek bar
        binding.content.seekBar.post(this)
        binding.content.seekBar.setOnSeekBarChangeListener(this)

        // Fragment result listeners
        supportFragmentManager.setFragmentResultListener(SONG_PICKER_DIALOG, this) { _, bundle ->
            val id = bundle.getLong("id")
            songRegions.add(id)
            if (savedCurrentPlayingStatus) {
                mediaPlayer.start()
            }
        }
        supportFragmentManager.setFragmentResultListener(NEW_SONG_NAME_DIALOG, this) { _, bundle ->
            val which = bundle.getInt("which")
            if (which == AlertDialog.BUTTON_POSITIVE) {
                val name = bundle.getString("name")
                lifecycleScope.launch {
                    val song = Song(name = name!!)
                    song.uid = database.songDao().insert(song)
                    songRegions.add(song.uid)
                    if (savedCurrentPlayingStatus) {
                        mediaPlayer.start()
                    }
                }
            } else {
                runSongSelector()
            }
        }
        supportFragmentManager.setFragmentResultListener(EXIT_DIALOG, this) { _, bundle ->
            val which = bundle.getInt("which")
            if (which == AlertDialog.BUTTON_POSITIVE) {
                super.onBackPressed()
            }
        }
    }

    override fun onBackPressed() {
        if (songRegions.size == 0) {
            super.onBackPressed()
            return
        }
        MaterialDialogFragment(
            R.string.dialog_confirm_exit_title,
            R.string.dialog_confirm_exit_message,
        ).show(supportFragmentManager, EXIT_DIALOG)
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
                Utils.secondsToTimeString(mediaPlayer.currentPosition / 1000L)

            binding.content.seekBar.postDelayed(this, 1000)
        }
    }

    override fun onCompletion(mp: MediaPlayer?) {
        binding.content.playPause.setIconResource(R.drawable.ic_play)
        binding.content.playPause.contentDescription = resources.getString(R.string.play)
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (fromUser) {
            mediaPlayer.seekTo(progress.coerceAtMost(mediaPlayer.duration))
            binding.content.currentTime.text = Utils.secondsToTimeString(progress / 1000L)
        }
    }

    override fun onClick(v: View?) {
        if (v == binding.content.playPause) {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                binding.content.playPause.setIconResource(R.drawable.ic_play)
                binding.content.playPause.contentDescription = resources.getString(R.string.play)
            } else {
                mediaPlayer.start()
                binding.content.playPause.setIconResource(R.drawable.ic_pause)
                binding.content.playPause.contentDescription = resources.getString(R.string.pause)
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
                songRegions.removeAt(songRegions.size - 1)
                if (songRegions.size % 3 == 2) {
                    songRegions.removeAt(songRegions.size - 1)
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
                ).show(supportFragmentManager, INCOMPLETE_PROCESS_DIALOG)
            } else {
                val baseDir =
                    if (externalStorage) getExternalFilesDir(null) ?: filesDir else filesDir

                val regions: ArrayList<Triple<Long, Long, Long>> = ArrayList(songRegions.size / 3)
                for (i in 0 until songRegions.size / 3) {
                    regions.add(
                        Triple(
                            songRegions[i * 3 + 2],
                            songRegions[i * 3],
                            songRegions[i * 3 + 1]
                        )
                    )
                }

                lifecycleScope.launch {
                    Database.getInstance(applicationContext).rehearsalDao()
                        .updateStatus(rehearsalId, Rehearsal.PROCESSING)
                    WorkerUtils.enqueueSplitting(
                        rehearsalId,
                        Utils.getRecordingPath(baseDir, fileName),
                        regions,
                        applicationContext
                    )
                    finish()
                }
            }
        }
    }

    private fun runSongSelector() {
        savedCurrentPlayingStatus = mediaPlayer.isPlaying
        if (savedCurrentPlayingStatus) {
            mediaPlayer.pause()
        }

        lifecycleScope.launch {
            val songs = database.songDao().getAllSorted()
            SongPickerDialogFragment(ArrayList(songs)).show(
                supportFragmentManager,
                SONG_PICKER_DIALOG
            )
        }
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

    override fun onStart() {
        super.onStart()
        if (savedCurrentPlayingStatusOnStop) {
            mediaPlayer.start()
        }
    }

    override fun onStop() {
        super.onStop()
        savedCurrentPlayingStatusOnStop = mediaPlayer.isPlaying
        if (savedCurrentPlayingStatusOnStop) {
            mediaPlayer.pause()
        }
    }
}