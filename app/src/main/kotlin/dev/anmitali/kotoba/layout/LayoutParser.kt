package dev.anmitali.kotoba.layout

import java.util.Locale
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
            layers = layers,
        )
    }

    private fun parseLayer(json: JSONObject, locale: Locale): Layer {
        val rowsJson = json.getJSONArray("rows")
        val rows = List(rowsJson.length()) { r ->
            val rowJson = rowsJson.getJSONArray(r)
            List(rowJson.length()) { k -> parseKey(rowJson.get(k), locale) }
        }
        return Layer(shiftable = json.getBoolean("shiftable"), rows = rows)
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
