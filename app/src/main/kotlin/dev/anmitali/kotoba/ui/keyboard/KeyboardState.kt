package dev.anmitali.kotoba.ui.keyboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import dev.anmitali.kotoba.emoji.EmojiCategory
import dev.anmitali.kotoba.input.EnterAction
import dev.anmitali.kotoba.input.ShiftState
import dev.anmitali.kotoba.layout.KeyboardLayout
import dev.anmitali.kotoba.settings.KeyboardSettings

class KeyPreviewTarget(val text: String, val bounds: Rect)

data class AlternatesPopup(
    val bounds: Rect,
    val items: List<String>,
    val left: Float,
    val cellWidthPx: Float,
    val selected: Int,
)

class KeyboardState(
    layouts: List<KeyboardLayout>,
    val emojiCategories: List<EmojiCategory>,
) {
    var layouts by mutableStateOf(layouts)
        private set
    var emojiCategory by mutableIntStateOf(0)
    var recentEmoji by mutableStateOf<List<String>>(emptyList())
    var layoutIndex by mutableIntStateOf(0)
    var layerId by mutableStateOf(layouts[0].defaultLayer)
    var shift by mutableStateOf(ShiftState.Off)
    var enterAction by mutableStateOf(EnterAction.Newline)
    var settings by mutableStateOf(KeyboardSettings())
    var toolsOpen by mutableStateOf(false)
    var preview by mutableStateOf<KeyPreviewTarget?>(null)
    var alternates by mutableStateOf<AlternatesPopup?>(null)
    var navigationBarHeightPx by mutableIntStateOf(0)
    var lastShiftTapAt = 0L

    val layout: KeyboardLayout get() = layouts[layoutIndex]

    fun replaceLayouts(newLayouts: List<KeyboardLayout>) {
        val currentId = layout.id
        layouts = newLayouts
        layoutIndex = newLayouts.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
        if (layerId !in layout.layers) layerId = layout.defaultLayer
    }

    fun selectLayout(id: String) {
        val index = layouts.indexOfFirst { it.id == id }
        if (index < 0 || index == layoutIndex) return
        layoutIndex = index
        layerId = layout.defaultLayer
        shift = ShiftState.Off
    }

    fun resetEmojiCategory() {
        val firstFilled = emojiCategories.indexOfFirst { !it.recent }.coerceAtLeast(0)
        emojiCategory = if (recentEmoji.isNotEmpty()) emojiCategories.indexOfFirst { it.recent }.coerceAtLeast(0) else firstFilled
    }
}
