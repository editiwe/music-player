package com.example.musicplayer.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row in the local tracks table.
 *
 * filePath points to an audio file in the app's private storage.
 * coverPath can be a drawable resource name or a file path (null means the default art).
 */
@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val artist: String,
    val filePath: String,
    val coverPath: String? = null,
    val isLiked: Boolean = false,
    val durationMs: Long = 0L,
    val dateAdded: Long = System.currentTimeMillis()
)
