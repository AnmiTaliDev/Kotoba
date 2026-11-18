package dev.anmitali.kotoba.layout

import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

object LayoutParser {

    fun parse(json: String): KeyboardLayout {
        val root = JSONObject(json)
        val locale = Locale.forLanguageTag(root.getString("locale"))
        val punctuation = root.getJSONObject("punctuation")
        val layersJson = root.getJSONObject("layers")
        val layers = LinkedHashMap<String, Layer>()
        for (id in layersJson.keys()) {
            layers[id] = parseLayer(layersJson.getJSONObject(id), locale)
        }
        val defaultLayer = root.getString("defaultLayer")
        require(defaultLayer in layers) { "Unknown default layer: $defaultLayer" }
        for (layer in layers.values) {
            layer.back?.let { require(it.target in layers) { "Unknown layer target: ${it.target}" } }
            for (row in layer.rows) {
                for (key in row) {
                    if (key is LayerKey) {
                        require(key.target in layers) { "Unknown layer target: ${key.target}" }
                    }
                }
            }
        }
        return KeyboardLayout(
            id = root.getString("id"),
            name = root.getString("name"),
            locale = root.getString("locale"),
            defaultLayer = defaultLayer,
            sentenceEnd = punctuation.getString("sentenceEnd"),
            doubleSpace = punctuation.getString("doubleSpace"),
            numberRow = parseRow(root.optJSONArray("numberRow"), locale),
            layers = layers,
        )
    }

    private fun parseLayer(json: JSONObject, locale: Locale): Layer {
        val rowsJson = json.optJSONArray("rows")
        val rows = if (rowsJson == null) {
            emptyList()
        } else {
            List(rowsJson.length()) { r -> parseRow(rowsJson.getJSONArray(r), locale) }
        }
        val kind = when (val name = json.optString("kind", "keys")) {
            "keys" -> LayerKind.Keys
            "emoji" -> LayerKind.Emoji
            else -> error("Unknown layer kind: $name")
        }
        val back = json.optJSONObject("back")?.let { LayerKey(it.getString("label"), it.getString("layer"), 1f) }
        require(kind != LayerKind.Emoji || back != null) { "Emoji layer requires back" }
        return Layer(
            kind = kind,
            back = back,
            shiftable = json.getBoolean("shiftable"),
            numberRow = json.optBoolean("numberRow", false),
            rows = rows,
        )
    }

    private fun parseRow(json: JSONArray?, locale: Locale): List<Key> {
        if (json == null) return emptyList()
        return List(json.length()) { k -> parseKey(json.get(k), locale) }
    }

    private fun parseKey(raw: Any, locale: Locale): Key {
        if (raw is String) {
            return CharKey(raw, raw.uppercase(locale), 1f)
        }
        val json = raw as JSONObject
        val weight = json.optDouble("weight", 1.0).toFloat()
        return when (val type = json.optString("type", "char")) {
            "char" -> {
                val label = json.getString("label")
                CharKey(label, json.optString("shifted", label.uppercase(locale)), weight)
            }
            "shift" -> ShiftKey(weight)
            "delete" -> DeleteKey(weight)
            "enter" -> EnterKey(weight)
            "space" -> SpaceKey(weight)
            "layer" -> LayerKey(json.getString("label"), json.getString("layer"), weight)
            "spacer" -> SpacerKey(weight)
            else -> error("Unknown key type: $type")
        }
    }
}
