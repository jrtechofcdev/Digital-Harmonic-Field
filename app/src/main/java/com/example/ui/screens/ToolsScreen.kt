package com.example.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.Metronome
import com.example.audio.ReferencePitches
import com.example.audio.TonePlayer
import com.example.ui.components.SectionLabel
import com.example.ui.theme.Brass
import com.example.ui.theme.FuncDominant
import com.example.ui.theme.FuncTonic
import com.example.ui.theme.Hairline
import com.example.ui.theme.Ink
import com.example.ui.theme.Surface1
import com.example.ui.theme.Surface2
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextStrong

@Composable
fun ToolsScreen(contentPadding: PaddingValues) {
    val metronome = remember { Metronome() }
    val tone = remember { TonePlayer() }

    DisposableEffect(Unit) {
        onDispose {
            metronome.stop()
            tone.stop()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Ferramentas", style = MaterialTheme.typography.headlineMedium, color = TextStrong)
        }
        item { MetronomeCard(metronome) }
        item { TunerCard(tone) }
        item { CapoCard() }
    }
}

// ---------------------------------------------------------------------------
// Metrônomo
// ---------------------------------------------------------------------------

@Composable
private fun MetronomeCard(metronome: Metronome) {
    var bpm by remember { mutableIntStateOf(90) }
    var beats by remember { mutableIntStateOf(4) }
    var taps by remember { mutableStateOf(listOf<Long>()) }

    LaunchedEffect(bpm) { metronome.bpm = bpm }
    LaunchedEffect(beats) { metronome.beatsPerMeasure = beats }

    ToolCard(title = "Metrônomo", subtitle = "Ritmo estável para ensaios e apresentações") {
        // BPM grande
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "$bpm",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = TextStrong,
            )
            Spacer(Modifier.width(6.dp))
            Text("BPM", style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.padding(bottom = 6.dp))
        }
        Spacer(Modifier.height(12.dp))

        // Indicador visual de tempos
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            for (b in 0 until beats) {
                val active = metronome.isPlaying && metronome.currentBeat == b
                val color = when {
                    active && b == 0 -> Brass
                    active -> TextStrong
                    else -> Surface2
                }
                Box(
                    Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color)
                )
            }
        }
        Spacer(Modifier.height(14.dp))

        Slider(
            value = bpm.toFloat(),
            onValueChange = { bpm = it.toInt().coerceIn(30, 240) },
            valueRange = 30f..240f,
            colors = SliderDefaults.colors(
                thumbColor = Brass,
                activeTrackColor = Brass,
                inactiveTrackColor = Surface2,
            ),
        )

        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            SmallStepper("−", "Diminuir BPM") { bpm = (bpm - 1).coerceIn(30, 240) }
            // Play/Stop
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (metronome.isPlaying) FuncDominant else Brass)
                    .clickable { metronome.toggle() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (metronome.isPlaying) "Parar" else "Iniciar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
            }
            SmallStepper("+", "Aumentar BPM") { bpm = (bpm + 1).coerceIn(30, 240) }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            // Tap tempo
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Surface2)
                    .border(1.dp, Hairline, RoundedCornerShape(10.dp))
                    .clickable {
                        val now = SystemClock.uptimeMillis()
                        val recent = (taps + now).filter { now - it < 2500 }.takeLast(5)
                        taps = recent
                        if (recent.size >= 2) {
                            val intervals = recent.zipWithNext { a, b -> (b - a).toDouble() }
                            val avg = intervals.average()
                            if (avg > 0) bpm = (60000.0 / avg).toInt().coerceIn(30, 240)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("Marcar tempo", style = MaterialTheme.typography.labelLarge, color = TextBody)
            }
        }

        Spacer(Modifier.height(12.dp))
        SectionLabel("Compasso")
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(2, 3, 4, 6).forEach { n ->
                val selected = beats == n
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (selected) Brass else Surface2)
                        .border(1.dp, if (selected) Brass else Hairline, RoundedCornerShape(9.dp))
                        .clickable { beats = n }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        "$n/4",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (selected) Ink else TextBody,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Diapasão / tons de referência
// ---------------------------------------------------------------------------

@Composable
private fun TunerCard(tone: TonePlayer) {
    val notes = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    val ptNames = mapOf(
        "C" to "Dó", "C#" to "Dó#", "D" to "Ré", "D#" to "Ré#", "E" to "Mi", "F" to "Fá",
        "F#" to "Fá#", "G" to "Sol", "G#" to "Sol#", "A" to "Lá", "A#" to "Lá#", "B" to "Si"
    )

    ToolCard(title = "Diapasão", subtitle = "Tons de referência (Lá = 440 Hz) para afinar de ouvido") {
        val playing = tone.playingKey
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            notes.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { note ->
                        val isPlaying = playing == note
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(if (isPlaying) FuncTonic.copy(alpha = 0.18f) else Surface2)
                                .border(
                                    1.dp,
                                    if (isPlaying) FuncTonic else Hairline,
                                    RoundedCornerShape(11.dp),
                                )
                                .clickable { tone.toggle(note, ReferencePitches.frequency(note)) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    note,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPlaying) FuncTonic else TextStrong,
                                )
                                Text(ptNames[note] ?: "", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                        }
                    }
                }
            }
        }
        if (playing != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Tocando ${ptNames[playing]} · ${"%.1f".format(ReferencePitches.frequency(playing))} Hz — toque de novo para parar.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextBody,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Capotraste
// ---------------------------------------------------------------------------

@Composable
private fun CapoCard() {
    val roots = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    val easyShapes = listOf("C", "A", "G", "E", "D") // formatos abertos fáceis (sistema CAGED)
    var target by remember { mutableStateOf("G") }

    val targetIndex = roots.indexOf(target)
    val suggestions = easyShapes
        .map { shape ->
            val shapeIndex = roots.indexOf(shape)
            val fret = ((targetIndex - shapeIndex) % 12 + 12) % 12
            shape to fret
        }
        .sortedBy { it.second }

    ToolCard(title = "Capotraste", subtitle = "Toque com acordes abertos e soe em qualquer tom") {
        SectionLabel("A música soa em")
        Spacer(Modifier.height(8.dp))
        // seletor de tom (duas linhas)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            roots.chunked(6).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { r ->
                        val selected = r == target
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (selected) Brass else Surface2)
                                .border(1.dp, if (selected) Brass else Hairline, RoundedCornerShape(9.dp))
                                .clickable { target = r },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                r,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Ink else TextBody,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionLabel("Como tocar")
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            suggestions.forEach { (shape, fret) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Ink)
                        .border(1.dp, Hairline, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (fret == 0) "Sem capotraste" else "Casa $fret",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (fret == 0) FuncTonic else Brass,
                        modifier = Modifier.width(120.dp),
                    )
                    Text(
                        text = "formato de $shape",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextBody,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Coloque o capotraste na casa indicada e toque no formato sugerido: sai no tom de $target.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
        )
    }
}

// ---------------------------------------------------------------------------
// Base
// ---------------------------------------------------------------------------

@Composable
private fun ToolCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = TextStrong)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        Spacer(Modifier.height(16.dp))
        content()
    }
}

@Composable
private fun SmallStepper(symbol: String, desc: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Surface2)
            .border(1.dp, Hairline, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = TextStrong)
    }
}
