package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.HarmonicDatabase
import com.example.music.DegreeTheory
import com.example.music.Guitar
import com.example.music.ptPitchClass
import com.example.music.pitchClassCiphers
import com.example.music.rootPc
import com.example.ui.components.ChordDiagram
import com.example.ui.components.Fretboard
import com.example.ui.components.NotationSettings
import com.example.ui.components.NotationToggle
import com.example.ui.components.SectionLabel
import com.example.ui.theme.Brass
import com.example.ui.theme.Hairline
import com.example.ui.theme.Ink
import com.example.ui.theme.Surface1
import com.example.ui.theme.Surface2
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextStrong

/**
 * Braço do violão: escolha o tom e o grau; veja como montar o acorde (desenho)
 * e onde ficam, no braço inteiro, as notas da escala com o número do grau — as
 * do acorde escolhido em destaque, na cor da função dele.
 */
@Composable
fun GuitarScreen(initialKey: String, onBack: () -> Unit, contentPadding: PaddingValues) {
    var root by rememberSaveable { mutableIntStateOf(rootPc(initialKey) ?: 7) }
    var minor by rememberSaveable { mutableStateOf(initialKey.endsWith("m")) }
    var degree by rememberSaveable { mutableIntStateOf(0) }
    var sevenths by rememberSaveable { mutableStateOf(false) }
    var showNotes by rememberSaveable { mutableStateOf(false) }

    val key = pitchClassCiphers[root] + if (minor) "m" else ""
    val field = remember(key) { HarmonicDatabase.getField(key) } ?: return
    val theory = remember(key) { DegreeTheory(field) }
    val examples = if (sevenths) theory.sevenths else theory.triads
    val chosen = examples[degree]
    val color = field.chords[degree].function.color()
    val shape = remember(chosen.chord.cipher) { Guitar.shape(chosen.chord) }
    val scalePcs = theory.triads.map { it.chord.root }
    val scaleLabels = scalePcs.mapIndexed { i, pc -> pc to if (showNotes) ptPitchClass(pc) else "${i + 1}" }.toMap()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp, end = 16.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Surface1)
                    .border(1.dp, Hairline, CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = TextStrong, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Braço do violão", style = MaterialTheme.typography.headlineMedium, color = TextStrong)
                Text("Tom de ${HarmonicDatabase.ptNameByCipher[key] ?: key}", style = MaterialTheme.typography.labelLarge, color = Brass)
            }
            NotationToggle()
        }

        // Tom.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Maior", !minor, Modifier.weight(1f)) { minor = false }
            Chip("Menor", minor, Modifier.weight(1f)) { minor = true }
        }
        (0 until 12).chunked(6).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { r ->
                    Chip(pitchClassCiphers[r] + if (minor) "m" else "", r == root, Modifier.weight(1f)) { root = r }
                }
            }
        }

        // Graus do campo.
        SectionLabel("Graus do campo — toque para ver no braço")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            examples.forEachIndexed { i, ex ->
                val c = field.chords[i].function.color()
                val on = i == degree
                val main = if (NotationSettings.byDegree) ex.label else ex.chord.cipher
                val sub = if (NotationSettings.byDegree) ex.chord.cipher else ex.label
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (on) c.copy(alpha = 0.18f) else Surface1)
                        .border(if (on) 2.dp else 1.dp, if (on) c else Hairline, RoundedCornerShape(10.dp))
                        .clickable { degree = i }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(main, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = c, maxLines = 1)
                    Text(sub, style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 1)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Tríades", !sevenths, Modifier.weight(1f)) { sevenths = false }
            Chip("Com sétima", sevenths, Modifier.weight(1f)) { sevenths = true }
        }

        // Desenho do acorde + explicação.
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Surface1)
                .border(1.dp, Hairline, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) { ChordDiagram(shape, color) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1.1f)) {
                Text("Grau ${chosen.label}", style = MaterialTheme.typography.labelLarge, color = color)
                Text(chosen.chord.cipher, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = TextStrong)
                Text(
                    "${ptPitchClass(chosen.chord.root)} ${chosen.chord.quality.ptName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextBody,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Notas: " + chosen.chord.pitchClasses.joinToString(" · ") { ptPitchClass(it) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextBody,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    when {
                        shape.barre != null -> "Pestana na ${shape.barre}ª casa (dedo 1 deitado)."
                        shape.frets.any { it == 0 } -> "Forma aberta: usa cordas soltas."
                        else -> "Forma sem pestana."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }
        }

        // Mapa do braço.
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Surface1)
                .border(1.dp, Hairline, RoundedCornerShape(14.dp))
                .padding(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("A escala no braço", style = MaterialTheme.typography.titleMedium, color = TextStrong, modifier = Modifier.weight(1f))
                Chip("Graus", !showNotes, Modifier.width(76.dp)) { showNotes = false }
                Spacer(Modifier.width(6.dp))
                Chip("Notas", showNotes, Modifier.width(76.dp)) { showNotes = true }
            }
            Spacer(Modifier.height(8.dp))
            Fretboard(
                scaleLabels = scaleLabels,
                chordTones = chosen.chord.pitchClasses.toSet(),
                chordRoot = chosen.chord.root,
                chordColor = color,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Cheias: notas do acorde ${chosen.chord.cipher} (contorno = fundamental). Apagadas: o resto da " +
                    "escala. 1ª corda (Mi agudo) em cima; casas de 0 a 12.",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
            )
        }

        // Dica didática.
        Text(
            "Dica: o número do grau não muda de tom para tom. Aprenda onde ficam o 1, o 4 e o 5 " +
                "no braço e você acha esses acordes em qualquer tonalidade — só muda a casa.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextBody,
        )
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Brass else Surface2)
            .border(1.dp, if (selected) Brass else Hairline, RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (selected) Ink else TextBody, maxLines = 1)
    }
}
