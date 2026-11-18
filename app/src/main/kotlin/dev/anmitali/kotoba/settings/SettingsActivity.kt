package dev.anmitali.kotoba.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.anmitali.kotoba.ui.settings.SettingsScreen
import dev.anmitali.kotoba.ui.theme.KotobaTheme

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val repository = SettingsRepository(this)
        setContent {
            KotobaTheme {
                SettingsScreen(repository)
            }
        }
    }
}
