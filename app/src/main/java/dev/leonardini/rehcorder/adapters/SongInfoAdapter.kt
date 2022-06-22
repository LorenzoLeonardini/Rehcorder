package dev.leonardini.rehcorder.adapters

import android.annotation.SuppressLint
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.database.getStringOrNull
import androidx.recyclerview.widget.RecyclerView
import dev.leonardini.rehcorder.R
import dev.leonardini.rehcorder.databinding.SongInfoHeaderBinding
import dev.leonardini.rehcorder.databinding.TrackItemBinding
import java.text.DateFormat
import java.util.*

class SongInfoAdapter(
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
                    .inflate(R.layout.song_info_header, parent, false)
            HeaderViewHolder(v)
        } else {
            val v =
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.track_item, parent, false)
            SongInfoViewHolder(v, shareElementListener, itemClickListener)
        }
    }

    @SuppressLint("Range")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, cursor: Cursor, position: Int) {
        val id: Long = cursor.getLong(cursor.getColumnIndex("uid"))
        val name: String? = cursor.getStringOrNull(cursor.getColumnIndex("name"))
        val date: Long = cursor.getLong(cursor.getColumnIndex("date"))
        val version: Int = cursor.getInt(cursor.getColumnIndex("version"))
        val fileName: String = cursor.getString(cursor.getColumnIndex("file_name"))
        var externalStorage: Boolean = cursor.getInt(cursor.getColumnIndex("external_storage")) == 1
        val formattedDate = "${
            DateFormat.getDateInstance().format(Date(date * 1000))
        } - ${DateFormat.getTimeInstance().format(Date(date * 1000))}"

        (holder as SongInfoViewHolder).let { holder ->
            holder.id = id
            holder.name = name
            holder.version = version
            holder.fileName = fileName
            holder.externalStorage = externalStorage
            holder.binding.trackTitle.text = name ?: formattedDate
            holder.binding.trackDate.text = formattedDate
            holder.binding.divider.visibility =
                if (position != itemCount - 2) View.VISIBLE else View.INVISIBLE
        }
    }

    override fun onBindHeaderViewHolder(holder: RecyclerView.ViewHolder) {
        headerBoundListener.onBound(holder as HeaderViewHolder)
    }

    class SongInfoViewHolder(
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
        var externalStorage: Boolean = false

        init {
            binding.root.setOnClickListener(this)
            binding.trackShareButton.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            if (v == binding.trackShareButton) {
                shareElementListener.onShare(this)
            } else {
                itemClickListener.onItemClicked(this)
            }
        }
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: SongInfoHeaderBinding = SongInfoHeaderBinding.bind(itemView)
    }

    interface OnTrackShareClickListener {
        fun onShare(holder: SongInfoViewHolder)
    }

    interface OnHeaderBoundListener {
        fun onBound(holder: HeaderViewHolder)
    }

    interface OnItemClickListener {
        fun onItemClicked(holder: SongInfoViewHolder)
    }

}