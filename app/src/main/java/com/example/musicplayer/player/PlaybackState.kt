package com.example.musicplayer.player

/** Immutable snapshot of what the playback service is doing right now. */
data class PlaybackState(
    val trackId: Long? = null,
    val title: String = "",
    val artist: String = "",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val error: String? = null
)
