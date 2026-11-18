package dev.anmitali.kotoba.ui.keyboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import dev.anmitali.kotoba.input.EnterAction
import dev.anmitali.kotoba.input.ShiftState
import dev.anmitali.kotoba.layout.KeyboardLayout

class KeyPreviewTarget(val text: String, val bounds: Rect)

class KeyboardState(val layouts: List<KeyboardLayout>) {
    var layoutIndex by mutableIntStateOf(0)
    var layerId by mutableStateOf(layouts[0].defaultLayer)
    var shift by mutableStateOf(ShiftState.Off)
    var enterAction by mutableStateOf(EnterAction.Newline)
    var preview by mutableStateOf<KeyPreviewTarget?>(null)
    var navigationBarHeightPx by mutableIntStateOf(0)
    var lastShiftTapAt = 0L

    val layout: KeyboardLayout get() = layouts[layoutIndex]
}
