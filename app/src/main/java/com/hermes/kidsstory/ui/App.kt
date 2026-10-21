package com.hermes.kidsstory.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.hermes.kidsstory.data.Category
import com.hermes.kidsstory.data.LibraryScanner
import com.hermes.kidsstory.data.Prefs
import com.hermes.kidsstory.data.Story
import com.hermes.kidsstory.player.PlayerController
import com.hermes.kidsstory.player.rememberPlayerController
import kotlinx.coroutines.delay

private val GRADS = listOf(
    listOf(Color(0xFFFFD54F), Color(0xFFFFA000)),
    listOf(Color(0xFF4FC3F7), Color(0xFF0288D1)),
    listOf(Color(0xFF81C784), Color(0xFF2E7D32)),
    listOf(Color(0xFFFF8A65), Color(0xFFE64A19)),
)

private fun catIcon(title: String): String = when {
    title.contains("睡") -> "月"
    title.contains("儿歌") || title.contains("音乐") -> "歌"
    title.contains("英语") -> "ABC"
    title.contains("国学") -> "文"
    else -> "书"
}

/** 主题化卡片：渐变主题 = 圆角填充；粗野主题 = 白底 + 粗黑边 + 硬阴影。 */
@Composable
private fun ThemedCard(
    theme: AppTheme,
    shape: RoundedCornerShape,
    brush: Brush? = null,
    solid: Color = theme.card,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val fillMod = if (brush != null) Modifier.background(brush) else Modifier.background(solid)
    val borderMod = if (theme.brutal) Modifier.border(3.dp, theme.text, shape) else Modifier
    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    if (theme.brutal) {
        Box(modifier) {
            Box(
                Modifier
                    .matchParentSize()
                    .offset(x = 5.dp, y = 5.dp)
                    .clip(shape)
                    .background(theme.text)
            )
            Box(
                Modifier
                    .matchParentSize()
                    .clip(shape)
                    .then(fillMod)
                    .then(borderMod)
                    .then(clickMod),
                content = content
            )
        }
    } else {
        Box(modifier.clip(shape).then(fillMod).then(clickMod)) {
            content()
        }
    }
}

sealed interface Screen {
    data object Home : Screen
    data object Settings : Screen
    data class Stories(val category: Category) : Screen
    data class Episodes(val category: Category, val story: Story) : Screen
    data class Player(val category: Category, val story: Story, val index: Int) : Screen
}

@Composable
fun KidsStoryApp() {
    val context = LocalContext.current
    var categories by remember { mutableStateOf(LibraryScanner.scan(context)) }
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var progress by remember { mutableStateOf(Prefs.loadProgress(context)) }
    var themeId by remember { mutableStateOf(Prefs.loadTheme(context)) }
    val theme = themeById(themeId)
    val pc = rememberPlayerController(context)

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            Prefs.saveMusicUri(context, uri.toString())
            categories = LibraryScanner.scan(context)
            screen = Screen.Home
        }
    }

    BackHandler(enabled = screen !is Screen.Home) {
        screen = when (val s = screen) {
            is Screen.Player -> Screen.Episodes(s.category, s.story)
            is Screen.Episodes -> Screen.Stories(s.category)
            is Screen.Stories -> Screen.Home
            is Screen.Settings -> Screen.Home
            else -> Screen.Home
        }
    }

    Surface(color = theme.bg, modifier = Modifier.fillMaxSize()) {
        when (val s = screen) {
            Screen.Home -> HomeScreen(
                theme = theme,
                categories = categories,
                progress = progress,
                onCategory = { screen = Screen.Stories(it) },
                onSettings = { screen = Screen.Settings },
                onContinue = { cat, story, idx ->
                    pc?.playStory(story, idx)
                    screen = Screen.Player(cat, story, idx)
                }
            )

            Screen.Settings -> SettingsScreen(
                theme = theme,
                musicUri = Prefs.loadMusicUri(context),
                onBack = { screen = Screen.Home },
                onPick = { picker.launch(null) },
                onReset = {
                    Prefs.saveMusicUri(context, null)
                    categories = LibraryScanner.scan(context)
                    screen = Screen.Home
                },
                onTheme = { id ->
                    Prefs.saveTheme(context, id)
                    themeId = id
                }
            )

            is Screen.Stories -> StoryListScreen(
                theme = theme,
                category = s.category,
                onBack = { screen = Screen.Home },
                onStory = { screen = Screen.Episodes(s.category, it) }
            )

            is Screen.Episodes -> EpisodeListScreen(
                theme = theme,
                story = s.story,
                onBack = { screen = Screen.Stories(s.category) },
                onEpisode = { idx ->
                    pc?.playStory(s.story, idx)
                    screen = Screen.Player(s.category, s.story, idx)
                }
            )

            is Screen.Player -> PlayerScreen(
                theme = theme,
                story = s.story,
                index = s.index,
                pc = pc,
                onBack = {
                    progress = Prefs.loadProgress(context)
                    screen = Screen.Episodes(s.category, s.story)
                },
                onIndex = { screen = Screen.Player(s.category, s.story, it) }
            )
        }
    }
}

