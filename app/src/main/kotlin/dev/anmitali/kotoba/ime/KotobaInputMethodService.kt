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
import android.view.inputmethod.InputConnection
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
import dev.anmitali.kotoba.engine.Command
import dev.anmitali.kotoba.engine.Context
import dev.anmitali.kotoba.engine.Engine
import dev.anmitali.kotoba.engine.EngineException
import dev.anmitali.kotoba.engine.Event
import dev.anmitali.kotoba.engine.emojiCategories
import dev.anmitali.kotoba.input.allowsAutoCapitalize
import dev.anmitali.kotoba.input.enterActionOf
import dev.anmitali.kotoba.layout.LayoutRepository
import dev.anmitali.kotoba.layout.UserLayoutStore
import dev.anmitali.kotoba.settings.SettingsActivity
import dev.anmitali.kotoba.settings.SettingsRepository
import dev.anmitali.kotoba.ui.keyboard.KeyboardActions
import dev.anmitali.kotoba.ui.keyboard.KeyboardPreviewArea
import dev.anmitali.kotoba.ui.keyboard.KeyboardScreen
import dev.anmitali.kotoba.ui.keyboard.KeyboardState
import dev.anmitali.kotoba.ui.theme.KotobaTheme
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

private const val CONTEXT_CHARS = 16
private const val TAG = "Kotoba"
const val UserLayoutsDirectory = "layouts"

