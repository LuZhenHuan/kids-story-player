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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
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

// ---------- 卡通配色 ----------
private val WarmYellow = Color(0xFFFFF3C4)
private val CardYellow = Color(0xFFFFC93C)
private val SkyBlue = Color(0xFF90CAF9)
private val CardBlue = Color(0xFF42A5F5)
private val CardGreen = Color(0xFF81C784)
private val CardPink = Color(0xFFF48FB1)
private val Coffee = Color(0xFF3E2723)
private val CoffeeLight = Color(0x993E2723)
private val Brown = Color(0xFF5D4037)

private val CARD_COLORS = listOf(CardYellow, SkyBlue, CardGreen, CardPink)

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
    val pc = rememberPlayerController(context)

    // 选择 MP3 文件夹（系统目录选择器，无需存储权限）
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

    // 返回键：返回上一层（首页时才退出 App）
    BackHandler(enabled = screen !is Screen.Home) {
        screen = when (val s = screen) {
            is Screen.Player -> Screen.Episodes(s.category, s.story)
            is Screen.Episodes -> Screen.Stories(s.category)
            is Screen.Stories -> Screen.Home
            is Screen.Settings -> Screen.Home
            else -> Screen.Home
        }
    }

    Surface(color = WarmYellow, modifier = Modifier.fillMaxSize()) {
        when (val s = screen) {
            Screen.Home -> HomeScreen(
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
                musicUri = Prefs.loadMusicUri(context),
                onBack = { screen = Screen.Home },
                onPick = { picker.launch(null) },
                onReset = {
                    Prefs.saveMusicUri(context, null)
                    categories = LibraryScanner.scan(context)
                    screen = Screen.Home
                }
            )

            is Screen.Stories -> StoryListScreen(
                category = s.category,
                onBack = { screen = Screen.Home },
                onStory = { screen = Screen.Episodes(s.category, it) }
            )

            is Screen.Episodes -> EpisodeListScreen(
                story = s.story,
                onBack = { screen = Screen.Stories(s.category) },
                onEpisode = { idx ->
                    pc?.playStory(s.story, idx)
                    screen = Screen.Player(s.category, s.story, idx)
                }
            )

            is Screen.Player -> PlayerScreen(
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
private fun TopBar(title: String, onBack: (() -> Unit)? = null, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = Brown)
            }
        } else {
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Brown,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (action != null) action()
    }
}

// ---------- 1. 首页：分类 ----------
@Composable
private fun HomeScreen(
    categories: List<Category>,
    progress: Prefs.Progress?,
    onCategory: (Category) -> Unit,
    onSettings: () -> Unit,
    onContinue: (Category, Story, Int) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        TopBar("📚 故事机", action = {
            IconButton(onClick = onSettings, modifier = Modifier.size(56.dp)) {
                Text("⚙️", fontSize = 26.sp)
            }
        })

        // 继续听（播放记忆）
        if (progress != null) {
            val found = categories.firstNotNullOfOrNull { cat ->
                cat.stories.firstOrNull { it.title == progress.story }?.let { cat to it }
            }
            if (found != null) {
                val (cat, story) = found
                val idx = progress.index.coerceIn(0, story.episodes.size - 1)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .height(78.dp)
                        .clickable { onContinue(cat, story, idx) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF263238))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("▶️", fontSize = 26.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("继续听", fontSize = 14.sp, color = Color(0xFFB0BEC5))
                            Text(
                                "${story.title} · 第 ${idx + 1} 集",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }

        if (categories.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "还没有故事\n\n点右上角 ⚙️ 选择 MP3 文件夹",
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    color = Coffee
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(categories) { cat ->
                    CategoryCard(cat, CARD_COLORS[categories.indexOf(cat) % CARD_COLORS.size]) {
                        onCategory(cat)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(cat: Category, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(cat.emoji, fontSize = 52.sp)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(cat.title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Coffee)
                Text("${cat.stories.size} 个故事", fontSize = 15.sp, color = CoffeeLight)
            }
        }
    }
}

// ---------- 2. 故事列表 ----------
@Composable
private fun StoryListScreen(category: Category, onBack: () -> Unit, onStory: (Story) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopBar("${category.emoji} ${category.title}", onBack)
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(category.stories) { story ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .clickable { onStory(story) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📖", fontSize = 34.sp)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                story.title,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                color = Coffee,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text("${story.episodes.size} 集", fontSize = 14.sp, color = CoffeeLight)
                        }
                    }
                }
            }
        }
    }
}

