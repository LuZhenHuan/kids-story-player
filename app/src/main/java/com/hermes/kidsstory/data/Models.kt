package com.hermes.kidsstory.data

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile

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
 * 数据扫描：
 * 1. 优先扫描用户选择的文件夹（SAF tree uri）
 * 2. 未选择 / 扫描为空时，回退到 assets/audio 内置示例
 *
 * 目录规则（v0.3）：
 *   根/分类A/故事X/01 第1集.mp3   → 多集故事
 *   根/分类A/单集故事.mp3          → 单集故事
 */
object LibraryScanner {

    private val EMOJI = mapOf(
        "睡前故事" to "🌙",
        "儿歌" to "🎵",
        "英语" to "🔤",
        "故事" to "📖",
        "音乐" to "🎶",
        "国学" to "📜",
        "绘本" to "📖",
        "科普" to "🔬"
    )

    fun scan(context: Context): List<Category> {
        val custom = Prefs.loadMusicUri(context)
        if (custom != null) {
            val list = runCatching { scanTree(context, Uri.parse(custom)) }.getOrNull()
            if (!list.isNullOrEmpty()) return list
        }
        return scanAssets(context)
    }

    // ---------- 外部文件夹（SAF）----------
    private fun scanTree(context: Context, tree: Uri): List<Category> {
        val root = DocumentFile.fromTreeUri(context, tree) ?: return emptyList()
        return root.listFiles()
            .filter { it.isDirectory }
            .mapNotNull { cat ->
                val files = cat.listFiles()
                val stories = mutableListOf<Story>()

                // 二级文件夹 = 多集故事
                files.filter { it.isDirectory }.forEach { st ->
                    val eps = st.listFiles()
                        .filter { isMp3(it.name) }
                        .sortedBy { it.name ?: "" }
                        .map { f -> Episode(cleanTitle(f.name), f.uri) }
                    if (eps.isNotEmpty()) stories.add(Story(cleanTitle(st.name), eps))
                }

                // 分类下直接放 MP3 = 单集故事
                files.filter { it.isFile && isMp3(it.name) }
                    .sortedBy { it.name ?: "" }
                    .forEach { f ->
                        stories.add(
                            Story(cleanTitle(f.name), listOf(Episode(cleanTitle(f.name), f.uri)))
                        )
                    }

                if (stories.isEmpty()) null
                else Category(cleanTitle(cat.name), EMOJI[cat.name] ?: "📚", stories)
            }
    }

    private fun isMp3(name: String?): Boolean =
        name?.lowercase()?.endsWith(".mp3") == true

    /** 去掉扩展名，并去掉 "01 " / "01-" / "01_" 这类序号前缀。 */
    private fun cleanTitle(name: String?): String {
        val n = name ?: return "未命名"
        val base = n.substringBeforeLast('.')
        return base.replace(Regex("^\\d{1,3}[\\s._-]+"), "").ifBlank { base }
    }

    // ---------- 内置示例（assets）----------
    private fun scanAssets(context: Context): List<Category> {
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
                    val eps = sub
                        .filter { it.endsWith(".mp3", ignoreCase = true) }
                        .sorted()
                        .map { file ->
                            Episode(
                                title = cleanTitle(file),
                                uri = "asset:///$path/$file".toUri()
                            )
                        }
                    if (eps.isNotEmpty()) stories.add(Story(cleanTitle(item), eps))
                } else if (item.endsWith(".mp3", ignoreCase = true)) {
                    stories.add(
                        Story(
                            title = cleanTitle(item),
                            episodes = listOf(
                                Episode(cleanTitle(item), "asset:///$path".toUri())
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
