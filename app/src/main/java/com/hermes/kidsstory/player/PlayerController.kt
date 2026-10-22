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
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.hermes.kidsstory.data.Story
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 播放器状态快照（由 Player.Listener 驱动，UI 无需高频轮询）。 */
data class PlaybackState(
    val isPlaying: Boolean = false,
    val position: Long = 0L,
    val duration: Long = 0L,
    val index: Int = 0,
    val title: String = "",
    val error: String? = null
)

/** 对 MediaController 的薄封装，给 Compose 界面用。 */
class PlayerController(val controller: MediaController) {

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            refresh()
        }

        override fun onPlayerError(error: PlaybackException) {
            _state.value = _state.value.copy(
                isPlaying = false,
                error = "播放失败：${error.message ?: "文件可能已删除"}"
            )
        }
    }

    init {
        controller.addListener(listener)
        refresh()
    }

    /** 刷新状态快照（播放进度需要 UI 定时调用）。 */
    fun refresh() {
        _state.value = PlaybackState(
            isPlaying = controller.isPlaying,
            position = controller.currentPosition,
            duration = controller.duration.coerceAtLeast(0L),
            index = controller.currentMediaItemIndex,
            title = controller.mediaMetadata.title?.toString() ?: "",
            error = _state.value.error
        )
    }

    /** 清除播放错误（UI 提示后调用）。 */
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

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
        refresh()
    }

    fun toggle() {
        if (controller.isPlaying) controller.pause() else controller.play()
        refresh()
    }

    fun next() {
        if (controller.hasNextMediaItem()) controller.seekToNextMediaItem()
        refresh()
    }

    fun prev() {
        if (controller.hasPreviousMediaItem()) controller.seekToPreviousMediaItem()
        refresh()
    }

    val isPlaying: Boolean get() = controller.isPlaying
    val currentIndex: Int get() = controller.currentMediaItemIndex
    val position: Long get() = controller.currentPosition
    val duration: Long get() = controller.duration.coerceAtLeast(0L)

    var volume: Float
        get() = controller.volume
        set(value) {
            controller.volume = value.coerceIn(0f, 1f)
        }

    fun seekTo(pos: Long) {
        controller.seekTo(pos)
        refresh()
    }

    fun pause() {
        controller.pause()
        refresh()
    }

    fun release() {
        runCatching { controller.removeListener(listener) }
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
            pc?.release()
            MediaController.releaseFuture(future)
        }
    }
    return pc
}
