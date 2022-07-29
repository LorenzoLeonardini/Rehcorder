package dev.leonardini.rehcorder

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import dev.leonardini.rehcorder.adapters.SongInfoAdapter
import dev.leonardini.rehcorder.databinding.ActivitySongBinding
import dev.leonardini.rehcorder.ui.MyMaterialDividerItemDecoration
import dev.leonardini.rehcorder.ui.dialogs.MaterialDialogFragment
import dev.leonardini.rehcorder.ui.dialogs.MaterialLoadingDialogFragment
import dev.leonardini.rehcorder.viewmodels.SongInfoViewModel
import dev.leonardini.rehcorder.viewmodels.SongViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SongInfoActivity : AppCompatActivity(), SongInfoAdapter.OnTrackShareClickListener,
    SongInfoAdapter.OnItemClickListener {

    private lateinit var binding: ActivitySongBinding
    private lateinit var adapter: SongInfoAdapter

    companion object {
        private const val DELETE_SONG_DIALOG = "DeleteSongDialog"
    }

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

        val linearLayoutManager = LinearLayoutManager(this)
        binding.recyclerView.layoutManager = linearLayoutManager
        adapter = SongInfoAdapter(this, this)
        binding.recyclerView.adapter = adapter
        val itemDecoration = MyMaterialDividerItemDecoration(
            binding.recyclerView.context,
            linearLayoutManager.orientation
        )
        itemDecoration.isLastItemDecorated = false
        itemDecoration.isFirstItemDecorated = false
        itemDecoration.setDividerInsetStartResource(this, R.dimen.divider_inset)
        itemDecoration.setDividerInsetEndResource(this, R.dimen.divider_inset)
        binding.recyclerView.addItemDecoration(itemDecoration)

        val model: SongInfoViewModel by viewModels {
            SongViewModelFactory(
                application,
                intent.getLongExtra("songId", -1)
            )
        }
        model.song.observe(this) { song ->
            binding.toolbar.title = song.name
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.songRehearsals.collectLatest { pagingData ->
                    adapter.submitData(pagingData)
                }
            }
        }

        supportFragmentManager.setFragmentResultListener(
            DELETE_SONG_DIALOG,
            this
        ) { _, bundle ->
            val which = bundle.getInt("which")
            if (which != AlertDialog.BUTTON_NEGATIVE) {
                val loadingFragment = MaterialLoadingDialogFragment()
                loadingFragment.show(supportFragmentManager, "Loading")
                model.deleteSong(applicationContext)
                loadingFragment.dismiss()
                finish()
            }
        }
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
                MaterialDialogFragment(
                    R.string.dialog_delete_song_title,
                    R.string.dialog_delete_song_message
                ).show(supportFragmentManager, DELETE_SONG_DIALOG)
                true
            }
            else -> super.onOptionsItemSelected(item)
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