class KotobaInputMethodService : InputMethodService(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val settingsRepository by lazy { SettingsRepository(this) }
    private val layoutRepository by lazy { LayoutRepository(UserLayoutStore(File(filesDir, UserLayoutsDirectory))) }
    private lateinit var engine: Engine
    private lateinit var state: KeyboardState

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    private val actions = KeyboardActions(
        onChar = { dispatch(Event.Char(it)) },
        onEmoji = ::onEmoji,
        onShift = { dispatch(Event.Shift) },
        onDelete = { dispatch(Event.Delete) },
        onEnter = { dispatch(Event.Enter) },
        onSpace = { dispatch(Event.Space) },
        onMoveCursor = ::moveCursor,
        onDeleteWords = ::deleteWords,
        onEditAction = ::performEditAction,
        onLayer = { dispatch(Event.SwitchLayer(it)) },
        onSwitchLanguage = { dispatch(Event.SwitchLanguage) },
        onOpenSettings = ::openSettings,
    )

    private fun moveCursor(steps: Int) {
        val connection = currentInputConnection ?: return
        val code = if (steps < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
        connection.beginBatchEdit()
        repeat(kotlin.math.abs(steps)) {
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
        }
        connection.endBatchEdit()
    }

    private fun performEditAction(id: Int) {
        val connection = currentInputConnection ?: return
        connection.performContextMenuAction(id)
        if (engine.needsTextAfterEdit()) engine.afterEdit(textBeforeCursor())
        state.update(engine.state())
    }

    private fun deleteWords(steps: Int) {
        val connection = currentInputConnection ?: return
        connection.beginBatchEdit()
        repeat(kotlin.math.abs(steps)) {
            val text = connection.getTextBeforeCursor(CONTEXT_CHARS, 0)?.toString().orEmpty()
            val count = wordLengthBefore(text)
            if (count > 0) connection.deleteSurroundingTextInCodePoints(count, 0)
        }
        connection.endBatchEdit()
        if (engine.needsTextAfterEdit()) engine.afterEdit(textBeforeCursor())
        state.update(engine.state())
    }

    private fun wordLengthBefore(text: String): Int {
        var index = text.length
        var count = 0
        while (index > 0) {
            val cp = text.codePointBefore(index)
            if (!Character.isWhitespace(cp)) break
            index -= Character.charCount(cp)
            count++
        }
        if (index == 0) return count
        val wordClass = Character.isLetterOrDigit(text.codePointBefore(index))
        while (index > 0) {
            val cp = text.codePointBefore(index)
            if (Character.isWhitespace(cp) || Character.isLetterOrDigit(cp) != wordClass) break
            index -= Character.charCount(cp)
            count++
        }
        return count
    }

    override fun onCreate() {
        super.onCreate()
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        engine = createEngine()
        engine.preferLanguage(resources.configuration.locales[0].language)
        state = KeyboardState(engine.layouts(), emojiCategories(), engine.state())
        SubtypeRegistrar.register(this, state.layouts)
        scope.launch { settingsRepository.settings.collect { state.settings = it } }
        scope.launch {
            settingsRepository.recentEmoji.collect {
                engine.setRecents(it)
                state.recentEmoji = it
            }
        }
        scope.launch { settingsRepository.layoutsRevision.drop(1).collect { reloadLayouts() } }
    }

    override fun onDestroy() {
        scope.cancel()
        engine.close()
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
        updateLanguageSwitch()
        val id = SubtypeRegistrar.layoutIdOf(subtype) ?: return
        engine.selectLayout(id)
        state.update(engine.state())
    }

    private fun updateLanguageSwitch() {
        state.languageSwitchVisible = SubtypeRegistrar.enabledSubtypeCount(this) > 1
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        applySubtype(getSystemService(InputMethodManager::class.java).currentInputMethodSubtype)
        engine.startInput(enterActionOf(info.imeOptions), allowsAutoCapitalize(info.inputType), restarting)
        if (engine.needsTextAfterEdit()) engine.afterEdit(textBeforeCursor())
        state.update(engine.state())
        if (!restarting) {
            state.toolsOpen = false
            state.resetEmojiCategory()
        }
        updateNavigationBarHeight()
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

    private fun createEngine(): Engine {
        val engine = try {
            Engine(layoutRepository.allSources())
        } catch (error: EngineException) {
            error("No valid keyboard layouts: ${error.message}")
        }
        logLayoutProblems(engine)
        return engine
    }

    private fun reloadLayouts() {
        engine.reload(layoutRepository.allSources())
        logLayoutProblems(engine)
        state.replaceLayouts(engine.layouts(), engine.state())
        SubtypeRegistrar.register(this, state.layouts)
    }

    private fun logLayoutProblems(engine: Engine) {
        for (failure in engine.failures()) {
            val details = failure.issues.joinToString("; ") { "${it.path}: ${it.message}" }
            Log.w(TAG, "Layout ${failure.origin} skipped: $details")
        }
    }

    private fun openSettings() {
        state.toolsOpen = false
        val intent = Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun onEmoji(emoji: String) {
        dispatch(Event.Emoji(emoji))
        val recents = engine.recents()
        state.recentEmoji = recents
        scope.launch { settingsRepository.saveRecentEmoji(recents) }
    }

    private fun textBeforeCursor(): String =
        currentInputConnection?.getTextBeforeCursor(CONTEXT_CHARS, 0)?.toString() ?: ""

    private fun dispatch(event: Event) {
        val connection = currentInputConnection
        val editing = event !is Event.Shift && event !is Event.SwitchLayer
        if (editing && connection == null) return
        val needs = engine.needs(event)
        val context = Context(
            nowMs = SystemClock.uptimeMillis(),
            textBefore = if (needs.textBefore) textBeforeCursor() else "",
            hasSelection = needs.selection && !connection?.getSelectedText(0).isNullOrEmpty(),
        )
        val outcome = engine.press(event, context)
        if (connection != null) outcome.commands.forEach { apply(connection, it) }
        if (outcome.needsTextAfterEdit) engine.afterEdit(textBeforeCursor())
        state.update(engine.state())
    }

    private fun apply(connection: InputConnection, command: Command) {
        when (command) {
            is Command.Commit -> connection.commitText(command.text, 1)
            is Command.DeleteBefore -> connection.deleteSurroundingTextInCodePoints(command.codePoints, 0)
            Command.BeginBatch -> connection.beginBatchEdit()
            Command.EndBatch -> connection.endBatchEdit()
            Command.SendEnterKey -> sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
            Command.SwitchLanguage -> switchToNextInputMethod(true)
            Command.PerformEditorAction ->
                connection.performEditorAction(currentInputEditorInfo.imeOptions and EditorInfo.IME_MASK_ACTION)
        }
    }
}
