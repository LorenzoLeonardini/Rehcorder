package dev.leonardini.rehcorder

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import dev.leonardini.rehcorder.adapters.SongInfoAdapter
import dev.leonardini.rehcorder.databinding.ActivitySongBinding
import dev.leonardini.rehcorder.db.Database
import dev.leonardini.rehcorder.ui.dialogs.MaterialInfoDialogFragment
import dev.leonardini.rehcorder.viewmodels.SongViewModel
import dev.leonardini.rehcorder.viewmodels.SongViewModelFactory
import java.io.File

class SongActivity : AppCompatActivity(), SongInfoAdapter.OnTrackShareClickListener,
    SongInfoAdapter.OnHeaderBoundListener, SongInfoAdapter.OnItemClickListener {

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
        adapter = SongInfoAdapter(this, this, this, null)
        binding.recyclerView.adapter = adapter

        val model: SongViewModel by viewModels {
            SongViewModelFactory(
                Database.getInstance(applicationContext),
                intent.getLongExtra("songId", -1)
            )
        }
        model.getSong().observe(this) { song ->
            binding.toolbar.title = song.name
        }
        model.getSongRehearsals().observe(this) { cursor ->
            adapter.swapCursor(cursor)
        }
    }

    override fun onShare(holder: SongInfoAdapter.SongInfoViewHolder) {
        val baseDir =
            if (holder.externalStorage) getExternalFilesDir(null) ?: filesDir else filesDir

        val file = File("${baseDir.absolutePath}/songs/${holder.fileName}_${holder.version}.m4a")
        if (!file.exists()) {
            MaterialInfoDialogFragment(
                R.string.dialog_not_found_title,
                R.string.dialog_not_found_message
            ).show(supportFragmentManager, "FileNotFound")
            return
        }

        val uri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.provider",
            file
        )
        val builder = ShareCompat.IntentBuilder(this)
        builder.addStream(uri)
        builder.setType("audio/*")
        builder.startChooser()
    }

    override fun onBound(holder: SongInfoAdapter.HeaderViewHolder) {}

    override fun onItemClicked(holder: SongInfoAdapter.SongInfoViewHolder) {
        val baseDir =
            if (holder.externalStorage) getExternalFilesDir(null) ?: filesDir else filesDir

        val file = File("${baseDir.absolutePath}/songs/${holder.fileName}_${holder.version}.m4a")
        if (!file.exists()) {
            MaterialInfoDialogFragment(
                R.string.dialog_not_found_title,
                R.string.dialog_not_found_message
            ).show(supportFragmentManager, "FileNotFound")
            return
        }

        val uri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.provider",
            file
        )
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.setDataAndType(uri, "audio/*")
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_GRANT_READ_URI_PERMISSION
        startActivity(intent)
    }

}