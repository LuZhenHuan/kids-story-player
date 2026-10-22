package com.hermes.kidsstory.ui

import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hermes.kidsstory.data.Prefs
import com.hermes.kidsstory.data.Story
import com.hermes.kidsstory.player.PlaybackState
import com.hermes.kidsstory.player.PlayerController
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    theme: AppTheme,
    story: Story,
    initialIndex: Int,
    pc: PlayerController?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val stateFlow = pc?.state
    val state by (stateFlow?.collectAsState() ?: remember { mutableStateOf(PlaybackState()) })

    // 播放错误提示（文件被删 / URI 失效等）
    LaunchedEffect(state.error) {
        val err = state.error ?: return@LaunchedEffect
        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
        pc?.clearError()
    }

    var sleepMinutes by remember { mutableIntStateOf(0) }
    var sleepLeft by remember { mutableIntStateOf(0) }
    var showSleep by remember { mutableStateOf(false) }
    var restored by remember { mutableStateOf(false) }
    var dragPos by remember { mutableLongStateOf(-1L) }
    val fg = theme.onPlayer

    // 进度刷新（位置不在 Player 事件里，1 秒一次足够）
    LaunchedEffect(pc) {
        while (true) {
            pc?.refresh()
            delay(1000)
        }
    }

    // 播放记忆：恢复位置（只在控制器连上后执行一次）
    LaunchedEffect(pc) {
        if (pc == null || restored) return@LaunchedEffect
        val p = Prefs.loadProgress(context)
        if (p != null && p.story == story.title && p.index == initialIndex && p.position > 3000L) {
            delay(1200)
            pc.seekTo(p.position)
        }
        restored = true
    }

    // 播放记忆：每 5 秒保存
    LaunchedEffect(pc, state.index) {
        while (true) {
            pc?.let { Prefs.saveProgress(context, story.title, it.currentIndex, it.position) }
            delay(5000)
        }
    }

    // 睡眠定时：倒计时 → 渐弱 → 暂停
    LaunchedEffect(sleepMinutes) {
        if (sleepMinutes <= 0) {
            sleepLeft = 0
            return@LaunchedEffect
        }
        var left = sleepMinutes * 60
        sleepLeft = left
        while (left > 0) {
            delay(1000)
            left--
            sleepLeft = left
        }
        val v0 = pc?.volume ?: 1f
        for (i in 1..10) {
            pc?.volume = v0 * (1f - i / 10f)
            delay(500)
        }
        pc?.pause()
        pc?.volume = v0
        sleepMinutes = 0
    }

    DisposableEffect(Unit) {
        onDispose {
            pc?.let { Prefs.saveProgress(context, story.title, it.currentIndex, it.position) }
        }
    }

    val ep = story.episodes.getOrNull(state.index) ?: story.episodes.first()
    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (state.isPlaying) 1.12f else 1f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(theme.playerBg))
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .pointerInput(Unit) { detectTapGestures(onTap = { onBack() }) },
                contentAlignment = Alignment.Center
            ) {
                Text("←", fontSize = 26.sp, color = fg)
            }
            Text(
                story.title,
                modifier = Modifier.weight(1f),
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box {
                IconButton(onClick = { showSleep = true }) {
                    Text("月", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = fg)
                }
                DropdownMenu(expanded = showSleep, onDismissRequest = { showSleep = false }) {
                    listOf(0 to "关闭", 15 to "15 分钟", 30 to "30 分钟", 60 to "60 分钟")
                        .forEach { (m, label) ->
                            DropdownMenuItem(
                                text = { Text(label, fontSize = 17.sp) },
                                onClick = {
                                    sleepMinutes = m
                                    showSleep = false
                                }
                            )
                        }
                }
            }
        }

        if (sleepMinutes > 0) {
            Text(
                "月 ${sleepLeft / 60}:${(sleepLeft % 60).toString().padStart(2, '0')}",
                fontSize = 13.sp,
                color = fg
            )
        }

        Spacer(Modifier.height(18.dp))
        Text("第 ${state.index + 1} 集", fontSize = 15.sp, color = fg.copy(alpha = 0.9f))
        Spacer(Modifier.height(6.dp))
        Text(
            ep.title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = fg,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(10.dp))
        Text(
            if (state.isPlaying) "♪" else "♫",
            fontSize = 52.sp,
            color = fg,
            modifier = Modifier.scale(pulseScale)
        )

        Spacer(Modifier.height(12.dp))

        val shownPos = if (dragPos >= 0) dragPos else state.position
        val dur = state.duration
        Slider(
            value = if (dur > 0) shownPos.toFloat() / dur else 0f,
            onValueChange = { frac ->
                if (dur > 0) dragPos = (frac * dur).toLong()
            },
            onValueChangeFinished = {
                if (dragPos >= 0) pc?.seekTo(dragPos)
                dragPos = -1L
            },
            colors = SliderDefaults.colors(
                thumbColor = if (theme.brutal) theme.text else Color.White,
                activeTrackColor = if (theme.brutal) theme.text else Color.White,
                inactiveTrackColor = if (theme.brutal) Color.White else Color.White.copy(alpha = 0.35f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(fmtTime(shownPos), fontSize = 12.sp, color = fg)
            Text(fmtTime(dur), fontSize = 12.sp, color = fg)
        }

        Spacer(Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            ThemedCard(
                theme = theme,
                shape = RoundedCornerShape(50),
                solid = Color.White,
                modifier = Modifier.size(62.dp),
                onClick = { pc?.prev() }
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.SkipPrevious, "上一集",
                        tint = theme.playerAccent, modifier = Modifier.size(34.dp)
                    )
                }
            }
            Spacer(Modifier.width(20.dp))
            ThemedCard(
                theme = theme,
                shape = RoundedCornerShape(50),
                solid = Color.White,
                modifier = Modifier.size(96.dp),
                onClick = { pc?.toggle() }
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "播放/暂停",
                        tint = theme.playerAccent,
                        modifier = Modifier.size(50.dp)
                    )
                }
            }
            Spacer(Modifier.width(20.dp))
            ThemedCard(
                theme = theme,
                shape = RoundedCornerShape(50),
                solid = Color.White,
                modifier = Modifier.size(62.dp),
                onClick = { pc?.next() }
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.SkipNext, "下一集",
                        tint = theme.playerAccent, modifier = Modifier.size(34.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("共 ${story.episodes.size} 集 · 自动连播", fontSize = 12.sp, color = fg.copy(alpha = 0.85f))
    }
}
