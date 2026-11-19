package dev.anmitali.kotoba.ime

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodSubtype
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dev.anmitali.kotoba.emoji.EmojiRepository
import dev.anmitali.kotoba.input.EnterAction
import dev.anmitali.kotoba.input.ShiftState
import dev.anmitali.kotoba.input.allowsAutoCapitalize
import dev.anmitali.kotoba.input.enterActionOf
import dev.anmitali.kotoba.input.shiftAfterEdit
import dev.anmitali.kotoba.input.spaceEdit
import dev.anmitali.kotoba.input.tapShift
import dev.anmitali.kotoba.layout.KeyboardLayout
import dev.anmitali.kotoba.layout.LayoutRepository
import dev.anmitali.kotoba.settings.SettingsActivity
import dev.anmitali.kotoba.settings.SettingsRepository
import dev.anmitali.kotoba.ui.keyboard.KeyboardActions
import dev.anmitali.kotoba.ui.keyboard.KeyboardPreviewArea
import dev.anmitali.kotoba.ui.keyboard.KeyboardScreen
import dev.anmitali.kotoba.ui.keyboard.KeyboardState
import dev.anmitali.kotoba.ui.theme.KotobaTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val CONTEXT_CHARS = 16
private const val TAG = "Kotoba"

class KotobaInputMethodService : InputMethodService(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val settingsRepository by lazy { SettingsRepository(this) }
    private lateinit var state: KeyboardState

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    private val actions = KeyboardActions(
        onChar = ::commit,
        onEmoji = ::commitEmoji,
        onShift = ::tapShiftKey,
        onDelete = ::deleteBackward,
        onEnter = ::enter,
        onSpace = ::space,
        onLayer = { state.layerId = it },
        onOpenSettings = ::openSettings,
    )

    override fun onCreate() {
        super.onCreate()
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        state = KeyboardState(loadLayouts(), EmojiRepository(assets).load())
        SubtypeRegistrar.register(this, state.layouts)
        scope.launch { settingsRepository.settings.collect { state.settings = it } }
        scope.launch { settingsRepository.recentEmoji.collect { state.recentEmoji = it } }
    }

    override fun onDestroy() {
        scope.cancel()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onComputeInsets(outInsets: Insets) {
        updateNavigationBarHeight()
        super.onComputeInsets(outInsets)
        val previewArea = Density(this).run { KeyboardPreviewArea.roundToPx() }
        outInsets.contentTopInsets += previewArea
        outInsets.visibleTopInsets += previewArea
    }

    override fun onCreateInputView(): View {
        val decor = window.window!!.decorView
        decor.setViewTreeLifecycleOwner(this)
        decor.setViewTreeSavedStateRegistryOwner(this)
        return ComposeView(this).apply {
            setContent {
                KotobaTheme {
                    KeyboardScreen(state = state, actions = actions)
                }
            }
        }
    }

    override fun onCurrentInputMethodSubtypeChanged(newSubtype: InputMethodSubtype?) {
        super.onCurrentInputMethodSubtypeChanged(newSubtype)
        applySubtype(newSubtype)
    }

    private fun applySubtype(subtype: InputMethodSubtype?) {
        SubtypeRegistrar.layoutIdOf(subtype)?.let(state::selectLayout)
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        applySubtype(getSystemService(InputMethodManager::class.java).currentInputMethodSubtype)
        if (!restarting) {
            state.layerId = state.layout.defaultLayer
            state.shift = ShiftState.Off
            state.toolsOpen = false
            state.resetEmojiCategory()
        }
        state.enterAction = enterActionOf(info.imeOptions)
        updateNavigationBarHeight()
        refreshShift()
    }

    private fun updateNavigationBarHeight() {
        val insets = window.window?.decorView?.rootWindowInsets
        val fromInsets = insets?.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars())?.bottom ?: 0
        val height = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            fromInsets
        } else {
            maxOf(fromInsets, systemDimension("navigation_bar_frame_height"))
        }
        if (state.navigationBarHeightPx != height) state.navigationBarHeightPx = height
    }

    private fun systemDimension(name: String): Int {
        val id = resources.getIdentifier(name, "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else 0
    }

    private fun loadLayouts(): List<KeyboardLayout> {
        val loaded = LayoutRepository(assets).loadAll()
        for (failure in loaded.failures) {
            val details = failure.issues.joinToString("; ") { "${it.path}: ${it.message}" }
            Log.w(TAG, "Layout ${failure.origin} skipped: $details")
        }
        check(loaded.layouts.isNotEmpty()) { "No valid keyboard layouts" }
        return loaded.layouts
    }

    private fun openSettings() {
        state.toolsOpen = false
        val intent = Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun commitEmoji(emoji: String) {
        commit(emoji)
        scope.launch { settingsRepository.recordEmoji(emoji) }
    }

    private fun commit(text: String) {
        val ic = currentInputConnection ?: return
        ic.commitText(text, 1)
        refreshShift()
    }

    private fun tapShiftKey() {
        val now = SystemClock.uptimeMillis()
        state.shift = tapShift(state.shift, now - state.lastShiftTapAt)
        state.lastShiftTapAt = now
    }

    private fun deleteBackward() {
        val ic = currentInputConnection ?: return
        if (ic.getSelectedText(0).isNullOrEmpty()) {
            ic.deleteSurroundingTextInCodePoints(1, 0)
        } else {
            ic.commitText("", 1)
        }
        refreshShift()
    }

    private fun space() {
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(CONTEXT_CHARS, 0) ?: ""
        val edit = spaceEdit(before, state.layout.doubleSpace)
        ic.beginBatchEdit()
        if (edit.deleteBefore > 0) ic.deleteSurroundingText(edit.deleteBefore, 0)
        ic.commitText(edit.insert, 1)
        ic.endBatchEdit()
        refreshShift()
    }

    private fun enter() {
        if (state.enterAction == EnterAction.Newline) {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        } else {
            val ic = currentInputConnection ?: return
            ic.performEditorAction(currentInputEditorInfo.imeOptions and EditorInfo.IME_MASK_ACTION)
        }
        refreshShift()
    }

    private fun refreshShift() {
        val ic = currentInputConnection ?: return
        val autoCapitalize = state.layout.autoCapitalize && allowsAutoCapitalize(currentInputEditorInfo.inputType)
        val before = if (autoCapitalize) ic.getTextBeforeCursor(CONTEXT_CHARS, 0) ?: "" else ""
        state.shift = shiftAfterEdit(
            current = state.shift,
            textBefore = before,
            sentenceEnd = state.layout.sentenceEnd,
            autoCapitalize = autoCapitalize,
        )
    }
}
