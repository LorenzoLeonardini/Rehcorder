package dev.leonardini.rehcorder.ui

import android.content.ContentValues
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.util.Log
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

class RehearsalsFragment : Fragment(), RehearsalsAdapter.OnRehearsalEditClickListener,
    RehearsalsAdapter.OnHeaderBoundListener, RehearsalsAdapter.OnItemClickListener, View.OnClickListener {

    private var _binding: FragmentRehearsalsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var database: SQLiteDatabase
    private lateinit var adapter: RehearsalsAdapter

    private var showCard: Boolean = false
    private var inNeedOfProcessingId :Long = -1
    private var inNeedOfProcessingFileName :String = ""

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
            arrayOf(REHEARSALS_ID, REHEARSALS_PROCESSED, REHEARSALS_FILE_NAME),
            "$REHEARSALS_PROCESSED=FALSE",
            null,
            null,
            null,
            "$REHEARSALS_DATE DESC"
        )
        val newShowCard = needProcessing.count != 0
        needProcessing.moveToFirst()
        if(newShowCard) {
            inNeedOfProcessingId = needProcessing.getLong(needProcessing.getColumnIndex(
                REHEARSALS_ID))
            inNeedOfProcessingFileName = needProcessing.getString(needProcessing.getColumnIndex(REHEARSALS_FILE_NAME))
        }
        needProcessing.close()

        val cursor = database.query(
            TABLE_REHEARSALS,
            arrayOf(REHEARSALS_ID, REHEARSALS_NAME, REHEARSALS_DATE, REHEARSALS_SONGS_COUNT, REHEARSALS_FILE_NAME),
            null,
            null,
            null,
            null,
            "$REHEARSALS_DATE DESC"
        )
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
                contentValues.put(REHEARSALS_NAME, name)
                Thread {
                    database.update(
                        TABLE_REHEARSALS,
                        contentValues,
                        "$REHEARSALS_ID=?",
                        arrayOf(id.toString())
                    )
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

        val intent = Intent(context, ProcessActivity::class.java)
        intent.putExtra("fileName", holder.fileName)
        intent.putExtra("rehearsalId", holder.id)
        startActivity(intent)
    }

    override fun onClick(v: View?) {
        val intent = Intent(context, ProcessActivity::class.java)
        intent.putExtra("fileName", inNeedOfProcessingFileName)
        intent.putExtra("rehearsalId", inNeedOfProcessingId)
        startActivity(intent)
    }
}