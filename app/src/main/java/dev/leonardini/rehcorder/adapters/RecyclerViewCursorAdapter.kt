package dev.leonardini.rehcorder.adapters

import android.database.Cursor
import androidx.recyclerview.widget.RecyclerView

// https://androidwave.com/implementation-of-recyclerview-with-cursor-adapter/
/**
 * RecyclerView Adapter for Sqlite Database Cursors.
 * Adds extra view type to insert a header in the view.
 */
abstract class RecyclerViewCursorAdapter<V : RecyclerView.ViewHolder>(c: Cursor?) :
    RecyclerView.Adapter<V>() {

    private var cursor: Cursor? = null
    private var dataValid: Boolean = false
    private var rowIDColumn = -1

    /**
     * Like normal adapters onBindViewHolder, with a cursor to access data
     */
    abstract fun onBindViewHolder(holder: V, cursor: Cursor, position: Int)

    /**
     * Like normal adapters onBindViewHolder, but specifically meant for the additional
     * header view type
     */
    abstract fun onBindHeaderViewHolder(holder: V)

    /**
     * Listener for when the cursor gets swapped with new data
     */
    abstract fun onCursorSwapped(cursor: Cursor)

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
        return if (dataValid) 1 + cursor!!.count else 0
    }

    override fun getItemId(position: Int): Long {
        if (position == 0) {
            return -1
        }
        checkValidity(position - 1)
        return cursor!!.getLong(rowIDColumn)
    }

    /**
     * Returns the cursor pointing to the desired item
     * @param position Element position (ignoring the header)
     */
    fun getItem(position: Int): Cursor? {
        if (position == 0) {
            return null
        }
        checkValidity(position - 1)
        return cursor
    }

    /**
     * Swap the internal cursor for a new one containing updated data
     * The old cursor gets closed
     */
    fun swapCursor(newCursor: Cursor?) {
        if (newCursor == cursor) {
            return
        }

        if (newCursor !== null) {
            cursor?.close()
            cursor = newCursor
            dataValid = true
            rowIDColumn = cursor!!.getColumnIndex("uid")
            onCursorSwapped(cursor!!)
            notifyDataSetChanged()
        } else {
            notifyItemRangeRemoved(0, itemCount)
            cursor = null
            rowIDColumn = -1
            dataValid = false
        }
    }
}