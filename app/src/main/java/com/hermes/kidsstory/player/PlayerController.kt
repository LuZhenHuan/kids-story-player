package com.hermes.kidsstory.player

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.hermes.kidsstory.data.Story

/** 对 MediaController 的薄封装，给 Compose 界面用。 */
class PlayerController(val controller: MediaController) {

    fun playStory(story: Story, index: Int) {
        val items = story.episodes.map { ep ->
            MediaItem.Builder()
                .setUri(ep.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(ep.title)
                        .setArtist(story.title)
                        .build()
                )
                .build()
        }
        controller.setMediaItems(items, index, 0L)
        controller.prepare()
        controller.play()
    }

    fun toggle() {
        if (controller.isPlaying) controller.pause() else controller.play()
    }

    fun next() {
        if (controller.hasNextMediaItem()) controller.seekToNextMediaItem()
    }

    fun prev() {
        if (controller.hasPreviousMediaItem()) controller.seekToPreviousMediaItem()
    }

    val isPlaying: Boolean get() = controller.isPlaying
    val currentIndex: Int get() = controller.currentMediaItemIndex
    val title: String get() = controller.mediaMetadata.title?.toString() ?: ""
    val position: Long get() = controller.currentPosition
    val duration: Long get() = controller.duration.coerceAtLeast(0L)

    var volume: Float
        get() = controller.volume
        set(value) {
            controller.volume = value.coerceIn(0f, 1f)
        }

    fun seekTo(pos: Long) {
        controller.seekTo(pos)
    }
}

/** Compose 里拿到与 PlaybackService 的连接。 */
@Composable
fun rememberPlayerController(context: Context): PlayerController? {
    var pc by remember { mutableStateOf<PlayerController?>(null) }

    DisposableEffect(context) {
        val token = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            try {
                pc = PlayerController(future.get())
            } catch (_: Exception) {
                pc = null
            }
        }, MoreExecutors.directExecutor())

        onDispose {
            MediaController.releaseFuture(future)
        }
    }
    return pc
}
