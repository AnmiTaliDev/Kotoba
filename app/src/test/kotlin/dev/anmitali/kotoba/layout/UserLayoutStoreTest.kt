package dev.anmitali.kotoba.layout

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UserLayoutStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun store() = UserLayoutStore(File(folder.root, "layouts"))

    @Test
    fun emptyWhenDirectoryMissing() {
        assertTrue(store().sources().isEmpty())
    }

    @Test
    fun savedLayoutIsListedWithOrigin() {
        val store = store()
        store.save("mine", "{}")
        val sources = store.sources()
        assertEquals(listOf(UserLayoutStore.originOf("mine")), sources.map { it.origin })
        assertFalse(sources.single().builtIn)
        assertEquals("{}", sources.single().text)
    }

    @Test
    fun saveReplacesExistingFile() {
        val store = store()
        store.save("mine", "one")
        store.save("mine", "two")
        assertEquals("two", store.sources().single().text)
    }

    @Test
    fun saveLeavesNoTemporaryFile() {
        val store = store()
        store.save("mine", "{}")
        val names = File(folder.root, "layouts").list().orEmpty().toList()
        assertEquals(listOf("mine.json"), names)
    }

    @Test
    fun deleteRemovesLayout() {
        val store = store()
        store.save("mine", "{}")
        assertTrue(store.delete("mine"))
        assertTrue(store.sources().isEmpty())
        assertFalse(store.delete("mine"))
    }

    @Test
    fun ignoresNonJsonFiles() {
        val store = store()
        store.save("mine", "{}")
        File(folder.root, "layouts/readme.txt").writeText("x")
        assertEquals(1, store.sources().size)
    }
}
