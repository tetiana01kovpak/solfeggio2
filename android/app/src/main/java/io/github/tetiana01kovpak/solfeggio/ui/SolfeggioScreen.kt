package io.github.tetiana01kovpak.solfeggio.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.tetiana01kovpak.solfeggio.FREQUENCIES
import io.github.tetiana01kovpak.solfeggio.PlayerState
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private val Serif = FontFamily.Serif
private val DockShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
private val CardShape = RoundedCornerShape(24.dp)

@Composable
fun SolfeggioScreen(
    state: PlayerState,
    onToggle: (Int) -> Unit,
    onStop: () -> Unit,
    onVolume: (Float) -> Unit,
    onTimer: (Int) -> Unit,
) {
    val p = LocalPalette.current
    val playing = state.playing
    val tone by animateColorAsState(
        if (playing != null) toneColor(playing) else p.accent, tween(700), label = "tone"
    )

    Box(Modifier.fillMaxSize().background(p.bg)) {
        Glow(tone, playing != null)
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(
                        WindowInsets.statusBars.union(WindowInsets.displayCutout)
                            .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                    )
                    .padding(horizontal = 20.dp)
            ) {
                Header()
                FrequencyGrid(playing, onToggle)
                Spacer(Modifier.height(28.dp))
            }
            Dock(state, tone, onStop, onVolume, onTimer)
        }
    }
}

/** A soft wash of the current tone's colour from the top of the screen. */
@Composable
private fun Glow(tone: Color, active: Boolean) {
    val strength by animateFloatAsState(if (active) 0.34f else 0.14f, tween(900), label = "glow")
    Canvas(Modifier.fillMaxSize()) {
        val radius = size.maxDimension * 0.75f
        drawCircle(
            brush = Brush.radialGradient(
                listOf(tone.copy(alpha = strength), Color.Transparent),
                center = Offset(size.width * 0.8f, -size.height * 0.05f),
                radius = radius,
            ),
            radius = radius,
            center = Offset(size.width * 0.8f, -size.height * 0.05f),
        )
    }
}

@Composable
private fun Header() {
    val p = LocalPalette.current
    Column(Modifier.padding(top = 28.dp, bottom = 22.dp)) {
        Text("Solfeggio", fontFamily = Serif, fontSize = 42.sp, lineHeight = 46.sp, color = p.ink, letterSpacing = 0.01.em)
        Text("Frequencies", fontFamily = Serif, fontStyle = FontStyle.Italic, fontSize = 24.sp, color = p.muted)
        Spacer(Modifier.height(12.dp))
        Text(
            "Pure sine tones at the eight Solfeggio frequencies. Tap a card to play it, tap again to stop.",
            fontFamily = Serif, fontSize = 15.sp, lineHeight = 22.sp, color = p.muted,
        )
    }
}

@Composable
private fun FrequencyGrid(playing: Int?, onToggle: (Int) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = max(2, (maxWidth / 172.dp).toInt())
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FREQUENCIES.indices.chunked(columns).forEach { row ->
                Row(
                    Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { i ->
                        FrequencyCard(i, playing == i, { onToggle(i) }, Modifier.weight(1f).fillMaxHeight())
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun FrequencyCard(index: Int, on: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val p = LocalPalette.current
    val f = FREQUENCIES[index]
    val tone = toneColor(index)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "press")
    val bg by animateColorAsState(if (on) tone else p.surface, tween(450), label = "bg")
    val fg by animateColorAsState(if (on) Cream else p.ink, tween(450), label = "fg")
    val sub by animateColorAsState(if (on) Cream.copy(alpha = 0.78f) else p.muted, tween(450), label = "sub")
    val border by animateColorAsState(if (on) Color.Transparent else p.line, tween(450), label = "border")

    Column(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (on) 14.dp else 0.dp, CardShape, ambientColor = tone, spotColor = tone)
            .clip(CardShape)
            .background(bg)
            .border(1.dp, border, CardShape)
            .clickable(interactionSource = interaction, indication = ripple(color = fg), onClick = onClick)
            .semantics { selected = on; role = Role.Button }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ToneDot(if (on) Cream else p.lineTone(tone), on)
            Spacer(Modifier.weight(1f))
            if (on) Text("Playing", fontSize = 11.sp, color = sub, letterSpacing = 0.06.em)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${f.hz}", fontFamily = Serif, fontSize = 34.sp, lineHeight = 36.sp, color = fg)
            Text(" Hz", fontFamily = Serif, fontSize = 15.sp, color = sub, modifier = Modifier.padding(bottom = 4.dp))
        }
        Spacer(Modifier.height(8.dp))
        if (f.note == null) {
            Text("Traditional association", fontSize = 11.sp, color = sub, letterSpacing = 0.04.em)
            Spacer(Modifier.height(2.dp))
        }
        Text(f.label, fontFamily = Serif, fontSize = 14.sp, lineHeight = 19.sp, color = fg)
        f.note?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, fontFamily = Serif, fontStyle = FontStyle.Italic, fontSize = 13.sp, lineHeight = 18.sp, color = sub)
        }
    }
}

/** A small dot that ripples outward while its tone plays. */
@Composable
private fun ToneDot(color: Color, on: Boolean) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val t by pulse.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "t")
    Canvas(Modifier.size(22.dp)) {
        val r = 4.5.dp.toPx()
        if (on) {
            for (k in 0..1) {
                val phase = (t + k * 0.5f) % 1f
                drawCircle(color.copy(alpha = (1f - phase) * 0.55f), radius = r + phase * 7.dp.toPx())
            }
        }
        drawCircle(color, radius = r)
    }
}

