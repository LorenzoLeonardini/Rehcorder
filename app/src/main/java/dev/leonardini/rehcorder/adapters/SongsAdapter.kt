package dev.leonardini.rehcorder.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.leonardini.rehcorder.R
import dev.leonardini.rehcorder.Song
import dev.leonardini.rehcorder.databinding.SongLayoutBinding

class SongsAdapter : RecyclerView.Adapter<SongsAdapter.DemoViewHolder>() {
    private val songs = ArrayList<Song>()

    init {
        songs.add(Song("Shadow", 4))
        songs.add(Song("I Love Rock n Roll", 2))
        songs.add(Song("Seven Nation Army", 3))
    }

    override fun getItemCount(): Int {
        return songs.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DemoViewHolder {
        val v =
            LayoutInflater.from(parent.context).inflate(R.layout.song_layout, parent, false)
        return DemoViewHolder(v)
    }

    override fun onBindViewHolder(holder: DemoViewHolder, position: Int) {
        songs[position].let {
            holder.binding.songTitle.text = it.title
            holder.binding.songVersions.text = it.versions.toString() + " Recordings"
            holder.binding.divider.visibility =
                if (position != songs.size - 1) View.VISIBLE else View.INVISIBLE
        }
    }

    class DemoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: SongLayoutBinding = SongLayoutBinding.bind(itemView)
    }

}