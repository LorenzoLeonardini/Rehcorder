package dev.leonardini.rehcorder.adapters

import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.leonardini.rehcorder.R
import dev.leonardini.rehcorder.databinding.RehearsalInfoHeaderBinding
import dev.leonardini.rehcorder.databinding.TrackItemBinding

/**
 * RecyclerView Adapter for information about a Rehearsal
 */
class RehearsalInfoAdapter(
    private val shareElementListener: OnTrackShareClickListener,
    private val itemClickListener: OnItemClickListener,
) :
    RecyclerViewCursorAdapter<RecyclerView.ViewHolder>(null) {

    private var uidIdx: Int = -1
    private var nameIdx: Int = -1
    private var versionIdx: Int = -1
    private var fileNameIdx: Int = -1
    private var externalStorageIdx: Int = -1

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

    override fun onCursorSwapped(cursor: Cursor) {
        uidIdx = cursor.getColumnIndexOrThrow("uid")
        nameIdx = cursor.getColumnIndexOrThrow("name")
        versionIdx = cursor.getColumnIndexOrThrow("version")
        fileNameIdx = cursor.getColumnIndexOrThrow("file_name")
        externalStorageIdx = cursor.getColumnIndexOrThrow("external_storage")
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, cursor: Cursor, position: Int) {
        val id: Long = cursor.getLong(uidIdx)
        val name: String? = cursor.getString(nameIdx)
        val version: Int = cursor.getInt(versionIdx)
        val fileName: String = cursor.getString(fileNameIdx)
        val externalStorage: Boolean = cursor.getInt(externalStorageIdx) == 1

        (holder as RehearsalInfoViewHolder).let { _holder ->
            _holder.id = id
            _holder.name = name
            _holder.version = version
            _holder.fileName = fileName
            _holder.externalStorage = externalStorage
            _holder.binding.trackTitle.text = name
            _holder.binding.trackDate.text =
                _holder.binding.trackDate.resources.getString(R.string.s_l_version, version)
            _holder.binding.divider.visibility =
                if (position != itemCount - 2) View.VISIBLE else View.INVISIBLE
        }
    }

    override fun onBindHeaderViewHolder(holder: RecyclerView.ViewHolder) {
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
        val binding: RehearsalInfoHeaderBinding = RehearsalInfoHeaderBinding.bind(itemView)
    }

    interface OnTrackShareClickListener {
        fun onShare(holder: RehearsalInfoViewHolder)
    }

    interface OnItemClickListener {
        fun onItemClicked(holder: RehearsalInfoViewHolder)
    }

}