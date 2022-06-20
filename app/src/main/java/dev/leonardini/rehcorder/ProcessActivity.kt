package dev.leonardini.rehcorder

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import dev.leonardini.rehcorder.databinding.ActivityProcessBinding
import dev.leonardini.rehcorder.db.AppDatabase
import dev.leonardini.rehcorder.db.Database
import dev.leonardini.rehcorder.services.SplitterService
import dev.leonardini.rehcorder.ui.dialogs.SongPickerDialogFragment
import kotlin.math.floor

class ProcessActivity : AppCompatActivity(), Runnable, SeekBar.OnSeekBarChangeListener,
    View.OnClickListener, MediaPlayer.OnCompletionListener {

    companion object {
        private const val PLAYING = "playing"
        private const val SEEK = "seek"
        private const val SONG_REGIONS = "songRegions"
    }

    private lateinit var database: AppDatabase

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityProcessBinding

    private var rehearsalId: Long = -1L
    private lateinit var fileName: String
    private lateinit var audioManager: AudioManager
    private lateinit var mediaPlayer: MediaPlayer
    private var stopped: Boolean = false
    private var savedCurrentPlayingStatus :Boolean = false

    // triplets of ints indicating start,end,songId
    private lateinit var songRegions :ArrayList<Long>

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityProcessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        if (!intent.hasExtra("rehearsalId") || !intent.hasExtra("fileName")) {
            finish()
            return
        }

        rehearsalId = intent.getLongExtra("rehearsalId", -1L)
        fileName = intent.getStringExtra("fileName")!!

        audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.STREAM_MUSIC

        mediaPlayer = MediaPlayer()
        mediaPlayer.setDataSource("${filesDir.absolutePath}/recordings/$fileName")
        mediaPlayer.setAudioAttributes(
            AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA).build()
        )
        mediaPlayer.setOnCompletionListener(this)
        mediaPlayer.prepare()

        binding.content.waveform.setAudioSession(mediaPlayer.audioSessionId)

        val minutes = mediaPlayer.duration / 60000
        val seconds = (mediaPlayer.duration / 1000) % 60
        binding.content.audioLength.text = String.format("%02d:%02d", minutes, seconds)
        binding.content.seekBar.max = mediaPlayer.duration

        binding.content.playPause.setOnClickListener(this)
        binding.content.seekBack.setOnClickListener(this)
        binding.content.seekForward.setOnClickListener(this)
        binding.content.toggleSong.setOnClickListener(this)
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

        if (songRegions.size % 3 == 1) {
            binding.content.toggleSong.text = "End song"
        } else if(songRegions.size % 3 == 2) {
            runSongSelector()                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           
        }

        for (i in 0 until floor(songRegions.size / 3.0).toInt()) {
            binding.content.seekBar.highlightRegion(songRegions[i * 3].toInt(), songRegions[i * 3 + 1].toInt())
        }

        runOnUiThread(this)
        binding.content.seekBar.setOnSeekBarChangeListener(this)

        database = Database.getInstance(applicationContext)

        supportFragmentManager.setFragmentResultListener("SongPickerDialog", this) { _, bundle ->
            val id = bundle.getLong("id")!!
            songRegions.add(id)
            if(savedCurrentPlayingStatus) {
                mediaPlayer.start()
            }
        }
    }

    override fun run() {
        if (!stopped) {
            binding.content.seekBar.progress = mediaPlayer.currentPosition
            val minutes = mediaPlayer.currentPosition / 60000
            val seconds = (mediaPlayer.currentPosition / 1000) % 60
            binding.content.currentTime.text = String.format("%02d:%02d", minutes, seconds)

            binding.content.seekBar.postDelayed(this, 1000)
        }
    }

    override fun onCompletion(mp: MediaPlayer?) {
        binding.content.playPause.setIconResource(R.drawable.ic_play)
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (fromUser) {
            mediaPlayer.seekTo(progress.coerceAtMost(mediaPlayer.duration - 10))

            val minutes = progress / 60000
            val seconds = (progress / 1000) % 60
            binding.content.currentTime.text = String.format("%02d:%02d", minutes, seconds)
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
                (mediaPlayer.currentPosition + 10000).coerceAtMost(mediaPlayer.duration - 10)
            binding.content.seekBar.progress = position
            mediaPlayer.seekTo(position)
        } else if (v == binding.content.toggleSong) {
            if (songRegions.size % 3 == 0) {
                // New song
                binding.content.toggleSong.text = "End song"
                songRegions.add(mediaPlayer.currentPosition.toLong())
            } else {
                if (mediaPlayer.currentPosition <= songRegions.last()) {
                    return
                }
                binding.content.toggleSong.text = "Begin song"
                binding.content.seekBar.highlightRegion(songRegions.last().toInt(), mediaPlayer.currentPosition)
                songRegions.add(mediaPlayer.currentPosition.toLong())
                runSongSelector()
            }
        } else if (v == binding.content.save) {
            if(songRegions.size == 0 || songRegions.size % 3 != 0) {
                // TODO: error alert
            } else {
                val intent = Intent(this, SplitterService::class.java)
                intent.putExtra("id", rehearsalId)
                intent.putExtra("file", "${filesDir.absolutePath}/recordings/$fileName")
                intent.putExtra("regions", songRegions)
                startForegroundService(intent)

                finish()
            }
        }
    }

    private fun runSongSelector() {
        savedCurrentPlayingStatus = mediaPlayer.isPlaying
        if(savedCurrentPlayingStatus) {
            mediaPlayer.pause()
        }
        Thread {
            val songs = database.songDao().getAllSorted()
            runOnUiThread {
                SongPickerDialogFragment(ArrayList(songs)).show(supportFragmentManager, "SongPickerDialog")
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
        audioManager.mode = AudioManager.USE_DEFAULT_STREAM_TYPE
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