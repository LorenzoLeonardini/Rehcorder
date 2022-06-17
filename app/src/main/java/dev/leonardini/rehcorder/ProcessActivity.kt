package dev.leonardini.rehcorder

import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.SeekBar
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import dev.leonardini.rehcorder.databinding.ActivityProcessBinding
import kotlin.math.min

class ProcessActivity : AppCompatActivity(), Runnable, SeekBar.OnSeekBarChangeListener,
    View.OnClickListener {

    companion object {
        private const val PLAYING = "playing"
        private const val SEEK = "seek"
        private const val SONG_COUNT = "songCount"
        private const val SONG_IDS = "songIds"
        private const val SONG_STARTS = "songStarts"
        private const val SONG_ENDS = "songEnds"
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityProcessBinding

    private var rehearsalId: Long = -1
    private lateinit var fileName: String
    private lateinit var mediaPlayer: MediaPlayer
    private var stopped: Boolean = false

    private var songIds = ArrayList<Long>()
    private lateinit var songStarts: ArrayList<Int>
    private lateinit var songEnds: ArrayList<Int>

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

        rehearsalId = intent.getLongExtra("rehearsalId", -1)
        fileName = intent.getStringExtra("fileName")!!
        mediaPlayer = MediaPlayer()
        mediaPlayer.setDataSource("${filesDir.absolutePath}/recordings/$fileName")
        mediaPlayer.prepare()
        val minutes = mediaPlayer.duration / 60000
        val seconds = (mediaPlayer.duration / 1000) % 60
        binding.content.audioLength.text = String.format("%02d:%02d", minutes, seconds)
        binding.content.seekBar.max = mediaPlayer.duration

        binding.content.playPause.setOnClickListener(this)
        binding.content.seekBack.setOnClickListener(this)
        binding.content.seekForward.setOnClickListener(this)
        binding.content.toggleSong.setOnClickListener(this)

        if (savedInstanceState != null) {
            if (savedInstanceState.getInt(SEEK, -1) > 0) {
                mediaPlayer.seekTo(savedInstanceState.getInt(SEEK))
            }
            if (savedInstanceState.getBoolean(PLAYING, false)) {
                mediaPlayer.start()
                binding.content.playPause.setIconResource(R.drawable.ic_pause)
            }
            if (savedInstanceState.getInt(SONG_COUNT, 0) > 0) {
                val arr = savedInstanceState.getLongArray(SONG_IDS)
                if (arr != null) {
                    for (id in arr) {
                        songIds.add(id)
                    }
                }
            }
            val startArr = savedInstanceState.getIntegerArrayList(SONG_STARTS)
            songStarts = startArr ?: ArrayList<Int>()
            val endArr = savedInstanceState.getIntegerArrayList(SONG_ENDS)
            songEnds = endArr ?: ArrayList<Int>()
        }
        if (!::songStarts.isInitialized) {
            songStarts = ArrayList()
        }
        if (!::songEnds.isInitialized) {
            songEnds = ArrayList()
        }

        if (songStarts.size != songEnds.size) {
            binding.content.toggleSong.text = "End song"
        }

        for (i in 0 until (min(songStarts.size, songEnds.size))) {
            binding.content.seekBar.highlightRegion(songStarts[i], songEnds[i])
        }

        runOnUiThread(this)
        binding.content.seekBar.setOnSeekBarChangeListener(this)
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

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (fromUser) {
            mediaPlayer.seekTo(progress)

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
            val position = (mediaPlayer.currentPosition + 10000).coerceAtMost(mediaPlayer.duration)
            binding.content.seekBar.progress = position
            mediaPlayer.seekTo(position)
        } else if (v == binding.content.toggleSong) {
            if (songStarts.size == songEnds.size) {
                // New song
                binding.content.toggleSong.text = "End song"
                songStarts.add(mediaPlayer.currentPosition)
            } else {
                if (mediaPlayer.currentPosition < songStarts.last()) {
                    return
                }
                binding.content.toggleSong.text = "Begin song"
                songEnds.add(mediaPlayer.currentPosition)
                binding.content.seekBar.highlightRegion(songStarts.last(), songEnds.last())
            }
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {}

    override fun onStopTrackingTouch(seekBar: SeekBar?) {}

    override fun onDestroy() {
        stopped = true
        mediaPlayer.stop()
        mediaPlayer.release()
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

        outState.putInt(SONG_COUNT, songIds.size)
        outState.putLongArray(SONG_IDS, songIds.toLongArray())
        outState.putIntegerArrayList(SONG_STARTS, songStarts)
        outState.putIntegerArrayList(SONG_ENDS, songEnds)

        super.onSaveInstanceState(outState)
    }
}