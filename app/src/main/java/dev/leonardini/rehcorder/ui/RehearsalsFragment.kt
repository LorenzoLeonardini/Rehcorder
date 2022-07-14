package dev.leonardini.rehcorder.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dev.leonardini.rehcorder.R
import dev.leonardini.rehcorder.RehearsalInfoActivity
import dev.leonardini.rehcorder.SplitterActivity
import dev.leonardini.rehcorder.adapters.RehearsalsAdapter
import dev.leonardini.rehcorder.databinding.FragmentRehearsalsBinding
import dev.leonardini.rehcorder.db.Rehearsal
import dev.leonardini.rehcorder.ui.dialogs.MaterialInfoDialogFragment
import dev.leonardini.rehcorder.ui.dialogs.RenameDialogFragment
import dev.leonardini.rehcorder.viewmodels.RehearsalsViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
    private lateinit var model: RehearsalsViewModel
    private lateinit var adapter: RehearsalsAdapter

    private var inNeedOfProcessing: Rehearsal? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRehearsalsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        val linearLayoutManager = LinearLayoutManager(context)
        binding.recyclerView.layoutManager = linearLayoutManager
        adapter = RehearsalsAdapter(this, this, this)
        binding.recyclerView.adapter = adapter
        val itemDecoration = MyMaterialDividerItemDecoration(
            binding.recyclerView.context,
            linearLayoutManager.orientation
        )
        itemDecoration.isLastItemDecorated = false
        itemDecoration.isFirstItemDecorated = false
        itemDecoration.setDividerInsetStartResource(requireContext(), R.dimen.divider_inset)
        itemDecoration.setDividerInsetEndResource(requireContext(), R.dimen.divider_inset)
        binding.recyclerView.addItemDecoration(itemDecoration)

        val model: RehearsalsViewModel by viewModels()
        this.model = model
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.rehearsals.collectLatest { pagingData ->
                    adapter.submitData(pagingData)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                adapter.loadStateFlow.collectLatest {
                    if (adapter.itemCount == 1) {
                        binding.recyclerView.visibility = View.GONE
                        binding.emptyView.visibility = View.VISIBLE
                    } else if (adapter.itemCount > 1) {
                        binding.recyclerView.visibility = View.VISIBLE
                        binding.emptyView.visibility = View.GONE
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.inNeedOfProcessRehearsal.collectLatest { rehearsal ->
                    inNeedOfProcessing = rehearsal
                    adapter.notifyItemChanged(0)
                }
            }
        }

        requireActivity().supportFragmentManager.setFragmentResultListener(
            RENAME_DIALOG_TAG,
            viewLifecycleOwner
        ) { _, bundle ->
            val name = bundle.getString("name")
            val id = bundle.getLong("id")
            model.updateRehearsalName(id, name!!.ifBlank { null })
        }
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
        holder.binding.card.visibility =
            if (inNeedOfProcessing != null) View.VISIBLE else View.GONE
        holder.binding.processNow.setOnClickListener(this)
    }

    override fun onItemClicked(holder: RehearsalsAdapter.RehearsalViewHolder) {
        when (holder.status) {
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
                val intent = Intent(context, SplitterActivity::class.java)
                intent.putExtra("fileName", holder.fileName)
                intent.putExtra("rehearsalId", holder.id)
                intent.putExtra("rehearsalName", holder.name ?: holder.formattedDate)
                intent.putExtra("externalStorage", holder.externalStorage)
                startActivity(intent)
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
                val intent = Intent(requireContext(), RehearsalInfoActivity::class.java)
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

    override fun onClick(v: View?) {
        inNeedOfProcessing?.let {
            val intent = Intent(context, SplitterActivity::class.java)
            intent.putExtra("fileName", it.fileName)
            intent.putExtra("rehearsalId", it.uid)
            intent.putExtra("rehearsalName", it.name)
            intent.putExtra("externalStorage", it.externalStorage)
            startActivity(intent)
        }
    }
}