package com.example.musicplayer.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.MusicPlayerApp
import com.example.musicplayer.R
import com.example.musicplayer.data.db.TrackEntity
import com.example.musicplayer.player.PlaybackState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as MusicPlayerApp).repository
    private val player = (application as MusicPlayerApp).playback

    val tracks: StateFlow<List<TrackEntity>> = repository.allTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val favorites: StateFlow<List<TrackEntity>> = repository.likedTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val playbackState: StateFlow<PlaybackState> = player.state

    private val _message = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val message: SharedFlow<Int> = _message.asSharedFlow()

    fun track(id: Long): Flow<TrackEntity?> = repository.track(id)

    fun play(track: TrackEntity) = player.play(track)

    fun toggle(track: TrackEntity) = player.toggle(track)

    fun pause() = player.pause()

    fun resume() = player.resume()

    fun stop() = player.stop()

    fun seekTo(positionMs: Long) = player.seekTo(positionMs)

    /** Starts the first track of the current library — handy "Play all" shortcut. */
    fun playAll() {
        tracks.value.firstOrNull()?.let(::play)
    }

    fun next() {
        val list = tracks.value
        if (list.isEmpty()) return
        val index = list.indexOfFirst { it.id == playbackState.value.trackId }
        play(list[if (index == -1) 0 else (index + 1) % list.size])
    }

    fun previous() {
        val list = tracks.value
        if (list.isEmpty()) return
        val index = list.indexOfFirst { it.id == playbackState.value.trackId }
        play(list[if (index <= 0) list.lastIndex else index - 1])
    }

    fun setLike(id: Long, liked: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { repository.setLike(id, liked) }
    }

    fun import(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val imported = repository.importAudio(uri)
            _message.emit(
                if (imported != null) R.string.track_added else R.string.file_not_supported
            )
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
