package dev.anmitali.kotoba.settings

data class KeyboardSettings(
    val keyCornerRadiusDp: Int = DefaultKeyCornerRadiusDp,
    val numberRow: Boolean = DefaultNumberRow,
    val haptics: Boolean = DefaultHaptics,
) {
    companion object {
        const val DefaultKeyCornerRadiusDp = 14
        const val DefaultNumberRow = true
        const val DefaultHaptics = true
        const val MinKeyCornerRadiusDp = 0
        const val MaxKeyCornerRadiusDp = 28
    }
}
