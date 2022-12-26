package dev.leonardini.rehcorder

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import dev.leonardini.rehcorder.adapters.RehearsalInfoAdapter
import dev.leonardini.rehcorder.adapters.SongInfoAdapter
import dev.leonardini.rehcorder.databinding.ActivitySongBinding
import dev.leonardini.rehcorder.ui.MyMaterialDividerItemDecoration
import dev.leonardini.rehcorder.ui.dialogs.MaterialDialogFragment
import dev.leonardini.rehcorder.ui.dialogs.MaterialLoadingDialogFragment
import dev.leonardini.rehcorder.ui.dialogs.RenameDialogFragment
import dev.leonardini.rehcorder.viewmodels.SongInfoViewModel
import dev.leonardini.rehcorder.viewmodels.SongViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SongInfoActivity : AppCompatActivity(), SongInfoAdapter.OnTrackButtonClickListener,
    SongInfoAdapter.OnItemClickListener, MenuProvider {

    private lateinit var binding: ActivitySongBinding
    private lateinit var adapter: SongInfoAdapter

    companion object {
        private const val DELETE_SONG_DIALOG = "DeleteSongDialog"
        private const val RENAME_SONG_DIALOG = "RenameSongDialog"
    }

    private var songName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivitySongBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }
        addMenuProvider(this)

        if (!intent.hasExtra("songId")) {
            finish()
            return
        }

        val linearLayoutManager = LinearLayoutManager(this)
        binding.recyclerView.layoutManager = linearLayoutManager
        adapter = SongInfoAdapter(this, this)
        binding.recyclerView.adapter = adapter
        val itemDecoration = MyMaterialDividerItemDecoration(
            binding.recyclerView.context, linearLayoutManager.orientation
        )
        itemDecoration.isLastItemDecorated = false
        itemDecoration.isFirstItemDecorated = false
        itemDecoration.setDividerInsetStartResource(this, R.dimen.divider_inset)
        itemDecoration.setDividerInsetEndResource(this, R.dimen.divider_inset)
        binding.recyclerView.addItemDecoration(itemDecoration)

        val model: SongInfoViewModel by viewModels {
            SongViewModelFactory(
                application, intent.getLongExtra("songId", -1)
            )
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    model.songRehearsals.collectLatest { pagingData ->
                        adapter.submitData(pagingData)
                    }
                }
                launch {
                    model.song.collectLatest { song ->
                        binding.toolbar.title = song?.name
                        songName = song?.name
                    }
                }
            }
        }

        supportFragmentManager.setFragmentResultListener(
            DELETE_SONG_DIALOG, this
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
        supportFragmentManager.setFragmentResultListener(
            RENAME_SONG_DIALOG, this
        ) { _, bundle ->
            val name = bundle.getString("name")
            if (name!!.isNotBlank()) {
                model.updateSongName(name)
            }
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_song_info, menu)
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_edit -> {
                RenameDialogFragment(
                    songName, R.string.s_rename
                ).show(supportFragmentManager, RENAME_SONG_DIALOG)
                true
            }
            R.id.action_delete -> {
                MaterialDialogFragment(
                    R.string.dialog_delete_song_title, R.string.dialog_delete_song_message
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

    override fun onPlay(holder: SongInfoAdapter.SongInfoViewHolder) {
        val baseDir =
            if (holder.externalStorage) getExternalFilesDir(null) ?: filesDir else filesDir
        Utils.playSongIntent(this, baseDir, holder.fileName!!)
    }

    override fun onItemClicked(holder: SongInfoAdapter.SongInfoViewHolder) {
        onPlay(holder)
    }

}