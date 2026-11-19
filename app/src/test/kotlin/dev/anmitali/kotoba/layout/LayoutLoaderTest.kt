package dev.anmitali.kotoba.layout

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutLoaderTest {

    private fun builtIn(origin: String, text: String) = LayoutSource(origin, text, builtIn = true)
    private fun user(origin: String, text: String) = LayoutSource(origin, text, builtIn = false)

    private fun child(extra: String): String = """
        {
          "id": "child",
          "name": "Child",
          "locale": "de",
          "extends": "test"
          $extra
        }
    """

    @Test
    fun loadsValidSources() {
        val loaded = LayoutLoader.load(listOf(builtIn("a.json", LayoutFixtures.Minimal)))
        assertEquals(1, loaded.layouts.size)
        assertTrue(loaded.failures.isEmpty())
    }

    @Test
    fun malformedJsonDoesNotStopOthers() {
        val loaded = LayoutLoader.load(listOf(builtIn("a.json", LayoutFixtures.Minimal), user("b.json", "{oops")))
        assertEquals(listOf("test"), loaded.layouts.map { it.id })
        assertEquals(listOf("b.json"), loaded.failures.map { it.origin })
    }

    @Test
    fun rejectsOversizedFile() {
        val padding = " ".repeat(LayoutLoader.MaxFileBytes)
        val loaded = LayoutLoader.load(listOf(user("big.json", LayoutFixtures.Minimal + padding)))
        assertTrue(loaded.layouts.isEmpty())
        assertEquals(1, loaded.failures.size)
    }

    @Test
    fun extendsInheritsLayersAndPunctuation() {
        val loaded = LayoutLoader.load(
            listOf(builtIn("base.json", LayoutFixtures.Minimal), user("child.json", child(""))),
        )
        val layout = loaded.layouts.single { it.id == "child" }
        assertEquals("Child", layout.name)
        assertEquals("de", layout.locale)
        assertEquals(".", layout.sentenceEnd)
        assertTrue(layout.layers.containsKey("letters"))
    }

    @Test
    fun extendsOverridesLayerAndPunctuationKeys() {
        val extra = """
            , "punctuation": {"sentenceEnd": "!"},
            "layers": {"letters": {"shiftable": false, "rows": [["z"], [{"type": "space"}, {"type": "delete"}]]}}
        """
        val loaded = LayoutLoader.load(
            listOf(builtIn("base.json", LayoutFixtures.Minimal), user("child.json", child(extra))),
        )
        val layout = loaded.layouts.single { it.id == "child" }
        assertEquals("!", layout.sentenceEnd)
        assertEquals(".", layout.doubleSpace)
        assertEquals("z", (layout.layers.getValue("letters").rows[0][0] as CharKey).label)
    }

    @Test
    fun unknownBaseFails() {
        val loaded = LayoutLoader.load(listOf(user("child.json", child(""))))
        assertEquals(1, loaded.failures.size)
        assertEquals("$.extends", loaded.failures[0].issues[0].path)
    }

    @Test
    fun userLayoutCannotBeBase() {
        val userBase = LayoutFixtures.Minimal.replace("\"test\"", "\"ubase\"")
        val text = child("").replace("\"test\"", "\"ubase\"")
        val loaded = LayoutLoader.load(listOf(user("a.json", userBase), user("b.json", text)))
        assertEquals(listOf("ubase"), loaded.layouts.map { it.id })
        assertEquals(1, loaded.failures.size)
    }

    @Test
    fun extendingExtendedLayoutFails() {
        val second = child("").replace("\"child\"", "\"grandchild\"").replace("\"test\"", "\"child\"")
        val loaded = LayoutLoader.load(
            listOf(builtIn("a.json", LayoutFixtures.Minimal), builtIn("b.json", child("")), builtIn("c.json", second)),
        )
        assertEquals(setOf("test", "child"), loaded.layouts.map { it.id }.toSet())
        assertEquals(1, loaded.failures.size)
    }

    @Test
    fun duplicateIdIsRejected() {
        val loaded = LayoutLoader.load(
            listOf(builtIn("a.json", LayoutFixtures.Minimal), user("b.json", LayoutFixtures.Minimal)),
        )
        assertEquals(1, loaded.layouts.size)
        assertEquals(listOf("b.json"), loaded.failures.map { it.origin })
    }

    @Test
    fun builtInLayoutsLoadBeforeUserLayouts() {
        val userText = LayoutFixtures.Minimal.replace("\"test\"", "\"zuser\"")
        val loaded = LayoutLoader.load(listOf(user("a.json", userText), builtIn("z.json", LayoutFixtures.Minimal)))
        assertEquals(listOf("test", "zuser"), loaded.layouts.map { it.id })
    }

    @Test
    fun invalidExtendsTypeIsReported() {
        val text = JSONObject(child("")).put("extends", 5).toString()
        val loaded = LayoutLoader.load(listOf(builtIn("a.json", LayoutFixtures.Minimal), user("b.json", text)))
        assertEquals("$.extends", loaded.failures.single().issues[0].path)
    }
}
