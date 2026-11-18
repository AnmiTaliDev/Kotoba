package dev.anmitali.kotoba.layout

data class KeyboardLayout(
    val id: String,
    val name: String,
    val locale: String,
    val defaultLayer: String,
    val sentenceEnd: String,
    val doubleSpace: String,
    val numberRow: List<Key>,
    val layers: Map<String, Layer>,
)

data class Layer(
    val shiftable: Boolean,
    val numberRow: Boolean,
    val rows: List<List<Key>>,
)

sealed interface Key {
    val weight: Float
}

data class CharKey(
    val label: String,
    val shifted: String,
    override val weight: Float,
) : Key

data class ShiftKey(override val weight: Float) : Key

data class DeleteKey(override val weight: Float) : Key

data class EnterKey(override val weight: Float) : Key

data class SpaceKey(override val weight: Float) : Key

data class LayerKey(
    val label: String,
    val target: String,
    override val weight: Float,
) : Key

data class SpacerKey(override val weight: Float) : Key
