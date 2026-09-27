package io.github.tetiana01kovpak.solfeggio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Keeps the tone playing with the screen off or the app in the background,
 * and shows a notification with a Stop action while it plays.
 */
class ToneService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var audio: AudioManager
    private var focus: AudioFocusRequest? = null
    private var started = false

    // Unplugging headphones stops the tone instead of blasting it from the speaker.
    private val noisy = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = Player.stop()
    }

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        Player.init(this)
        audio = getSystemService(AudioManager::class.java)
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Playback", NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false)
            }
        )
        ContextCompat.registerReceiver(
            this, noisy, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), ContextCompat.RECEIVER_EXPORTED
        )
        focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener { if (it == AudioManager.AUDIOFOCUS_LOSS) Player.stop() }
            .build()
            .also { audio.requestAudioFocus(it) }

        scope.launch {
            // Only the tone and the timer show in the notification; ignore volume changes.
            Player.state.distinctUntilChanged { a, b -> a.playing == b.playing && a.endsAtMillis == b.endsAtMillis }.collect { state ->
                if (!started) return@collect
                if (state.playing == null) {
                    ServiceCompat.stopForeground(this@ToneService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(state))
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            Player.stop()
            if (!started) stopSelf()
            return START_NOT_STICKY
        }
        // startForegroundService() requires startForeground() even if the tone already stopped.
        val state = Player.state.value
        val type = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(state), type)
        started = true
        if (state.playing == null) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        unregisterReceiver(noisy)
        focus?.let { audio.abandonAudioFocusRequest(it) }
        super.onDestroy()
    }

    private fun notification(state: PlayerState): Notification {
        val index = state.playing
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, ToneService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (index != null) "${FREQUENCIES[index].hz} Hz sine" else "Silence")
            .setContentText(index?.let { FREQUENCIES[it].label })
            .setColor(if (index != null) TONE_COLORS[index].toInt() else 0xFF23402B.toInt())
            .setContentIntent(open)
            .addAction(0, "Stop", stop)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        if (state.endsAtMillis > 0) {
            builder.setWhen(state.endsAtMillis).setShowWhen(true).setUsesChronometer(true).setChronometerCountDown(true)
        } else {
            builder.setShowWhen(false)
        }
        return builder.build()
    }

    companion object {
        private const val CHANNEL = "playback"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_STOP = "io.github.tetiana01kovpak.solfeggio.STOP"

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, ToneService::class.java))
        }
    }
}
