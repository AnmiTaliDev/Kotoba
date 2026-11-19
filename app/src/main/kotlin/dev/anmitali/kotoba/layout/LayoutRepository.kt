package dev.anmitali.kotoba.layout

import android.content.res.AssetManager

class LayoutRepository(
    private val assets: AssetManager,
    private val userStore: UserLayoutStore,
) {

    fun loadAll(): LoadedLayouts = LayoutLoader.load(builtInSources() + userStore.sources())

    fun builtInSources(): List<LayoutSource> =
        assets.list(DIRECTORY).orEmpty()
            .filter { it.endsWith(".json") }
            .sorted()
            .map { file ->
                val text = assets.open("$DIRECTORY/$file").bufferedReader().use { it.readText() }
                LayoutSource(origin = "$DIRECTORY/$file", text = text, builtIn = true)
            }

    fun userSources(): List<LayoutSource> = userStore.sources()

    fun check(text: String): LayoutImport.Result = LayoutImport.check(text, builtInSources(), userStore.sources())

    fun import(text: String): LayoutImport.Result {
        val result = check(text)
        if (result is LayoutImport.Result.Valid) userStore.save(result.layout.id, text)
        return result
    }

    fun delete(id: String): Boolean = userStore.delete(id)

    private companion object {
        const val DIRECTORY = "layouts"
    }
}
