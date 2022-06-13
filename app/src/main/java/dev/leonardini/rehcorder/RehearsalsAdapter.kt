package dev.leonardini.rehcorder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.leonardini.rehcorder.databinding.RehearsalLayoutBinding

class RehearsalsAdapter : RecyclerView.Adapter<RehearsalsAdapter.DemoViewHolder>() {
    private val rehearsals = ArrayList<Rehearsal>()

    init {
        rehearsals.add(Rehearsal("June 19th, 2022", "June 19th, 2022", 10))
        rehearsals.add(Rehearsal("June 11th, 2022", "June 11th, 2022", 6))
        rehearsals.add(Rehearsal("June 4th, 2022", "June 4th, 2022", 20))
    }

    override fun getItemCount(): Int {
        return rehearsals.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DemoViewHolder {
        val v =
            LayoutInflater.from(parent.context).inflate(R.layout.rehearsal_layout, parent, false)
        return DemoViewHolder(v)
    }

    override fun onBindViewHolder(holder: DemoViewHolder, position: Int) {
        rehearsals[position].let {
            holder.binding.rehearsalTitle.text = it.title
            holder.binding.rehearsalDate.text = it.date
            holder.binding.rehearsalSongs.text = it.songs.toString() + " Songs"
            holder.binding.divider.visibility = if (position != rehearsals.size - 1) View.VISIBLE else View.INVISIBLE
        }
    }

    class DemoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: RehearsalLayoutBinding = RehearsalLayoutBinding.bind(itemView)
    }

}