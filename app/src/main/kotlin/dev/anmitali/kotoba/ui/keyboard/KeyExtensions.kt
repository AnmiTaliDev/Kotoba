package dev.anmitali.kotoba.ui.keyboard

import dev.anmitali.kotoba.engine.Key

val Key.weight: Float
    get() = when (this) {
        is Key.Char -> weight
        is Key.Shift -> weight
        is Key.Delete -> weight
        is Key.Enter -> weight
        is Key.Space -> weight
        is Key.Spacer -> weight
        is Key.Layer -> weight
    }

fun Key.Char.text(shifted: Boolean): String = if (shifted) this.shifted else label

fun Key.Char.alternatives(shifted: Boolean): List<String> = if (shifted) holdShifted else hold