@Composable
private fun Dock(
    state: PlayerState,
    tone: Color,
    onStop: () -> Unit,
    onVolume: (Float) -> Unit,
    onTimer: (Int) -> Unit,
) {
    val p = LocalPalette.current
    val playing = state.playing
    val line = p.lineTone(tone)

    Column(
        Modifier
            .fillMaxWidth()
            .shadow(28.dp, DockShape, ambientColor = Color.Black.copy(alpha = 0.3f), spotColor = Color.Black.copy(alpha = 0.3f))
            .clip(DockShape)
            .background(p.surface)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 14.dp)
    ) {
        Wave(line, playing?.let { FREQUENCIES[it].hz }, Modifier.fillMaxWidth().height(60.dp))
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = playing,
                    transitionSpec = {
                        (fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 3 }) togetherWith
                            (fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it / 3 })
                    },
                    label = "readout",
                ) { index ->
                    Text(
                        if (index == null) "Silence" else "${FREQUENCIES[index].hz} Hz sine",
                        fontFamily = Serif, fontSize = 24.sp, color = p.ink,
                    )
                }
                Text(statusLine(state), fontSize = 13.sp, color = p.muted)
            }
            StopButton(playing != null, onStop)
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Volume", fontSize = 13.sp, color = p.muted, modifier = Modifier.width(64.dp))
            Slider(
                value = state.volume,
                onValueChange = onVolume,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = line,
                    activeTrackColor = line,
                    inactiveTrackColor = p.line,
                ),
            )
            Text(
                "${(state.volume * 100).roundToInt()}%",
                fontSize = 13.sp, color = p.ink, textAlign = TextAlign.End, modifier = Modifier.width(44.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Timer", fontSize = 13.sp, color = p.muted, modifier = Modifier.width(64.dp))
            TimerPicker(state.timerMinutes, tone, onTimer, Modifier.weight(1f))
        }
        Text(
            "Start quietly, especially on headphones.",
            fontSize = 12.sp, color = p.muted, modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun statusLine(state: PlayerState): String {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.endsAtMillis) {
        while (state.endsAtMillis > 0) {
            now = System.currentTimeMillis()
            delay(250)
        }
    }
    return when {
        state.playing == null -> "Tap a frequency to begin"
        state.endsAtMillis > 0 -> {
            val s = max(0L, (state.endsAtMillis - now + 999) / 1000)
            "%d:%02d left".format(s / 60, s % 60)
        }
        else -> FREQUENCIES[state.playing].label
    }
}

@Composable
private fun StopButton(enabled: Boolean, onStop: () -> Unit) {
    val p = LocalPalette.current
    val bg by animateColorAsState(if (enabled) p.ink else p.line, tween(300), label = "stopBg")
    Box(
        Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(enabled = enabled, onClick = onStop)
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).background(if (enabled) p.surface else p.muted.copy(alpha = 0.5f)))
    }
}

@Composable
private fun TimerPicker(minutes: Int, tone: Color, onTimer: (Int) -> Unit, modifier: Modifier) {
    val p = LocalPalette.current
    Row(modifier.clip(CircleShape).background(p.bg).padding(4.dp)) {
        listOf(0 to "Off", 5 to "5 min", 10 to "10 min", 20 to "20 min").forEach { (value, label) ->
            val selected = value == minutes
            val bg by animateColorAsState(if (selected) tone else Color.Transparent, tween(250), label = "chip")
            val fg by animateColorAsState(if (selected) Cream else p.ink, tween(250), label = "chipText")
            Box(
                Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(bg)
                    .clickable { onTimer(value) }
                    .semantics { this.selected = selected; role = Role.RadioButton }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, fontSize = 13.sp, color = fg, maxLines = 1)
            }
        }
    }
}

/**
 * Layered, drifting sine waves. The line count per width follows the pitch,
 * and the waves settle to a faint ripple when nothing plays.
 */
@Composable
private fun Wave(color: Color, hz: Int?, modifier: Modifier) {
    val drift = rememberInfiniteTransition(label = "wave")
    val phase by drift.animateFloat(
        0f, (2 * PI).toFloat(), infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), label = "phase",
    )
    var lastHz by remember { mutableIntStateOf(528) }
    LaunchedEffect(hz) { if (hz != null) lastHz = hz }
    val amp by animateFloatAsState(if (hz != null) 1f else 0.07f, tween(800), label = "amp")
    val cycles by animateFloatAsState(1.5f + (lastHz - 174f) / (852f - 174f) * 4.5f, tween(800), label = "cycles")

    Canvas(modifier) {
        val mid = size.height / 2
        val steps = 180
        fun wave(c: Float, ph: Float, a: Float, alpha: Float, width: Float) {
            val path = Path()
            for (s in 0..steps) {
                val u = s / steps.toFloat()
                val taper = sqrt(sin(PI * u)).toFloat()
                val y = mid - sin(2 * PI * c * u + ph).toFloat() * a * mid * 0.85f * taper
                val x = size.width * u
                if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color.copy(alpha = alpha), style = Stroke(width, cap = StrokeCap.Round))
        }
        wave(cycles * 0.5f, -phase * 0.6f, amp * 0.55f, 0.18f, 2.dp.toPx())
        wave(cycles * 0.75f, phase * 0.8f + 1f, amp * 0.75f, 0.35f, 2.dp.toPx())
        wave(cycles, phase, amp, 1f, 3.dp.toPx())
    }
}
