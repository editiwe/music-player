package com.example.musicplayer.ui

import android.view.View
import android.widget.ImageView
import com.example.musicplayer.R

private const val PRESS_DURATION_MS = 90L
private const val RELEASE_DURATION_MS = 130L
private const val PRESSED_SCALE = 0.9f
private const val FADE_DURATION_MS = 200L

/** Small tactile response for all playback buttons. */
fun View.bounceOnClick(action: () -> Unit) {
    setOnClickListener {
        animate().cancel()
        scaleX = 1f
        scaleY = 1f
        animate()
            .scaleX(PRESSED_SCALE)
            .scaleY(PRESSED_SCALE)
            .setDuration(PRESS_DURATION_MS)
            .withEndAction {
                animate().scaleX(1f).scaleY(1f).setDuration(RELEASE_DURATION_MS).start()
                action()
            }
            .start()
    }
}

/** Soft crossfade when a cover or a label is replaced. */
fun View.fadeIn(durationMs: Long = FADE_DURATION_MS) {
    animate().cancel()
    alpha = 0.3f
    animate().alpha(1f).setDuration(durationMs).start()
}

/** Resolves a cover stored in Room: either a drawable resource name or a file path. */
fun ImageView.loadCover(coverPath: String?, animate: Boolean = false) {
    val resource = if (!coverPath.isNullOrBlank() && coverPath.startsWith("cover_")) {
        resources.getIdentifier(coverPath, "drawable", context.packageName)
            .takeIf { it != 0 } ?: R.drawable.ic_album_art
    } else {
        R.drawable.ic_album_art
    }
    setImageResource(resource)
    if (animate) fadeIn() else alpha = 1f
}

fun formatDuration(millis: Long): String {
    if (millis <= 0) return "0:00"
    val totalSeconds = millis / 1_000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
