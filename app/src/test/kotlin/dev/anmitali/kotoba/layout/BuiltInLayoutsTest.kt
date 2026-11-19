package dev.anmitali.kotoba.layout

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltInLayoutsTest {

    private val directory = File("src/main/assets/layouts")

    @Test
    fun everyBuiltInLayoutLoadsWithoutErrors() {
        val files = directory.listFiles { f -> f.extension == "json" }.orEmpty().sortedBy { it.name }
        assertTrue("no built-in layouts found in ${directory.absolutePath}", files.isNotEmpty())
        val sources = files.map { LayoutSource(it.name, it.readText(), builtIn = true) }
        val loaded = LayoutLoader.load(sources)
        val details = loaded.failures.joinToString("\n") { f ->
            "${f.origin}: " + f.issues.joinToString { "${it.path} ${it.message}" }
        }
        assertTrue(details, loaded.failures.isEmpty())
        assertTrue(loaded.layouts.size == files.size)
    }
}
