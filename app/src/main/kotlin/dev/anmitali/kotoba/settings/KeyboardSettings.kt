package dev.anmitali.kotoba.settings

enum class IntSetting(val id: String, val default: Int, val min: Int, val max: Int) {
    KeyHeight("key_height_dp", 44, 32, 64),
    KeyGap("key_gap_dp", 3, 0, 10),
    RowGap("row_gap_dp", 4, 0, 12),
    LabelScale("label_scale_percent", 100, 80, 140),
    Width("width_percent", 100, 60, 100),
    Alignment("alignment", 1, 0, 2),
    BottomOffset("bottom_offset_dp", 0, 0, 48),
}

enum class FlagSetting(val id: String, val default: Boolean) {
    Toolbar("toolbar", true),
    KeyPreview("key_preview", true),
    Hints("hints", true),
}

data class KeyboardSettings(
    val keyCornerRadiusDp: Int = DefaultKeyCornerRadiusDp,
    val numberRow: Boolean = DefaultNumberRow,
    val haptics: Boolean = DefaultHaptics,
    val ints: Map<IntSetting, Int> = emptyMap(),
    val flags: Map<FlagSetting, Boolean> = emptyMap(),
) {
    fun int(setting: IntSetting): Int = ints[setting] ?: setting.default

    fun flag(setting: FlagSetting): Boolean = flags[setting] ?: setting.default

    companion object {
        const val DefaultKeyCornerRadiusDp = 14
        const val DefaultNumberRow = true
        const val DefaultHaptics = true
        const val MinKeyCornerRadiusDp = 0
        const val MaxKeyCornerRadiusDp = 28
    }
}
