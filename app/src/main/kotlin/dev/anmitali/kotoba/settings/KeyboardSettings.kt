package dev.anmitali.kotoba.settings

data class KeyboardSettings(
    val keyCornerRadiusDp: Int = DefaultKeyCornerRadiusDp,
    val numberRow: Boolean = DefaultNumberRow,
) {
    companion object {
        const val DefaultKeyCornerRadiusDp = 14
        const val DefaultNumberRow = true
        const val MinKeyCornerRadiusDp = 0
        const val MaxKeyCornerRadiusDp = 28
    }
}
