package dev.leonardini.rehcorder.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import dev.leonardini.rehcorder.R
import dev.leonardini.rehcorder.databinding.SongInfoHeaderBinding
import dev.leonardini.rehcorder.databinding.TrackItemBinding
import dev.leonardini.rehcorder.db.SongRehearsals
import java.text.DateFormat
import java.util.*

/**
 * RecyclerView Adapter for information about a Rehearsal
 */
class SongInfoAdapter(
    private val shareElementListener: OnTrackShareClickListener,
    private val itemClickListener: OnItemClickListener,
) :
    PagingDataAdapter<UiModel, RecyclerView.ViewHolder>(COMPARATOR) {

    object COMPARATOR : DiffUtil.ItemCallback<UiModel>() {
        override fun areItemsTheSame(
            oldItem: UiModel,
            newItem: UiModel
        ): Boolean {
            return (oldItem is SongsInfoHeader
                    && newItem is SongsInfoHeader)
                    || (oldItem is SongRehearsals
                    && newItem is SongRehearsals
                    && oldItem.uid == newItem.uid)
        }

        override fun areContentsTheSame(
            oldItem: UiModel,
            newItem: UiModel
        ): Boolean {
            return (oldItem is SongsInfoHeader
                    && newItem is SongsInfoHeader)
                    || (oldItem is SongRehearsals
                    && newItem is SongRehearsals
                    && oldItem == newItem)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (peek(position)) {
            is SongRehearsals -> UiModelType.ITEM
            is SongsInfoHeader -> UiModelType.HEADER
            else -> throw IllegalStateException("Unknown view")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            UiModelType.ITEM -> {
                val v =
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.track_item, parent, false)
                SongInfoViewHolder(v, shareElementListener, itemClickListener)
            }
            UiModelType.HEADER -> {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.song_info_header, parent, false)
                HeaderViewHolder(v)
            }
            else -> throw IllegalStateException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is SongRehearsals -> {
                val formattedDate = "${
                    DateFormat.getDateInstance().format(Date(item.date * 1000))
                } - ${DateFormat.getTimeInstance().format(Date(item.date * 1000))}"

                (holder as SongInfoViewHolder).let { _holder ->
                    _holder.id = item.uid
                    _holder.name = item.name
                    _holder.version = item.version
                    _holder.fileName = item.file_name
                    _holder.externalStorage = item.external_storage
                    _holder.binding.trackTitle.text = item.name ?: formattedDate
                    _holder.binding.trackDate.text = formattedDate
                }
            }
            is SongsInfoHeader -> {
            }
        }
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

    interface OnItemClickListener {
        fun onItemClicked(holder: SongInfoViewHolder)
    }

}

class SongsInfoHeader : UiModel()