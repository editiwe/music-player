package com.example.musicplayer.ui.favorites

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicplayer.R
import com.example.musicplayer.databinding.FragmentFavoritesBinding
import com.example.musicplayer.ui.MainViewModel
import com.example.musicplayer.ui.TrackListAdapter
import kotlinx.coroutines.launch

class FavoritesFragment : Fragment(R.layout.fragment_favorites) {

    private val viewModel: MainViewModel by activityViewModels()
    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFavoritesBinding.bind(view)

        val adapter = TrackListAdapter(
            onOpen = { track ->
                findNavController().navigate(
                    R.id.action_global_track,
                    bundleOf(ARG_TRACK_ID to track.id)
                )
            },
            onPlayToggle = { track -> viewModel.toggle(track) },
            onLikeToggle = { track -> viewModel.setLike(track.id, track.isLiked) }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.favorites.collect { tracks ->
                        adapter.submitList(tracks)
                        binding.emptyState.visibility =
                            if (tracks.isEmpty()) View.VISIBLE else View.GONE
                        binding.tvFavoritesCount.text = if (tracks.isEmpty()) {
                            getString(R.string.favorites_count_zero)
                        } else {
                            getString(R.string.favorites_count, tracks.size)
                        }
                    }
                }
                launch {
                    viewModel.playbackState.collect { adapter.setPlaybackState(it) }
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.recyclerView.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val ARG_TRACK_ID = "trackId"
    }
}
