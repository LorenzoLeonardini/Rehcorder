package dev.leonardini.rehcorder.adapters

import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.database.getStringOrNull
import androidx.recyclerview.widget.RecyclerView
import dev.leonardini.rehcorder.R
import dev.leonardini.rehcorder.databinding.SongLayoutBinding
import dev.leonardini.rehcorder.databinding.SongHeaderBinding
import java.text.DateFormat
import java.util.*

class SongsAdapter(
    private val editElementListener: OnSongEditClickListener,
    private val headerBoundListener: OnHeaderBoundListener,
    private val itemClickListener: OnItemClickListener,
    cursor: Cursor?
) :
    RecyclerViewCursorAdapter<RecyclerView.ViewHolder>(cursor) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == HEADER_VIEW) {
            val v =
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.song_header, parent, false)
            HeaderViewHolder(v)
        } else {
            val v =
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.song_layout, parent, false)
            SongViewHolder(v, editElementListener, itemClickListener)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, cursor: Cursor, position: Int) {
        val id: Long = cursor.getLong(cursor.getColumnIndex("uid"))
        val name: String = cursor.getString(cursor.getColumnIndex("name"))

        (holder as SongViewHolder).let { holder ->
            holder.id = id
            holder.name = name
            holder.binding.songTitle.text = name
            holder.binding.songVersions.text = "0 Versions"
            holder.binding.divider.visibility =
                if (position != itemCount - 2) View.VISIBLE else View.INVISIBLE
        }
    }

    override fun onBindHeaderViewHolder(holder: RecyclerView.ViewHolder) {
        headerBoundListener.onBound(holder as HeaderViewHolder)
    }

    class SongViewHolder(
        itemView: View,
        private val editElementListener: OnSongEditClickListener,
        private val itemClickListener: OnItemClickListener
    ) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val binding: SongLayoutBinding = SongLayoutBinding.bind(itemView)
        var id: Long = -1
        var name: String? = null

        init {
            binding.root.setOnClickListener(this)
            binding.songEditButton.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            if (v == binding.songEditButton) {
                editElementListener.onEdit(id, name)
            } else {
                itemClickListener.onItemClicked(this)
            }
        }
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: SongHeaderBinding = SongHeaderBinding.bind(itemView)
    }

    interface OnSongEditClickListener {
        fun onEdit(id: Long, currentName: String?)
    }

    interface OnHeaderBoundListener {
        fun onBound(holder: HeaderViewHolder)
    }

    interface OnItemClickListener {
        fun onItemClicked(holder: SongViewHolder)
    }

}