package dev.anmitali.kotoba.ui.keyboard

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

private const val SHIFT_PATH = "M12 3l8 9h-5v6H9v-6H4l8-9z"
private const val CAPS_PATH = "M12 2l8 8.5h-5V16H9v-5.5H4L12 2z"
private const val CAPS_BAR_PATH = "M9 21h6"
private const val BACKSPACE_PATH =
    "M22,3H7c-0.69,0 -1.23,0.35 -1.59,0.88L0,12l5.41,8.11c0.36,0.53 0.9,0.89 1.59,0.89h15c1.1,0 2,-0.9 2,-2V5" +
        "c0,-1.1 -0.9,-2 -2,-2zM19,15.59L17.59,17 14,13.41 10.41,17 9,15.59 12.59,12 9,8.41 10.41,7 14,10.59 " +
        "17.59,7 19,8.41 15.41,12 19,15.59z"
private const val RETURN_PATH = "M19,7v4H5.83l3.58,-3.59L8,6l-6,6 6,6 1.41,-1.41L5.83,13H21V7z"

private fun icon(name: String, build: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply(build).build()

private val ink = SolidColor(Color.Black)

val ShiftOffIcon: ImageVector = icon("ShiftOff") {
    addPath(
        pathData = addPathNodes(SHIFT_PATH),
        stroke = ink,
        strokeLineWidth = 2f,
        strokeLineJoin = StrokeJoin.Round,
    )
}

val ShiftOnceIcon: ImageVector = icon("ShiftOnce") {
    addPath(
        pathData = addPathNodes(SHIFT_PATH),
        fill = ink,
        stroke = ink,
        strokeLineWidth = 2f,
        strokeLineJoin = StrokeJoin.Round,
    )
}

val ShiftCapsIcon: ImageVector = icon("ShiftCaps") {
    addPath(
        pathData = addPathNodes(CAPS_PATH),
        fill = ink,
        stroke = ink,
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    )
    addPath(
        pathData = addPathNodes(CAPS_BAR_PATH),
        stroke = ink,
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
    )
}

val BackspaceIcon: ImageVector = icon("Backspace") {
    addPath(pathData = addPathNodes(BACKSPACE_PATH), fill = ink)
}

val ReturnIcon: ImageVector = icon("Return") {
    addPath(pathData = addPathNodes(RETURN_PATH), fill = ink)
}

private const val SELECT_ALL_PATH =
    "M3,5h2L5,3c-1.1,0 -2,0.9 -2,2zM3,13h2v-2L3,11v2zM7,21h2v-2L7,19v2zM3,9h2L5,7L3,7v2zM13,3h-2v2h2L13,3z" +
        "M19,3v2h2c0,-1.1 -0.9,-2 -2,-2zM5,21v-2L3,19c0,1.1 0.9,2 2,2zM3,17h2v-2L3,15v2zM9,3L7,3v2h2L9,3z" +
        "M11,21h2v-2h-2v2zM19,13h2v-2h-2v2zM19,21c1.1,0 2,-0.9 2,-2h-2v2zM19,9h2L21,7h-2v2zM19,17h2v-2h-2v2z" +
        "M15,21h2v-2h-2v2zM15,5h2L17,3h-2v2zM7,17h10L17,7L7,7v10zM9,9h6v6L9,15L9,9z"
private const val COPY_PATH =
    "M16,1L4,1c-1.1,0 -2,0.9 -2,2v14h2L4,3h12L16,1zM19,5L8,5c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h11" +
        "c1.1,0 2,-0.9 2,-2L21,7c0,-1.1 -0.9,-2 -2,-2zM19,21L8,21L8,7h11v14z"
private const val CUT_PATH =
    "M9.64,7.64c0.23,-0.5 0.36,-1.05 0.36,-1.64 0,-2.21 -1.79,-4 -4,-4S2,3.79 2,6s1.79,4 4,4c0.59,0 1.14,-0.13 1.64,-0.36" +
        "L10,12l-2.36,2.36C7.14,14.13 6.59,14 6,14c-2.21,0 -4,1.79 -4,4s1.79,4 4,4 4,-1.79 4,-4c0,-0.59 -0.13,-1.14 -0.36,-1.64" +
        "L12,14l7,7h3v-1L9.64,7.64zM6,8c-1.1,0 -2,-0.89 -2,-2s0.9,-2 2,-2 2,0.89 2,2 -0.9,2 -2,2z" +
        "M6,20c-1.1,0 -2,-0.89 -2,-2s0.9,-2 2,-2 2,0.89 2,2 -0.9,2 -2,2zM12,12.5c-0.28,0 -0.5,-0.22 -0.5,-0.5s0.22,-0.5 0.5,-0.5" +
        " 0.5,0.22 0.5,0.5 -0.22,0.5 -0.5,0.5zM19,3l-6,6 2,2 7,-7L22,3z"
private const val PASTE_PATH =
    "M19,2h-4.18C14.4,0.84 13.3,0 12,0c-1.3,0 -2.4,0.84 -2.82,2L5,2c-1.1,0 -2,0.9 -2,2v16c0,1.1 0.9,2 2,2h14" +
        "c1.1,0 2,-0.9 2,-2L21,4c0,-1.1 -0.9,-2 -2,-2zM12,2c0.55,0 1,0.45 1,1s-0.45,1 -1,1 -1,-0.45 -1,-1 0.45,-1 1,-1z" +
        "M19,20L5,20L5,4h2v3h10L17,4h2v16z"

private fun filled(name: String, path: String): ImageVector =
    icon(name) { addPath(pathData = addPathNodes(path), fill = ink) }

val SelectAllIcon: ImageVector = filled("SelectAll", SELECT_ALL_PATH)
val CopyIcon: ImageVector = filled("Copy", COPY_PATH)
val CutIcon: ImageVector = filled("Cut", CUT_PATH)
val PasteIcon: ImageVector = filled("Paste", PASTE_PATH)
