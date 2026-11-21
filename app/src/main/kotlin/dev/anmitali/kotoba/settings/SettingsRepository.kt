package dev.anmitali.kotoba.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.anmitali.kotoba.engine.decodeRecentEmoji
import dev.anmitali.kotoba.engine.encodeRecentEmoji
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

private val KeyCornerRadiusKey = intPreferencesKey("key_corner_radius_dp")
private val NumberRowKey = booleanPreferencesKey("number_row")
private val HapticsKey = booleanPreferencesKey("haptics")
private val RecentEmojiKey = stringPreferencesKey("recent_emoji")
private val LayoutsRevisionKey = intPreferencesKey("layouts_revision")

class SettingsRepository(context: Context) {

    private val store = context.applicationContext.dataStore

    val settings: Flow<KeyboardSettings> = store.data.map { prefs ->
        KeyboardSettings(
            keyCornerRadiusDp = (prefs[KeyCornerRadiusKey] ?: KeyboardSettings.DefaultKeyCornerRadiusDp)
                .coerceIn(KeyboardSettings.MinKeyCornerRadiusDp, KeyboardSettings.MaxKeyCornerRadiusDp),
            numberRow = prefs[NumberRowKey] ?: KeyboardSettings.DefaultNumberRow,
            haptics = prefs[HapticsKey] ?: KeyboardSettings.DefaultHaptics,
            ints = IntSetting.entries.associateWith { setting ->
                (prefs[intPreferencesKey(setting.id)] ?: setting.default).coerceIn(setting.min, setting.max)
            },
            flags = FlagSetting.entries.associateWith { setting ->
                prefs[booleanPreferencesKey(setting.id)] ?: setting.default
            },
        )
    }

    val recentEmoji: Flow<List<String>> = store.data.map { prefs ->
        prefs[RecentEmojiKey]?.let(::decodeRecentEmoji) ?: emptyList()
    }

    val layoutsRevision: Flow<Int> = store.data.map { it[LayoutsRevisionKey] ?: 0 }

    suspend fun bumpLayoutsRevision() {
        store.edit { it[LayoutsRevisionKey] = (it[LayoutsRevisionKey] ?: 0) + 1 }
    }

    suspend fun saveRecentEmoji(recents: List<String>) {
        store.edit { it[RecentEmojiKey] = encodeRecentEmoji(recents) }
    }

    suspend fun setKeyCornerRadius(dp: Int) {
        store.edit { it[KeyCornerRadiusKey] = dp }
    }

    suspend fun setInt(setting: IntSetting, value: Int) {
        store.edit { it[intPreferencesKey(setting.id)] = value.coerceIn(setting.min, setting.max) }
    }

    suspend fun setFlag(setting: FlagSetting, value: Boolean) {
        store.edit { it[booleanPreferencesKey(setting.id)] = value }
    }

    suspend fun resetAppearance() {
        store.edit { prefs ->
            IntSetting.entries.forEach { prefs.remove(intPreferencesKey(it.id)) }
            FlagSetting.entries.forEach { prefs.remove(booleanPreferencesKey(it.id)) }
            prefs.remove(KeyCornerRadiusKey)
            prefs.remove(NumberRowKey)
            prefs.remove(HapticsKey)
        }
    }

    suspend fun setHaptics(enabled: Boolean) {
        store.edit { it[HapticsKey] = enabled }
    }

    suspend fun setNumberRow(enabled: Boolean) {
        store.edit { it[NumberRowKey] = enabled }
    }
}
