package dev.anmitali.kotoba.layout

import dev.anmitali.kotoba.layout.LayoutFixtures.errors
import dev.anmitali.kotoba.layout.LayoutFixtures.minimal
import dev.anmitali.kotoba.layout.LayoutFixtures.parse
import dev.anmitali.kotoba.layout.LayoutFixtures.warnings
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutParserTest {

    private fun assertError(json: JSONObject, path: String) {
        val result = parse(json)
        assertNull(result.layout)
        assertTrue("expected error at $path, got ${result.issues}", errors(result).any { it.path == path })
    }

    @Test
    fun parsesMinimalLayout() {
        val result = parse(minimal())
        assertTrue(result.issues.toString(), errors(result).isEmpty())
        val layout = result.layout!!
        assertEquals("test", layout.id)
        assertEquals(true, layout.autoCapitalize)
        assertEquals("A", (layout.layers.getValue("letters").rows[0][0] as CharKey).shifted)
    }

    @Test
    fun uppercaseFollowsLocale() {
        val json = minimal().put("locale", "tr")
        json.getJSONObject("layers").getJSONObject("letters").getJSONArray("rows")
            .put(0, JSONArray().put("i").put("ı"))
        val keys = parse(json).layout!!.layers.getValue("letters").rows[0].map { (it as CharKey).shifted }
        assertEquals(listOf("İ", "I"), keys)
    }

    @Test
    fun explicitShiftedOverridesUppercase() {
        val json = minimal()
        json.getJSONObject("layers").getJSONObject("letters").getJSONArray("rows")
            .put(0, JSONArray().put(JSONObject().put("label", "ß").put("shifted", "ẞ")))
        val key = parse(json).layout!!.layers.getValue("letters").rows[0][0] as CharKey
        assertEquals("ẞ", key.shifted)
    }

    @Test
    fun autoCapitalizeCanBeDisabled() {
        assertEquals(false, parse(minimal().put("autoCapitalize", false)).layout!!.autoCapitalize)
    }

    @Test
    fun rejectsMissingId() {
        val json = minimal()
        json.remove("id")
        assertError(json, "$.id")
    }

    @Test
    fun rejectsBadId() {
        assertError(minimal().put("id", "Bad Id"), "$.id")
    }

    @Test
    fun rejectsBadLocale() {
        assertError(minimal().put("locale", "not a locale"), "$.locale")
    }

    @Test
    fun rejectsNewerFormatVersion() {
        assertError(minimal().put("formatVersion", 99), "$.formatVersion")
    }

    @Test
    fun rejectsUnknownKeyType() {
        val json = minimal()
        json.getJSONObject("layers").getJSONObject("letters").getJSONArray("rows")
            .put(0, JSONArray().put(JSONObject().put("type", "teleport")))
        assertError(json, "$.layers.letters.rows[0][0].type")
    }

    @Test
    fun rejectsUnknownLayerTarget() {
        val json = minimal()
        json.getJSONObject("layers").getJSONObject("letters").getJSONArray("rows")
            .put(0, JSONArray().put(JSONObject().put("type", "layer").put("label", "x").put("layer", "nope")))
        assertError(json, "$.layers.letters.rows[0][0].layer")
    }

    @Test
    fun rejectsUnknownDefaultLayer() {
        assertError(minimal().put("defaultLayer", "nope"), "$.defaultLayer")
    }

    @Test
    fun rejectsDefaultLayerWithoutSpace() {
        val json = minimal()
        json.getJSONObject("layers").getJSONObject("letters").put("rows", JSONArray().put(JSONArray().put("a")))
        assertError(json, "$.layers.letters")
    }

    @Test
    fun warnsAboutMissingDelete() {
        val json = minimal()
        json.getJSONObject("layers").getJSONObject("letters")
            .put("rows", JSONArray().put(JSONArray().put(JSONObject().put("type", "space"))))
        val result = parse(json)
        assertNotNull(result.layout)
        assertTrue(warnings(result).any { it.path == "$.layers.letters" })
    }

    @Test
    fun rejectsEmptyRow() {
        val json = minimal()
        json.getJSONObject("layers").getJSONObject("letters").getJSONArray("rows").put(JSONArray())
        assertError(json, "$.layers.letters.rows[2]")
    }

    @Test
    fun rejectsLongLabel() {
        val json = minimal()
        json.getJSONObject("layers").getJSONObject("letters").getJSONArray("rows")
            .put(0, JSONArray().put("123456789"))
        assertError(json, "$.layers.letters.rows[0][0]")
    }

    @Test
    fun rejectsNonPositiveWeight() {
        val json = minimal()
        json.getJSONObject("layers").getJSONObject("letters").getJSONArray("rows")
            .put(0, JSONArray().put(JSONObject().put("label", "a").put("weight", 0)))
        assertError(json, "$.layers.letters.rows[0][0].weight")
    }

    @Test
    fun warnsAboutWideRow() {
        val json = minimal()
        val row = JSONArray()
        repeat(15) { row.put("a") }
        json.getJSONObject("layers").getJSONObject("letters").getJSONArray("rows").put(row)
        val result = parse(json)
        assertNotNull(result.layout)
        assertTrue(warnings(result).any { it.path == "$.layers.letters.rows[2]" })
    }

    @Test
    fun rejectsTooManyKeys() {
        val json = minimal()
        val rows = json.getJSONObject("layers").getJSONObject("letters").getJSONArray("rows")
        repeat(LayoutParser.MaxKeys / 10 + 1) {
            val row = JSONArray()
            repeat(10) { row.put("a") }
            rows.put(row)
        }
        assertError(json, "$")
    }

    @Test
    fun rejectsEmojiLayerWithoutBack() {
        val json = minimal()
        json.getJSONObject("layers").put("emoji", JSONObject().put("kind", "emoji"))
        assertError(json, "$.layers.emoji.back")
    }

    @Test
    fun rejectsWrongTypes() {
        assertError(minimal().put("name", 5), "$.name")
        assertError(minimal().put("punctuation", "x"), "$.punctuation")
    }

    @Test
    fun collectsSeveralIssuesAtOnce() {
        val json = minimal().put("id", "Bad").put("locale", "???")
        val paths = errors(parse(json)).map { it.path }
        assertTrue(paths.containsAll(listOf("$.id", "$.locale")))
    }

    private fun withKey(key: JSONObject): JSONObject {
        val json = minimal()
        json.getJSONObject("layers").getJSONObject("letters").getJSONArray("rows").put(0, JSONArray().put(key))
        return json
    }

    private fun firstKey(json: JSONObject): CharKey =
        parse(json).layout!!.layers.getValue("letters").rows[0][0] as CharKey

    @Test
    fun readsHoldAlternatives() {
        val key = firstKey(withKey(JSONObject().put("label", "e").put("hold", JSONArray(listOf("é", "è")))))
        assertEquals(listOf("é", "è"), key.hold)
        assertEquals(listOf("É", "È"), key.holdShifted)
    }

    @Test
    fun explicitHoldShiftedOverridesUppercase() {
        val key = firstKey(
            withKey(
                JSONObject().put("label", "s")
                    .put("hold", JSONArray(listOf("ß")))
                    .put("holdShifted", JSONArray(listOf("ẞ"))),
            ),
        )
        assertEquals(listOf("ẞ"), key.holdShifted)
    }

    @Test
    fun holdDefaultsToEmpty() {
        assertEquals(emptyList<String>(), firstKey(withKey(JSONObject().put("label", "e"))).hold)
    }

    @Test
    fun rejectsHoldShiftedWithDifferentSize() {
        val key = JSONObject().put("label", "e")
            .put("hold", JSONArray(listOf("é", "è"))).put("holdShifted", JSONArray(listOf("É")))
        assertError(withKey(key), "$.layers.letters.rows[0][0].holdShifted")
    }

    @Test
    fun rejectsTooManyAlternatives() {
        val items = JSONArray(List(LayoutParser.MaxHold + 1) { "a" })
        assertError(withKey(JSONObject().put("label", "e").put("hold", items)), "$.layers.letters.rows[0][0].hold")
    }

    @Test
    fun rejectsBadAlternativeItems() {
        assertError(
            withKey(JSONObject().put("label", "e").put("hold", JSONArray().put(""))),
            "$.layers.letters.rows[0][0].hold[0]",
        )
        assertError(
            withKey(JSONObject().put("label", "e").put("hold", JSONArray().put(5))),
            "$.layers.letters.rows[0][0].hold[0]",
        )
    }

    @Test
    fun rejectsNonArrayHold() {
        assertError(withKey(JSONObject().put("label", "e").put("hold", "é")), "$.layers.letters.rows[0][0].hold")
    }
}
