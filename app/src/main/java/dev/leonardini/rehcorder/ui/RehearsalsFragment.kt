package dev.leonardini.rehcorder.ui

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.cursoradapter.widget.CursorAdapter
import androidx.cursoradapter.widget.SimpleCursorAdapter
import androidx.loader.app.LoaderManager
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
class RehearsalsFragment : Fragment(), RehearsalsAdapter.OnRehearsalEditClick {

    private var _binding: FragmentRehearsalsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var database: SQLiteDatabase
    private lateinit var adapter: RehearsalsAdapter

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

        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        database = Database(context!!).writableDatabase
        val cursor = database.query(
            TABLE_REHEARSALS,
            arrayOf("_id", "name", "date", "songsCount"),
            null,
            null,
            null,
            null,
            null
        )
        Log.i("CURSOR", cursor.toString())
        adapter = RehearsalsAdapter(this, null)
        adapter.swapCursor(cursor)
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

    override fun onEdit(id: Long, currentName: String) {
        (activity!! as AppCompatActivity).let { activity ->
            RenameDialogFragment(currentName) { name ->
                val contentValues = ContentValues()
                contentValues.put("name", name)
                database.update(TABLE_REHEARSALS, contentValues, "_ID=?", arrayOf(id.toString()))
                val cursor = database.query(
                    TABLE_REHEARSALS,
                    arrayOf("_id", "name", "date", "songsCount"),
                    null,
                    null,
                    null,
                    null,
                    null
                )
                adapter.swapCursor(cursor)
            }.show(activity.supportFragmentManager, "RenameDialog")
        }

    }
}