package dev.leonardini.rehcorder.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dev.leonardini.rehcorder.R
import dev.leonardini.rehcorder.SongActivity
import dev.leonardini.rehcorder.adapters.SongsAdapter
import dev.leonardini.rehcorder.databinding.FragmentSongsBinding
import dev.leonardini.rehcorder.db.AppDatabase
import dev.leonardini.rehcorder.db.Database

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class SongsFragment : Fragment(), SongsAdapter.OnSongEditClickListener,
    SongsAdapter.OnHeaderBoundListener, SongsAdapter.OnItemClickListener {

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
    }

    override fun onBound(holder: SongsAdapter.HeaderViewHolder) {
    }

    override fun onItemClicked(holder: SongsAdapter.SongViewHolder) {
        val intent = Intent(requireContext(), SongActivity::class.java)
        intent.putExtra("songId", holder.id)
        startActivity(intent)
//        Thread {
//            val status = database.rehearsalDao().getRehearsal(holder.id)?.status
//            requireActivity().runOnUiThread {
//                when (status) {
//                    Rehearsal.RECORDED -> {
//                        MaterialInfoDialogFragment(
//                            R.string.dialog_recorded_title,
//                            R.string.dialog_recorded_message,
//                            null
//                        ).show(
//                            parentFragmentManager,
//                            RehearsalsFragment.RECORDED_DIALOG_TAG
//                        )
//                    }
//                    Rehearsal.NORMALIZED -> {
//                        val intent = Intent(context, ProcessActivity::class.java)
//                        intent.putExtra("fileName", holder.fileName)
//                        intent.putExtra("rehearsalId", holder.id)
//                        startActivity(intent)
//                    }
//                    Rehearsal.PROCESSING -> {
//                        MaterialInfoDialogFragment(
//                            R.string.dialog_processing_title,
//                            R.string.dialog_processing_message,
//                            null
//                        ).show(
//                            parentFragmentManager,
//                            RehearsalsFragment.PROCESSING_DIALOG_TAG
//                        )
//                    }
//                    Rehearsal.PROCESSED -> {
//                        // todo rehearsal activity
//                    }
//                    else -> {
//                        MaterialInfoDialogFragment(
//                            R.string.dialog_error_state_title,
//                            R.string.dialog_error_state_message,
//                            null
//                        ).show(
//                            parentFragmentManager,
//                            RehearsalsFragment.ERROR_STATE_DIALOG_TAG
//                        )
//                    }
//                }
//            }
//        }.start()
    }

}