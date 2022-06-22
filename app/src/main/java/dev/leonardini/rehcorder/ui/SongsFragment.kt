package dev.leonardini.rehcorder.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dev.leonardini.rehcorder.R
import dev.leonardini.rehcorder.SongActivity
import dev.leonardini.rehcorder.adapters.SongsAdapter
import dev.leonardini.rehcorder.databinding.FragmentSongsBinding
import dev.leonardini.rehcorder.db.AppDatabase
import dev.leonardini.rehcorder.db.Database
import dev.leonardini.rehcorder.ui.dialogs.RenameDialogFragment

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class SongsFragment : Fragment(), SongsAdapter.OnSongEditClickListener,
    SongsAdapter.OnHeaderBoundListener, SongsAdapter.OnItemClickListener {

    companion object {
        private const val RENAME_DIALOG_TAG = "RenameDialog"
    }

    private var _binding: FragmentSongsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var database: AppDatabase
    private lateinit var adapter: SongsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSongsBinding.inflate(inflater, container, false)

        requireActivity().supportFragmentManager.setFragmentResultListener(
            SongsFragment.RENAME_DIALOG_TAG,
            viewLifecycleOwner
        ) { _, bundle ->
            Log.i("Test", "Received rename dialog tag")
            val name = bundle.getString("name")
            val id = bundle.getLong("id")
            if (name!!.isNotBlank()) {
                Thread {
                    database.songDao().updateName(id, name)
                    updateDbData()
                }.start()
            }
        }

        return binding.root
    }

    private fun updateDbData() {
        val cursor = database.songDao().getAllCursor()
        binding.recyclerView.post {
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
        adapter = SongsAdapter(this, this, this, null)
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
                R.string.s_rename
            ).show(activity.supportFragmentManager, SongsFragment.RENAME_DIALOG_TAG)
        }
    }

    override fun onBound(holder: SongsAdapter.HeaderViewHolder) {
    }

    override fun onItemClicked(holder: SongsAdapter.SongViewHolder) {
        val intent = Intent(requireContext(), SongActivity::class.java)
        intent.putExtra("songId", holder.id)
        startActivity(intent)
    }

}