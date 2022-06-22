package dev.leonardini.rehcorder.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dev.leonardini.rehcorder.ProcessActivity
import dev.leonardini.rehcorder.R
import dev.leonardini.rehcorder.RehearsalActivity
import dev.leonardini.rehcorder.adapters.RehearsalsAdapter
import dev.leonardini.rehcorder.databinding.FragmentRehearsalsBinding
import dev.leonardini.rehcorder.db.AppDatabase
import dev.leonardini.rehcorder.db.Database
import dev.leonardini.rehcorder.db.Rehearsal
import dev.leonardini.rehcorder.ui.dialogs.MaterialInfoDialogFragment
import dev.leonardini.rehcorder.ui.dialogs.RenameDialogFragment
import java.text.DateFormat
import java.util.*

class RehearsalsFragment : Fragment(), RehearsalsAdapter.OnRehearsalEditClickListener,
    RehearsalsAdapter.OnHeaderBoundListener, RehearsalsAdapter.OnItemClickListener,
    View.OnClickListener {

    companion object {
        private const val RECORDED_DIALOG_TAG = "RecordedDialog"
        private const val PROCESSING_DIALOG_TAG = "ProcessingDialog"
        private const val ERROR_STATE_DIALOG_TAG = "ErrorStateDialog"
        private const val RENAME_DIALOG_TAG = "RenameDialog"
    }

    private var _binding: FragmentRehearsalsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var database: AppDatabase
    private lateinit var adapter: RehearsalsAdapter

    private lateinit var activityLauncher: ActivityResultLauncher<Intent>

    private var showCard: Boolean = false
    private var inNeedOfProcessingId: Long = -1
    private var inNeedOfProcessingFileName: String = ""
    private var inNeedOfProcessingName: String = ""
    private var inNeedOfProcessingExternalStorage: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRehearsalsBinding.inflate(inflater, container, false)

        requireActivity().supportFragmentManager.setFragmentResultListener(
            RECORDED_DIALOG_TAG,
            viewLifecycleOwner
        ) { _, _ -> }
        requireActivity().supportFragmentManager.setFragmentResultListener(
            PROCESSING_DIALOG_TAG,
            viewLifecycleOwner
        ) { _, _ -> }
        requireActivity().supportFragmentManager.setFragmentResultListener(
            ERROR_STATE_DIALOG_TAG,
            viewLifecycleOwner
        ) { _, _ -> }
        requireActivity().supportFragmentManager.setFragmentResultListener(
            RENAME_DIALOG_TAG,
            viewLifecycleOwner
        ) { _, bundle ->
            Log.i("Test", "Received rename dialog tag")
            val name = bundle.getString("name")
            val id = bundle.getLong("id")
            Thread {
                database.rehearsalDao().updateName(id, name!!.ifBlank { null })
                updateDbData()
            }.start()

        }

        activityLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                Thread {
                    updateDbData()
                }.start()
            }

        return binding.root
    }

    private fun updateDbData() {
        val needProcessing = database.rehearsalDao().getUnprocessedRehearsal()
        var newShowCard = false
        if (needProcessing != null) {
            newShowCard = true
            val formattedDate = "${
                DateFormat.getDateInstance().format(Date(needProcessing.date * 1000))
            } - ${DateFormat.getTimeInstance().format(Date(needProcessing.date * 1000))}"

            inNeedOfProcessingId = needProcessing.uid
            inNeedOfProcessingFileName = needProcessing.fileName
            inNeedOfProcessingName = needProcessing.name ?: formattedDate
            inNeedOfProcessingExternalStorage = needProcessing.externalStorage
        }

        val cursor = database.rehearsalDao().getAllCursor()
        binding.recyclerView.post {
            showCard = newShowCard

            if (cursor.count > 0 && adapter.itemCount == 0) {
                binding.recyclerView.visibility = View.VISIBLE
                binding.emptyView.visibility = View.GONE
            } else if (cursor.count == 0 && adapter.itemCount > 0) {
                binding.recyclerView.visibility = View.GONE
                binding.emptyView.visibility = View.VISIBLE
            }
            adapter.swapCursor(cursor)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = RehearsalsAdapter(this, this, this, null)
        Thread {
            database = Database.getInstance(requireContext())
            updateDbData()
        }.start()
        binding.recyclerView.adapter = adapter
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                findNavController().navigate(R.id.to_settings_fragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onEdit(id: Long, currentName: String?) {
        (requireActivity() as AppCompatActivity).let { activity ->
            RenameDialogFragment(
                id,
                currentName,
                R.string.r_rename
            ).show(activity.supportFragmentManager, RENAME_DIALOG_TAG)
        }
    }

    override fun onBound(holder: RehearsalsAdapter.HeaderViewHolder) {
        holder.binding.card.visibility = if (showCard) View.VISIBLE else View.GONE
        holder.binding.processNow.setOnClickListener(this)
    }

    override fun onItemClicked(holder: RehearsalsAdapter.RehearsalViewHolder) {
        Thread {
            val status = database.rehearsalDao().getRehearsal(holder.id)?.status
            requireActivity().runOnUiThread {
                when (status) {
                    Rehearsal.RECORDED -> {
                        MaterialInfoDialogFragment(
                            R.string.dialog_recorded_title,
                            R.string.dialog_recorded_message,
                        ).show(
                            parentFragmentManager,
                            RECORDED_DIALOG_TAG
                        )
                    }
                    Rehearsal.NORMALIZED -> {
                        val intent = Intent(context, ProcessActivity::class.java)
                        intent.putExtra("fileName", holder.fileName)
                        intent.putExtra("rehearsalId", holder.id)
                        intent.putExtra("rehearsalName", holder.name ?: holder.formattedDate)
                        intent.putExtra("externalStorage", holder.externalStorage)
                        activityLauncher.launch(intent)
                    }
                    Rehearsal.PROCESSING -> {
                        MaterialInfoDialogFragment(
                            R.string.dialog_processing_title,
                            R.string.dialog_processing_message,
                        ).show(
                            parentFragmentManager,
                            PROCESSING_DIALOG_TAG
                        )
                    }
                    Rehearsal.PROCESSED -> {
                        val intent = Intent(requireContext(), RehearsalActivity::class.java)
                        intent.putExtra("rehearsalId", holder.id)
                        startActivity(intent)
                    }
                    else -> {
                        MaterialInfoDialogFragment(
                            R.string.dialog_error_state_title,
                            R.string.dialog_error_state_message,
                        ).show(
                            parentFragmentManager,
                            ERROR_STATE_DIALOG_TAG
                        )
                    }
                }
            }
        }.start()
    }

    override fun onClick(v: View?) {
        val intent = Intent(context, ProcessActivity::class.java)
        intent.putExtra("fileName", inNeedOfProcessingFileName)
        intent.putExtra("rehearsalId", inNeedOfProcessingId)
        intent.putExtra("rehearsalName", inNeedOfProcessingName)
        intent.putExtra("externalStorage", inNeedOfProcessingExternalStorage)
        activityLauncher.launch(intent)
    }
}