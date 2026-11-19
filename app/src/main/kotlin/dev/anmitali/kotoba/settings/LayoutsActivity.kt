package dev.anmitali.kotoba.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.anmitali.kotoba.ime.UserLayoutsDirectory
import dev.anmitali.kotoba.layout.LayoutRepository
import dev.anmitali.kotoba.layout.UserLayoutStore
import dev.anmitali.kotoba.ui.settings.LayoutsScreen
import dev.anmitali.kotoba.ui.theme.KotobaTheme
import java.io.File

class LayoutsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val repository = LayoutRepository(UserLayoutStore(File(filesDir, UserLayoutsDirectory)))
        val settings = SettingsRepository(this)
        setContent {
            KotobaTheme {
                LayoutsScreen(repository = repository, settings = settings, onBack = ::finish)
            }
        }
    }
}
