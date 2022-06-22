package dev.leonardini.rehcorder.adapters

import android.annotation.SuppressLint
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.leonardini.rehcorder.R
import dev.leonardini.rehcorder.databinding.RehearsalInfoHeaderBinding
import dev.leonardini.rehcorder.databinding.TrackItemBinding

class RehearsalInfoAdapter(
    private val shareElementListener: OnTrackShareClickListener,
    private val headerBoundListener: OnHeaderBoundListener,
    private val itemClickListener: OnItemClickListener,
    cursor: Cursor?
) :
    RecyclerViewCursorAdapter<RecyclerView.ViewHolder>(cursor) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == HEADER_VIEW) {
            val v =
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.rehearsal_info_header, parent, false)
            HeaderViewHolder(v)
        } else {
            val v =
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.track_item, parent, false)
            RehearsalInfoViewHolder(v, shareElementListener, itemClickListener)
        }
    }

    @SuppressLint("Range")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, cursor: Cursor, position: Int) {
        val id: Long = cursor.getLong(cursor.getColumnIndex("uid"))
        val name: String? = cursor.getString(cursor.getColumnIndex("name"))
        val version: Int = cursor.getInt(cursor.getColumnIndex("version"))
        val fileName: String = cursor.getString(cursor.getColumnIndex("file_name"))

        (holder as RehearsalInfoViewHolder).let { holder ->
            holder.id = id
            holder.name = name
            holder.version = version
            holder.fileName = fileName
            holder.binding.trackTitle.text = name
            holder.binding.trackDate.text = "Version " + version
            holder.binding.divider.visibility =
                if (position != itemCount - 2) View.VISIBLE else View.INVISIBLE
        }
    }

    override fun onBindHeaderViewHolder(holder: RecyclerView.ViewHolder) {
        headerBoundListener.onBound(holder as HeaderViewHolder)
    }

    class RehearsalInfoViewHolder(
        itemView: View,
        private val shareElementListener: OnTrackShareClickListener,
        private val itemClickListener: OnItemClickListener
    ) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val binding: TrackItemBinding = TrackItemBinding.bind(itemView)
        var id: Long = -1
        var name: String? = null
        var version: Int = 0
        var fileName: String? = null

        init {
            binding.root.setOnClickListener(this)
            binding.trackShareButton.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            if (v == binding.trackShareButton) {
                shareElementListener.onShare(id, fileName!!, version)
            } else {
                itemClickListener.onItemClicked(this)
            }
        }
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: RehearsalInfoHeaderBinding = RehearsalInfoHeaderBinding.bind(itemView)
    }

    interface OnTrackShareClickListener {
        fun onShare(id: Long, fileName: String, version: Int)
    }

    interface OnHeaderBoundListener {
        fun onBound(holder: HeaderViewHolder)
    }

    interface OnItemClickListener {
        fun onItemClicked(holder: RehearsalInfoViewHolder)
    }

}