// ---------- 3. 集数列表 ----------
@Composable
private fun EpisodeListScreen(story: Story, onBack: () -> Unit, onEpisode: (Int) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopBar("📖 ${story.title}", onBack)
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(story.episodes) { idx, ep ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clickable { onEpisode(idx) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${idx + 1}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CardBlue)
                        Spacer(Modifier.width(16.dp))
                        Text(
                            ep.title,
                            fontSize = 19.sp,
                            color = Coffee,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ---------- 4. 播放页（睡眠定时 / 播放记忆 / 防误触）----------
@Composable
private fun PlayerScreen(
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

    // 轮询播放器状态（连续播放时当前集会自动变化）
    LaunchedEffect(pc) {
        while (true) {
            pc?.let {
                playing = it.isPlaying
                if (it.currentIndex in story.episodes.indices) curIndex = it.currentIndex
            }
            delay(400)
        }
    }

    // 播放记忆：进入时恢复上次位置（同一故事同一集）
    LaunchedEffect(pc) {
        if (restored) return@LaunchedEffect
        restored = true
        val p = Prefs.loadProgress(context) ?: return@LaunchedEffect
        if (p.story == story.title && p.index == index && p.position > 3000L) {
            delay(1200)
            pc?.seekTo(p.position)
        }
    }

    // 播放记忆：每 5 秒记录进度
    LaunchedEffect(pc, curIndex) {
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
        pc?.controller?.pause()
        pc?.volume = v0
        sleepMinutes = 0
    }

    // 退出时保存进度
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
        modifier = Modifier.fillMaxSize().background(SkyBlue),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 顶部：←（长按也回上一层）+ 标题 + 🌙 睡眠定时
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onBack() },
                            onLongPress = { onBack() }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("←", fontSize = 28.sp, color = Brown)
            }
            Text(
                story.title,
                modifier = Modifier.weight(1f),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Brown,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box {
                IconButton(onClick = { showSleep = true }) {
                    Text("🌙", fontSize = 26.sp)
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
                fontSize = 14.sp,
                color = Color(0xCC1A237E)
            )
        }

        Spacer(Modifier.height(10.dp))
        Text("第 ${curIndex + 1} 集", fontSize = 16.sp, color = Color(0xCC1A237E))
        Spacer(Modifier.height(6.dp))
        Text(
            ep.title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A237E),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(16.dp))
        Text(
            if (playing) "🎵" else "🎶",
            fontSize = 44.sp,
            modifier = Modifier.scale(pulseScale)
        )

        Spacer(Modifier.height(22.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = {
                    pc?.prev()
                    if (curIndex > 0) onIndex(curIndex - 1)
                },
                modifier = Modifier.size(64.dp)
            ) {
                Icon(Icons.Filled.SkipPrevious, "上一集", tint = Color.White, modifier = Modifier.size(44.dp))
            }

            Spacer(Modifier.width(18.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = CardYellow,
                modifier = Modifier
                    .size(100.dp)
                    .clickable { pc?.toggle() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "播放/暂停",
                        tint = Coffee,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }
            Spacer(Modifier.width(18.dp))

            IconButton(
                onClick = {
                    pc?.next()
                    if (curIndex < story.episodes.size - 1) onIndex(curIndex + 1)
                },
                modifier = Modifier.size(64.dp)
            ) {
                Icon(Icons.Filled.SkipNext, "下一集", tint = Color.White, modifier = Modifier.size(44.dp))
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("共 ${story.episodes.size} 集 · 自动连播", fontSize = 13.sp, color = Color(0xAA1A237E))
    }
}

// ---------- 5. 设置 ----------
@Composable
private fun SettingsScreen(
    musicUri: String?,
    onBack: () -> Unit,
    onPick: () -> Unit,
    onReset: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        TopBar("⚙️ 设置", onBack)
        Column(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("MP3 存放目录", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Coffee)
                    Text(
                        if (musicUri == null) "当前：内置示例" else "当前：自定义文件夹 ✓",
                        fontSize = 15.sp,
                        color = Color(0xFF6D4C41)
                    )
                    if (musicUri != null) {
                        Text(
                            musicUri.take(70),
                            fontSize = 10.sp,
                            color = CoffeeLight,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Button(
                        onClick = onPick,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text("选择文件夹", fontSize = 17.sp)
                    }
                    if (musicUri != null) {
                        TextButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
                            Text("恢复内置示例", fontSize = 15.sp)
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("目录怎么放？", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Coffee)
                    Text(
                        "分类/故事/01 第1集.mp3\n分类/单集故事.mp3\n\n名字会去掉开头的 \"01 \" 序号",
                        fontSize = 14.sp,
                        color = Color(0xFF6D4C41),
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}
