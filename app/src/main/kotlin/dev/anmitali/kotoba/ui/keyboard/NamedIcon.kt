package dev.anmitali.kotoba.ui.keyboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource

private const val ICON_PREFIX = "ic_"

@Composable
fun namedIconPainter(name: String): Painter {
    val painter = namedIconPainterOrNull(name)
    check(painter != null) { "Unknown icon: $name" }
    return painter
}

@Composable
fun namedIconPainterOrNull(name: String): Painter? {
    val context = LocalContext.current
    val id = remember(name) {
        if (name.startsWith(ICON_PREFIX)) context.resources.getIdentifier(name, "drawable", context.packageName) else 0
    }
    return if (id != 0) painterResource(id) else null
}
