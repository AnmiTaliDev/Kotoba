package dev.anmitali.kotoba.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.anmitali.kotoba.settings.IntSetting

@Composable
fun SliderSettingItem(
    title: String,
    value: Int,
    setting: IntSetting,
    valueLabel: String,
    onChange: (Int) -> Unit,
) {
    ListItem(
        colors = TransparentListItem(),
        headlineContent = { Text(title) },
        supportingContent = {
            Slider(
                value = value.toFloat(),
                onValueChange = { onChange(it.toInt()) },
                valueRange = setting.min.toFloat()..setting.max.toFloat(),
            )
        },
        trailingContent = { Text(valueLabel) },
    )
}

@Composable
fun SwitchSettingItem(title: String, body: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        colors = TransparentListItem(),
        headlineContent = { Text(title) },
        supportingContent = { Text(body) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                thumbContent = if (checked) {
                    { Icon(Icons.Filled.Check, contentDescription = null) }
                } else {
                    null
                },
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoiceSettingItem(title: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    ListItem(
        colors = TransparentListItem(),
        headlineContent = { Text(title) },
        supportingContent = {
            SingleChoiceSegmentedButtonRow(modifier = Modifier) {
                options.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = index == selected,
                        onClick = { onSelect(index) },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    ) { Text(label) }
                }
            }
        },
    )
}
