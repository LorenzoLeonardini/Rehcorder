package dev.leonardini.rehcorder.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dev.leonardini.rehcorder.R
import dev.leonardini.rehcorder.SongInfoActivity
import dev.leonardini.rehcorder.adapters.SongsAdapter
import dev.leonardini.rehcorder.databinding.FragmentSongsBinding
import dev.leonardini.rehcorder.ui.dialogs.RenameDialogFragment
import dev.leonardini.rehcorder.viewmodels.SongsViewModel

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

        val model: SongsViewModel by viewModels()
        model.songs.observe(viewLifecycleOwner) { cursor ->
            if (cursor.count > 0 && adapter.itemCount == 0) {
                binding.recyclerView.visibility = View.VISIBLE
                binding.emptyView.visibility = View.GONE
            } else if (cursor.count == 0 && adapter.itemCount > 0) {
                binding.recyclerView.visibility = View.GONE
                binding.emptyView.visibility = View.VISIBLE
            }
            adapter.swapCursor(cursor)
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

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = SongsAdapter(this, this, this)
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