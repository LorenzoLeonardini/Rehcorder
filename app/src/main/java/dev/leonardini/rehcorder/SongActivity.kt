package dev.leonardini.rehcorder

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import dev.leonardini.rehcorder.adapters.SongInfoAdapter
import dev.leonardini.rehcorder.databinding.ActivitySongBinding
import dev.leonardini.rehcorder.db.AppDatabase
import dev.leonardini.rehcorder.db.Database
import dev.leonardini.rehcorder.ui.dialogs.MaterialInfoDialogFragment
import java.io.File

class SongActivity : AppCompatActivity(), SongInfoAdapter.OnTrackShareClickListener,
    SongInfoAdapter.OnHeaderBoundListener, SongInfoAdapter.OnItemClickListener {

    private lateinit var database: AppDatabase

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
        Thread {
            database = Database.getInstance(applicationContext)

            val song = database.songDao().getSong(intent.getLongExtra("songId", -1))!!

            binding.toolbar.title = song.name

            val cursor = database.songRecordingDao().getSongSortedCursor(song.uid)
            binding.recyclerView.post {
                adapter.swapCursor(cursor)
            }
        }.start()
        binding.recyclerView.adapter = adapter

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
//        menuInflater.inflate(R.menu.menu_delete, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
//            R.id.action_delete -> {
//                Snackbar.make(binding.root, "ciao", Snackbar.LENGTH_SHORT).show()
//                true
//            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onShare(holder: SongInfoAdapter.SongInfoViewHolder) {
        val baseDir =
            if (holder.externalStorage) getExternalFilesDir(null) ?: filesDir else filesDir

        val file = File("${baseDir.absolutePath}/songs/${holder.fileName}_${holder.version}.aac")
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

        val file = File("${baseDir.absolutePath}/songs/${holder.fileName}_${holder.version}.aac")
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