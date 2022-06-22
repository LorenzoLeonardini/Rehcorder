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
import com.google.android.material.snackbar.Snackbar
import dev.leonardini.rehcorder.adapters.RehearsalInfoAdapter
import dev.leonardini.rehcorder.databinding.ActivityRehearsalBinding
import dev.leonardini.rehcorder.db.AppDatabase
import dev.leonardini.rehcorder.db.Database
import java.io.File
import java.text.DateFormat
import java.util.*

class RehearsalActivity : AppCompatActivity(), RehearsalInfoAdapter.OnTrackShareClickListener,
    RehearsalInfoAdapter.OnHeaderBoundListener, RehearsalInfoAdapter.OnItemClickListener {

    private lateinit var database: AppDatabase

    private lateinit var binding: ActivityRehearsalBinding
    private lateinit var adapter: RehearsalInfoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityRehearsalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        if (!intent.hasExtra("rehearsalId")) {
            finish()
            return
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = RehearsalInfoAdapter(this, this, this, null)
        Thread {
            database = Database.getInstance(applicationContext)

            val rehearsal =
                database.rehearsalDao().getRehearsal(intent.getLongExtra("rehearsalId", -1))!!
            val formattedDate = "${
                DateFormat.getDateInstance().format(Date(rehearsal.date * 1000))
            } - ${DateFormat.getTimeInstance().format(Date(rehearsal.date * 1000))}"

            binding.toolbar.title = rehearsal.name ?: formattedDate
            binding.toolbar.subtitle = if (rehearsal.name != null) formattedDate else ""

            val cursor = database.songRecordingDao().getRehearsalSortedCursor(rehearsal.uid)
            binding.recyclerView.post {
                adapter.swapCursor(cursor)
            }
        }.start()
        binding.recyclerView.adapter = adapter

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_delete, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_delete -> {
                Snackbar.make(binding.root, "ciao", Snackbar.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onShare(id: Long, fileName: String, version: Int) {
        val uri = FileProvider.getUriForFile(
            this,
            "${this.applicationContext.packageName}.provider",
            File("${this.filesDir.absolutePath}/songs/${fileName}_$version.aac")
        )
        val builder = ShareCompat.IntentBuilder(this)
        builder.addStream(uri)
        builder.setType("audio/*")
        builder.startChooser()
    }

    override fun onBound(holder: RehearsalInfoAdapter.HeaderViewHolder) {}

    override fun onItemClicked(holder: RehearsalInfoAdapter.RehearsalInfoViewHolder) {
        val uri = FileProvider.getUriForFile(
            this,
            "${this.applicationContext.packageName}.provider",
            File("${this.filesDir.absolutePath}/songs/${holder.fileName}_${holder.version}.aac")
        )
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.setDataAndType(uri, "audio/*")
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_GRANT_READ_URI_PERMISSION
        startActivity(intent)
    }

}