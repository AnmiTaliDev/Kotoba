package dev.anmitali.kotoba.input

import android.view.inputmethod.EditorInfo
import dev.anmitali.kotoba.engine.EnterAction

fun enterActionOf(imeOptions: Int): EnterAction {
    if (imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0) return EnterAction.NEWLINE
    return when (imeOptions and EditorInfo.IME_MASK_ACTION) {
        EditorInfo.IME_ACTION_GO -> EnterAction.GO
        EditorInfo.IME_ACTION_SEARCH -> EnterAction.SEARCH
        EditorInfo.IME_ACTION_SEND -> EnterAction.SEND
        EditorInfo.IME_ACTION_NEXT -> EnterAction.NEXT
        EditorInfo.IME_ACTION_DONE -> EnterAction.DONE
        else -> EnterAction.NEWLINE
    }
}
