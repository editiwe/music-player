package com.example.musicplayer.ui.main

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicplayer.R
import com.example.musicplayer.databinding.FragmentMainBinding
import com.example.musicplayer.ui.MainViewModel
import com.example.musicplayer.ui.TrackListAdapter
import com.example.musicplayer.ui.bounceOnClick
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MainFragment : Fragment(R.layout.fragment_main) {

    private val viewModel: MainViewModel by activityViewModels()
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private val openDocument = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.import(uri)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMainBinding.bind(view)

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
                    viewModel.tracks.collect { tracks ->
                        adapter.submitList(tracks)
                        binding.emptyState.visibility =
                            if (tracks.isEmpty()) View.VISIBLE else View.GONE
                        binding.tvTrackCount.text = if (tracks.isEmpty()) {
                            getString(R.string.tracks_count_zero)
                        } else {
                            getString(R.string.tracks_count, tracks.size)
                        }
                        binding.btnPlayAll.isEnabled = tracks.isNotEmpty()
                    }
                }
                launch {
                    viewModel.playbackState.collect { adapter.setPlaybackState(it) }
                }
                launch {
                    viewModel.message.collect { resId ->
                        Snackbar.make(binding.root, resId, Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.fabAdd.bounceOnClick { openDocument.launch(arrayOf("audio/*")) }
        binding.btnEmptyAdd.bounceOnClick { openDocument.launch(arrayOf("audio/*")) }
        binding.btnPlayAll.bounceOnClick { viewModel.playAll() }
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
