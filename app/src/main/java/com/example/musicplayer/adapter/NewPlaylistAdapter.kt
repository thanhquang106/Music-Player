package com.example.musicplayer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.databinding.ItemNewPlaylistBinding
import com.example.musicplayer.model.Playlist

class NewPlaylistAdapter() : RecyclerView.Adapter<NewPlaylistAdapter.ViewHolder>() {
    private var playlists = arrayListOf<Playlist>()
    private lateinit var listener: OnItemClickListener
    private lateinit var btnListener: OnItemButtonClickListener

    fun submitData(temp: ArrayList<Playlist>) {
        val diff = DiffUtil.calculateDiff(DiffPlaylist(playlists, temp))
        playlists.clear()
        playlists.addAll(temp)
        diff.dispatchUpdatesTo(this)
    }

    fun setOnItemClickListener(
        onItemClickListener: OnItemClickListener,
        onItemButtonClickListener: OnItemButtonClickListener
    ) {
        listener = onItemClickListener
        btnListener = onItemButtonClickListener
    }

    class ViewHolder(
        private var binding: ItemNewPlaylistBinding,
        val listener: OnItemClickListener,
        val btnListener: OnItemButtonClickListener
    ) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener() {
                listener.onItemClick(absoluteAdapterPosition)
            }
            binding.imgAdd.setOnClickListener() {
                btnListener.onItemClick(absoluteAdapterPosition, it)
            }
        }

        fun bind(p: Playlist) {
            binding.playlist = p
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemNewPlaylistBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            ), listener, btnListener
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        playlists[position].let {
            holder.bind(it)
        }
    }

    override fun getItemCount() = playlists.size
}