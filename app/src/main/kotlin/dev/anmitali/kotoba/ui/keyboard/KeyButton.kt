package dev.anmitali.kotoba.ui.keyboard

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val REPEAT_DELAY_MS = 420L
private const val REPEAT_INTERVAL_MS = 55L
private val PressedCorner = 28.dp

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
    onPreviewShow: (KeyPreviewTarget) -> Unit = {},
    onPreviewHide: (KeyPreviewTarget) -> Unit = {},
    body: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val corner by animateDpAsState(if (pressed) PressedCorner else cornerRadius, label = "keyCorner")
    val shape = if (pill) CircleShape else RoundedCornerShape(corner)
    val currentTap by rememberUpdatedState(onTap)
    val currentPreviewText by rememberUpdatedState(previewText)
    val currentShow by rememberUpdatedState(onPreviewShow)
    val currentHide by rememberUpdatedState(onPreviewHide)
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .weight(weight)
            .fillMaxHeight()
            .onGloballyPositioned { coordinates = it }
            .clip(shape)
            .background(container)
            .indication(interaction, LocalIndication.current)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = description
                onClick { currentTap(); true }
            }
            .pointerInput(repeatOnHold) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val press = PressInteraction.Press(down.position)
                    interaction.tryEmit(press)
                    val target = currentPreviewText?.let { text ->
                        coordinates?.let { KeyPreviewTarget(text, it.boundsInRoot()) }
                    }
                    if (target != null) currentShow(target)
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
                    if (target != null) currentHide(target)
                    interaction.tryEmit(
                        if (up != null) PressInteraction.Release(press) else PressInteraction.Cancel(press),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides content) { body() }
    }
}
