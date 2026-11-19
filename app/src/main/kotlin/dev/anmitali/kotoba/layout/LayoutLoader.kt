package dev.anmitali.kotoba.layout

import org.json.JSONException
import org.json.JSONObject

class LayoutSource(val origin: String, val text: String, val builtIn: Boolean)

class LayoutFailure(val origin: String, val issues: List<LayoutIssue>)

class LoadedLayouts(
    val layouts: List<KeyboardLayout>,
    val failures: List<LayoutFailure>,
    val warnings: List<LayoutFailure>,
    val sources: Map<String, LayoutSource>,
)

object LayoutLoader {

    const val MaxFileBytes = 256 * 1024

    fun load(sources: List<LayoutSource>): LoadedLayouts {
        val failures = ArrayList<LayoutFailure>()
        val warnings = ArrayList<LayoutFailure>()
        val loadedSources = HashMap<String, LayoutSource>()
        val parsed = ArrayList<Pair<LayoutSource, JSONObject>>()
        for (source in sources.sortedByDescending { it.builtIn }) {
            if (source.text.toByteArray().size > MaxFileBytes) {
                failures += failure(source, "$", "file is larger than $MaxFileBytes bytes")
                continue
            }
            val json = try {
                JSONObject(source.text)
            } catch (e: JSONException) {
                failures += failure(source, "$", "malformed JSON: ${e.message}")
                continue
            }
            parsed += source to json
        }

        val bases = HashMap<String, JSONObject>()
        for ((source, json) in parsed) {
            val id = json.optString("id")
            if (source.builtIn && id.isNotEmpty() && !json.has("extends")) bases[id] = json
        }

        val layouts = ArrayList<KeyboardLayout>()
        val usedIds = HashSet<String>()
        for ((source, json) in parsed) {
            val resolved = resolve(source, json, bases)
            if (resolved is Resolved.Failed) {
                failures += resolved.failure
                continue
            }
            val result = LayoutParser.parse((resolved as Resolved.Ok).json)
            val layout = result.layout
            if (layout == null) {
                failures += LayoutFailure(source.origin, result.issues)
                continue
            }
            if (!usedIds.add(layout.id)) {
                failures += LayoutFailure(
                    source.origin,
                    result.issues + LayoutIssue("$.id", "id is already used by another layout: ${layout.id}", Severity.Error),
                )
                continue
            }
            layouts += layout
            loadedSources[layout.id] = source
            if (result.issues.isNotEmpty()) warnings += LayoutFailure(source.origin, result.issues)
        }
        return LoadedLayouts(layouts, failures, warnings, loadedSources)
    }

    private sealed interface Resolved {
        class Ok(val json: JSONObject) : Resolved
        class Failed(val failure: LayoutFailure) : Resolved
    }

    private fun resolve(source: LayoutSource, json: JSONObject, bases: Map<String, JSONObject>): Resolved {
        if (!json.has("extends")) return Resolved.Ok(json)
        val baseId = json.opt("extends") as? String
            ?: return Resolved.Failed(failure(source, "$.extends", "must be a string"))
        val base = bases[baseId]
            ?: return Resolved.Failed(failure(source, "$.extends", "unknown or non-extendable base layout: $baseId"))
        return Resolved.Ok(merge(json, base))
    }

    private fun merge(child: JSONObject, base: JSONObject): JSONObject {
        val merged = JSONObject(base.toString())
        merged.remove("id")
        merged.remove("name")
        merged.remove("locale")
        for (key in child.keys()) {
            when (key) {
                "extends" -> Unit
                "layers", "punctuation" -> {
                    val value = child.get(key)
                    if (value is JSONObject) mergeInto(merged.getJSONObject(key), value) else merged.put(key, value)
                }
                else -> merged.put(key, child.get(key))
            }
        }
        return merged
    }

    private fun mergeInto(target: JSONObject, source: JSONObject) {
        for (key in source.keys()) target.put(key, source.get(key))
    }

    private fun failure(source: LayoutSource, path: String, message: String) =
        LayoutFailure(source.origin, listOf(LayoutIssue(path, message, Severity.Error)))
}
