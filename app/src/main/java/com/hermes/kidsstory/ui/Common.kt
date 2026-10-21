package com.hermes.kidsstory.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 渐变风 4 组配色。 */
val GRADS = listOf(
    listOf(Color(0xFFFFD54F), Color(0xFFFFA000)),
    listOf(Color(0xFF4FC3F7), Color(0xFF0288D1)),
    listOf(Color(0xFF81C784), Color(0xFF2E7D32)),
    listOf(Color(0xFFFF8A65), Color(0xFFE64A19)),
)

fun catIcon(title: String): String = when {
    title.contains("睡") -> "月"
    title.contains("儿歌") || title.contains("音乐") -> "歌"
    title.contains("英语") -> "ABC"
    title.contains("国学") -> "文"
    else -> "书"
}

fun fmtTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

/**
 * 主题化卡片。
 * - 渐变主题：圆角填充（无边框）
 * - 粗野主题：白底 + 3dp 黑边 + 5dp 硬阴影
 *
 * fillContent = true：内容层填满卡片（卡片有固定尺寸时用，如 aspectRatio / height）
 * fillContent = false：内容层 wrap（卡片高度由内容撑开时用，如设置页）
 */
@Composable
fun ThemedCard(
    theme: AppTheme,
    shape: RoundedCornerShape,
    brush: Brush? = null,
    solid: Color = theme.card,
    modifier: Modifier = Modifier,
    fillContent: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val fillMod = if (brush != null) Modifier.background(brush) else Modifier.background(solid)
    val borderMod = if (theme.brutal) Modifier.border(3.dp, theme.text, shape) else Modifier
    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    if (theme.brutal) {
        // 阴影层：卡片有固定尺寸时用 fillMaxSize，wrap 时用 matchParentSize
        val shadowSize = if (fillContent) Modifier.fillMaxSize() else Modifier.matchParentSize()
        Box(modifier) {
            Box(
                shadowSize
                    .offset(x = 5.dp, y = 5.dp)
                    .clip(shape)
                    .background(theme.text)
            )
            Box(
                (if (fillContent) Modifier.fillMaxSize() else Modifier)
                    .clip(shape)
                    .then(fillMod)
                    .then(borderMod)
                    .then(clickMod),
                content = content
            )
        }
    } else {
        Box(modifier.clip(shape).then(fillMod).then(clickMod)) {
            if (fillContent) {
                Box(Modifier.fillMaxSize(), content = content)
            } else {
                content()
            }
        }
    }
}

@Composable
fun TopBar(
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

/** 供各屏共享的导航目标。 */
sealed interface Screen {
    data object Home : Screen
    data object Settings : Screen
    data class Stories(val category: com.hermes.kidsstory.data.Category) : Screen
    data class Episodes(val category: com.hermes.kidsstory.data.Category, val story: com.hermes.kidsstory.data.Story) : Screen
    data class Player(val category: com.hermes.kidsstory.data.Category, val story: com.hermes.kidsstory.data.Story, val index: Int) : Screen
}
