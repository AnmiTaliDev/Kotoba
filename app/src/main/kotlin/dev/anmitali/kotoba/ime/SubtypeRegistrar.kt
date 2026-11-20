package dev.anmitali.kotoba.ime

import android.content.ComponentName
import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodSubtype
import android.view.inputmethod.InputMethodSubtype.InputMethodSubtypeBuilder
import dev.anmitali.kotoba.engine.KeyboardLayout
import java.util.Locale

object SubtypeRegistrar {

    private const val MODE = "keyboard"
    private const val LAYOUT_PREFIX = "layout="

    fun register(context: Context, layouts: List<KeyboardLayout>) {
        val manager = context.getSystemService(InputMethodManager::class.java)
        val id = ComponentName(context, KotobaInputMethodService::class.java).flattenToShortString()
        val subtypes = layouts.map { layout ->
            InputMethodSubtypeBuilder()
                .setSubtypeLocale(subtypeLocale(layout.locale))
                .setLanguageTag(layout.locale)
                .setSubtypeMode(MODE)
                .setSubtypeNameOverride(layout.name)
                .setSubtypeExtraValue(LAYOUT_PREFIX + layout.id)
                .build()
        }
        manager.setAdditionalInputMethodSubtypes(id, subtypes.toTypedArray())
    }

    private fun subtypeLocale(tag: String): String {
        val locale = Locale.forLanguageTag(tag)
        return Locale.Builder().setLanguage(locale.language).setRegion(locale.country).build().toString()
    }

    fun enabledSubtypeCount(context: Context): Int {
        val manager = context.getSystemService(InputMethodManager::class.java)
        val method = manager.enabledInputMethodList.firstOrNull { it.packageName == context.packageName } ?: return 0
        return manager.getEnabledInputMethodSubtypeList(method, true).size
    }

    fun layoutIdOf(subtype: InputMethodSubtype?): String? =
        subtype?.extraValue
            ?.split(',')
            ?.firstOrNull { it.startsWith(LAYOUT_PREFIX) }
            ?.removePrefix(LAYOUT_PREFIX)
}
