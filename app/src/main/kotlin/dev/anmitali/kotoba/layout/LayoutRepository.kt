package dev.anmitali.kotoba.layout

import dev.anmitali.kotoba.engine.Engine
import dev.anmitali.kotoba.engine.ImportResult
import dev.anmitali.kotoba.engine.KeyboardLayout
import dev.anmitali.kotoba.engine.LayoutFailure
import dev.anmitali.kotoba.engine.LayoutSource
import dev.anmitali.kotoba.engine.LoadedSource
import dev.anmitali.kotoba.engine.builtInSources
import dev.anmitali.kotoba.engine.checkImport
import dev.anmitali.kotoba.engine.userLayoutOrigin

class LoadedLayouts(
    val layouts: List<KeyboardLayout>,
    val failures: List<LayoutFailure>,
    val sources: Map<String, LoadedSource>,
)

class LayoutRepository(private val userStore: UserLayoutStore) {

    fun builtInLayoutSources(): List<LayoutSource> = builtInSources()

    fun userSources(): List<LayoutSource> =
        userStore.files().map { LayoutSource(origin = userLayoutOrigin(it.id), text = it.text, builtIn = false) }

    fun allSources(): List<LayoutSource> = builtInLayoutSources() + userSources()

    fun loadAll(): LoadedLayouts =
        Engine(allSources()).use { engine ->
            LoadedLayouts(engine.layouts(), engine.failures(), engine.sources().associateBy { it.layoutId })
        }

    fun check(text: String): ImportResult = checkImport(text, builtInLayoutSources(), userSources())

    fun import(text: String): ImportResult {
        val result = check(text)
        if (result is ImportResult.Valid) userStore.save(result.layout.id, text)
        return result
    }

    fun delete(id: String): Boolean = userStore.delete(id)
}
