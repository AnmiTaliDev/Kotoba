package dev.anmitali.kotoba.input

import dev.anmitali.kotoba.layout.CharKey
import dev.anmitali.kotoba.layout.Layer

fun outputOf(key: CharKey, layer: Layer, shift: ShiftState): String =
    if (layer.shiftable && shift != ShiftState.Off) key.shifted else key.label
