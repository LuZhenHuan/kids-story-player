package com.hermes.kidsstory.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hermes.kidsstory.data.Category
import com.hermes.kidsstory.data.Prefs
import com.hermes.kidsstory.data.Story

/** 首页：2 列网格 + 继续听。 */
@Composable
fun HomeScreen(
    theme: AppTheme,
    categories: List<Category>,
    loading: Boolean,
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

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = theme.text)
            }
            return@Column
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

        val found = progress?.let { p ->
            categories.firstNotNullOfOrNull { cat ->
                cat.stories.firstOrNull { it.title == p.story }?.let { cat to it }
            }
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
                CategoryCard(theme, categories[i], i) { onCategory(categories[i]) }
            }
        }
    }
}

@Composable
private fun CategoryCard(theme: AppTheme, cat: Category, index: Int, onClick: () -> Unit) {
    val paletteColor = theme.palette[index % theme.palette.size]
    val brutal = theme.brutal && index % 4 == 0
    ThemedCard(
        theme = theme,
        shape = RoundedCornerShape(if (theme.brutal) 14.dp else 26.dp),
        brush = if (theme.brutal) null else Brush.linearGradient(GRADS[index % GRADS.size]),
        solid = if (theme.brutal) paletteColor else theme.palette[0],
        modifier = Modifier.fillMaxWidth().height(170.dp),
        onClick = onClick
    ) {
        Column(
            Modifier.fillMaxSize().padding(10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val icon = catIcon(cat.title)
            Text(
                icon,
                fontSize = if (icon.length > 1) 34.sp else 44.sp,
                fontWeight = FontWeight.Bold,
                color = if (brutal) theme.text else Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text(
                cat.title,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = if (brutal) theme.text else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${cat.stories.size} 个故事",
                fontSize = 13.sp,
                color = if (brutal) theme.text.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.85f)
            )
        }
    }
}
