package dev.anmitali.kotoba.settings

data class KeyboardSettings(
    val keyCornerRadiusDp: Int = DefaultKeyCornerRadiusDp,
    val numberRow: Boolean = false,
) {
    companion object {
        const val DefaultKeyCornerRadiusDp = 14
        const val MinKeyCornerRadiusDp = 0
        const val MaxKeyCornerRadiusDp = 28
    }
}
