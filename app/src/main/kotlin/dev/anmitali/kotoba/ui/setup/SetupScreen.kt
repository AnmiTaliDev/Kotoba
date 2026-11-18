package dev.anmitali.kotoba.ui.setup

import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.anmitali.kotoba.R
import dev.anmitali.kotoba.setup.ImeStatus

private enum class SetupStep { Enable, Select, Done }

private const val ActionSteps = 2

@Composable
fun SetupScreen(onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    var status by remember { mutableStateOf(ImeStatus.read(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) status = ImeStatus.read(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val step = when {
        !status.enabled -> SetupStep.Enable
        !status.selected -> SetupStep.Select
        else -> SetupStep.Done
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        AnimatedContent(
            targetState = step,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "setupStep",
            modifier = Modifier.padding(padding),
        ) { current ->
            when (current) {
                SetupStep.Enable -> StepPage(
                    index = 0,
                    icon = Icons.Filled.Settings,
                    title = stringResource(R.string.setup_enable_title),
                    body = stringResource(R.string.setup_enable_body),
                    action = stringResource(R.string.setup_enable_action),
                    onAction = { context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) },
                )
                SetupStep.Select -> StepPage(
                    index = 1,
                    icon = Icons.Filled.Settings,
                    title = stringResource(R.string.setup_select_title),
                    body = stringResource(R.string.setup_select_body),
                    action = stringResource(R.string.setup_select_action),
                    onAction = {
                        context.getSystemService(InputMethodManager::class.java).showInputMethodPicker()
                    },
                )
                SetupStep.Done -> StepPage(
                    index = ActionSteps,
                    icon = Icons.Filled.CheckCircle,
                    title = stringResource(R.string.setup_done_title),
                    body = stringResource(R.string.setup_done_body),
                    action = stringResource(R.string.setup_done_action),
                    onAction = onOpenSettings,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StepPage(
    index: Int,
    icon: ImageVector,
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LinearProgressIndicator(
            progress = { index.toFloat() / ActionSteps },
            modifier = Modifier.fillMaxWidth(),
        )
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                shape = MaterialShapes.Cookie9Sided.toShape(),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(112.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(56.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
            if (index < ActionSteps) {
                Text(
                    text = stringResource(R.string.setup_step_counter, index + 1, ActionSteps),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Button(onClick = onAction, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text(action) }
    }
}
