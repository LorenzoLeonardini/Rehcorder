package dev.leonardini.rehcorder.ui

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dev.leonardini.rehcorder.ProcessActivity
import dev.leonardini.rehcorder.R
import dev.leonardini.rehcorder.adapters.RehearsalsAdapter
import dev.leonardini.rehcorder.databinding.FragmentRehearsalsBinding
import dev.leonardini.rehcorder.db.*
import dev.leonardini.rehcorder.utils.MaterialInfoDialogFragment

class RehearsalsFragment : Fragment(), RehearsalsAdapter.OnRehearsalEditClickListener,
    RehearsalsAdapter.OnHeaderBoundListener, RehearsalsAdapter.OnItemClickListener,
    View.OnClickListener {

    companion object {
        private const val RECORDED_DIALOG_TAG = "RecordedDialog"
        private const val PROCESSING_DIALOG_TAG = "ProcessingDialog"
        private const val ERROR_STATE_DIALOG_TAG = "ErrorStateDialog"
    }

    private var _binding: FragmentRehearsalsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var database: AppDatabase
    private lateinit var adapter: RehearsalsAdapter

    private var showCard: Boolean = false
    private var inNeedOfProcessingId: Long = -1
    private var inNeedOfProcessingFileName: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentRehearsalsBinding.inflate(inflater, container, false)

        return binding.root

    }

    private fun updateDbData() {
        val needProcessing = database.rehearsalDao().getUnprocessedRehearsal()
        var newShowCard = false
        if (needProcessing != null) {
            newShowCard = true
            inNeedOfProcessingId = needProcessing.uid
            inNeedOfProcessingFileName = needProcessing.fileName
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
            database = Database.getInstance(context!!)
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
        (activity!! as AppCompatActivity).let { activity ->
            RenameDialogFragment(currentName, R.string.r_rename) { name ->
                Thread {
                    database.rehearsalDao().updateName(id, name.ifBlank { null })
                    updateDbData()
                }.start()
            }.show(activity.supportFragmentManager, "RenameDialog")
        }

    }

    override fun onBound(holder: RehearsalsAdapter.HeaderViewHolder) {
        holder.binding.card.visibility = if (showCard) View.VISIBLE else View.GONE
        holder.binding.processNow.setOnClickListener(this)
    }

    override fun onItemClicked(holder: RehearsalsAdapter.RehearsalViewHolder) {
        // SHARING
//        val uri = FileProvider.getUriForFile(context!!, "${context!!.applicationContext.packageName}.provider", File("${context!!.filesDir.absolutePath}/recordings/${holder.fileName}"))
//        val builder = ShareCompat.IntentBuilder(activity as AppCompatActivity)
//        builder.addStream(uri)
//        builder.setType("audio/*")
//        builder.startChooser()

        Thread {
            val status = database.rehearsalDao().getRehearsal(holder.id)?.status
            activity!!.runOnUiThread {
                when (status) {
                    Rehearsal.RECORDED -> {
                        MaterialInfoDialogFragment(
                            R.string.dialog_recorded_title,
                            R.string.dialog_recorded_message,
                            null
                        ).show(
                            parentFragmentManager,
                            RECORDED_DIALOG_TAG
                        )
                    }
                    Rehearsal.NORMALIZED -> {
                        val intent = Intent(context, ProcessActivity::class.java)
                        intent.putExtra("fileName", holder.fileName)
                        intent.putExtra("rehearsalId", holder.id)
                        startActivity(intent)
                    }
                    Rehearsal.PROCESSING -> {
                        MaterialInfoDialogFragment(
                            R.string.dialog_processing_title,
                            R.string.dialog_processing_message,
                            null
                        ).show(
                            parentFragmentManager,
                            PROCESSING_DIALOG_TAG
                        )
                    }
                    Rehearsal.PROCESSED -> {
                        // todo rehearsal activity
                    }
                    else -> {
                        MaterialInfoDialogFragment(
                            R.string.dialog_error_state_title,
                            R.string.dialog_error_state_message,
                            null
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
        startActivity(intent)
    }
}