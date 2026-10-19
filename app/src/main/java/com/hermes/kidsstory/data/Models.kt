package com.hermes.kidsstory.data

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri

data class Episode(
    val title: String,
    val uri: Uri
)

data class Story(
    val title: String,
    val episodes: List<Episode>
)

data class Category(
    val title: String,
    val emoji: String,
    val stories: List<Story>
)

/**
 * 数据扫描：优先扫 assets/audio/<分类>/<故事>/<集>.mp3（内置示例），
 * 之后可以扩展扫描 /sdcard/故事机/（外部导入）。
 */
object LibraryScanner {

    private val EMOJI = mapOf(
        "睡前故事" to "🌙",
        "儿歌" to "🎵",
        "英语" to "🔤",
        "故事" to "📖",
        "音乐" to "🎶"
    )

    fun scan(context: Context): List<Category> {
        val categories = mutableListOf<Category>()
        val root = "audio"
        val catNames = context.assets.list(root)?.sorted() ?: emptyList()

        for (cat in catNames) {
            val items = context.assets.list("$root/$cat")?.sorted() ?: continue
            val stories = mutableListOf<Story>()

            for (item in items) {
                val path = "$root/$cat/$item"
                val sub = context.assets.list(path)
                if (!sub.isNullOrEmpty()) {
                    // 子目录 = 一个故事（多集）
                    val eps = sub
                        .filter { it.endsWith(".mp3", ignoreCase = true) }
                        .sorted()
                        .map { file ->
                            Episode(
                                title = file.substringBeforeLast('.'),
                                uri = "asset:///$path/$file".toUri()
                            )
                        }
                    if (eps.isNotEmpty()) stories.add(Story(item, eps))
                } else if (item.endsWith(".mp3", ignoreCase = true)) {
                    // 直接放的 MP3 = 单集故事
                    stories.add(
                        Story(
                            title = item.substringBeforeLast('.'),
                            episodes = listOf(
                                Episode(
                                    title = item.substringBeforeLast('.'),
                                    uri = "asset:///$path".toUri()
                                )
                            )
                        )
                    )
                }
            }
            if (stories.isNotEmpty()) {
                categories.add(Category(cat, EMOJI[cat] ?: "📚", stories))
            }
        }
        return categories
    }
}
