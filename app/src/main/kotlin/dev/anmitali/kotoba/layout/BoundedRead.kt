package dev.anmitali.kotoba.layout

import java.io.ByteArrayOutputStream
import java.io.InputStream

fun readBounded(input: InputStream, limit: Int): String? {
    val buffer = ByteArrayOutputStream()
    val chunk = ByteArray(8192)
    while (true) {
        val read = input.read(chunk)
        if (read < 0) break
        buffer.write(chunk, 0, read)
        if (buffer.size() > limit) return null
    }
    return buffer.toString(Charsets.UTF_8.name())
}
