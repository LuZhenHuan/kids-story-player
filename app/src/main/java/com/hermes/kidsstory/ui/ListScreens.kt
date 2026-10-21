package com.hermes.kidsstory.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hermes.kidsstory.data.Category
import com.hermes.kidsstory.data.Story

@Composable
fun StoryListScreen(
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

@Composable
fun EpisodeListScreen(
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
