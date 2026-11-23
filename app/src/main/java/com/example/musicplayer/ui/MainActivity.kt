package com.example.musicplayer.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.example.musicplayer.R
import com.example.musicplayer.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var shownError: String? = null
    private var hadTrack = false
    private var shownTrackId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        NavigationUI.setupWithNavController(binding.bottomNav, navController)

        requestNotificationPermission()
        setupMiniPlayer()
        observePlayback()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) return

        requestPermissions(
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            NOTIFICATION_PERMISSION_REQUEST
        )
    }

    private fun setupMiniPlayer() {
        binding.btnMiniPlay.bounceOnClick {
            val state = viewModel.playbackState.value
            if (state.isPlaying) viewModel.pause() else viewModel.resume()
        }
        binding.btnMiniPrevious.bounceOnClick { viewModel.previous() }
        binding.btnMiniNext.bounceOnClick { viewModel.next() }
        binding.miniPlayer.setOnClickListener {
            val trackId = viewModel.playbackState.value.trackId ?: return@setOnClickListener
            navController.navigate(R.id.action_global_track, bundleOf(ARG_TRACK_ID to trackId))
        }
    }

    private fun observePlayback() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.playbackState.collect { state ->
                    val hasTrack = state.trackId != null
                    if (hasTrack && !hadTrack) {
                        binding.miniPlayer.alpha = 0f
                        binding.miniPlayer.translationY = MINI_PLAYER_OFFSET_PX
                        binding.miniPlayer.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(MINI_APPEAR_MS)
                            .start()
                    }
                    hadTrack = hasTrack
                    if (hasTrack && state.trackId != shownTrackId) {
                        binding.ivMiniCover.fadeIn()
                    }
                    shownTrackId = state.trackId

                    binding.tvMiniTitle.text = state.title.ifBlank { getString(R.string.no_track) }
                    binding.tvMiniArtist.text =
                        state.artist.ifBlank { getString(R.string.unknown_artist) }
                    binding.btnMiniPlay.setImageResource(
                        if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                    )
                    binding.btnMiniPlay.contentDescription = getString(
                        if (state.isPlaying) R.string.pause else R.string.play
                    )
                    listOf(
                        binding.btnMiniPlay,
                        binding.btnMiniPrevious,
                        binding.btnMiniNext
                    ).forEach { button ->
                        button.isEnabled = hasTrack
                        button.alpha = if (hasTrack) 1f else DISABLED_ALPHA
                    }
                    binding.miniPlayer.isClickable = hasTrack

                    val error = state.error
                    if (!error.isNullOrBlank() && error != shownError) {
                        Snackbar.make(binding.root, error, Snackbar.LENGTH_SHORT).show()
                    }
                    shownError = error
                }
            }
        }
    }

    private companion object {
        const val NOTIFICATION_PERMISSION_REQUEST = 42
        const val ARG_TRACK_ID = "trackId"
        const val DISABLED_ALPHA = 0.4f
        const val MINI_APPEAR_MS = 240L
        const val MINI_PLAYER_OFFSET_PX = 16f
    }
}
