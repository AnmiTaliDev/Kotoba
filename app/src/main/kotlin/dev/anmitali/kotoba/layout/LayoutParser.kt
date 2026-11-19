package dev.anmitali.kotoba.layout

import java.util.IllformedLocaleException
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

class LayoutParseResult(val layout: KeyboardLayout?, val issues: List<LayoutIssue>)

object LayoutParser {

    const val SupportedFormatVersion = 1
    const val MaxKeys = 200
    const val MaxLabelLength = 8
    const val MaxKeyWeight = 10.0
    const val MaxRowWeight = 14.0

    private val IdPattern = Regex("[a-z0-9_-]{1,32}")

    fun parse(root: JSONObject): LayoutParseResult {
        val reader = Reader()
        val layout = build(root, reader)
        val hasErrors = reader.issues.any { it.severity == Severity.Error }
        return LayoutParseResult(if (hasErrors) null else layout, reader.issues)
    }

    private fun build(root: JSONObject, r: Reader): KeyboardLayout? {
        val version = r.optInt(root, "formatVersion", "$") ?: 1
        if (version < 1 || version > SupportedFormatVersion) {
            r.error("$.formatVersion", "unsupported format version $version, supported: $SupportedFormatVersion")
        }
        val id = r.string(root, "id", "$")
        if (id != null && !IdPattern.matches(id)) {
            r.error("$.id", "must match [a-z0-9_-]{1,32}")
        }
        val name = r.string(root, "name", "$")
        if (name != null && name.isBlank()) r.error("$.name", "must not be blank")
        val localeTag = r.string(root, "locale", "$")
        val locale = localeTag?.let { parseLocale(it, r) }
        val autoCapitalize = r.optBoolean(root, "autoCapitalize", "$") ?: true

        val punctuation = r.obj(root, "punctuation", "$")
        val sentenceEnd = punctuation?.let { r.string(it, "sentenceEnd", "$.punctuation") }
        val doubleSpace = punctuation?.let { r.string(it, "doubleSpace", "$.punctuation") }

        val counter = KeyCounter()
        val numberRow = r.optArray(root, "numberRow", "$")?.let { readRow(it, "$.numberRow", locale, r, counter) }
            ?: emptyList()

        val layersJson = r.obj(root, "layers", "$")
        val layers = LinkedHashMap<String, Layer>()
        if (layersJson != null) {
            if (layersJson.length() == 0) r.error("$.layers", "must contain at least one layer")
            for (layerId in layersJson.keys()) {
                val path = "$.layers.$layerId"
                if (!IdPattern.matches(layerId)) r.error(path, "layer id must match [a-z0-9_-]{1,32}")
                val layerJson = r.obj(layersJson, layerId, "$.layers") ?: continue
                readLayer(layerJson, path, locale, r, counter)?.let { layers[layerId] = it }
            }
        }
        if (counter.count > MaxKeys) r.error("$", "too many keys: ${counter.count}, maximum $MaxKeys")

        val defaultLayer = r.string(root, "defaultLayer", "$")
        if (defaultLayer != null && layersJson != null && defaultLayer !in layers) {
            r.error("$.defaultLayer", "unknown layer: $defaultLayer")
        }
        for ((layerId, layer) in layers) {
            layer.back?.let {
                if (it.target !in layers) r.error("$.layers.$layerId.back.layer", "unknown layer: ${it.target}")
            }
            layer.rows.forEachIndexed { rowIndex, row ->
                row.forEachIndexed { keyIndex, key ->
                    if (key is LayerKey && key.target !in layers) {
                        r.error("$.layers.$layerId.rows[$rowIndex][$keyIndex].layer", "unknown layer: ${key.target}")
                    }
                }
            }
        }
        val start = layers[defaultLayer]
        if (start != null) {
            val keys = start.rows.flatten()
            if (start.kind == LayerKind.Keys && keys.none { it is SpaceKey }) {
                r.error("$.layers.$defaultLayer", "default layer must contain a space key")
            }
            if (start.kind == LayerKind.Keys && keys.none { it is DeleteKey }) {
                r.warn("$.layers.$defaultLayer", "default layer has no delete key")
            }
        }

        if (id == null || name == null || localeTag == null || locale == null ||
            sentenceEnd == null || doubleSpace == null || defaultLayer == null
        ) {
            return null
        }
        return KeyboardLayout(
            id = id,
            name = name,
            locale = localeTag,
            defaultLayer = defaultLayer,
            sentenceEnd = sentenceEnd,
            doubleSpace = doubleSpace,
            autoCapitalize = autoCapitalize,
            numberRow = numberRow,
            layers = layers,
        )
    }

