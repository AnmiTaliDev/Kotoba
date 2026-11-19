package dev.anmitali.kotoba.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.anmitali.kotoba.emoji.addRecent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

private val KeyCornerRadiusKey = intPreferencesKey("key_corner_radius_dp")
private val NumberRowKey = booleanPreferencesKey("number_row")
private val RecentEmojiKey = stringPreferencesKey("recent_emoji")
private val LayoutsRevisionKey = intPreferencesKey("layouts_revision")
private const val RecentSeparator = "\u001F"

class SettingsRepository(context: Context) {

    private val store = context.applicationContext.dataStore

    val settings: Flow<KeyboardSettings> = store.data.map { prefs ->
        KeyboardSettings(
            keyCornerRadiusDp = (prefs[KeyCornerRadiusKey] ?: KeyboardSettings.DefaultKeyCornerRadiusDp)
                .coerceIn(KeyboardSettings.MinKeyCornerRadiusDp, KeyboardSettings.MaxKeyCornerRadiusDp),
            numberRow = prefs[NumberRowKey] ?: false,
        )
    }

    val recentEmoji: Flow<List<String>> = store.data.map { prefs ->
        prefs[RecentEmojiKey]?.split(RecentSeparator)?.filter { it.isNotEmpty() } ?: emptyList()
    }

    val layoutsRevision: Flow<Int> = store.data.map { it[LayoutsRevisionKey] ?: 0 }

    suspend fun bumpLayoutsRevision() {
        store.edit { it[LayoutsRevisionKey] = (it[LayoutsRevisionKey] ?: 0) + 1 }
    }

    suspend fun recordEmoji(emoji: String) {
        store.edit { prefs ->
            val current = prefs[RecentEmojiKey]?.split(RecentSeparator)?.filter { it.isNotEmpty() } ?: emptyList()
            prefs[RecentEmojiKey] = addRecent(current, emoji).joinToString(RecentSeparator)
        }
    }

    suspend fun setKeyCornerRadius(dp: Int) {
        store.edit { it[KeyCornerRadiusKey] = dp }
    }

    suspend fun setNumberRow(enabled: Boolean) {
        store.edit { it[NumberRowKey] = enabled }
    }
}
