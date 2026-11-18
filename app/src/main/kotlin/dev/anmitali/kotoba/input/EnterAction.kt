package dev.anmitali.kotoba.input

import android.view.inputmethod.EditorInfo

enum class EnterAction { Newline, Go, Search, Send, Next, Done }

fun enterActionOf(imeOptions: Int): EnterAction {
    if (imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0) return EnterAction.Newline
    return when (imeOptions and EditorInfo.IME_MASK_ACTION) {
        EditorInfo.IME_ACTION_GO -> EnterAction.Go
        EditorInfo.IME_ACTION_SEARCH -> EnterAction.Search
        EditorInfo.IME_ACTION_SEND -> EnterAction.Send
        EditorInfo.IME_ACTION_NEXT -> EnterAction.Next
        EditorInfo.IME_ACTION_DONE -> EnterAction.Done
        else -> EnterAction.Newline
    }
}
