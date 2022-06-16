package dev.leonardini.rehcorder.adapters

import android.database.Cursor
import androidx.recyclerview.widget.RecyclerView

// https://androidwave.com/implementation-of-recyclerview-with-cursor-adapter/
abstract class RecyclerViewCursorAdapter<V : RecyclerView.ViewHolder>(c: Cursor?) :
    RecyclerView.Adapter<V>() {

    private var cursor: Cursor? = null
    private var dataValid: Boolean = false
    private var rowIDColumn = -1

    abstract fun onBindViewHolder(holder: V, cursor: Cursor, position: Int)
    abstract fun onBindHeaderViewHolder(holder: V)

    companion object ViewTypes {
        const val HEADER_VIEW = 0
        const val ITEM_VIEW = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) HEADER_VIEW else ITEM_VIEW
    }

    init {
//        setHasStableIds(true)
        swapCursor(c)
    }

    private fun checkValidity(position: Int) {
        if (!dataValid) {
            throw IllegalStateException("Cannot bind view holder when cursor is in invalid state.")
        }
        if (!cursor!!.moveToPosition(position)) {
            throw IllegalStateException("Could not move cursor to position $position when trying to bind view holder")
        }
    }

    override fun onBindViewHolder(holder: V, position: Int) {
        if (position == 0) {
            onBindHeaderViewHolder(holder)
        } else {
            checkValidity(position - 1)
            onBindViewHolder(holder, cursor!!, position - 1)
        }
    }

    override fun getItemCount(): Int {
        return 1 + if (dataValid) cursor!!.count else 0
    }

    override fun getItemId(position: Int): Long {
        if (position == 0) {
            return -1
        }
        checkValidity(position - 1)
        return cursor!!.getLong(rowIDColumn)
    }

    fun getItem(position: Int): Cursor? {
        if (position == 0) {
            return null
        }
        checkValidity(position - 1)
        return cursor
    }

    fun swapCursor(newCursor: Cursor?) {
        if (newCursor == cursor) {
            return
        }

        if (newCursor !== null) {
            cursor?.close()
            cursor = newCursor
            dataValid = true
            rowIDColumn = cursor!!.getColumnIndex("_ID")
            notifyDataSetChanged()
        } else {
            notifyItemRangeRemoved(0, itemCount)
            cursor = null
            rowIDColumn = -1
            dataValid = false
        }
    }
}