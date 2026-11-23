package com.example.musicplayer.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.data.db.TrackEntity
import com.example.musicplayer.databinding.ItemTrackBinding
import com.example.musicplayer.player.PlaybackState

class TrackListAdapter(
    private val onOpen: (TrackEntity) -> Unit,
    private val onPlayToggle: (TrackEntity) -> Unit,
    private val onLikeToggle: (TrackEntity) -> Unit
) : ListAdapter<TrackEntity, TrackListAdapter.TrackViewHolder>(DiffCallback) {

    private var activeTrackId: Long? = null
    private var isPlaying = false

    /** Highlights the current track and switches its action icon to pause. */
    fun setPlaybackState(state: PlaybackState) {
        val previousId = activeTrackId
        val previousPlaying = isPlaying
        if (previousId == state.trackId && previousPlaying == state.isPlaying) return

        activeTrackId = state.trackId
        isPlaying = state.isPlaying
        listOf(previousId, state.trackId).distinct().forEach { id ->
            val index = currentList.indexOfFirst { it.id == id }
            if (index >= 0) notifyItemChanged(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val binding = ItemTrackBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TrackViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TrackViewHolder(
        private val binding: ItemTrackBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(track: TrackEntity) {
            val context = binding.root.context
            val isActive = track.id == activeTrackId

            binding.tvTitle.text = track.title
            binding.tvArtist.text = track.artist.ifBlank {
                context.getString(R.string.unknown_artist)
            }
            binding.tvTitle.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (isActive) R.color.primary else R.color.on_surface
                )
            )
            binding.ivCover.loadCover(track.coverPath)
            binding.btnLike.setImageResource(
                if (track.isLiked) R.drawable.ic_like_filled else R.drawable.ic_like_outline
            )
            binding.btnLike.contentDescription = context.getString(
                if (track.isLiked) R.string.remove_like else R.string.add_like
            )
            binding.btnPlay.setImageResource(
                if (isActive && isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            )
            binding.btnPlay.contentDescription = context.getString(
                if (isActive && isPlaying) R.string.pause else R.string.play
            )

            listOf(binding.btnPlay, binding.btnLike).forEach { button ->
                button.animate().cancel()
                button.scaleX = 1f
                button.scaleY = 1f
            }
            binding.root.setOnClickListener { onOpen(track) }
            binding.btnPlay.bounceOnClick { onPlayToggle(track) }
            binding.btnLike.bounceOnClick { onLikeToggle(track.copy(isLiked = !track.isLiked)) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<TrackEntity>() {
        override fun areItemsTheSame(oldItem: TrackEntity, newItem: TrackEntity) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: TrackEntity, newItem: TrackEntity) =
            oldItem == newItem
    }
}
