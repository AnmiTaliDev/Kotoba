package dev.anmitali.kotoba.layout

import java.io.ByteArrayInputStream
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        assertTrue(store().files().isEmpty())
    }

    @Test
    fun savedLayoutIsListedWithItsId() {
        val store = store()
        store.save("mine", "{}")
        val files = store.files()
        assertEquals(listOf("mine"), files.map { it.id })
        assertEquals("{}", files.single().text)
    }

    @Test
    fun saveReplacesExistingFile() {
        val store = store()
        store.save("mine", "one")
        store.save("mine", "two")
        assertEquals("two", store.files().single().text)
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
        assertTrue(store.files().isEmpty())
        assertFalse(store.delete("mine"))
    }

    @Test
    fun ignoresNonJsonFiles() {
        val store = store()
        store.save("mine", "{}")
        File(folder.root, "layouts/readme.txt").writeText("x")
        assertEquals(1, store.files().size)
    }

    @Test
    fun readBoundedReturnsTextWithinLimit() {
        assertEquals("abc", readBounded(ByteArrayInputStream("abc".toByteArray()), limit = 3))
    }

    @Test
    fun readBoundedRejectsOversizedInput() {
        assertNull(readBounded(ByteArrayInputStream("abcd".toByteArray()), limit = 3))
    }

    @Test
    fun readBoundedKeepsUtf8() {
        val text = "Ыў"
        assertEquals(text, readBounded(ByteArrayInputStream(text.toByteArray()), limit = 100))
    }
}
