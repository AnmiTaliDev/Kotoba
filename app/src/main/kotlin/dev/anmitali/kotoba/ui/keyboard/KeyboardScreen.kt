package dev.anmitali.kotoba.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import dev.anmitali.kotoba.layout.ShiftKey
import dev.anmitali.kotoba.layout.SpaceKey
import dev.anmitali.kotoba.layout.SpacerKey

data class KeyboardActions(
    val onChar: (String) -> Unit,
    val onShift: () -> Unit,
    val onDelete: () -> Unit,
    val onEnter: () -> Unit,
    val onSpace: () -> Unit,
    val onLayer: (String) -> Unit,
)

private val BodyHeight = 216.dp
private val BodyPadding = PaddingValues(start = 4.dp, top = 4.dp, end = 4.dp, bottom = 6.dp)
private val KeyGap = 4.dp
private val RowGap = 6.dp
private val DefaultKeyCorner = 14.dp

@Composable
fun KeyboardScreen(
    state: KeyboardState,
    actions: KeyboardActions,
    modifier: Modifier = Modifier,
    keyCornerRadius: Dp = DefaultKeyCorner,
) {
    val layer = state.layout.layers.getValue(state.layerId)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(BodyHeight)
                .padding(BodyPadding),
            verticalArrangement = Arrangement.spacedBy(RowGap),
        ) {
            for (row in layer.rows) {
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
            Text(
                text = key.label,
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSecondaryContainer,
            )
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
