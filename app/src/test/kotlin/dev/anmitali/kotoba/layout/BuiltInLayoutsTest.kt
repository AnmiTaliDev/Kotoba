package dev.anmitali.kotoba.layout

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltInLayoutsTest {

    private val directory = File("src/main/assets/layouts")

    private val alphabets = mapOf(
        "latin" to "abcdefghijklmnopqrstuvwxyz",
        "ru" to "абвгдеёжзийклмнопрстуфхцчшщъыьэюя",
        "uk" to "абвгґдеєжзиіїйклмнопрстуфхцчшщьюяʼ",
        "be" to "абвгдеёжзійклмнопрстуўфхцчшыьэюя'",
        "kk" to "абвгғдеёжзийкқлмнңоөпрстуұүфхһцчшщъыіьэюяә",
        "kk-compact" to "абвгғдеёжзийкқлмнңоөпрстуұүфхһцчшщъыіьэюяә",
        "kk-anmitalidev-1a" to "аәбгғдежзийкқлмнңоөпрстуұүхһшыі",
        "de" to "abcdefghijklmnopqrstuvwxyzäöüß",
    )

    private fun load(): LoadedLayouts {
        val files = directory.listFiles { f -> f.extension == "json" }.orEmpty().sortedBy { it.name }
        assertTrue("no built-in layouts found in ${directory.absolutePath}", files.isNotEmpty())
        return LayoutLoader.load(files.map { LayoutSource(it.name, it.readText(), builtIn = true) })
    }

    private fun format(failures: List<LayoutFailure>) = failures.joinToString("\n") { f ->
        "${f.origin}: " + f.issues.joinToString { "${it.path} ${it.message}" }
    }

    @Test
    fun everyBuiltInLayoutLoadsWithoutErrorsOrWarnings() {
        val loaded = load()
        assertTrue(format(loaded.failures), loaded.failures.isEmpty())
        assertTrue(format(loaded.warnings), loaded.warnings.isEmpty())
        assertEquals(directory.listFiles { f -> f.extension == "json" }!!.size, loaded.layouts.size)
    }

    @Test
    fun everyBuiltInLayoutKeepsSharedLayers() {
        for (layout in load().layouts) {
            for (id in listOf("letters", "symbols", "symbols2", "emoji")) {
                assertTrue("${layout.id} misses layer $id", layout.layers.containsKey(id))
            }
        }
    }

    @Test
    fun layoutIdsAreUniqueAndOrdered() {
        val layouts = load().layouts
        assertEquals(layouts.size, layouts.map { it.id }.toSet().size)
        assertEquals(layouts.sortedWith(compareBy({ it.order }, { it.name })).map { it.id }, layouts.map { it.id })
    }

    @Test
    fun letterKeysAreNotDuplicated() {
        for (layout in load().layouts) {
            val labels = layout.layers.getValue(layout.defaultLayer).rows.flatten()
                .filterIsInstance<CharKey>().map { it.label }
            assertEquals("${layout.id} has duplicated keys: $labels", labels.size, labels.toSet().size)
        }
    }

    @Test
    fun layoutsCoverTheirAlphabets() {
        val layouts = load().layouts.associateBy { it.id }
        for ((id, alphabet) in alphabets) {
            val layout = layouts[id] ?: error("layout $id is missing")
            val keys = layout.layers.getValue(layout.defaultLayer).rows.flatten().filterIsInstance<CharKey>()
            val typable = keys.flatMap { listOf(it.label) + it.hold }.toSet()
            val missing = alphabet.map { it.toString() }.filter { it !in typable }
            assertTrue("$id cannot type: $missing", missing.isEmpty())
        }
    }

    @Test
    fun everyBuiltInLayoutHasAnAlphabetCheck() {
        assertEquals(load().layouts.map { it.id }.toSet(), alphabets.keys)
    }

    @Test
    fun uppercaseOfLettersFollowsLocale() {
        val layouts = load().layouts.associateBy { it.id }
        val kazakh = layouts.getValue("kk").layers.getValue("letters").rows[0].map { (it as CharKey).shifted }
        assertEquals(listOf("Ә", "І", "Ң", "Ғ", "Ү", "Ұ", "Қ", "Ө", "Һ"), kazakh)
    }

    @Test
    fun cyrillicLayoutsContainNoLatinLookalikes() {
        val layouts = load().layouts.associateBy { it.id }
        for (id in listOf("ru", "uk", "be", "kk", "kk-compact", "kk-anmitalidev-1a")) {
            val layout = layouts.getValue(id)
            val keys = layout.layers.getValue(layout.defaultLayer).rows.flatten().filterIsInstance<CharKey>()
            val letters = keys.flatMap { listOf(it.label) + it.hold }.flatMap { it.toList() }
                .filter { it.isLetter() && it != 'ʼ' }
            val foreign = letters.filter { it.code !in 0x0400..0x052F }
            assertTrue("$id contains non-Cyrillic letters: $foreign", foreign.isEmpty())
        }
    }

    @Test
    fun compactKazakhLayoutKeepsFourRowsAndUsesLongPress() {
        val layout = load().layouts.single { it.id == "kk-compact" }
        val rows = layout.layers.getValue("letters").rows
        assertEquals(4, rows.size)
        val plain = rows.flatten().filterIsInstance<CharKey>().map { it.label }.toSet()
        for (letter in "әғқңөүұһі") assertTrue("$letter must not be a plain key", letter.toString() !in plain)
    }

    @Test
    fun reducedKazakhLayoutHasExactlyItsAlphabet() {
        val layout = load().layouts.single { it.id == "kk-anmitalidev-1a" }
        val keys = layout.layers.getValue("letters").rows.flatten().filterIsInstance<CharKey>()
        val letters = keys.map { it.label }.filter { it.single().isLetter() }
        assertEquals(31, letters.size)
        assertEquals(letters.size, letters.toSet().size)
        assertEquals(alphabets.getValue("kk-anmitalidev-1a").toSet(), letters.toSet().flatMap { it.toList() }.toSet())
        assertTrue(keys.all { it.hold.isEmpty() })
        for (letter in "вёцчщъьэюяф") {
            assertTrue("$letter must not be typable", letters.none { it == letter.toString() })
        }
    }
}
