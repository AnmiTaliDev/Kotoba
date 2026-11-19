package dev.anmitali.kotoba.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val GroupOuterCorner = 20.dp
private val GroupInnerCorner = 4.dp
private val GroupItemGap = 2.dp

@Composable
fun TransparentListItem() = ListItemDefaults.colors(containerColor = Color.Transparent)

@Composable
fun SettingsGroup(title: String, items: List<@Composable () -> Unit>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(GroupItemGap)) {
            items.forEachIndexed { index, item ->
                val top = if (index == 0) GroupOuterCorner else GroupInnerCorner
                val bottom = if (index == items.lastIndex) GroupOuterCorner else GroupInnerCorner
                Surface(
                    shape = RoundedCornerShape(top, top, bottom, bottom),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) { item() }
            }
        }
    }
}
