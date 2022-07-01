package dev.leonardini.rehcorder.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import dev.leonardini.rehcorder.R
import dev.leonardini.rehcorder.databinding.SongHeaderBinding
import dev.leonardini.rehcorder.databinding.SongLayoutBinding
import dev.leonardini.rehcorder.db.SongWithVersionCount

class SongsAdapter(
    private val editElementListener: OnSongEditClickListener,
    private val headerBoundListener: OnHeaderBoundListener,
    private val itemClickListener: OnItemClickListener,
) :
    PagingDataAdapter<UiModel, RecyclerView.ViewHolder>(COMPARATOR) {

    object COMPARATOR : DiffUtil.ItemCallback<UiModel>() {
        override fun areItemsTheSame(
            oldItem: UiModel,
            newItem: UiModel
        ): Boolean {
            return (oldItem is SongsHeader
                    && newItem is SongsHeader)
                    || (oldItem is SongWithVersionCount
                    && newItem is SongWithVersionCount
                    && oldItem.uid == newItem.uid)
        }

        override fun areContentsTheSame(
            oldItem: UiModel,
            newItem: UiModel
        ): Boolean {
            return (oldItem is SongsHeader
                    && newItem is SongsHeader)
                    || (oldItem is SongWithVersionCount
                    && newItem is SongWithVersionCount
                    && oldItem == newItem)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (peek(position)) {
            is SongWithVersionCount -> UiModelType.ITEM
            is SongsHeader -> UiModelType.HEADER
            else -> throw IllegalStateException("Unknown view")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            UiModelType.ITEM -> {
                val v =
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.song_layout, parent, false)
                SongViewHolder(v, editElementListener, itemClickListener)
            }
            UiModelType.HEADER -> {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.song_header, parent, false)
                HeaderViewHolder(v)
            }
            else -> throw IllegalStateException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is SongWithVersionCount -> {
                (holder as SongViewHolder).let { _holder ->
                    _holder.id = item.uid
                    _holder.name = item.name
                    _holder.binding.songTitle.text = item.name
                    _holder.binding.songVersions.text =
                        _holder.binding.songVersions.resources.getQuantityString(
                            R.plurals.s_versions,
                            item.versions_count,
                            item.versions_count
                        )
                    _holder.binding.divider.visibility =
                        if (position != itemCount - 1) View.VISIBLE else View.INVISIBLE
                }
            }
            is SongsHeader -> {
                headerBoundListener.onBound(holder as HeaderViewHolder)
            }
        }
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

class SongsHeader : UiModel()