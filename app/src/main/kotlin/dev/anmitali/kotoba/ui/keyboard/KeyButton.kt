package dev.anmitali.kotoba.ui.keyboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val REPEAT_DELAY_MS = 420L
private const val REPEAT_INTERVAL_MS = 55L
private const val ColorDurationMs = 60
val AlternativeCellWidth = 44.dp

private enum class HoldOutcome { Tap, Cancelled }

private suspend fun AwaitPointerEventScope.chooseAlternative(
    pointerId: PointerId,
    bounds: Rect,
    items: List<String>,
    cellWidth: Float,
    rootWidth: Float,
    onChange: (AlternatesPopup?) -> Unit,
    onPick: (String) -> Unit,
): Boolean {
    val left = (bounds.center.x - cellWidth / 2).coerceIn(0f, (rootWidth - cellWidth * items.size).coerceAtLeast(0f))
    fun indexAt(x: Float): Int = floor((x - left) / cellWidth).toInt().coerceIn(0, items.lastIndex)
    var selected = indexAt(bounds.center.x)
    onChange(AlternatesPopup(bounds, items, left, cellWidth, selected))
    try {
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == pointerId } ?: return false
            if (!change.pressed) {
                if (change.changedToUp()) {
                    onPick(items[selected])
                    return true
                }
                return false
            }
            val index = indexAt(bounds.left + change.position.x)
            if (index != selected) {
                selected = index
                onChange(AlternatesPopup(bounds, items, left, cellWidth, selected))
            }
            change.consume()
        }
    } finally {
        onChange(null)
    }
}

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
    alternatives: List<String> = emptyList(),
    onAlternativesChange: (AlternatesPopup?) -> Unit = {},
    onAlternative: (String) -> Unit = {},
    onPreviewShow: (KeyPreviewTarget) -> Unit = {},
    onPreviewHide: (KeyPreviewTarget) -> Unit = {},
    body: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val containerColor = animateColorAsState(
        targetValue = container,
        animationSpec = tween(durationMillis = ColorDurationMs),
        label = "keyContainer",
    )
    val currentTap by rememberUpdatedState(onTap)
    val currentPreviewText by rememberUpdatedState(previewText)
    val currentShow by rememberUpdatedState(onPreviewShow)
    val currentHide by rememberUpdatedState(onPreviewHide)
    val currentAlternatives by rememberUpdatedState(alternatives)
    val currentPopupChange by rememberUpdatedState(onAlternativesChange)
    val currentAlternative by rememberUpdatedState(onAlternative)
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .weight(weight)
            .fillMaxHeight()
            .onGloballyPositioned { coordinates = it }
            .graphicsLayer {
                shape = if (pill) CircleShape else RoundedCornerShape(cornerRadius)
                clip = true
            }
            .drawBehind { drawRect(containerColor.value) }
            .indication(interaction, LocalIndication.current)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = description
                onClick { currentTap(); true }
                customActions = alternatives.map { item ->
                    CustomAccessibilityAction(item) {
                        onAlternative(item)
                        true
                    }
                }
            }
            .pointerInput(repeatOnHold) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val press = PressInteraction.Press(down.position)
                    interaction.tryEmit(press)
                    val attached = coordinates?.takeIf { it.isAttached }
                    val bounds = attached?.boundsInRoot()
                    val target = currentPreviewText?.let { text -> bounds?.let { KeyPreviewTarget(text, it) } }
                    if (target != null) currentShow(target)
                    val items = currentAlternatives
                    val completed = if (items.isEmpty() || bounds == null || attached == null) {
                        currentTap()
                        val repeat = if (repeatOnHold) {
                            scope.launch {
                                delay(REPEAT_DELAY_MS)
                                while (true) {
                                    currentTap()
                                    delay(REPEAT_INTERVAL_MS)
                                }
                            }
                        } else {
                            null
                        }
                        val up = waitForUpOrCancellation()
                        repeat?.cancel()
                        up != null
                    } else {
                        val outcome = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                            if (waitForUpOrCancellation() != null) HoldOutcome.Tap else HoldOutcome.Cancelled
                        }
                        when (outcome) {
                            HoldOutcome.Tap -> {
                                currentTap()
                                true
                            }
                            HoldOutcome.Cancelled -> false
                            null -> {
                                if (target != null) currentHide(target)
                                chooseAlternative(
                                    pointerId = down.id,
                                    bounds = bounds,
                                    items = items,
                                    cellWidth = AlternativeCellWidth.toPx(),
                                    rootWidth = attached.findRootCoordinates().size.width.toFloat(),
                                    onChange = currentPopupChange,
                                    onPick = currentAlternative,
                                )
                            }
                        }
                    }
                    if (target != null) currentHide(target)
                    interaction.tryEmit(if (completed) PressInteraction.Release(press) else PressInteraction.Cancel(press))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides content) { body() }
    }
}
