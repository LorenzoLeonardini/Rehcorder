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
        checkValidity(position)
        onBindViewHolder(holder, cursor!!, position)
    }

    override fun getItemCount(): Int {
        return if (dataValid) cursor!!.count else 0
    }

    override fun getItemId(position: Int): Long {
        checkValidity(position)
        return cursor!!.getLong(rowIDColumn)
    }

    fun getItem(position: Int): Cursor? {
        checkValidity(position)
        return cursor
    }

    fun swapCursor(newCursor: Cursor?) {
        if (newCursor == cursor) {
            return
        }

        if (newCursor !== null) {
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