// ---------- 顶部标题栏 ----------
@Composable
private fun TopBar(
    theme: AppTheme,
    title: String,
    onBack: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.size(52.dp)) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = theme.text)
            }
        } else {
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            color = theme.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (action != null) action()
    }
}

// ---------- 1. 首页 ----------
@Composable
private fun HomeScreen(
    theme: AppTheme,
    categories: List<Category>,
    progress: Prefs.Progress?,
    onCategory: (Category) -> Unit,
    onSettings: () -> Unit,
    onContinue: (Category, Story, Int) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        TopBar(theme, "故事机", action = {
            IconButton(onClick = onSettings, modifier = Modifier.size(52.dp)) {
                Text("⚙", fontSize = 26.sp, color = theme.text)
            }
        })

        val found = progress?.let { p ->
            categories.firstNotNullOfOrNull { cat ->
                cat.stories.firstOrNull { it.title == p.story }?.let { cat to it }
            }
        }

        if (categories.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "还没有故事\n\n点右上角 ⚙ 选择 MP3 文件夹",
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    color = theme.text
                )
            }
            return@Column
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (found != null && progress != null) {
                item(span = { GridItemSpan(2) }) {
                    val (cat, story) = found
                    val idx = progress.index.coerceIn(0, story.episodes.size - 1)
                    ThemedCard(
                        theme = theme,
                        shape = RoundedCornerShape(20.dp),
                        solid = if (theme.brutal) theme.text else Color(0xFF263238),
                        modifier = Modifier.fillMaxWidth().height(70.dp),
                        onClick = { onContinue(cat, story, idx) }
                    ) {
                        Row(
                            Modifier.fillMaxSize().padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "▶",
                                fontSize = 22.sp,
                                color = if (theme.brutal) theme.palette[0] else Color(0xFFFFD54F)
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text("继续听", fontSize = 13.sp, color = Color(0xFFB0BEC5))
                                Text(
                                    "${story.title} · 第 ${idx + 1} 集",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            items(categories.size) { i ->
                val colors = if (theme.brutal) {
                    val c = theme.palette[i % theme.palette.size]
                    listOf(c, c)
                } else {
                    GRADS[i % GRADS.size]
                }
                ThemedCard(
                    theme = theme,
                    shape = RoundedCornerShape(if (theme.brutal) 14.dp else 26.dp),
                    brush = if (theme.brutal) null else Brush.linearGradient(colors),
                    solid = colors[0],
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    onClick = { onCategory(categories[i]) }
                ) {
                    Column(
                        Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val icon = catIcon(categories[i].title)
                        Text(
                            icon,
                            fontSize = if (icon.length > 1) 34.sp else 44.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (theme.brutal && i % 4 == 0) theme.text else Color.White
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            categories[i].title,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (theme.brutal && i % 4 == 0) theme.text else Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${categories[i].stories.size} 个故事",
                            fontSize = 13.sp,
                            color = if (theme.brutal && i % 4 == 0) theme.text.copy(alpha = 0.7f)
                            else Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}

// ---------- 2. 故事列表 ----------
@Composable
private fun StoryListScreen(
    theme: AppTheme,
    category: Category,
    onBack: () -> Unit,
    onStory: (Story) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        TopBar(theme, category.title, onBack)
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(category.stories) { i, story ->
                ThemedCard(
                    theme = theme,
                    shape = RoundedCornerShape(if (theme.brutal) 12.dp else 22.dp),
                    solid = theme.card,
                    modifier = Modifier.fillMaxWidth().height(92.dp),
                    onClick = { onStory(story) }
                ) {
                    Row(
                        Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(if (theme.brutal) 8.dp else 18.dp))
                                .background(theme.palette[i % theme.palette.size])
                                .then(
                                    if (theme.brutal) Modifier.border(
                                        3.dp, theme.text, RoundedCornerShape(8.dp)
                                    ) else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "书",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (theme.brutal) Color.White else theme.text
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                story.title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text("${story.episodes.size} 集", fontSize = 13.sp, color = theme.muted)
                        }
                        Text("›", fontSize = 24.sp, color = theme.muted)
                    }
                }
            }
        }
    }
}

// ---------- 3. 集数列表 ----------
@Composable
private fun EpisodeListScreen(
    theme: AppTheme,
    story: Story,
    onBack: () -> Unit,
    onEpisode: (Int) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        TopBar(theme, story.title, onBack)
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(story.episodes) { idx, ep ->
                ThemedCard(
                    theme = theme,
                    shape = RoundedCornerShape(if (theme.brutal) 10.dp else 18.dp),
                    solid = theme.card,
                    modifier = Modifier.fillMaxWidth().height(72.dp),
                    onClick = { onEpisode(idx) }
                ) {
                    Row(
                        Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(theme.palette[1 % theme.palette.size])
                                .then(
                                    if (theme.brutal) Modifier.border(3.dp, theme.text, CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${idx + 1}",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(
                            ep.title,
                            fontSize = 19.sp,
                            color = theme.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ---------- 4. 播放页 ----------
@Composable
private fun PlayerScreen(
    theme: AppTheme,
    story: Story,
    index: Int,
    pc: PlayerController?,
    onBack: () -> Unit,
    onIndex: (Int) -> Unit
) {
    val context = LocalContext.current
    var playing by remember { mutableStateOf(false) }
    var curIndex by remember { mutableIntStateOf(index) }
    var sleepMinutes by remember { mutableIntStateOf(0) }
    var sleepLeft by remember { mutableIntStateOf(0) }
    var showSleep by remember { mutableStateOf(false) }
    var restored by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    val fg = theme.onPlayer

    LaunchedEffect(pc) {
        while (true) {
            pc?.let {
                playing = it.isPlaying
                position = it.position
                duration = it.duration
                if (it.currentIndex in story.episodes.indices) curIndex = it.currentIndex
            }
            delay(400)
        }
    }

    LaunchedEffect(pc) {
        if (restored) return@LaunchedEffect
        restored = true
        val p = Prefs.loadProgress(context) ?: return@LaunchedEffect
        if (p.story == story.title && p.index == index && p.position > 3000L) {
            delay(1200)
            pc?.seekTo(p.position)
        }
    }

    LaunchedEffect(pc, curIndex) {
        while (true) {
            pc?.let { Prefs.saveProgress(context, story.title, it.currentIndex, it.position) }
            delay(5000)
        }
    }

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
        pc?.controller?.pause()
        pc?.volume = v0
        sleepMinutes = 0
    }

    DisposableEffect(Unit) {
        onDispose {
            pc?.let { Prefs.saveProgress(context, story.title, it.currentIndex, it.position) }
        }
    }

    val ep = story.episodes.getOrNull(curIndex) ?: story.episodes.first()
    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (playing) 1.12f else 1f,
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
                    Text("🌙", fontSize = 24.sp)
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
                "🌙 ${sleepLeft / 60}:${(sleepLeft % 60).toString().padStart(2, '0')}",
                fontSize = 13.sp,
                color = fg
            )
        }

        Spacer(Modifier.height(18.dp))
        Text("第 ${curIndex + 1} 集", fontSize = 15.sp, color = fg.copy(alpha = 0.9f))
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
            if (playing) "♪" else "♫",
            fontSize = 52.sp,
            color = fg,
            modifier = Modifier.scale(pulseScale)
        )

        Spacer(Modifier.height(12.dp))

        Slider(
            value = if (duration > 0) position.toFloat() / duration else 0f,
            onValueChange = { frac ->
                if (duration > 0) {
                    val p = (frac * duration).toLong()
                    position = p
                    pc?.seekTo(p)
                }
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
            Text(fmtTime(position), fontSize = 12.sp, color = fg)
            Text(fmtTime(duration), fontSize = 12.sp, color = fg)
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
                onClick = {
                    pc?.prev()
                    if (curIndex > 0) onIndex(curIndex - 1)
                }
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
                        if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
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
                onClick = {
                    pc?.next()
                    if (curIndex < story.episodes.size - 1) onIndex(curIndex + 1)
                }
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

private fun fmtTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

// ---------- 5. 设置 ----------
@Composable
private fun SettingsScreen(
    theme: AppTheme,
    musicUri: String?,
    onBack: () -> Unit,
    onPick: () -> Unit,
    onReset: () -> Unit,
    onTheme: (String) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        TopBar(theme, "设置", onBack)
        Column(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 主题
            ThemedCard(
                theme = theme,
                shape = RoundedCornerShape(20.dp),
                solid = theme.card,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("主题", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = theme.text)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ThemeChip(theme, "渐变风", "gradient", onTheme)
                        ThemeChip(theme, "新粗野", "brutal", onTheme)
                    }
                    Text(
                        "默认渐变风；新粗野 = 粗黑边框 + 硬阴影",
                        fontSize = 12.sp,
                        color = theme.muted
                    )
                }
            }

            // MP3 目录
            ThemedCard(
                theme = theme,
                shape = RoundedCornerShape(20.dp),
                solid = theme.card,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("MP3 存放目录", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = theme.text)
                    Text(
                        if (musicUri == null) "当前：内置示例" else "当前：自定义文件夹 ✓",
                        fontSize = 15.sp,
                        color = theme.muted
                    )
                    if (musicUri != null) {
                        Text(
                            musicUri.take(70),
                            fontSize = 10.sp,
                            color = theme.muted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Button(onClick = onPick, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Text("选择文件夹", fontSize = 17.sp)
                    }
                    if (musicUri != null) {
                        TextButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
                            Text("恢复内置示例", fontSize = 15.sp)
                        }
                    }
                }
            }

            // 说明
            ThemedCard(
                theme = theme,
                shape = RoundedCornerShape(20.dp),
                solid = theme.card,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("目录怎么放？", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = theme.text)
                    Text(
                        "分类/故事/01 第1集.mp3\n分类/单集故事.mp3\n\n名字会去掉开头的 \"01 \" 序号",
                        fontSize = 14.sp,
                        color = theme.muted,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeChip(theme: AppTheme, label: String, id: String, onTheme: (String) -> Unit) {
    val selected = theme.id == id
    ThemedCard(
        theme = theme,
        shape = RoundedCornerShape(12.dp),
        solid = if (selected) theme.palette[0] else theme.card,
        modifier = Modifier.height(52.dp).width(120.dp),
        onClick = { onTheme(id) }
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(label, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = theme.text)
        }
    }
}
