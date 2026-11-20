package dev.anmitali.kotoba.ui.keyboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import dev.anmitali.kotoba.engine.EmojiCategory
import dev.anmitali.kotoba.engine.KeyboardLayout
import dev.anmitali.kotoba.engine.Layer
import dev.anmitali.kotoba.engine.SessionState
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
    session: SessionState,
) {
    var layouts by mutableStateOf(layouts)
        private set
    var session by mutableStateOf(session)
        private set
    var emojiCategory by mutableIntStateOf(0)
    var recentEmoji by mutableStateOf<List<String>>(emptyList())
    var settings by mutableStateOf(KeyboardSettings())
    var toolsOpen by mutableStateOf(false)
    var preview by mutableStateOf<KeyPreviewTarget?>(null)
    var alternates by mutableStateOf<AlternatesPopup?>(null)
    var navigationBarHeightPx by mutableIntStateOf(0)
    var languageSwitchVisible by mutableStateOf(false)

    val layout: KeyboardLayout get() = layouts.first { it.id == session.layoutId }

    fun layerOf(id: String): Layer = layout.layers.first { it.id == id }

    fun update(session: SessionState) {
        this.session = session
    }

    fun replaceLayouts(newLayouts: List<KeyboardLayout>, session: SessionState) {
        layouts = newLayouts
        this.session = session
    }

    fun resetEmojiCategory() {
        val firstFilled = emojiCategories.indexOfFirst { !it.recent }.coerceAtLeast(0)
        emojiCategory = if (recentEmoji.isNotEmpty()) emojiCategories.indexOfFirst { it.recent }.coerceAtLeast(0) else firstFilled
    }
}
