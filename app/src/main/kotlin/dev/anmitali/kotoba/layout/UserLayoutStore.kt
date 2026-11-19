package dev.anmitali.kotoba.layout

import java.io.File

class UserLayoutFile(val id: String, val text: String)

class UserLayoutStore(private val directory: File) {

    fun files(): List<UserLayoutFile> =
        directory.listFiles { f -> f.isFile && f.extension == "json" }.orEmpty()
            .sortedBy { it.name }
            .mapNotNull { file ->
                val text = runCatching { file.readText() }.getOrNull() ?: return@mapNotNull null
                UserLayoutFile(file.nameWithoutExtension, text)
            }

    fun save(id: String, text: String) {
        directory.mkdirs()
        val target = File(directory, "$id.json")
        val temporary = File(directory, "$id.json.tmp")
        temporary.writeText(text)
        check(temporary.renameTo(target)) { "Cannot write layout $id" }
    }

    fun delete(id: String): Boolean = File(directory, "$id.json").delete()
}
