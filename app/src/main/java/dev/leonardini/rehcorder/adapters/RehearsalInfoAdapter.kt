package dev.leonardini.rehcorder.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import dev.leonardini.rehcorder.R
import dev.leonardini.rehcorder.databinding.ListHeaderBinding
import dev.leonardini.rehcorder.databinding.TrackItemBinding
import dev.leonardini.rehcorder.db.RehearsalSongs

/**
 * RecyclerView Adapter for information about a Rehearsal
 */
class RehearsalInfoAdapter(
    private val shareElementListener: OnTrackShareClickListener,
    private val itemClickListener: OnItemClickListener,
) :
    PagingDataAdapter<UiModel, RecyclerView.ViewHolder>(COMPARATOR) {

    object COMPARATOR : DiffUtil.ItemCallback<UiModel>() {
        override fun areItemsTheSame(
            oldItem: UiModel,
            newItem: UiModel
        ): Boolean {
            return (oldItem is RehearsalsInfoHeader
                    && newItem is RehearsalsInfoHeader)
                    || (oldItem is RehearsalSongs
                    && newItem is RehearsalSongs
                    && oldItem.uid == newItem.uid)
        }

        override fun areContentsTheSame(
            oldItem: UiModel,
            newItem: UiModel
        ): Boolean {
            return (oldItem is RehearsalsInfoHeader
                    && newItem is RehearsalsInfoHeader)
                    || (oldItem is RehearsalSongs
                    && newItem is RehearsalSongs
                    && oldItem == newItem)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (peek(position)) {
            is RehearsalSongs -> UiModelType.ITEM
            is RehearsalsInfoHeader -> UiModelType.HEADER
            else -> throw IllegalStateException("Unknown view")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            UiModelType.ITEM -> {
                val v =
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.track_item, parent, false)
                RehearsalInfoViewHolder(v, shareElementListener, itemClickListener)
            }
            UiModelType.HEADER -> {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.list_header, parent, false)
                HeaderViewHolder(v)
            }
            else -> throw IllegalStateException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is RehearsalSongs -> {
                (holder as RehearsalInfoViewHolder).let { _holder ->
                    _holder.id = item.uid
                    _holder.name = item.name
                    _holder.version = item.version
                    _holder.fileName = item.file_name
                    _holder.externalStorage = item.external_storage
                    _holder.binding.trackTitle.text = item.name
                    _holder.binding.trackDate.text =
                        _holder.binding.trackDate.resources.getString(
                            R.string.s_l_version,
                            item.version
                        )
                }
            }
            is RehearsalsInfoHeader -> {
            }
        }
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
        init {
            val binding: ListHeaderBinding = ListHeaderBinding.bind(itemView)
            binding.listHeaderTitle.setText(R.string.r_l_title)
        }
    }

    interface OnTrackShareClickListener {
        fun onShare(holder: RehearsalInfoViewHolder)
    }

    interface OnItemClickListener {
        fun onItemClicked(holder: RehearsalInfoViewHolder)
    }

}

class RehearsalsInfoHeader : UiModel()