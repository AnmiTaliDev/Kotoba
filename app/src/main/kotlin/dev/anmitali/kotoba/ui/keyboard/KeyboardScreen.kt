package dev.anmitali.kotoba.ui.keyboard

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.anmitali.kotoba.R
import dev.anmitali.kotoba.input.EnterAction
import dev.anmitali.kotoba.input.ShiftState
import dev.anmitali.kotoba.input.outputOf
import dev.anmitali.kotoba.layout.CharKey
import dev.anmitali.kotoba.layout.DeleteKey
import dev.anmitali.kotoba.layout.EnterKey
import dev.anmitali.kotoba.layout.Key
import dev.anmitali.kotoba.layout.Layer
import dev.anmitali.kotoba.layout.LayerKey
import dev.anmitali.kotoba.layout.LayerKind
import dev.anmitali.kotoba.layout.ShiftKey
import dev.anmitali.kotoba.layout.SpaceKey
import dev.anmitali.kotoba.layout.SpacerKey

data class KeyboardActions(
    val onChar: (String) -> Unit,
    val onEmoji: (String) -> Unit,
    val onShift: () -> Unit,
    val onDelete: () -> Unit,
    val onEnter: () -> Unit,
    val onSpace: () -> Unit,
    val onLayer: (String) -> Unit,
    val onOpenSettings: () -> Unit,
)

private const val LayerFadeMs = 150
private val PreviewHeight = 60.dp
private val PreviewGap = 8.dp
val KeyboardPreviewArea = PreviewHeight + PreviewGap
private val KeyRowHeight = 47.dp
private const val DefaultBodyRows = 4
private val ToolbarHeight = 48.dp
private val ToolbarButtonSize = 40.dp
private val BodyPadding = PaddingValues(start = 4.dp, top = 4.dp, end = 4.dp, bottom = 6.dp)
private val KeyGap = 4.dp
private val RowGap = 6.dp
private val BodyVerticalPadding = 10.dp

private fun bodyHeight(rows: Int): Dp = KeyRowHeight * rows + RowGap * (rows - 1) + BodyVerticalPadding
private val BodyHeight = bodyHeight(DefaultBodyRows)

@Composable
fun KeyboardScreen(
    state: KeyboardState,
    actions: KeyboardActions,
    modifier: Modifier = Modifier,
) {
    val keyCornerRadius = state.settings.keyCornerRadiusDp.dp
    val navigationBarHeight = with(LocalDensity.current) { state.navigationBarHeightPx.toDp() }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier = modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.height(KeyboardPreviewArea))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(bottom = navigationBarHeight),
                ) {
                    Toolbar(open = state.toolsOpen, onToggle = { state.toolsOpen = !state.toolsOpen })
                    if (state.toolsOpen) {
                        ToolsPanel(actions = actions, height = BodyHeight)
                    } else Crossfade(
                        targetState = state.layerId,
                        animationSpec = tween(durationMillis = LayerFadeMs),
                        label = "keyboardLayer",
                    ) { layerId ->
                        val layer = state.layout.layers.getValue(layerId)
                        if (layer.kind == LayerKind.Emoji) {
                            EmojiPanel(state, layer, actions, BodyHeight, keyCornerRadius)
                            return@Crossfade
                        }
                        val showNumberRow = state.settings.numberRow && layer.numberRow &&
                            state.layout.numberRow.isNotEmpty()
                        val rows = if (showNumberRow) listOf(state.layout.numberRow) + layer.rows else layer.rows
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(bodyHeight(rows.size))
                                .padding(BodyPadding),
                            verticalArrangement = Arrangement.spacedBy(RowGap),
                        ) {
                            for (row in rows) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(KeyGap),
                                ) {
                                    for (key in row) {
                                        KeyCell(key, layer, state, actions, keyCornerRadius)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            state.preview?.let { KeyPreview(it) }
        }
    }
}

@Composable
private fun Toolbar(open: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ToolbarHeight)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconToggleButton(
            checked = open,
            onCheckedChange = { onToggle() },
            modifier = Modifier.size(ToolbarButtonSize),
        ) {
            Icon(
                imageVector = if (open) Icons.Filled.Close else Icons.Filled.Build,
                contentDescription = stringResource(if (open) R.string.tools_close else R.string.tools_open),
            )
        }
    }
}

@Composable
private fun KeyPreview(target: KeyPreviewTarget) {
    val gap = with(LocalDensity.current) { PreviewGap.roundToPx() }
    val appear = remember { Animatable(0f) }
    LaunchedEffect(target) {
        appear.snapTo(0f)
        appear.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh))
    }
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.large,
        shadowElevation = 3.dp,
        modifier = Modifier
            .sizeIn(minWidth = 42.dp, minHeight = PreviewHeight)
            .graphicsLayer {
                val progress = appear.value
                alpha = progress.coerceIn(0f, 1f)
                scaleX = 0.8f + 0.2f * progress
                scaleY = 0.8f + 0.2f * progress
                transformOrigin = TransformOrigin(0.5f, 1f)
            }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                layout(0, 0) {
                    val x = (target.bounds.center.x - placeable.width / 2f).roundToInt()
                        .coerceIn(0, (constraints.maxWidth - placeable.width).coerceAtLeast(0))
                    val y = (target.bounds.top - placeable.height - gap).roundToInt()
                    placeable.place(x, y)
                }
            },
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
            Text(text = target.text, style = MaterialTheme.typography.headlineLarge)
        }
    }
}

