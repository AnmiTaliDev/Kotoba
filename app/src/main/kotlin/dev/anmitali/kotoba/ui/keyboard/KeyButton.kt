package dev.anmitali.kotoba.ui.keyboard

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val REPEAT_DELAY_MS = 420L
private const val REPEAT_INTERVAL_MS = 55L
private val PressedCorner = 28.dp
private val PreviewGap = 8.dp

@Composable
fun RowScope.KeyButton(
    weight: Float,
    description: String,
    container: Color,
    content: Color,
    cornerRadius: Dp,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    pill: Boolean = false,
    repeatOnHold: Boolean = false,
    previewText: String? = null,
    body: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val corner by animateDpAsState(if (pressed) PressedCorner else cornerRadius, label = "keyCorner")
    val shape = if (pill) CircleShape else RoundedCornerShape(corner)
    val currentTap by rememberUpdatedState(onTap)
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .weight(weight)
            .fillMaxHeight()
            .clip(shape)
            .background(container)
            .indication(interaction, LocalIndication.current)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = description
                onClick { currentTap(); true }
            }
            .pointerInput(repeatOnHold) {
                detectTapGestures(
                    onPress = { offset ->
                        val press = PressInteraction.Press(offset)
                        interaction.emit(press)
                        val repeat = if (repeatOnHold) {
                            scope.launch {
                                currentTap()
                                delay(REPEAT_DELAY_MS)
                                while (true) {
                                    currentTap()
                                    delay(REPEAT_INTERVAL_MS)
                                }
                            }
                        } else {
                            null
                        }
                        val released = tryAwaitRelease()
                        repeat?.cancel()
                        interaction.emit(
                            if (released) PressInteraction.Release(press) else PressInteraction.Cancel(press),
                        )
                    },
                    onTap = { if (!repeatOnHold) currentTap() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides content) { body() }
        if (pressed && previewText != null) {
            KeyPreview(previewText)
        }
    }
}

@Composable
private fun KeyPreview(text: String) {
    val gap = with(LocalDensity.current) { PreviewGap.roundToPx() }
    val positionProvider = remember(gap) { AboveKeyPositionProvider(gap) }
    Popup(
        popupPositionProvider = positionProvider,
        properties = PopupProperties(focusable = false, clippingEnabled = false),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = MaterialTheme.shapes.large,
            shadowElevation = 3.dp,
            modifier = Modifier.sizeIn(minWidth = 42.dp, minHeight = 60.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
                Text(text = text, style = MaterialTheme.typography.headlineLarge)
            }
        }
    }
}

private class AboveKeyPositionProvider(private val gapPx: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = (anchorBounds.center.x - popupContentSize.width / 2)
            .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        return IntOffset(x, anchorBounds.top - popupContentSize.height - gapPx)
    }
}
