package dev.anmitali.kotoba.ui.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.anmitali.kotoba.R
import dev.anmitali.kotoba.ime.KotobaInputMethodService
import dev.anmitali.kotoba.engine.ImportResult
import dev.anmitali.kotoba.engine.KeyboardLayout
import dev.anmitali.kotoba.engine.LayoutIssue
import dev.anmitali.kotoba.engine.importTooLargeIssue
import dev.anmitali.kotoba.engine.maxLayoutFileBytes
import dev.anmitali.kotoba.layout.LayoutRepository
import dev.anmitali.kotoba.layout.LoadedLayouts
import dev.anmitali.kotoba.layout.readBounded
import dev.anmitali.kotoba.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private class ExportRequest(val fileName: String, val text: String)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LayoutsScreen(
    repository: LayoutRepository,
    settings: SettingsRepository,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var refresh by remember { mutableIntStateOf(0) }
    var loaded by remember { mutableStateOf<LoadedLayouts?>(null) }
    var rejected by remember { mutableStateOf<List<LayoutIssue>?>(null) }
    var deleteTarget by remember { mutableStateOf<KeyboardLayout?>(null) }
    var exportRequest by remember { mutableStateOf<ExportRequest?>(null) }

    LaunchedEffect(refresh) {
        loaded = withContext(Dispatchers.IO) { repository.loadAll() }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) { importFrom(context, repository, uri) }
            when (result) {
                is ImportResult.Valid -> {
                    settings.bumpLayoutsRevision()
                    refresh++
                    snackbar.showSnackbar(context.getString(R.string.layout_imported, result.layout.name))
                }
                is ImportResult.Invalid -> rejected = result.issues
            }
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val request = exportRequest
        exportRequest = null
        if (uri == null || request == null) return@rememberLauncherForActivityResult
        scope.launch {
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { it.write(request.text.toByteArray()) }
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.layouts_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val data = loaded
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { importLauncher.launch(arrayOf("*/*")) }) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.layouts_import), modifier = Modifier.padding(start = 8.dp))
                    }
                    FilledTonalButton(onClick = { openSubtypeSettings(context) }) {
                        Text(stringResource(R.string.layouts_enable))
                    }
                }
            }
            if (data != null) {
                val (builtIn, user) = data.layouts.partition { data.sources[it.id]?.builtIn == true }
                if (user.isNotEmpty()) {
                    item {
                        SettingsGroup(
                            title = stringResource(R.string.layouts_user),
                            items = user.map { layout ->
                                {
                                    LayoutRow(
                                        layout = layout,
                                        onExport = {
                                            data.sources[layout.id]?.let {
                                                exportRequest = ExportRequest("${layout.id}.json", it.text)
                                                exportLauncher.launch("${layout.id}.json")
                                            }
                                        },
                                        onDelete = { deleteTarget = layout },
                                    )
                                }
                            },
                        )
                    }
                }
                item {
                    SettingsGroup(
                        title = stringResource(R.string.layouts_builtin),
                        items = builtIn.map { layout ->
                            {
                                LayoutRow(
                                    layout = layout,
                                    onExport = {
                                        data.sources[layout.id]?.let {
                                            exportRequest = ExportRequest("${layout.id}.json", it.text)
                                            exportLauncher.launch("${layout.id}.json")
                                        }
                                    },
                                    onDelete = null,
                                )
                            }
                        },
                    )
                }
                if (data.failures.isNotEmpty()) {
                    item {
                        SettingsGroup(
                            title = stringResource(R.string.layouts_skipped),
                            items = data.failures.map { failure ->
                                {
                                    ListItem(
                                        colors = TransparentListItem(),
                                        headlineContent = { Text(failure.origin) },
                                        supportingContent = {
                                            Text(failure.issues.joinToString("\n") { "${it.path}: ${it.message}" })
                                        },
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    rejected?.let { issues ->
        AlertDialog(
            onDismissRequest = { rejected = null },
            title = { Text(stringResource(R.string.layout_rejected_title)) },
            text = {
                Text(
                    text = issues.joinToString("\n") { "${it.path}: ${it.message}" },
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = { TextButton(onClick = { rejected = null }) { Text(stringResource(R.string.ok)) } },
        )
    }

    deleteTarget?.let { layout ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.layout_delete_title)) },
            text = { Text(stringResource(R.string.layout_delete_body, layout.name)) },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    scope.launch {
                        withContext(Dispatchers.IO) { repository.delete(layout.id) }
                        settings.bumpLayoutsRevision()
                        refresh++
                    }
                }) { Text(stringResource(R.string.layout_delete)) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun LayoutRow(layout: KeyboardLayout, onExport: () -> Unit, onDelete: (() -> Unit)?) {
    ListItem(
        colors = TransparentListItem(),
        headlineContent = { Text(layout.name) },
        supportingContent = { Text("${layout.locale} / ${layout.id}") },
        trailingContent = {
            Row {
                IconButton(onClick = onExport) {
                    Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.layout_export))
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.layout_delete))
                    }
                }
            }
        },
    )
}

private fun importFrom(context: Context, repository: LayoutRepository, uri: Uri): ImportResult {
    val text = runCatching {
        context.contentResolver.openInputStream(uri)?.use { readBounded(it, maxLayoutFileBytes()) }
    }.getOrNull() ?: return ImportResult.Invalid(listOf(importTooLargeIssue()))
    return repository.import(text)
}

private fun openSubtypeSettings(context: Context) {
    val id = ComponentName(context, KotobaInputMethodService::class.java).flattenToShortString()
    val intent = Intent(Settings.ACTION_INPUT_METHOD_SUBTYPE_SETTINGS)
        .putExtra(Settings.EXTRA_INPUT_METHOD_ID, id)
    context.startActivity(intent)
}
