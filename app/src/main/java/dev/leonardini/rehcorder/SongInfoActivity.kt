package dev.leonardini.rehcorder

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import dev.leonardini.rehcorder.adapters.SongInfoAdapter
import dev.leonardini.rehcorder.databinding.ActivitySongBinding
import dev.leonardini.rehcorder.db.Database
import dev.leonardini.rehcorder.viewmodels.SongInfoViewModel
import dev.leonardini.rehcorder.viewmodels.SongViewModelFactory

class SongInfoActivity : AppCompatActivity(), SongInfoAdapter.OnTrackShareClickListener,
    SongInfoAdapter.OnItemClickListener {

    private lateinit var binding: ActivitySongBinding
    private lateinit var adapter: SongInfoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivitySongBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        if (!intent.hasExtra("songId")) {
            finish()
            return
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = SongInfoAdapter(this, this)
        binding.recyclerView.adapter = adapter

        val model: SongInfoViewModel by viewModels {
            SongViewModelFactory(
                Database.getInstance(applicationContext),
                intent.getLongExtra("songId", -1)
            )
        }
        model.song.observe(this) { song ->
            binding.toolbar.title = song.name
        }
        model.songRehearsals.observe(this) { cursor ->
            adapter.swapCursor(cursor)
        }
    }

    override fun onShare(holder: SongInfoAdapter.SongInfoViewHolder) {
        val baseDir =
            if (holder.externalStorage) getExternalFilesDir(null) ?: filesDir else filesDir
        Utils.shareSong(this, baseDir, holder.fileName!!)
    }

    override fun onItemClicked(holder: SongInfoAdapter.SongInfoViewHolder) {
        val baseDir =
            if (holder.externalStorage) getExternalFilesDir(null) ?: filesDir else filesDir
        Utils.playSongIntent(this, baseDir, holder.fileName!!)
    }

}