package com.example.musicplayer.ui.track

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.musicplayer.R
import com.example.musicplayer.data.db.TrackEntity
import com.example.musicplayer.databinding.FragmentTrackBinding
import com.example.musicplayer.player.PlaybackState
import com.example.musicplayer.ui.MainViewModel
import com.example.musicplayer.ui.bounceOnClick
import com.example.musicplayer.ui.formatDuration
import com.example.musicplayer.ui.loadCover
import kotlinx.coroutines.launch

class TrackFragment : Fragment(R.layout.fragment_track) {

    private val viewModel: MainViewModel by activityViewModels()
    private var _binding: FragmentTrackBinding? = null
    private val binding get() = _binding!!

    private var currentTrack: TrackEntity? = null
    private var shownTrackId: Long? = null
    private var isUserSeeking = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTrackBinding.bind(view)

        val trackId = arguments?.getLong(ARG_TRACK_ID, -1L) ?: -1L

        binding.btnBack.bounceOnClick {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.btnPlay.bounceOnClick { currentTrack?.let(viewModel::toggle) }
        binding.btnPrevious.bounceOnClick { viewModel.previous() }
        binding.btnNext.bounceOnClick { viewModel.next() }
        binding.btnStop.bounceOnClick { viewModel.stop() }
        binding.btnLike.bounceOnClick {
            currentTrack?.let { viewModel.setLike(it.id, !it.isLiked) }
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) binding.tvPosition.text = formatDuration(progress.toLong())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = false
                seekBar?.let { viewModel.seekTo(it.progress.toLong()) }
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.track(trackId).collect { renderTrack(it) } }
                launch { viewModel.playbackState.collect { renderPlayback(it) } }
            }
        }
    }

    private fun renderTrack(track: TrackEntity?) {
        currentTrack = track
        binding.btnPlay.isEnabled = track != null
        binding.btnPrevious.isEnabled = track != null
        binding.btnNext.isEnabled = track != null
        binding.btnStop.isEnabled = track != null
        binding.btnLike.isEnabled = track != null

        if (track == null) {
            shownTrackId = null
            binding.tvTitle.setText(R.string.track_not_found)
            binding.tvArtist.text = ""
            return
        }
        val coverChanged = track.id != shownTrackId
        shownTrackId = track.id
        binding.tvTitle.text = track.title
        binding.tvArtist.text = track.artist.ifBlank { getString(R.string.unknown_artist) }
        binding.ivCover.loadCover(track.coverPath, animate = coverChanged)
        renderLike(track.isLiked)
        binding.tvDuration.text = formatDuration(track.durationMs)
    }

    private fun renderPlayback(state: PlaybackState) {
        binding.btnPlay.setImageResource(
            if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
        binding.btnPlay.contentDescription = getString(
            if (state.isPlaying) R.string.pause else R.string.play
        )
        binding.tvPosition.text = formatDuration(state.positionMs)
        val duration = if (state.durationMs > 0) state.durationMs else currentTrack?.durationMs ?: 0L
        binding.tvDuration.text = formatDuration(duration)
        binding.seekBar.max = duration.coerceAtLeast(1L).toInt()
        if (!isUserSeeking) {
            binding.seekBar.progress = state.positionMs.coerceIn(0L, duration).toInt()
        }
        binding.tvError.text = state.error.orEmpty()
    }

    private fun renderLike(liked: Boolean) {
        binding.btnLike.setImageResource(
            if (liked) R.drawable.ic_like_filled else R.drawable.ic_like_outline
        )
        binding.btnLike.contentDescription = getString(
            if (liked) R.string.remove_like else R.string.add_like
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val ARG_TRACK_ID = "trackId"
    }
}
