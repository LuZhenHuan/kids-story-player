package com.hermes.kidsstory.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

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
 * 性能要点（v0.6）：
 * - 用 DocumentsContract + ContentResolver.query 批量取目录子项（一次 query 拿全部名字/类型），
 *   避免 DocumentFile 逐项 IPC（旧实现每个文件要查 4~5 次，大目录会慢到几十秒）
 * - 扫描结果 JSON 缓存，冷启动先读缓存秒开，再后台刷新
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

    private val PREFIX = Regex("^\\d{1,3}[\\s._-]+")
    private const val CACHE_FILE = "scan_cache.json"

    fun scan(context: Context): List<Category> {
        val custom = Prefs.loadMusicUri(context)
        if (custom != null) {
            val list = runCatching { scanTree(context, Uri.parse(custom)) }.getOrNull()
            if (!list.isNullOrEmpty()) return list
        }
        return scanAssets(context)
    }

    // ---------- SAF 批量扫描 ----------
    private data class Doc(val id: String, val name: String, val isDir: Boolean, val uri: Uri)

    /** 一次 query 取出一个目录的所有子项（名字/类型/URI）。 */
    private fun listChildren(context: Context, tree: Uri, parentId: String): List<Doc> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )
        val out = ArrayList<Doc>()
        runCatching {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0) ?: continue
                    val name = c.getString(1) ?: ""
                    val mime = c.getString(2) ?: ""
                    val isDir = mime == DocumentsContract.Document.MIME_TYPE_DIR
                    out.add(
                        Doc(id, name, isDir, DocumentsContract.buildDocumentUriUsingTree(tree, id))
                    )
                }
            }
        }
        return out
    }

    private fun scanTree(context: Context, tree: Uri): List<Category> {
        val rootId = runCatching { DocumentsContract.getTreeDocumentId(tree) }.getOrNull()
            ?: return emptyList()
        val top = listChildren(context, tree, rootId).filter { it.isDir }

        return top.mapNotNull { catDoc ->
            val stories = collectStories(context, tree, catDoc.id, 0)
            if (stories.isEmpty()) null
            else {
                val title = cleanTitle(catDoc.name)
                Category(title, EMOJI[title] ?: "📚", stories)
            }
        }
    }

    /**
     * 在目录下收集故事（宽松层级）：
     * - 目录里直接的音频 = 单集故事
     * - 子目录里有音频 = 一个故事（多集）
     * - 子目录里没有直接音频 = 继续向下找（兼容"专辑"等额外层级，最多 3 层）
     */
    private fun collectStories(context: Context, tree: Uri, dirId: String, depth: Int): List<Story> {
        if (depth > 3) return emptyList()
        val children = listChildren(context, tree, dirId)
        val stories = mutableListOf<Story>()

        // 直接放音频 = 单集故事
        children.filter { !it.isDir && isAudio(it.name) }
            .sortedBy { it.name }
            .forEach { f ->
                stories.add(Story(cleanTitle(f.name), listOf(Episode(cleanTitle(f.name), f.uri))))
            }

        // 子目录 = 故事（或继续向下）
        children.filter { it.isDir }.sortedBy { it.name }.forEach { sub ->
            val subChildren = listChildren(context, tree, sub.id)
            val direct = subChildren.filter { !it.isDir && isAudio(it.name) }.sortedBy { it.name }
            if (direct.isNotEmpty()) {
                stories.add(
                    Story(
                        cleanTitle(sub.name),
                        direct.map { Episode(cleanTitle(it.name), it.uri) }
                    )
                )
            } else {
                stories.addAll(collectStories(context, tree, sub.id, depth + 1))
            }
        }
        return stories
    }

    private val AUDIO_EXT = listOf(".mp3", ".m4a", ".wav", ".flac", ".aac", ".ogg", ".opus")

    private fun isAudio(name: String): Boolean {
        val n = name.lowercase()
        return AUDIO_EXT.any { n.endsWith(it) }
    }

    /** 去掉扩展名，并去掉 "01 " / "01-" / "01_" 这类序号前缀。 */
    private fun cleanTitle(name: String): String {
        val base = name.substringBeforeLast('.')
        return base.replace(PREFIX, "").ifBlank { base }
    }

    // ---------- 扫描结果缓存 ----------
    fun saveCache(context: Context, categories: List<Category>) {
        runCatching {
            val arr = JSONArray()
            categories.forEach { c ->
                val co = JSONObject().put("t", c.title).put("e", c.emoji)
                val sa = JSONArray()
                c.stories.forEach { s ->
                    val so = JSONObject().put("t", s.title)
                    val ea = JSONArray()
                    s.episodes.forEach { e ->
                        ea.put(JSONObject().put("t", e.title).put("u", e.uri.toString()))
                    }
                    so.put("e", ea)
                    sa.put(so)
                }
                co.put("s", sa)
                arr.put(co)
            }
            File(context.filesDir, CACHE_FILE).writeText(arr.toString())
        }
    }

    fun loadCache(context: Context): List<Category>? {
        val f = File(context.filesDir, CACHE_FILE)
        if (!f.exists()) return null
        return runCatching {
            val arr = JSONArray(f.readText())
            (0 until arr.length()).map { i ->
                val co = arr.getJSONObject(i)
                val sa = co.getJSONArray("s")
                val stories = (0 until sa.length()).map { j ->
                    val so = sa.getJSONObject(j)
                    val ea = so.getJSONArray("e")
                    val eps = (0 until ea.length()).map { k ->
                        val eo = ea.getJSONObject(k)
                        Episode(eo.getString("t"), eo.getString("u").toUri())
                    }
                    Story(so.getString("t"), eps)
                }
                Category(co.getString("t"), co.getString("e"), stories)
            }
        }.getOrNull()
    }

    fun clearCache(context: Context) {
        runCatching { File(context.filesDir, CACHE_FILE).delete() }
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
                            Episode(cleanTitle(file), "asset:///$path/$file".toUri())
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
