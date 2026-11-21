package dev.anmitali.kotoba.ui.keyboard

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import dev.anmitali.kotoba.settings.FlagSetting
import dev.anmitali.kotoba.settings.IntSetting
import dev.anmitali.kotoba.settings.KeyboardSettings
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.anmitali.kotoba.R
import dev.anmitali.kotoba.engine.EnterAction
import dev.anmitali.kotoba.engine.Key
import dev.anmitali.kotoba.engine.Layer
import dev.anmitali.kotoba.engine.LayerKind
import dev.anmitali.kotoba.engine.ShiftState
import kotlin.math.roundToInt

data class KeyboardActions(
    val onChar: (String) -> Unit,
    val onEmoji: (String) -> Unit,
    val onShift: () -> Unit,
    val onDelete: () -> Unit,
    val onEnter: () -> Unit,
    val onSpace: () -> Unit,
    val onMoveCursor: (Int) -> Unit,
    val onDeleteWords: (Int) -> Unit,
    val onLayer: (String) -> Unit,
    val onSwitchLanguage: () -> Unit,
    val onOpenSettings: () -> Unit,
)

private const val LayerFadeMs = 0
private val PreviewHeight = 60.dp
private val PreviewGap = 8.dp
val KeyboardPreviewArea = PreviewHeight + PreviewGap
private val AlternativeCellHeight = 52.dp
private const val DefaultBodyRows = 4
private val ToolbarHeight = 36.dp
private val ToolbarButtonSize = 32.dp
private val BodyPadding = PaddingValues(start = 4.dp, top = 4.dp, end = 4.dp, bottom = 6.dp)
private val BodyVerticalPadding = 10.dp

private const val NumberRowWeight = 0.75f

private fun bodyHeight(settings: KeyboardSettings, rows: Int, compactFirst: Boolean = false): Dp =
    settings.int(IntSetting.KeyHeight).dp * (if (compactFirst) rows - (1f - NumberRowWeight) else rows.toFloat()) +
        settings.int(IntSetting.RowGap).dp * (rows - 1) + BodyVerticalPadding

