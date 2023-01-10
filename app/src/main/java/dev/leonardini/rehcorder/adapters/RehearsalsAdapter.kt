package dev.leonardini.rehcorder.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import dev.leonardini.rehcorder.R
import dev.leonardini.rehcorder.databinding.RehearsalHeaderBinding
import dev.leonardini.rehcorder.databinding.RehearsalItemBinding
import dev.leonardini.rehcorder.db.RehearsalWithSongsCount
import java.text.DateFormat
import java.util.*

/**
 * RecyclerView Adapter for a list of all Rehearsals
 */
class RehearsalsAdapter(
    private val editElementListener: OnRehearsalEditClickListener,
    private val headerBoundListener: OnHeaderBoundListener,
    private val itemClickListener: OnItemClickListener,
) :
    PagingDataAdapter<UiModel, RecyclerView.ViewHolder>(COMPARATOR) {

    object COMPARATOR : DiffUtil.ItemCallback<UiModel>() {
        override fun areItemsTheSame(
            oldItem: UiModel,
            newItem: UiModel
        ): Boolean {
            return (oldItem is RehearsalsHeader
                    && newItem is RehearsalsHeader)
                    || (oldItem is RehearsalWithSongsCount
                    && newItem is RehearsalWithSongsCount
                    && oldItem.uid == newItem.uid)
        }

        override fun areContentsTheSame(
            oldItem: UiModel,
            newItem: UiModel
        ): Boolean {
            return oldItem is RehearsalWithSongsCount
                    && newItem is RehearsalWithSongsCount
                    && oldItem == newItem
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (peek(position)) {
            is RehearsalWithSongsCount -> UiModelType.ITEM
            is RehearsalsHeader -> UiModelType.HEADER
            else -> throw IllegalStateException("Unknown view")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            UiModelType.ITEM -> {
                val v =
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.rehearsal_item, parent, false)
                RehearsalViewHolder(v, editElementListener, itemClickListener)
            }
            UiModelType.HEADER -> {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.rehearsal_header, parent, false)
                HeaderViewHolder(v)
            }
            else -> throw IllegalStateException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is RehearsalWithSongsCount -> {
                val formattedDate = "${
                    DateFormat.getDateInstance().format(Date(item.date * 1000))
                } - ${DateFormat.getTimeInstance().format(Date(item.date * 1000))}"

                (holder as RehearsalViewHolder).let { _holder ->
                    _holder.id = item.uid
                    _holder.name = item.name
                    _holder.formattedDate = formattedDate
                    _holder.fileName = item.file_name
                    _holder.externalStorage = item.external_storage
                    _holder.status = item.status
                    _holder.binding.rehearsalTitle.text = item.name ?: formattedDate
                    _holder.binding.rehearsalDate.text = formattedDate
                    _holder.binding.rehearsalSongs.text =
                        _holder.binding.rehearsalSongs.resources.getQuantityString(
                            R.plurals.r_count,
                            item.songs_count,
                            item.songs_count
                        )
                }
            }
            is RehearsalsHeader -> {
                headerBoundListener.onBound(holder as HeaderViewHolder)
            }
        }
    }

    class RehearsalViewHolder(
        itemView: View,
        private val editElementListener: OnRehearsalEditClickListener,
        private val itemClickListener: OnItemClickListener
    ) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {

        val binding: RehearsalItemBinding = RehearsalItemBinding.bind(itemView)
        var id: Long = -1
        var name: String? = null
        var formattedDate: String? = null
        var fileName: String? = null
        var externalStorage: Boolean = false
        var status: Int = -1

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

class RehearsalsHeader : UiModel()