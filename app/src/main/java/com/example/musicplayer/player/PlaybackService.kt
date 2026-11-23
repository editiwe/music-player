package com.example.musicplayer.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.musicplayer.R
import com.example.musicplayer.data.db.TrackEntity
import com.example.musicplayer.ui.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

/**
 * Foreground service that owns the MediaPlayer. Because the player lives in a service,
 * screen rotation and navigation between fragments do not interrupt playback.
 */
class PlaybackService : Service() {

    inner class LocalBinder : Binder() {
        fun service(): PlaybackService = this@PlaybackService
    }

    private val binder = LocalBinder()
    private val handler = Handler(Looper.getMainLooper())
    private val notificationManager by lazy { NotificationManagerCompat.from(this) }

    private var player: MediaPlayer? = null
    private var currentTrack: TrackEntity? = null

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val progressRunnable = object : Runnable {
        override fun run() {
            val activePlayer = player
            if (activePlayer != null && activePlayer.isPlaying) {
                _state.update {
                    it.copy(
                        positionMs = activePlayer.currentPosition.toLong(),
                        durationMs = activePlayer.duration.toLong()
                    )
                }
                handler.postDelayed(this, PROGRESS_INTERVAL_MS)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(null))
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification(currentTrack))
        return START_STICKY
    }

    fun play(track: TrackEntity) {
        if (currentTrack?.id == track.id && player?.isPlaying == true) return

        releasePlayer()
        currentTrack = track

        if (!File(track.filePath).exists()) {
            _state.value = PlaybackState(error = "Файл трека не найден")
            return
        }

        try {
            player = MediaPlayer().apply {
                setDataSource(track.filePath)
                setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.start()
                    _state.value = PlaybackState(
                        trackId = track.id,
                        title = track.title,
                        artist = track.artist,
                        isPlaying = true,
                        durationMs = mediaPlayer.duration.toLong()
                    )
                    handler.post(progressRunnable)
                    refreshNotification()
                }
                setOnCompletionListener {
                    handler.removeCallbacks(progressRunnable)
                    _state.update { state -> state.copy(isPlaying = false, positionMs = 0L) }
                    refreshNotification()
                }
                setOnErrorListener { _, what, _ ->
                    _state.update { state -> state.copy(error = "Ошибка воспроизведения ($what)") }
                    true
                }
                prepareAsync()
            }
        } catch (exception: Exception) {
            _state.value = PlaybackState(error = exception.message ?: "Не удалось открыть трек")
        }
    }

    fun toggle(track: TrackEntity) {
        when {
            currentTrack?.id != track.id -> play(track)
            player?.isPlaying == true -> pause()
            else -> resume()
        }
    }

    fun pause() {
        player?.takeIf { it.isPlaying }?.pause()
        handler.removeCallbacks(progressRunnable)
        _state.update { it.copy(isPlaying = false) }
        refreshNotification()
    }

    fun resume() {
        val activePlayer = player ?: return
        activePlayer.start()
        _state.update { it.copy(isPlaying = true) }
        handler.post(progressRunnable)
        refreshNotification()
    }

    fun stop() {
        releasePlayer()
        currentTrack = null
        _state.value = PlaybackState()
        refreshNotification()
    }

    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs.toInt())
        _state.update { it.copy(positionMs = positionMs) }
    }

    private fun releasePlayer() {
        handler.removeCallbacks(progressRunnable)
        player?.release()
        player = null
    }

    private fun refreshNotification() {
        notificationManager.notify(NOTIFICATION_ID, buildNotification(currentTrack))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Воспроизведение музыки",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Управление локальным музыкальным плеером" }

        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(track: TrackEntity?): android.app.Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = track?.title ?: getString(R.string.app_name)
        val text = if (track == null) {
            getString(R.string.notification_idle)
        } else {
            track.artist.ifBlank { getString(R.string.unknown_artist) }
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_music)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(_state.value.isPlaying)
            .build()
    }

    override fun onDestroy() {
        releasePlayer()
        super.onDestroy()
    }

    private companion object {
        const val CHANNEL_ID = "music_playback"
        const val NOTIFICATION_ID = 1001
        const val PROGRESS_INTERVAL_MS = 500L
    }
}
