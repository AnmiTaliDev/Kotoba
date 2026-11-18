package dev.anmitali.kotoba.layout

import android.content.res.AssetManager

class LayoutRepository(private val assets: AssetManager) {

    fun loadAll(): List<KeyboardLayout> =
        assets.list(DIRECTORY).orEmpty()
            .filter { it.endsWith(".json") }
            .sorted()
            .map { file ->
                assets.open("$DIRECTORY/$file").bufferedReader().use { LayoutParser.parse(it.readText()) }
            }

    private companion object {
        const val DIRECTORY = "layouts"
    }
}
