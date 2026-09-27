package io.github.tetiana01kovpak.solfeggio

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import io.github.tetiana01kovpak.solfeggio.ui.SolfeggioScreen
import io.github.tetiana01kovpak.solfeggio.ui.SolfeggioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        volumeControlStream = AudioManager.STREAM_MUSIC
        Player.init(this)

        setContent {
            SolfeggioTheme {
                val state by Player.state.collectAsState()
                val askNotifications = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) {}
                SolfeggioScreen(
                    state = state,
                    onToggle = { index ->
                        if (shouldAskForNotifications()) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                        Player.toggle(index)
                    },
                    onStop = { Player.stop() },
                    onVolume = Player::setVolume,
                    onTimer = Player::setTimer,
                )
            }
        }
    }

    // Ask once, on the first tone, so the Stop control can show in the notification shade.
    private fun shouldAskForNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < 33) return false
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) return false
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        if (prefs.getBoolean("asked_notifications", false)) return false
        prefs.edit().putBoolean("asked_notifications", true).apply()
        return true
    }
}
