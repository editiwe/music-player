package com.example.musicplayer.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.musicplayer.data.db.TrackDao
import com.example.musicplayer.data.db.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Single source of truth for tracks. The UI never reads files directly:
 * it observes Room flows, while this class takes care of importing files and likes.
 */
class TrackRepository(
    private val context: Context,
    private val dao: TrackDao
) {

    fun allTracks(): Flow<List<TrackEntity>> = dao.observeAll()

    fun likedTracks(): Flow<List<TrackEntity>> = dao.observeLiked()

    fun track(id: Long): Flow<TrackEntity?> = dao.observeById(id)

    suspend fun setLike(id: Long, liked: Boolean) = withContext(Dispatchers.IO) {
        dao.setLiked(id, liked)
    }

    /**
     * Copies a document picked through SAF into app-private storage.
     * We keep our own copy so the path stored in Room stays valid after the pick.
     */
    suspend fun importAudio(uri: Uri): TrackEntity? = withContext(Dispatchers.IO) {
        val displayName = queryDisplayName(uri) ?: "track_${System.currentTimeMillis()}"
        val extension = displayName.substringAfterLast('.', "").lowercase()
            .takeIf { it == "mp3" || it == "wav" }
            ?: "mp3"

        val audioDir = File(context.filesDir, AUDIO_DIR).apply { mkdirs() }
        val target = File(audioDir, "import_${System.currentTimeMillis()}.$extension")

        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return@withContext null

        val title = displayName.substringBeforeLast('.').replace('_', ' ').trim()
            .ifEmpty { "Новый трек" }

        val track = TrackEntity(
            title = title,
            artist = "Импортированный трек",
            filePath = target.absolutePath,
            coverPath = null
        )
        val id = dao.insert(track)
        dao.getById(id)
    }

    private fun queryDisplayName(uri: Uri): String? {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        return context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) cursor.getString(index) else null
            } else {
                null
            }
        }
    }

    private companion object {
        const val AUDIO_DIR = "audio"
    }
}