    private fun parseLocale(tag: String, r: Reader): Locale? = try {
        val locale = Locale.Builder().setLanguageTag(tag).build()
        if (locale.language.isEmpty()) {
            r.error("$.locale", "language is missing: $tag")
            null
        } else {
            locale
        }
    } catch (e: IllformedLocaleException) {
        r.error("$.locale", "not a valid BCP 47 tag: $tag")
        null
    }

    private fun readLayer(json: JSONObject, path: String, locale: Locale?, r: Reader, counter: KeyCounter): Layer? {
        val kind = when (val name = r.optString(json, "kind", path) ?: "keys") {
            "keys" -> LayerKind.Keys
            "emoji" -> LayerKind.Emoji
            else -> {
                r.error("$path.kind", "unknown layer kind: $name")
                return null
            }
        }
        val rowsJson = r.optArray(json, "rows", path)
        val rows = ArrayList<List<Key>>()
        if (rowsJson != null) {
            for (i in 0 until rowsJson.length()) {
                val rowPath = "$path.rows[$i]"
                val rowJson = rowsJson.optJSONArray(i)
                if (rowJson == null) {
                    r.error(rowPath, "must be an array")
                    continue
                }
                val row = readRow(rowJson, rowPath, locale, r, counter)
                if (row.isEmpty()) {
                    r.error(rowPath, "must not be empty")
                    continue
                }
                val weight = row.sumOf { it.weight.toDouble() }
                if (weight > MaxRowWeight) {
                    r.warn(rowPath, "row is wide: total weight $weight, keys may be too small")
                }
                rows += row
            }
        }
        val backJson = r.optObject(json, "back", path)
        val back = backJson?.let {
            val label = r.string(it, "label", "$path.back")
            val target = r.string(it, "layer", "$path.back")
            if (label != null && target != null) LayerKey(label, target, null, 1f) else null
        }
        if (kind == LayerKind.Keys && rows.isEmpty()) r.error("$path.rows", "must contain at least one row")
        if (kind == LayerKind.Emoji && back == null) r.error("$path.back", "emoji layer requires back")
        return Layer(
            kind = kind,
            shiftable = r.optBoolean(json, "shiftable", path) ?: false,
            numberRow = r.optBoolean(json, "numberRow", path) ?: false,
            rows = rows,
            back = back,
        )
    }

    private fun readRow(json: JSONArray, path: String, locale: Locale?, r: Reader, counter: KeyCounter): List<Key> {
        val keys = ArrayList<Key>(json.length())
        for (i in 0 until json.length()) {
            readKey(json.opt(i), "$path[$i]", locale, r)?.let {
                keys += it
                counter.count++
            }
        }
        return keys
    }

