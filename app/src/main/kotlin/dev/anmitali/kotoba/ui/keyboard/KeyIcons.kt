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
