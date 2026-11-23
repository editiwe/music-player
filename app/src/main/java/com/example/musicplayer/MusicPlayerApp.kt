package com.example.musicplayer

import android.app.Application
import com.example.musicplayer.data.db.AppDatabase
import com.example.musicplayer.data.repository.TrackRepository
import com.example.musicplayer.player.PlaybackController

class MusicPlayerApp : Application() {

    lateinit var repository: TrackRepository
        private set

    lateinit var playback: PlaybackController
        private set

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.build(this)
        repository = TrackRepository(this, database.trackDao())
        playback = PlaybackController(this)
    }
}