    private fun readKey(raw: Any?, path: String, locale: Locale?, r: Reader): Key? {
        if (raw is String) return charKey(raw, null, 1f, path, locale, r)
        if (raw !is JSONObject) {
            r.error(path, "must be a string or an object")
            return null
        }
        val weight = r.optNumber(raw, "weight", path) ?: 1.0
        if (weight <= 0 || weight > MaxKeyWeight) {
            r.error("$path.weight", "must be greater than 0 and at most $MaxKeyWeight")
            return null
        }
        val w = weight.toFloat()
        return when (val type = r.optString(raw, "type", path) ?: "char") {
            "char" -> {
                val label = r.string(raw, "label", path) ?: return null
                charKey(label, r.optString(raw, "shifted", path), w, path, locale, r)
            }
            "shift" -> ShiftKey(w)
            "delete" -> DeleteKey(w)
            "enter" -> EnterKey(w)
            "space" -> SpaceKey(w)
            "spacer" -> SpacerKey(w)
            "layer" -> {
                val label = r.string(raw, "label", path) ?: return null
                val target = r.string(raw, "layer", path) ?: return null
                LayerKey(label, target, r.optString(raw, "icon", path)?.ifEmpty { null }, w)
            }
            else -> {
                r.error("$path.type", "unknown key type: $type")
                null
            }
        }
    }

    private fun charKey(label: String, shifted: String?, weight: Float, path: String, locale: Locale?, r: Reader): Key? {
        if (label.isEmpty()) {
            r.error(path, "label must not be empty")
            return null
        }
        if (label.codePointCount(0, label.length) > MaxLabelLength) {
            r.error(path, "label is longer than $MaxLabelLength characters")
            return null
        }
        if (shifted != null && shifted.codePointCount(0, shifted.length) > MaxLabelLength) {
            r.error("$path.shifted", "is longer than $MaxLabelLength characters")
            return null
        }
        val upper = shifted ?: label.uppercase(locale ?: Locale.ROOT)
        return CharKey(label, upper, weight)
    }

    private class KeyCounter {
        var count = 0
    }

    private class Reader {
        val issues = ArrayList<LayoutIssue>()

        fun error(path: String, message: String) {
            issues += LayoutIssue(path, message, Severity.Error)
        }

        fun warn(path: String, message: String) {
            issues += LayoutIssue(path, message, Severity.Warning)
        }

        private fun present(obj: JSONObject, key: String): Any? {
            val v = obj.opt(key)
            return if (v == null || v == JSONObject.NULL) null else v
        }

        fun string(obj: JSONObject, key: String, path: String): String? {
            val v = present(obj, key)
            if (v == null) {
                error("$path.$key", "is required")
                return null
            }
            if (v !is String) {
                error("$path.$key", "must be a string")
                return null
            }
            return v
        }

        fun optString(obj: JSONObject, key: String, path: String): String? {
            val v = present(obj, key) ?: return null
            if (v !is String) {
                error("$path.$key", "must be a string")
                return null
            }
            return v
        }

        fun optBoolean(obj: JSONObject, key: String, path: String): Boolean? {
            val v = present(obj, key) ?: return null
            if (v !is Boolean) {
                error("$path.$key", "must be true or false")
                return null
            }
            return v
        }

        fun optInt(obj: JSONObject, key: String, path: String): Int? {
            val v = present(obj, key) ?: return null
            if (v !is Int) {
                error("$path.$key", "must be an integer")
                return null
            }
            return v
        }

        fun optNumber(obj: JSONObject, key: String, path: String): Double? {
            val v = present(obj, key) ?: return null
            if (v !is Number) {
                error("$path.$key", "must be a number")
                return null
            }
            return v.toDouble()
        }

        fun obj(obj: JSONObject, key: String, path: String): JSONObject? {
            val v = present(obj, key)
            if (v == null) {
                error("$path.$key", "is required")
                return null
            }
            if (v !is JSONObject) {
                error("$path.$key", "must be an object")
                return null
            }
            return v
        }

        fun optObject(obj: JSONObject, key: String, path: String): JSONObject? {
            val v = present(obj, key) ?: return null
            if (v !is JSONObject) {
                error("$path.$key", "must be an object")
                return null
            }
            return v
        }

        fun optArray(obj: JSONObject, key: String, path: String): JSONArray? {
            val v = present(obj, key) ?: return null
            if (v !is JSONArray) {
                error("$path.$key", "must be an array")
                return null
            }
            return v
        }
    }
}
