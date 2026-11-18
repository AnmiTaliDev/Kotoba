package dev.anmitali.kotoba.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import dev.anmitali.kotoba.R
import dev.anmitali.kotoba.layout.Layer

private val EmojiCellSize = 44.dp
private val EmojiBarHeight = 48.dp
private val TabCorner = 12.dp

@Composable
fun EmojiPanel(
    state: KeyboardState,
    layer: Layer,
    actions: KeyboardActions,
    height: Dp,
    cornerRadius: Dp,
) {
    val colors = MaterialTheme.colorScheme
    val categories = state.emojiCategories
    val selected = state.emojiCategory.coerceIn(0, categories.lastIndex)
    Column(modifier = Modifier.fillMaxWidth().height(height)) {
        key(selected) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(EmojiCellSize),
                state = rememberLazyGridState(),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
            ) {
                items(categories[selected].emoji) { emoji ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .height(EmojiCellSize)
                            .clip(RoundedCornerShape(TabCorner))
                            .clickable { actions.onChar(emoji) },
                    ) {
                        Text(text = emoji, fontSize = 26.sp)
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(EmojiBarHeight)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            layer.back?.let { back ->
                KeyButton(
                    weight = 1.4f,
                    description = back.label,
                    container = colors.secondaryContainer,
                    content = colors.onSecondaryContainer,
                    cornerRadius = cornerRadius,
                    onTap = { actions.onLayer(back.target) },
                ) {
                    Text(text = back.label, style = MaterialTheme.typography.labelLarge)
                }
            }
            categories.forEachIndexed { index, category ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(TabCorner))
                        .background(if (index == selected) colors.secondaryContainer else Color.Transparent)
                        .clickable { state.emojiCategory = index }
                        .semantics { contentDescription = category.name },
                ) {
                    Text(text = category.icon, fontSize = 18.sp)
                }
            }
            KeyButton(
                weight = 1.4f,
                description = stringResource(R.string.key_delete),
                container = colors.secondaryContainer,
                content = colors.onSecondaryContainer,
                cornerRadius = cornerRadius,
                onTap = actions.onDelete,
                repeatOnHold = true,
            ) {
                Icon(imageVector = BackspaceIcon, contentDescription = null, tint = colors.onSecondaryContainer)
            }
        }
    }
}
