package com.example.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.HarmonicDatabase
import com.example.R
import com.example.audio.Metronome
import com.example.music.pitchClassCiphers
import com.example.ui.components.OptionChip
import com.example.ui.components.Panel
import com.example.ui.components.PrimaryButton
import com.example.ui.components.RoundIconButton
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SecondaryButton
import com.example.ui.components.SectionLabel
import com.example.ui.components.Segmented
import com.example.ui.theme.Brass
import com.example.ui.theme.Hairline
import com.example.ui.theme.Selection
import com.example.ui.theme.Surface2
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextStrong

/** Coluna rolável padrão das telas de ferramenta (margens e respiro do sistema). */
@Composable
private fun ToolColumn(contentPadding: PaddingValues, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp, end = 16.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) { content() }
}

// ---------------------------------------------------------------------------
// Metrônomo
// ---------------------------------------------------------------------------

/**
 * Metrônomo do culto: BPM grande, batidas do compasso (a primeira, a forte, em
 * latão), ajuste fino, "marcar tempo" tocando na tela e escolha do compasso.
 */
@Composable
fun MetronomeScreen(onBack: () -> Unit, contentPadding: PaddingValues) {
    val metronome = remember { Metronome() }
    DisposableEffect(Unit) { onDispose { metronome.stop() } }
    var bpm by rememberSaveable { mutableIntStateOf(90) }
    var beats by rememberSaveable { mutableIntStateOf(4) }
    var taps by remember { mutableStateOf(listOf<Long>()) }
    LaunchedEffect(bpm) { metronome.bpm = bpm }
    LaunchedEffect(beats) { metronome.beatsPerMeasure = beats }

    ToolColumn(contentPadding) {
        ScreenHeader("Metrônomo", subtitle = "Tempo firme para ensaio e culto", onBack = onBack)

        Panel {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Text("$bpm", style = MaterialTheme.typography.displayLarge, color = TextStrong)
                Spacer(Modifier.width(8.dp))
                Text("batidas por minuto", style = MaterialTheme.typography.bodyMedium, color = TextMuted, modifier = Modifier.padding(bottom = 8.dp))
            }
            Spacer(Modifier.height(16.dp))
            // Batidas do compasso: a 1ª (forte) acende em latão, as outras em claro.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                for (b in 0 until beats) {
                    val active = metronome.isPlaying && metronome.currentBeat == b
                    Box(
                        Modifier
                            .weight(1f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                when {
                                    active && b == 0 -> Brass
                                    active -> Selection
                                    else -> Surface2
                                }
                            )
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Slider(
                value = bpm.toFloat(),
                onValueChange = { bpm = it.toInt().coerceIn(30, 240) },
                valueRange = 30f..240f,
                colors = SliderDefaults.colors(thumbColor = Selection, activeTrackColor = TextBody, inactiveTrackColor = Hairline),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RoundIconButton(R.drawable.ic_minus, "Diminuir 1 BPM") { bpm = (bpm - 1).coerceIn(30, 240) }
                if (metronome.isPlaying) {
                    SecondaryButton("Parar", Modifier.weight(1f).height(52.dp), icon = R.drawable.ic_stop) { metronome.toggle() }
                } else {
                    PrimaryButton("Iniciar", Modifier.weight(1f), icon = R.drawable.ic_play) { metronome.toggle() }
                }
                RoundIconButton(R.drawable.ic_plus, "Aumentar 1 BPM") { bpm = (bpm + 1).coerceIn(30, 240) }
            }
        }

        SecondaryButton("Marcar o tempo (toque no ritmo da música)", Modifier.fillMaxWidth().height(56.dp)) {
            val now = SystemClock.uptimeMillis()
            val recent = (taps + now).filter { now - it < 2500 }.takeLast(5)
            taps = recent
            if (recent.size >= 2) {
                val avg = recent.zipWithNext { a, b -> (b - a).toDouble() }.average()
                if (avg > 0) bpm = (60000.0 / avg).toInt().coerceIn(30, 240)
            }
        }

        SectionLabel("Compasso")
        Segmented(listOf("2/4", "3/4", "4/4", "6/8"), listOf(2, 3, 4, 6).indexOf(beats).coerceAtLeast(0), { beats = listOf(2, 3, 4, 6)[it] }, Modifier.fillMaxWidth())
        Text(
            "Hinos lentos costumam ficar entre 60 e 80. Em 3/4 (valsa) a 1ª batida de cada grupo de três é a forte.",
            style = MaterialTheme.typography.bodyMedium, color = TextBody,
        )
    }
}

// ---------------------------------------------------------------------------
// Capotraste
// ---------------------------------------------------------------------------

/**
 * Capotraste: escolha o tom em que a música soa e veja em que casa colocar o
 * capo para tocar com um formato aberto (Dó, Lá, Sol, Mi ou Ré).
 */
@Composable
fun CapoScreen(onBack: () -> Unit, contentPadding: PaddingValues) {
    val easyShapes = listOf(0 to "Dó", 9 to "Lá", 7 to "Sol", 4 to "Mi", 2 to "Ré") // formatos abertos (CAGED)
    var target by rememberSaveable { mutableIntStateOf(7) }
    val suggestions = easyShapes
        .map { (pc, name) -> Triple(pitchClassCiphers[pc], name, Math.floorMod(target - pc, 12)) }
        .sortedBy { it.third }
    val targetName = HarmonicDatabase.ptNameByCipher[pitchClassCiphers[target]] ?: pitchClassCiphers[target]

    ToolColumn(contentPadding) {
        ScreenHeader("Capotraste", subtitle = "Acordes abertos em qualquer tom", onBack = onBack)
        SectionLabel("A música soa em")
        (0 until 12).chunked(6).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { pc -> OptionChip(pitchClassCiphers[pc], pc == target, Modifier.weight(1f)) { target = pc } }
            }
        }
        SectionLabel("Como tocar em $targetName")
        Panel {
            suggestions.forEachIndexed { i, (cipher, name, fret) ->
                if (i > 0) Box(Modifier.fillMaxWidth().padding(vertical = 10.dp).height(1.dp).background(Hairline))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (fret == 0) "Sem capo" else "Casa $fret",
                        style = MaterialTheme.typography.titleLarge, color = TextStrong, modifier = Modifier.width(120.dp),
                    )
                    Column(Modifier.weight(1f)) {
                        Text("Formato de $name ($cipher)", style = MaterialTheme.typography.bodyLarge, color = TextBody)
                        if (i == 0) Text("O mais perto da cabeça do violão", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                }
            }
        }
        Text(
            "Coloque o capotraste na casa indicada e toque os acordes do formato: o som sai em $targetName. " +
                "Os números dos graus não mudam com o capo.",
            style = MaterialTheme.typography.bodyMedium, color = TextBody,
        )
    }
}
