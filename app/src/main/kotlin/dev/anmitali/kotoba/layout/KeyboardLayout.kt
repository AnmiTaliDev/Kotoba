package dev.anmitali.kotoba.layout

data class KeyboardLayout(
    val id: String,
    val name: String,
    val locale: String,
    val defaultLayer: String,
    val sentenceEnd: String,
    val doubleSpace: String,
    val autoCapitalize: Boolean,
    val order: Int,
    val numberRow: List<Key>,
    val layers: Map<String, Layer>,
)

enum class LayerKind { Keys, Emoji }

data class Layer(
    val kind: LayerKind,
    val shiftable: Boolean,
    val numberRow: Boolean,
    val rows: List<List<Key>>,
    val back: LayerKey?,
)

sealed interface Key {
    val weight: Float
}

data class CharKey(
    val label: String,
    val shifted: String,
    override val weight: Float,
    val hold: List<String> = emptyList(),
    val holdShifted: List<String> = emptyList(),
) : Key

data class ShiftKey(override val weight: Float) : Key

data class DeleteKey(override val weight: Float) : Key

data class EnterKey(override val weight: Float) : Key

data class SpaceKey(override val weight: Float) : Key

data class LayerKey(
    val label: String,
    val target: String,
    val icon: String?,
    override val weight: Float,
) : Key

data class SpacerKey(override val weight: Float) : Key
