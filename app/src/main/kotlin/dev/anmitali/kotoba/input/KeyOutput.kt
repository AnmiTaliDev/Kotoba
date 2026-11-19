package dev.anmitali.kotoba.input

import dev.anmitali.kotoba.layout.CharKey
import dev.anmitali.kotoba.layout.Layer

private fun shiftApplies(layer: Layer, shift: ShiftState): Boolean = layer.shiftable && shift != ShiftState.Off

fun outputOf(key: CharKey, layer: Layer, shift: ShiftState): String =
    if (shiftApplies(layer, shift)) key.shifted else key.label

fun alternativesOf(key: CharKey, layer: Layer, shift: ShiftState): List<String> =
    if (shiftApplies(layer, shift)) key.holdShifted else key.hold
