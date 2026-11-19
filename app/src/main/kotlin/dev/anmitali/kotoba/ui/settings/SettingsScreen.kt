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
                    title = stringResource(R.string.layout_title),
                    items = listOf(
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
