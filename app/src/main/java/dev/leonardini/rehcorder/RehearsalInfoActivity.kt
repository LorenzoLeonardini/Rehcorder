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
import dev.leonardini.rehcorder.adapters.RehearsalInfoAdapter
import dev.leonardini.rehcorder.databinding.ActivityRehearsalBinding
import dev.leonardini.rehcorder.ui.MyMaterialDividerItemDecoration
import dev.leonardini.rehcorder.ui.dialogs.MaterialDialogFragment
import dev.leonardini.rehcorder.ui.dialogs.MaterialLoadingDialogFragment
import dev.leonardini.rehcorder.viewmodels.RehearsalInfoViewModel
import dev.leonardini.rehcorder.viewmodels.RehearsalViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.*

class RehearsalInfoActivity : AppCompatActivity(), RehearsalInfoAdapter.OnTrackButtonClickListener,
    RehearsalInfoAdapter.OnItemClickListener {

    private lateinit var binding: ActivityRehearsalBinding
    private lateinit var adapter: RehearsalInfoAdapter

    companion object {
        private const val DELETE_REHEARSAL_DIALOG = "DeleteRehearsalDialog"
    }

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

        val linearLayoutManager = LinearLayoutManager(this)
        binding.recyclerView.layoutManager = linearLayoutManager
        adapter = RehearsalInfoAdapter(this, this)
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

        val model: RehearsalInfoViewModel by viewModels {
            RehearsalViewModelFactory(
                application,
                intent.getLongExtra("rehearsalId", -1)
            )
        }
        model.rehearsal.observe(this) { rehearsal ->
            val formattedDate = "${
                DateFormat.getDateInstance().format(Date(rehearsal.date * 1000))
            } - ${DateFormat.getTimeInstance().format(Date(rehearsal.date * 1000))}"

            binding.toolbar.title = rehearsal.name ?: formattedDate
            binding.toolbar.subtitle = if (rehearsal.name != null) formattedDate else ""
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.rehearsalSongs.collectLatest { pagingData ->
                    adapter.submitData(pagingData)
                }
            }
        }

        supportFragmentManager.setFragmentResultListener(
            DELETE_REHEARSAL_DIALOG,
            this
        ) { _, bundle ->
            val which = bundle.getInt("which")
            if (which != AlertDialog.BUTTON_NEGATIVE) {
                val loadingFragment = MaterialLoadingDialogFragment()
                loadingFragment.show(supportFragmentManager, "Loading")
                model.deleteRehearsal(applicationContext)
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
                    R.string.dialog_delete_rehearsal_title,
                    R.string.dialog_delete_rehearsal_message
                ).show(supportFragmentManager, DELETE_REHEARSAL_DIALOG)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onShare(holder: RehearsalInfoAdapter.RehearsalInfoViewHolder) {
        val baseDir =
            if (holder.externalStorage) getExternalFilesDir(null) ?: filesDir else filesDir
        Utils.shareSong(this, baseDir, holder.fileName!!)
    }

    override fun onPlay(holder: RehearsalInfoAdapter.RehearsalInfoViewHolder) {
        val baseDir =
            if (holder.externalStorage) getExternalFilesDir(null) ?: filesDir else filesDir
        Utils.playSongIntent(this, baseDir, holder.fileName!!)
    }

    override fun onItemClicked(holder: RehearsalInfoAdapter.RehearsalInfoViewHolder) {
        onPlay(holder)
    }

}