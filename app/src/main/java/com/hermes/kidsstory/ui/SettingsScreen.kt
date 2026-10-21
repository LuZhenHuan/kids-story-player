package com.hermes.kidsstory.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
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
                modifier = Modifier.fillMaxWidth(),
                fillContent = false
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
                modifier = Modifier.fillMaxWidth(),
                fillContent = false
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
                    // 用 ThemedCard 做按钮，保证两种主题风格统一
                    ThemedCard(
                        theme = theme,
                        shape = RoundedCornerShape(if (theme.brutal) 8.dp else 12.dp),
                        solid = theme.palette[0],
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        onClick = onPick
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "选择文件夹",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.text
                            )
                        }
                    }
                    if (musicUri != null) {
                        ThemedCard(
                            theme = theme,
                            shape = RoundedCornerShape(if (theme.brutal) 8.dp else 12.dp),
                            solid = theme.card,
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            onClick = onReset
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("恢复内置示例", fontSize = 15.sp, color = theme.text)
                            }
                        }
                    }
                }
            }

            // 说明
            ThemedCard(
                theme = theme,
                shape = RoundedCornerShape(20.dp),
                solid = theme.card,
                modifier = Modifier.fillMaxWidth(),
                fillContent = false
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
