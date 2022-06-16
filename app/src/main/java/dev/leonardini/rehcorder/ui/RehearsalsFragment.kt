package dev.leonardini.rehcorder.ui

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dev.leonardini.rehcorder.R
import dev.leonardini.rehcorder.adapters.RehearsalsAdapter
import dev.leonardini.rehcorder.databinding.FragmentRehearsalsBinding
import dev.leonardini.rehcorder.db.Database
import dev.leonardini.rehcorder.db.TABLE_REHEARSALS

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class RehearsalsFragment : Fragment(), RehearsalsAdapter.OnRehearsalEditClick,
    RehearsalsAdapter.OnHeaderBound {

    private var _binding: FragmentRehearsalsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var database: SQLiteDatabase
    private lateinit var adapter: RehearsalsAdapter

    private var showCard: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentRehearsalsBinding.inflate(inflater, container, false)

        return binding.root

    }

    private fun updateDbData() {
        val needProcessing = database.query(
            TABLE_REHEARSALS,
            arrayOf("_id", "processed"),
            "processed=FALSE",
            null,
            null,
            null,
            "date DESC"
        )
        synchronized(showCard) {
            showCard = needProcessing.count == 0
        }
        needProcessing.close()

        val cursor = database.query(
            TABLE_REHEARSALS,
            arrayOf("_id", "name", "date", "songsCount"),
            null,
            null,
            null,
            null,
            "date DESC"
        )
        binding.recyclerView.post {
            adapter.swapCursor(cursor)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = RehearsalsAdapter(this, this, null)
        Thread {
            database = Database(context!!).writableDatabase
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
        database.close()
    }

    override fun onEdit(id: Long, currentName: String?) {
        (activity!! as AppCompatActivity).let { activity ->
            RenameDialogFragment(currentName, R.string.r_rename) { name ->
                val contentValues = ContentValues()
                contentValues.put("name", name)
                Thread {
                    database.update(
                        TABLE_REHEARSALS,
                        contentValues,
                        "_ID=?",
                        arrayOf(id.toString())
                    )
                    updateDbData()
                }.start()
            }.show(activity.supportFragmentManager, "RenameDialog")
        }

    }

    override fun onBound(holder: RehearsalsAdapter.HeaderViewHolder) {
        synchronized(showCard) {
            holder.binding.card.visibility = if (showCard) View.GONE else View.VISIBLE
        }
    }
}