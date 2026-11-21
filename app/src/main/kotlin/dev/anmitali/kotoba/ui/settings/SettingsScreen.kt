package dev.anmitali.kotoba.ui.settings

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.anmitali.kotoba.R
import dev.anmitali.kotoba.settings.FlagSetting
import dev.anmitali.kotoba.settings.IntSetting
import dev.anmitali.kotoba.settings.KeyboardSettings
import dev.anmitali.kotoba.settings.LayoutsActivity
import dev.anmitali.kotoba.settings.SettingsRepository
import kotlinx.coroutines.launch

private val RadiusPreviewSize = 40.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(repository: SettingsRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by repository.settings.collectAsState(initial = KeyboardSettings())
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                SettingsGroup(
                    title = stringResource(R.string.appearance_title),
                    items = listOf {
                        ListItem(
                            colors = TransparentListItem(),
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(RadiusPreviewSize)
                                        .background(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            RoundedCornerShape(settings.keyCornerRadiusDp.dp),
                                        ),
                                )
                            },
                            headlineContent = { Text(stringResource(R.string.key_radius_title)) },
                            supportingContent = {
                                Slider(
                                    value = settings.keyCornerRadiusDp.toFloat(),
                                    onValueChange = { scope.launch { repository.setKeyCornerRadius(it.toInt()) } },
                                    valueRange = KeyboardSettings.MinKeyCornerRadiusDp.toFloat()..
                                        KeyboardSettings.MaxKeyCornerRadiusDp.toFloat(),
                                )
                            },
                            trailingContent = {
                                Text(stringResource(R.string.key_radius_value, settings.keyCornerRadiusDp))
                            },
                        )
                    },
                )
            }
            item {
                SettingsGroup(
                    title = stringResource(R.string.size_title),
                    items = listOf(
                        {
                            SliderSettingItem(
                                title = stringResource(R.string.key_height_title),
                                value = settings.int(IntSetting.KeyHeight),
                                setting = IntSetting.KeyHeight,
                                valueLabel = stringResource(R.string.dp_value, settings.int(IntSetting.KeyHeight)),
                            ) { scope.launch { repository.setInt(IntSetting.KeyHeight, it) } }
                        },
                        {
                            SliderSettingItem(
                                title = stringResource(R.string.key_gap_title),
                                value = settings.int(IntSetting.KeyGap),
                                setting = IntSetting.KeyGap,
                                valueLabel = stringResource(R.string.dp_value, settings.int(IntSetting.KeyGap)),
                            ) { scope.launch { repository.setInt(IntSetting.KeyGap, it) } }
                        },
                        {
                            SliderSettingItem(
                                title = stringResource(R.string.row_gap_title),
                                value = settings.int(IntSetting.RowGap),
                                setting = IntSetting.RowGap,
                                valueLabel = stringResource(R.string.dp_value, settings.int(IntSetting.RowGap)),
                            ) { scope.launch { repository.setInt(IntSetting.RowGap, it) } }
                        },
                        {
                            SliderSettingItem(
                                title = stringResource(R.string.label_scale_title),
                                value = settings.int(IntSetting.LabelScale),
                                setting = IntSetting.LabelScale,
                                valueLabel = stringResource(R.string.percent_value, settings.int(IntSetting.LabelScale)),
                            ) { scope.launch { repository.setInt(IntSetting.LabelScale, it) } }
                        },
                        {
                            SliderSettingItem(
                                title = stringResource(R.string.bottom_offset_title),
                                value = settings.int(IntSetting.BottomOffset),
                                setting = IntSetting.BottomOffset,
                                valueLabel = stringResource(R.string.dp_value, settings.int(IntSetting.BottomOffset)),
                            ) { scope.launch { repository.setInt(IntSetting.BottomOffset, it) } }
                        },
                    ),
                )
            }
            item {
                SettingsGroup(
                    title = stringResource(R.string.one_handed_title),
                    items = listOf(
                        {
                            SliderSettingItem(
                                title = stringResource(R.string.width_title),
                                value = settings.int(IntSetting.Width),
                                setting = IntSetting.Width,
                                valueLabel = stringResource(R.string.percent_value, settings.int(IntSetting.Width)),
                            ) { scope.launch { repository.setInt(IntSetting.Width, it) } }
                        },
                        {
                            ChoiceSettingItem(
                                title = stringResource(R.string.alignment_title),
                                options = listOf(
                                    stringResource(R.string.alignment_left),
                                    stringResource(R.string.alignment_center),
                                    stringResource(R.string.alignment_right),
                                ),
                                selected = settings.int(IntSetting.Alignment),
                            ) { scope.launch { repository.setInt(IntSetting.Alignment, it) } }
                        },
                    ),
                )
            }
            item {
                SettingsGroup(
                    title = stringResource(R.string.behavior_title),
                    items = listOf(
                        {
                            SwitchSettingItem(
                                title = stringResource(R.string.toolbar_title),
                                body = stringResource(R.string.toolbar_body),
                                checked = settings.flag(FlagSetting.Toolbar),
                            ) { scope.launch { repository.setFlag(FlagSetting.Toolbar, it) } }
                        },
                        {
                            SwitchSettingItem(
                                title = stringResource(R.string.key_preview_title),
                                body = stringResource(R.string.key_preview_body),
                                checked = settings.flag(FlagSetting.KeyPreview),
                            ) { scope.launch { repository.setFlag(FlagSetting.KeyPreview, it) } }
                        },
                        {
                            SwitchSettingItem(
                                title = stringResource(R.string.hints_title),
                                body = stringResource(R.string.hints_body),
                                checked = settings.flag(FlagSetting.Hints),
                            ) { scope.launch { repository.setFlag(FlagSetting.Hints, it) } }
                        },
                        {
                            ListItem(
                                modifier = Modifier.clickable { scope.launch { repository.resetAppearance() } },
                                colors = TransparentListItem(),
                                headlineContent = { Text(stringResource(R.string.reset_title)) },
                                supportingContent = { Text(stringResource(R.string.reset_body)) },
                            )
                        },
                    ),
                )
            }
            item {
                SettingsGroup(
                    title = stringResource(R.string.layout_title),
                    items = listOf(
                        {
                            ListItem(
                                colors = TransparentListItem(),
                                headlineContent = { Text(stringResource(R.string.haptics_title)) },
                                supportingContent = { Text(stringResource(R.string.haptics_body)) },
                                trailingContent = {
                                    Switch(
                                        checked = settings.haptics,
                                        onCheckedChange = { scope.launch { repository.setHaptics(it) } },
                                        thumbContent = if (settings.haptics) {
                                            { Icon(Icons.Filled.Check, contentDescription = null) }
                                        } else {
                                            null
                                        },
                                    )
                                },
                            )
                        },
                        {
                            ListItem(
                                colors = TransparentListItem(),
                                headlineContent = { Text(stringResource(R.string.number_row_title)) },
                                supportingContent = { Text(stringResource(R.string.number_row_body)) },
                                trailingContent = {
                                    Switch(
                                        checked = settings.numberRow,
                                        onCheckedChange = { scope.launch { repository.setNumberRow(it) } },
                                        thumbContent = if (settings.numberRow) {
                                            { Icon(Icons.Filled.Check, contentDescription = null) }
                                        } else {
                                            null
                                        },
                                    )
                                },
                            )
                        },
                        {
                            ListItem(
                                modifier = Modifier.clickable {
                                    context.startActivity(Intent(context, LayoutsActivity::class.java))
                                },
                                colors = TransparentListItem(),
                                headlineContent = { Text(stringResource(R.string.layouts_nav_title)) },
                                supportingContent = { Text(stringResource(R.string.layouts_nav_body)) },
                                trailingContent = {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                                },
                            )
                        },
                    ),
                )
            }
        }
    }
}
