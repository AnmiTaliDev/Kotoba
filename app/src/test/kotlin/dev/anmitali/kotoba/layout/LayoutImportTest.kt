package dev.anmitali.kotoba.layout

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutImportTest {

    private val builtIn = listOf(LayoutSource("layouts/base.json", LayoutFixtures.Minimal, builtIn = true))

    private fun candidate(id: String, name: String = "Mine"): String =
        LayoutFixtures.Minimal.replace("\"test\"", "\"$id\"").replace("\"Test\"", "\"$name\"")

    private fun valid(result: LayoutImport.Result) = result as LayoutImport.Result.Valid
    private fun invalid(result: LayoutImport.Result) = result as LayoutImport.Result.Invalid

    @Test
    fun acceptsValidLayout() {
        val result = valid(LayoutImport.check(candidate("mine"), builtIn, emptyList()))
        assertEquals("mine", result.layout.id)
    }

    @Test
    fun rejectsInvalidLayoutWithIssues() {
        val result = invalid(LayoutImport.check("{\"id\": \"x\"}", builtIn, emptyList()))
        assertTrue(result.issues.isNotEmpty())
    }

    @Test
    fun rejectsMalformedJson() {
        val result = invalid(LayoutImport.check("not json", builtIn, emptyList()))
        assertEquals("$", result.issues[0].path)
    }

    @Test
    fun rejectsIdOfBuiltInLayout() {
        val result = invalid(LayoutImport.check(candidate("test"), builtIn, emptyList()))
        assertTrue(result.issues.any { it.path == "$.id" })
    }

    @Test
    fun rejectsIdOfOtherUserLayoutFile() {
        val user = listOf(LayoutSource(UserLayoutStore.originOf("other"), candidate("mine"), builtIn = false))
        val result = invalid(LayoutImport.check(candidate("mine"), builtIn, user))
        assertTrue(result.issues.any { it.path == "$.id" })
    }

    @Test
    fun allowsReplacingUserLayoutWithSameId() {
        val user = listOf(LayoutSource(UserLayoutStore.originOf("mine"), candidate("mine", "Old"), builtIn = false))
        val result = valid(LayoutImport.check(candidate("mine", "New"), builtIn, user))
        assertEquals("New", result.layout.name)
    }

    @Test
    fun allowsExtendingBuiltInLayout() {
        val text = """{"id": "mine", "name": "Mine", "locale": "de", "extends": "test"}"""
        val result = valid(LayoutImport.check(text, builtIn, emptyList()))
        assertTrue(result.layout.layers.containsKey("letters"))
    }

    @Test
    fun rejectsExtendingUnknownBase() {
        val text = """{"id": "mine", "name": "Mine", "locale": "de", "extends": "missing"}"""
        val result = invalid(LayoutImport.check(text, builtIn, emptyList()))
        assertEquals("$.extends", result.issues[0].path)
    }

    @Test
    fun readBoundedReturnsTextWithinLimit() {
        assertEquals("abc", LayoutImport.readBounded(ByteArrayInputStream("abc".toByteArray()), limit = 3))
    }

    @Test
    fun readBoundedRejectsOversizedInput() {
        assertNull(LayoutImport.readBounded(ByteArrayInputStream("abcd".toByteArray()), limit = 3))
    }

    @Test
    fun readBoundedKeepsUtf8() {
        val text = "Ыў"
        assertEquals(text, LayoutImport.readBounded(ByteArrayInputStream(text.toByteArray()), limit = 100))
    }
}
