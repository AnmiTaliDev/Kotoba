package dev.anmitali.kotoba.layout

import java.io.InputStream
import org.json.JSONObject

object LayoutImport {

    const val CandidateOrigin = "import"

    sealed interface Result {
        class Valid(val layout: KeyboardLayout, val warnings: List<LayoutIssue>) : Result
        class Invalid(val issues: List<LayoutIssue>) : Result
    }

    fun readBounded(input: InputStream, limit: Int = LayoutLoader.MaxFileBytes): String? {
        val buffer = java.io.ByteArrayOutputStream()
        val chunk = ByteArray(8192)
        while (true) {
            val read = input.read(chunk)
            if (read < 0) break
            buffer.write(chunk, 0, read)
            if (buffer.size() > limit) return null
        }
        return buffer.toString(Charsets.UTF_8)
    }

    fun tooLarge(): Result =
        Result.Invalid(listOf(LayoutIssue("$", "file is larger than ${LayoutLoader.MaxFileBytes} bytes", Severity.Error)))

    fun check(text: String, builtIn: List<LayoutSource>, user: List<LayoutSource>): Result {
        val candidateId = runCatching { JSONObject(text).optString("id") }.getOrDefault("")
        val others = user.filter { it.origin != UserLayoutStore.originOf(candidateId) }
        val candidate = LayoutSource(CandidateOrigin, text, builtIn = false)
        val loaded = LayoutLoader.load(builtIn + others + candidate)
        loaded.failures.firstOrNull { it.origin == CandidateOrigin }?.let { return Result.Invalid(it.issues) }
        val layout = loaded.layouts.firstOrNull { loaded.sources[it.id]?.origin == CandidateOrigin }
            ?: return Result.Invalid(listOf(LayoutIssue("$", "layout was not loaded", Severity.Error)))
        val warnings = loaded.warnings.firstOrNull { it.origin == CandidateOrigin }?.issues.orEmpty()
        return Result.Valid(layout, warnings)
    }
}