@Composable
fun KeyboardScreen(
    state: KeyboardState,
    actions: KeyboardActions,
    modifier: Modifier = Modifier,
) {
    val keyCornerRadius = state.settings.keyCornerRadiusDp.dp
    val navigationBarHeight = with(LocalDensity.current) { state.navigationBarHeightPx.toDp() } +
        state.settings.int(IntSetting.BottomOffset).dp
    val outerDensity = LocalDensity.current
    val scaledDensity = Density(outerDensity.density, outerDensity.fontScale * state.settings.int(IntSetting.LabelScale) / 100f)
    val widthFraction = state.settings.int(IntSetting.Width) / 100f
    val columnAlignment = when (state.settings.int(IntSetting.Alignment)) {
        0 -> Alignment.BottomStart
        2 -> Alignment.BottomEnd
        else -> Alignment.BottomCenter
    }
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Ltr,
        LocalKeyHaptics provides state.settings.haptics,
        LocalDensity provides scaledDensity,
    ) {
        Box(modifier = modifier.fillMaxWidth()) {
            Column(modifier = Modifier.align(columnAlignment).fillMaxWidth(widthFraction)) {
                Spacer(Modifier.height(KeyboardPreviewArea))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(bottom = navigationBarHeight),
                ) {
                    if (state.settings.flag(FlagSetting.Toolbar)) {
                        Toolbar(open = state.toolsOpen, onToggle = { state.toolsOpen = !state.toolsOpen })
                    }
                    if (state.toolsOpen) {
                        ToolsPanel(actions = actions, height = bodyHeight(state.settings, DefaultBodyRows))
                    } else Crossfade(
                        targetState = state.session.layerId,
                        animationSpec = tween(durationMillis = LayerFadeMs),
                        label = "keyboardLayer",
                    ) { layerId ->
                        val layer = state.layerOf(layerId)
                        if (layer.kind == LayerKind.EMOJI) {
                            EmojiPanel(state, layer, actions, bodyHeight(state.settings, DefaultBodyRows), keyCornerRadius)
                            return@Crossfade
                        }
                        val showNumberRow = state.settings.numberRow && layer.numberRow &&
                            state.layout.numberRow.isNotEmpty()
                        val layerRows = if (state.languageSwitchVisible) {
                            layer.rows
                        } else {
                            layer.rows.map { row -> row.filter { it !is Key.Language } }
                        }
                        val rows = if (showNumberRow) listOf(state.layout.numberRow) + layerRows else layerRows
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(bodyHeight(state.settings, rows.size, showNumberRow))
                                .padding(BodyPadding),
                            verticalArrangement = Arrangement.spacedBy(state.settings.int(IntSetting.RowGap).dp),
                        ) {
                            rows.forEachIndexed { index, row ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(if (showNumberRow && index == 0) NumberRowWeight else 1f),
                                    horizontalArrangement = Arrangement.spacedBy(state.settings.int(IntSetting.KeyGap).dp),
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
            state.alternates?.let { AlternativesPopup(it) }
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
private fun AlternativesPopup(popup: AlternatesPopup) {
    val density = LocalDensity.current
    val gap = with(density) { PreviewGap.roundToPx() }
    val cellWidth = with(density) { popup.cellWidthPx.toDp() }
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.large,
        shadowElevation = 3.dp,
        modifier = Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
            layout(0, 0) {
                placeable.place(popup.left.roundToInt(), (popup.bounds.top - placeable.height - gap).roundToInt())
            }
        },
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            popup.items.forEachIndexed { index, item ->
                val selected = index == popup.selected
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(width = cellWidth, height = AlternativeCellHeight)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            MaterialTheme.shapes.medium,
                        ),
                ) {
                    Text(
                        text = item,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyPreview(target: KeyPreviewTarget) {
    val gap = with(LocalDensity.current) { PreviewGap.roundToPx() }
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.large,
        shadowElevation = 3.dp,
        modifier = Modifier
            .sizeIn(minWidth = 42.dp, minHeight = PreviewHeight)
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
        is Key.Char -> {
            val shifted = layer.id == state.session.layerId && state.session.shiftApplies
            val text = key.text(shifted)
            KeyButton(
                weight = key.weight,
                description = text,
                container = keyContainer,
                content = colors.onSurface,
                cornerRadius = cornerRadius,
                onTap = { actions.onChar(text) },
                previewText = text.takeIf { state.settings.flag(FlagSetting.KeyPreview) },
                alternatives = key.alternatives(shifted),
                onAlternativesChange = { state.alternates = it },
                onAlternative = actions.onChar,
                onPreviewShow = { state.preview = it },
                onPreviewHide = { if (state.preview === it) state.preview = null },
            ) {
                val hint = key.alternatives(shifted).firstOrNull()?.takeIf { state.settings.flag(FlagSetting.Hints) }
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(text = text, style = MaterialTheme.typography.titleLarge)
                    if (hint != null) {
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.align(Alignment.TopEnd).padding(top = 3.dp, end = 5.dp),
                        )
                    }
                }
            }
        }

        is Key.Shift -> {
            val icon: ImageVector
            val container: Color
            val content: Color
            when (state.session.shift) {
                ShiftState.OFF -> {
                    icon = ShiftOffIcon
                    container = colors.secondaryContainer
                    content = colors.onSecondaryContainer
                }
                ShiftState.ONCE -> {
                    icon = ShiftOnceIcon
                    container = colors.primaryContainer
                    content = colors.onPrimaryContainer
                }
                ShiftState.CAPS -> {
                    icon = ShiftCapsIcon
                    container = colors.primary
                    content = colors.onPrimary
                }
            }
            KeyButton(
                weight = key.weight,
                description = stringResource(
                    if (state.session.shift == ShiftState.CAPS) R.string.key_caps_lock else R.string.key_shift,
                ),
                container = container,
                content = content,
                cornerRadius = cornerRadius,
                onTap = actions.onShift,
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = content)
            }
        }

        is Key.Delete -> KeyButton(
            weight = key.weight,
            description = stringResource(R.string.key_delete),
            container = colors.secondaryContainer,
            content = colors.onSecondaryContainer,
            cornerRadius = cornerRadius,
            onTap = actions.onDelete,
            repeatOnHold = true,
            onDragStep = actions.onDeleteWords,
        ) {
            Icon(imageVector = BackspaceIcon, contentDescription = null, tint = colors.onSecondaryContainer)
        }

        is Key.Enter -> {
            val action = state.session.enterAction
            val isAction = action != EnterAction.NEWLINE
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

        is Key.Space -> KeyButton(
            weight = key.weight,
            description = stringResource(R.string.key_space),
            container = keyContainer,
            content = colors.onSurfaceVariant,
            cornerRadius = cornerRadius,
            onTap = actions.onSpace,
            onDragStep = actions.onMoveCursor,
        ) {
            Text(
                text = state.layout.name,
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurfaceVariant,
            )
        }

        is Key.Layer -> KeyButton(
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

        is Key.Language -> KeyButton(
            weight = key.weight,
            description = key.label,
            container = colors.secondaryContainer,
            content = colors.onSecondaryContainer,
            cornerRadius = cornerRadius,
            onTap = actions.onSwitchLanguage,
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

        is Key.Spacer -> Spacer(Modifier.weight(key.weight))
    }
}

private fun enterDescription(action: EnterAction): Int = when (action) {
    EnterAction.NEWLINE -> R.string.key_enter
    EnterAction.GO -> R.string.key_go
    EnterAction.SEARCH -> R.string.key_search
    EnterAction.SEND -> R.string.key_send
    EnterAction.NEXT -> R.string.key_next
    EnterAction.DONE -> R.string.key_done
}

private fun enterIcon(action: EnterAction): ImageVector = when (action) {
    EnterAction.NEWLINE -> ReturnIcon
    EnterAction.GO, EnterAction.NEXT -> Icons.AutoMirrored.Filled.ArrowForward
    EnterAction.SEARCH -> Icons.Filled.Search
    EnterAction.SEND -> Icons.AutoMirrored.Filled.Send
    EnterAction.DONE -> Icons.Filled.Check
}
