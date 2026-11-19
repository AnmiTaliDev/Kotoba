package dev.anmitali.kotoba.emoji

import android.content.res.AssetManager
import org.json.JSONObject

class EmojiRepository(private val assets: AssetManager) {

    fun load(): List<EmojiCategory> {
        val root = JSONObject(assets.open(FILE).bufferedReader().use { it.readText() })
        val categories = root.getJSONArray("categories")
        return List(categories.length()) { c ->
            val json = categories.getJSONObject(c)
            val emoji = json.getJSONArray("emoji")
            EmojiCategory(
                id = json.getString("id"),
                name = json.getString("name"),
                icon = json.getString("icon"),
                emoji = List(emoji.length()) { emoji.getString(it) },
                recent = json.optBoolean("recent", false),
            )
        }
    }

    private companion object {
        const val FILE = "emoji/emoji.json"
    }
}
