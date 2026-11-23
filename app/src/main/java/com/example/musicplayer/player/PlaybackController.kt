package com.example.musicplayer.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.example.musicplayer.data.db.TrackEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Binds the UI to [PlaybackService] lazily and mirrors the service state into a
 * stable StateFlow, so fragments can keep collecting across configuration changes.
 * Commands issued before the service is connected are queued and replayed.
 */
class PlaybackController(private val context: Context) {

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var service: PlaybackService? = null
    private var collectJob: Job? = null
    private var pendingAction: ((PlaybackService) -> Unit)? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder) {
            val boundService = (binder as PlaybackService.LocalBinder).service()
            service = boundService
            collectJob?.cancel()
            collectJob = scope.launch {
                boundService.state.collect { _state.value = it }
            }
            pendingAction?.invoke(boundService)
            pendingAction = null
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            collectJob?.cancel()
            service = null
        }
    }

    fun play(track: TrackEntity) = execute { it.play(track) }

    fun toggle(track: TrackEntity) = execute { it.toggle(track) }

    fun pause() = execute { it.pause() }

    fun resume() = execute { it.resume() }

    fun stop() = execute { it.stop() }

    fun seekTo(positionMs: Long) = execute { it.seekTo(positionMs) }

    private fun execute(action: (PlaybackService) -> Unit) {
        val activeService = service
        if (activeService != null) {
            action(activeService)
        } else {
            pendingAction = action
            ensureStarted()
        }
    }

    private fun ensureStarted() {
        if (service != null) return
        val intent = Intent(context, PlaybackService::class.java)
        context.startForegroundService(intent)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }
}
