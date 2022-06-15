package dev.leonardini.rehcorder.adapters

import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.database.getStringOrNull
import androidx.recyclerview.widget.RecyclerView
import dev.leonardini.rehcorder.R
import dev.leonardini.rehcorder.databinding.RehearsalLayoutBinding
import java.text.DateFormat
import java.util.*

class RehearsalsAdapter(private val editElementListener: OnRehearsalEditClick, cursor: Cursor?) :
    RecyclerViewCursorAdapter<RehearsalsAdapter.DemoViewHolder>(cursor) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DemoViewHolder {
        val v =
            LayoutInflater.from(parent.context).inflate(R.layout.rehearsal_layout, parent, false)
        return DemoViewHolder(v, editElementListener)
    }

    override fun onBindViewHolder(holder: DemoViewHolder, cursor: Cursor, position: Int) {
        val id: Long = cursor.getLong(cursor.getColumnIndex("_ID"))
        val name: String? = cursor.getStringOrNull(cursor.getColumnIndex("name"))
        val date: Long = cursor.getLong(cursor.getColumnIndex("date"))
        val songsCount: Int = cursor.getInt(cursor.getColumnIndex("songsCount"))
        val formattedDate = "${
            DateFormat.getDateInstance().format(Date(date * 1000))
        } - ${DateFormat.getTimeInstance().format(Date(date * 1000))}"

        holder.id = id
        holder.name = name
        holder.binding.rehearsalTitle.text = name ?: formattedDate
        holder.binding.rehearsalDate.text = formattedDate
        holder.binding.rehearsalSongs.text =
            holder.binding.rehearsalSongs.resources.getString(R.string.r_count, songsCount)
        holder.binding.divider.visibility =
            if (position != itemCount - 1) View.VISIBLE else View.INVISIBLE
    }

    class DemoViewHolder(itemView: View, private val editElementListener: OnRehearsalEditClick) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val binding: RehearsalLayoutBinding = RehearsalLayoutBinding.bind(itemView)
        var id: Long = -1
        var name: String? = null

        init {
            binding.rehearsalEditButton.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            editElementListener.onEdit(id, name)
        }
    }

    interface OnRehearsalEditClick {
        fun onEdit(id: Long, currentName: String?)
    }

}