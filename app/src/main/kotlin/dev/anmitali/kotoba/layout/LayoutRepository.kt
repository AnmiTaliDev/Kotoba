package dev.anmitali.kotoba.layout

import android.content.res.AssetManager

class LayoutRepository(private val assets: AssetManager) {

    fun loadAll(): LoadedLayouts = LayoutLoader.load(builtInSources())

    private fun builtInSources(): List<LayoutSource> =
        assets.list(DIRECTORY).orEmpty()
            .filter { it.endsWith(".json") }
            .map { file ->
                val text = assets.open("$DIRECTORY/$file").bufferedReader().use { it.readText() }
                LayoutSource(origin = "$DIRECTORY/$file", text = text, builtIn = true)
            }

    private companion object {
        const val DIRECTORY = "layouts"
    }
}
