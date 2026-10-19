package com.hermes.kidsstory.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hermes.kidsstory.data.Category
import com.hermes.kidsstory.data.LibraryScanner
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

private val CARD_COLORS = listOf(CardYellow, SkyBlue, CardGreen, CardPink)

sealed interface Screen {
    data object Home : Screen
    data class Stories(val category: Category) : Screen
    data class Episodes(val category: Category, val story: Story) : Screen
    data class Player(val category: Category, val story: Story, val index: Int) : Screen
}

@Composable
fun KidsStoryApp() {
    val context = LocalContext.current
    val categories = remember { LibraryScanner.scan(context) }
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    val pc = rememberPlayerController(context)

    Surface(color = WarmYellow, modifier = Modifier.fillMaxSize()) {
        when (val s = screen) {
            Screen.Home -> HomeScreen(categories) { screen = Screen.Stories(it) }

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
                onBack = { screen = Screen.Episodes(s.category, s.story) },
                onIndex = { screen = Screen.Player(s.category, s.story, it) }
            )
        }
    }
}

// ---------- 顶部标题栏 ----------
@Composable
private fun TopBar(title: String, onBack: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = Color(0xFF5D4037))
            }
        } else {
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5D4037),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ---------- 1. 首页：分类 ----------
@Composable
private fun HomeScreen(categories: List<Category>, onCategory: (Category) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopBar("📚 故事机")
        if (categories.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("还没有故事\n把 MP3 放进 故事机/ 文件夹", fontSize = 18.sp, textAlign = TextAlign.Center)
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
                Text(
                    cat.title,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3E2723)
                )
                Text(
                    "${cat.stories.size} 个故事",
                    fontSize = 15.sp,
                    color = Color(0x993E2723)
                )
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
                                color = Color(0xFF3E2723),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text("${story.episodes.size} 集", fontSize = 14.sp, color = Color(0x993E2723))
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
                        Text(
                            "${idx + 1}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF42A5F5)
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            ep.title,
                            fontSize = 19.sp,
                            color = Color(0xFF3E2723),
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
    story: Story,
    index: Int,
    pc: PlayerController?,
    onBack: () -> Unit,
    onIndex: (Int) -> Unit
) {
    var playing by remember { mutableStateOf(false) }
    var curIndex by remember { mutableIntStateOf(index) }

    // 轮询播放器状态（MVP 简单可靠）
    LaunchedEffect(pc) {
        while (true) {
            pc?.let {
                playing = it.isPlaying
                if (it.currentIndex in story.episodes.indices) curIndex = it.currentIndex
            }
            delay(400)
        }
    }

    val ep = story.episodes.getOrNull(curIndex) ?: story.episodes.first()

    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (playing) 1.12f else 1f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "scale"
    )

    Column(
        modifier = Modifier.fillMaxSize().background(SkyBlue),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopBar("${story.title}", onBack)

        Spacer(Modifier.height(16.dp))
        Text(
            "第 ${curIndex + 1} 集",
            fontSize = 16.sp,
            color = Color(0xCC1A237E)
        )
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

        Spacer(Modifier.height(18.dp))
        Text(
            if (playing) "🎵" else "🎶",
            fontSize = 44.sp,
            modifier = Modifier.scale(scale)
        )

        Spacer(Modifier.height(26.dp))
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
                color = Color(0xFFFFC93C),
                modifier = Modifier
                    .size(100.dp)
                    .clickable { pc?.toggle() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "播放/暂停",
                        tint = Color(0xFF3E2723),
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

        Spacer(Modifier.height(30.dp))
        Text(
            "共 ${story.episodes.size} 集",
            fontSize = 14.sp,
            color = Color(0xCC1A237E)
        )
    }
}
