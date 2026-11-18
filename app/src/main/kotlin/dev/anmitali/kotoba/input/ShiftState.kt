package dev.anmitali.kotoba.input

enum class ShiftState { Off, Once, Caps }

private const val DOUBLE_TAP_MS = 350L

fun tapShift(current: ShiftState, sincePreviousTapMs: Long): ShiftState = when {
    current == ShiftState.Caps -> ShiftState.Off
    sincePreviousTapMs < DOUBLE_TAP_MS -> ShiftState.Caps
    current == ShiftState.Off -> ShiftState.Once
    else -> ShiftState.Off
}

fun shiftAfterEdit(
    current: ShiftState,
    textBefore: CharSequence,
    sentenceEnd: String,
    autoCapitalize: Boolean,
): ShiftState = when {
    current == ShiftState.Caps -> ShiftState.Caps
    autoCapitalize && startsSentence(textBefore, sentenceEnd) -> ShiftState.Once
    else -> ShiftState.Off
}

private fun startsSentence(textBefore: CharSequence, sentenceEnd: String): Boolean {
    if (textBefore.isEmpty()) return true
    if (!textBefore.last().isWhitespace()) return false
    val trimmed = textBefore.trimEnd()
    return trimmed.isNotEmpty() && trimmed.last() in sentenceEnd
}
