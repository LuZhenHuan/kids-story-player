package com.hermes.kidsstory.data

import android.content.Context

/** 播放记忆：记住上次听到的故事/集数/位置。 */
object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("kids_story", Context.MODE_PRIVATE)

    fun saveProgress(context: Context, story: String, index: Int, position: Long) {
        sp(context).edit()
            .putString("last_story", story)
            .putInt("last_index", index)
            .putLong("last_pos", position)
            .apply()
    }

    data class Progress(val story: String, val index: Int, val position: Long)

    fun loadProgress(context: Context): Progress? {
        val p = sp(context)
        val s = p.getString("last_story", null) ?: return null
        return Progress(s, p.getInt("last_index", 0), p.getLong("last_pos", 0))
    }

    /** 用户自选的 MP3 根目录（SAF tree uri）；null = 用内置示例。 */
    fun saveMusicUri(context: Context, uri: String?) {
        sp(context).edit().apply {
            if (uri == null) remove("music_uri") else putString("music_uri", uri)
        }.apply()
    }

    fun loadMusicUri(context: Context): String? = sp(context).getString("music_uri", null)
}
