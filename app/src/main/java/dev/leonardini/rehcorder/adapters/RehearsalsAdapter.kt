package dev.leonardini.rehcorder.adapters

import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.database.getStringOrNull
import androidx.recyclerview.widget.RecyclerView
import dev.leonardini.rehcorder.R
import dev.leonardini.rehcorder.databinding.RehearsalHeaderBinding
import dev.leonardini.rehcorder.databinding.RehearsalLayoutBinding
import java.text.DateFormat
import java.util.*

class RehearsalsAdapter(
    private val editElementListener: OnRehearsalEditClickListener,
    private val headerBoundListener: OnHeaderBoundListener,
    private val itemClickListener: OnItemClickListener,
    cursor: Cursor?
) :
    RecyclerViewCursorAdapter<RecyclerView.ViewHolder>(cursor) {

    private var uidIdx: Int = -1
    private var nameIdx: Int = -1
    private var dateIdx: Int = -1
    private var songsCountIdx: Int = -1
    private var fileNameIdx: Int = -1
    private var externalStorageIdx: Int = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == HEADER_VIEW) {
            val v =
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.rehearsal_header, parent, false)
            HeaderViewHolder(v)
        } else {
            val v =
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.rehearsal_layout, parent, false)
            RehearsalViewHolder(v, editElementListener, itemClickListener)
        }
    }

    override fun onCursorSwapped(cursor: Cursor) {
        uidIdx = cursor.getColumnIndexOrThrow("uid")
        nameIdx = cursor.getColumnIndexOrThrow("name")
        dateIdx = cursor.getColumnIndexOrThrow("date")
        songsCountIdx = cursor.getColumnIndexOrThrow("songs_count")
        fileNameIdx = cursor.getColumnIndexOrThrow("file_name")
        externalStorageIdx = cursor.getColumnIndexOrThrow("external_storage")
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, cursor: Cursor, position: Int) {
        val id: Long = cursor.getLong(uidIdx)
        val name: String? = cursor.getStringOrNull(nameIdx)
        val date: Long = cursor.getLong(dateIdx)
        val songsCount: Int = cursor.getInt(songsCountIdx)
        val fileName: String = cursor.getString(fileNameIdx)
        val externalStorage: Boolean = cursor.getInt(externalStorageIdx) == 1
        val formattedDate = "${
            DateFormat.getDateInstance().format(Date(date * 1000))
        } - ${DateFormat.getTimeInstance().format(Date(date * 1000))}"

        (holder as RehearsalViewHolder).let { holder ->
            holder.id = id
            holder.name = name
            holder.formattedDate = formattedDate
            holder.fileName = fileName
            holder.externalStorage = externalStorage
            holder.binding.rehearsalTitle.text = name ?: formattedDate
            holder.binding.rehearsalDate.text = formattedDate
            holder.binding.rehearsalSongs.text =
                holder.binding.rehearsalSongs.resources.getQuantityString(
                    R.plurals.r_count,
                    songsCount
                )
            holder.binding.divider.visibility =
                if (position != itemCount - 2) View.VISIBLE else View.INVISIBLE
        }
    }

    override fun onBindHeaderViewHolder(holder: RecyclerView.ViewHolder) {
        headerBoundListener.onBound(holder as HeaderViewHolder)
    }

    class RehearsalViewHolder(
        itemView: View,
        private val editElementListener: OnRehearsalEditClickListener,
        private val itemClickListener: OnItemClickListener
    ) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val binding: RehearsalLayoutBinding = RehearsalLayoutBinding.bind(itemView)
        var id: Long = -1
        var name: String? = null
        var formattedDate: String? = null
        var fileName: String? = null
        var externalStorage: Boolean = false

        init {
            binding.root.setOnClickListener(this)
            binding.rehearsalEditButton.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            if (v == binding.rehearsalEditButton) {
                editElementListener.onEdit(id, name)
            } else {
                itemClickListener.onItemClicked(this)
            }
        }
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: RehearsalHeaderBinding = RehearsalHeaderBinding.bind(itemView)
    }

    interface OnRehearsalEditClickListener {
        fun onEdit(id: Long, currentName: String?)
    }

    interface OnHeaderBoundListener {
        fun onBound(holder: HeaderViewHolder)
    }

    interface OnItemClickListener {
        fun onItemClicked(holder: RehearsalViewHolder)
    }

}