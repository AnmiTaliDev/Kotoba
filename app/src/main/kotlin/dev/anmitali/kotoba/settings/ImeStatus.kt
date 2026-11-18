package dev.anmitali.kotoba.settings

import android.content.Context
import android.provider.Settings
import android.view.inputmethod.InputMethodManager

data class ImeStatus(val enabled: Boolean, val selected: Boolean) {
    companion object {
        fun read(context: Context): ImeStatus {
            val manager = context.getSystemService(InputMethodManager::class.java)
            val enabled = manager.enabledInputMethodList.any { it.packageName == context.packageName }
            val current = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            val selected = current?.startsWith(context.packageName + "/") == true
            return ImeStatus(enabled, selected)
        }
    }
}
