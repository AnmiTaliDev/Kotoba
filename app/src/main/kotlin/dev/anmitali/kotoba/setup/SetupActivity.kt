package dev.anmitali.kotoba.setup

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.anmitali.kotoba.settings.SettingsActivity
import dev.anmitali.kotoba.ui.setup.SetupScreen
import dev.anmitali.kotoba.ui.theme.KotobaTheme

class SetupActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val status = ImeStatus.read(this)
        if (status.enabled && status.selected) {
            openSettings()
            return
        }
        setContent {
            KotobaTheme {
                SetupScreen(onOpenSettings = ::openSettings)
            }
        }
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
        finish()
    }
}
