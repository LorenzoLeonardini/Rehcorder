package dev.leonardini.rehcorder

import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import dev.leonardini.rehcorder.adapters.RehearsalInfoAdapter
import dev.leonardini.rehcorder.databinding.ActivityRehearsalBinding
import dev.leonardini.rehcorder.db.Database
import dev.leonardini.rehcorder.ui.dialogs.MaterialDialogFragment
import dev.leonardini.rehcorder.ui.dialogs.MaterialLoadingDialogFragment
import dev.leonardini.rehcorder.viewmodels.RehearsalInfoViewModel
import dev.leonardini.rehcorder.viewmodels.RehearsalViewModelFactory
import java.text.DateFormat
import java.util.*

class RehearsalInfoActivity : AppCompatActivity(), RehearsalInfoAdapter.OnTrackShareClickListener,
    RehearsalInfoAdapter.OnHeaderBoundListener, RehearsalInfoAdapter.OnItemClickListener {

    private lateinit var binding: ActivityRehearsalBinding
    private lateinit var adapter: RehearsalInfoAdapter
    private lateinit var model: RehearsalInfoViewModel

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

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = RehearsalInfoAdapter(this, this, this)
        binding.recyclerView.adapter = adapter

        val model: RehearsalInfoViewModel by viewModels {
            RehearsalViewModelFactory(
                Database.getInstance(applicationContext),
                intent.getLongExtra("rehearsalId", -1)
            )
        }
        this.model = model
        model.rehearsal.observe(this) { rehearsal ->
            val formattedDate = "${
                DateFormat.getDateInstance().format(Date(rehearsal.date * 1000))
            } - ${DateFormat.getTimeInstance().format(Date(rehearsal.date * 1000))}"

            binding.toolbar.title = rehearsal.name ?: formattedDate
            binding.toolbar.subtitle = if (rehearsal.name != null) formattedDate else ""
        }
        model.rehearsalSongs.observe(this) { cursor ->
            adapter.swapCursor(cursor)
        }

        supportFragmentManager.setFragmentResultListener(
            DELETE_REHEARSAL_DIALOG,
            this
        ) { _, bundle ->
            val which = bundle.getInt("which")
            if (which != AlertDialog.BUTTON_NEGATIVE) {
                val loadingFragment = MaterialLoadingDialogFragment()
                loadingFragment.show(supportFragmentManager, "Loading")
                Thread {
                    model.deleteRehearsal(applicationContext)
                    runOnUiThread {
                        loadingFragment.dismiss()
                        finish()
                    }
                }.start()
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

    override fun onBound(holder: RehearsalInfoAdapter.HeaderViewHolder) {
        model.rehearsal.value?.let { rehearsal ->
            holder.binding.card.visibility =
                if (rehearsal.hasLocationData) View.VISIBLE else View.GONE
            if (rehearsal.hasLocationData) {
                // Fallback coordinates in case Geocoder is not present
                holder.binding.location.text =
                    String.format("%.5f %.5f", rehearsal.latitude, rehearsal.longitude)

                // Geocoding
                if (Geocoder.isPresent()) {
                    val geocoder = Geocoder(this, resources.configuration.locale)
                    val address =
                        geocoder.getFromLocation(
                            rehearsal.latitude!!,
                            rehearsal.longitude!!,
                            1
                        )[0]
                    holder.binding.location.text = address.getAddressLine(0)
                }

                // Open Maps
                holder.binding.card.setOnClickListener {
                    val locationUri =
                        Uri.parse("geo:${rehearsal.latitude},${rehearsal.longitude}?q=${rehearsal.latitude},${rehearsal.longitude}")
                    startActivity(Intent(Intent.ACTION_VIEW, locationUri))
                }
            }
        }
    }

    override fun onItemClicked(holder: RehearsalInfoAdapter.RehearsalInfoViewHolder) {
        val baseDir =
            if (holder.externalStorage) getExternalFilesDir(null) ?: filesDir else filesDir
        Utils.playSongIntent(this, baseDir, holder.fileName!!)
    }

}