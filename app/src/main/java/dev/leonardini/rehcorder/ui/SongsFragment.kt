package dev.leonardini.rehcorder.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dev.leonardini.rehcorder.R
import dev.leonardini.rehcorder.SongInfoActivity
import dev.leonardini.rehcorder.adapters.SongsAdapter
import dev.leonardini.rehcorder.databinding.FragmentSongsBinding
import dev.leonardini.rehcorder.ui.dialogs.RenameDialogFragment
import dev.leonardini.rehcorder.viewmodels.SongsViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SongsFragment : Fragment(), SongsAdapter.OnSongEditClickListener,
    SongsAdapter.OnHeaderBoundListener, SongsAdapter.OnItemClickListener {

    companion object {
        private const val RENAME_DIALOG_TAG = "RenameDialog"
    }

    private var _binding: FragmentSongsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var adapter: SongsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSongsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        val linearLayoutManager = LinearLayoutManager(context)
        binding.recyclerView.layoutManager = linearLayoutManager
        adapter = SongsAdapter(this, this, this)
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

        val model: SongsViewModel by viewModels()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.songs.collectLatest { pagingData ->
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

        requireActivity().supportFragmentManager.setFragmentResultListener(
            RENAME_DIALOG_TAG,
            viewLifecycleOwner
        ) { _, bundle ->
            val name = bundle.getString("name")
            val id = bundle.getLong("id")
            if (name!!.isNotBlank()) {
                model.updateSongName(id, name)
            }
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
        RenameDialogFragment(
            id,
            currentName,
            R.string.s_rename
        ).show(parentFragmentManager, RENAME_DIALOG_TAG)
    }

    override fun onBound(holder: SongsAdapter.HeaderViewHolder) {
    }

    override fun onItemClicked(holder: SongsAdapter.SongViewHolder) {
        val intent = Intent(requireContext(), SongInfoActivity::class.java)
        intent.putExtra("songId", holder.id)
        startActivity(intent)
    }

}