@Composable
private fun RowScope.KeyCell(
    key: Key,
    layer: Layer,
    state: KeyboardState,
    actions: KeyboardActions,
    cornerRadius: Dp,
) {
    val colors = MaterialTheme.colorScheme
    val keyContainer = if (isSystemInDarkTheme()) colors.surfaceContainerHighest else colors.surface
    when (key) {
        is CharKey -> {
            val text = outputOf(key, layer, state.shift)
            KeyButton(
                weight = key.weight,
                description = text,
                container = keyContainer,
                content = colors.onSurface,
                cornerRadius = cornerRadius,
                onTap = { actions.onChar(text) },
                previewText = text,
                onPreviewShow = { state.preview = it },
                onPreviewHide = { if (state.preview === it) state.preview = null },
            ) {
                Text(text = text, style = MaterialTheme.typography.titleLarge)
            }
        }

        is ShiftKey -> {
            val icon: ImageVector
            val container: Color
            val content: Color
            when (state.shift) {
                ShiftState.Off -> {
                    icon = ShiftOffIcon
                    container = colors.secondaryContainer
                    content = colors.onSecondaryContainer
                }
                ShiftState.Once -> {
                    icon = ShiftOnceIcon
                    container = colors.primaryContainer
                    content = colors.onPrimaryContainer
                }
                ShiftState.Caps -> {
                    icon = ShiftCapsIcon
                    container = colors.primary
                    content = colors.onPrimary
                }
            }
            KeyButton(
                weight = key.weight,
                description = stringResource(
                    if (state.shift == ShiftState.Caps) R.string.key_caps_lock else R.string.key_shift,
                ),
                container = container,
                content = content,
                cornerRadius = cornerRadius,
                onTap = actions.onShift,
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = content)
            }
        }

        is DeleteKey -> KeyButton(
            weight = key.weight,
            description = stringResource(R.string.key_delete),
            container = colors.secondaryContainer,
            content = colors.onSecondaryContainer,
            cornerRadius = cornerRadius,
            onTap = actions.onDelete,
            repeatOnHold = true,
        ) {
            Icon(imageVector = BackspaceIcon, contentDescription = null, tint = colors.onSecondaryContainer)
        }

        is EnterKey -> {
            val action = state.enterAction
            val isAction = action != EnterAction.Newline
            val container = if (isAction) colors.primary else colors.secondaryContainer
            val content = if (isAction) colors.onPrimary else colors.onSecondaryContainer
            KeyButton(
                weight = key.weight,
                description = stringResource(enterDescription(action)),
                container = container,
                content = content,
                cornerRadius = cornerRadius,
                onTap = actions.onEnter,
                pill = true,
            ) {
                Icon(imageVector = enterIcon(action), contentDescription = null, tint = content)
            }
        }

        is SpaceKey -> KeyButton(
            weight = key.weight,
            description = stringResource(R.string.key_space),
            container = keyContainer,
            content = colors.onSurfaceVariant,
            cornerRadius = cornerRadius,
            onTap = actions.onSpace,
        ) {
            Text(
                text = state.layout.name,
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurfaceVariant,
            )
        }

        is LayerKey -> KeyButton(
            weight = key.weight,
            description = key.label,
            container = colors.secondaryContainer,
            content = colors.onSecondaryContainer,
            cornerRadius = cornerRadius,
            onTap = { actions.onLayer(key.target) },
        ) {
            val iconPainter = key.icon?.let { namedIconPainterOrNull(it) }
            if (iconPainter != null) {
                Icon(
                    painter = iconPainter,
                    contentDescription = null,
                    tint = colors.onSecondaryContainer,
                )
            } else {
                Text(
                    text = key.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSecondaryContainer,
                )
            }
        }

        is SpacerKey -> Spacer(Modifier.weight(key.weight))
    }
}

private fun enterDescription(action: EnterAction): Int = when (action) {
    EnterAction.Newline -> R.string.key_enter
    EnterAction.Go -> R.string.key_go
    EnterAction.Search -> R.string.key_search
    EnterAction.Send -> R.string.key_send
    EnterAction.Next -> R.string.key_next
    EnterAction.Done -> R.string.key_done
}

private fun enterIcon(action: EnterAction): ImageVector = when (action) {
    EnterAction.Newline -> ReturnIcon
    EnterAction.Go, EnterAction.Next -> Icons.AutoMirrored.Filled.ArrowForward
    EnterAction.Search -> Icons.Filled.Search
    EnterAction.Send -> Icons.AutoMirrored.Filled.Send
    EnterAction.Done -> Icons.Filled.Check
}
