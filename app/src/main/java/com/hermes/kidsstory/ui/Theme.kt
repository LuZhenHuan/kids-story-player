package com.hermes.kidsstory.ui

import androidx.compose.ui.graphics.Color

/**
 * 主题定义：
 * - Gradient：v0.4 渐变风（默认）
 * - Neobrutalism：新粗野风（粗黑边框 + 硬阴影 + 鲜亮撞色）
 */
data class AppTheme(
    val id: String,
    val name: String,
    val bg: Color,
    val text: Color,
    val muted: Color,
    val card: Color,
    val palette: List<Color>,
    val playerBg: List<Color>,
    val onPlayer: Color,
    val playerAccent: Color,
    val brutal: Boolean
)

val GradientTheme = AppTheme(
    id = "gradient",
    name = "渐变风",
    bg = Color(0xFFFFFBF5),
    text = Color(0xFF3E2723),
    muted = Color(0xFF6D4C41),
    card = Color.White,
    palette = listOf(
        Color(0xFFFFD54F), Color(0xFF4FC3F7), Color(0xFF81C784), Color(0xFFFF8A65)
    ),
    playerBg = listOf(Color(0xFF81D4FA), Color(0xFF29B6F6)),
    onPlayer = Color.White,
    playerAccent = Color(0xFF29B6F6),
    brutal = false
)

val BrutalTheme = AppTheme(
    id = "brutal",
    name = "新粗野",
    bg = Color(0xFFFBFBF9),
    text = Color(0xFF1C293C),
    muted = Color(0xFF5A6577),
    card = Color.White,
    palette = listOf(
        Color(0xFFFDC800), Color(0xFF432DD7), Color(0xFF16A34A), Color(0xFFD97706)
    ),
    playerBg = listOf(Color(0xFFFDC800), Color(0xFFFDC800)),
    onPlayer = Color(0xFF1C293C),
    playerAccent = Color(0xFF432DD7),
    brutal = true
)

fun themeById(id: String): AppTheme = if (id == "brutal") BrutalTheme else GradientTheme
