package io.github.tetiana01kovpak.solfeggio

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerState(
    val playing: Int? = null,
    val volume: Float = 0.2f,
    val timerMinutes: Int = 0,
    val endsAtMillis: Long = 0L,
)

/** Process-wide playback state. The UI and [ToneService] both observe [state]. */
object Player {
    const val FADE_S = 0.08
    const val TIMER_FADE_S = 3.0

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var app: Context
    private lateinit var prefs: SharedPreferences
    private var engine: ToneEngine? = null
    private var timerJob: Job? = null
    private var releaseJob: Job? = null

    fun init(context: Context) {
        if (::app.isInitialized) return
        app = context.applicationContext
        prefs = app.getSharedPreferences("player", Context.MODE_PRIVATE)
        _state.value = PlayerState(
            volume = prefs.getFloat("volume", 0.2f),
            timerMinutes = prefs.getInt("timer", 0),
        )
    }

    fun toggle(index: Int) {
        if (state.value.playing == index) stop() else play(index)
    }

    fun play(index: Int) {
        releaseJob?.cancel()
        val tone = engine ?: ToneEngine().also { engine = it }
        tone.setVolume(state.value.volume)
        tone.play(FREQUENCIES[index].hz.toDouble(), FADE_S)
        _state.update { it.copy(playing = index) }
        startTimer()
        ToneService.start(app)
    }

    fun stop(fade: Double = FADE_S) {
        timerJob?.cancel()
        timerJob = null
        if (state.value.playing == null) return
        engine?.stop(fade)
        _state.update { it.copy(playing = null, endsAtMillis = 0L) }
        releaseJob?.cancel()
        releaseJob = scope.launch {
            delay(((fade + 0.3) * 1000).toLong())
            engine?.release()
            engine = null
        }
    }

    fun setVolume(value: Float) {
        _state.update { it.copy(volume = value) }
        engine?.setVolume(value)
        prefs.edit().putFloat("volume", value).apply()
    }

    fun setTimer(minutes: Int) {
        _state.update { it.copy(timerMinutes = minutes) }
        prefs.edit().putInt("timer", minutes).apply()
        if (state.value.playing != null) startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        val minutes = state.value.timerMinutes
        if (minutes == 0) {
            _state.update { it.copy(endsAtMillis = 0L) }
            return
        }
        val end = System.currentTimeMillis() + minutes * 60_000L
        _state.update { it.copy(endsAtMillis = end) }
        timerJob = scope.launch {
            delay(end - (TIMER_FADE_S * 1000).toLong() - System.currentTimeMillis())
            stop(TIMER_FADE_S)
        }
    }
}
