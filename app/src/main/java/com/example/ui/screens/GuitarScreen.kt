package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.DisposableEffect
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
import com.example.audio.ChordPlayer
import com.example.music.ChordType
import com.example.music.Guitar
import com.example.music.LabChord
import com.example.music.NeckQuiz
import com.example.music.parseCipher
import com.example.music.pitchClassCiphers
import com.example.music.ptPitchClass
import com.example.music.rootPc
import com.example.ui.components.GuitarNeck
import com.example.ui.components.NeckDot
import com.example.ui.components.DotStyle
import com.example.ui.theme.Brass
import com.example.ui.theme.Hairline
import com.example.ui.theme.Ink
import com.example.ui.theme.Surface1
import com.example.ui.theme.Surface2
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextStrong

/** As quatro partes do laboratório do braço. */
private enum class NeckTab(val title: String) { NOTAS("Notas"), ACORDES("Acordes"), HARMONIZAR("Harmonizar"), DECORAR("Decorar") }

/** Nome de uma nota no estilo escolhido: "Dó" ou "C". */
internal fun noteName(pc: Int, latin: Boolean): String = if (latin) ptPitchClass(pc) else pitchClassCiphers[Math.floorMod(pc, 12)]

/**
 * Braço do violão — um laboratório para quem está começando:
 *  • Notas: o braço inteiro com as notas; toque numa e veja TODAS as posições dela.
 *  • Acordes: escolha o acorde e ele se monta sozinho no braço, com variações;
 *    ou veja onde estão o 1, a 3 e a 5 para montar você mesmo.
 *  • Harmonizar: trocas para sair do acorde "seco" nos hinos (9, 7M, sus4…).
 *  • Decorar: o método passo a passo e jogos rápidos.
 */
@Composable
fun GuitarScreen(initialKey: String, onBack: () -> Unit, contentPadding: PaddingValues) {
    var tab by rememberSaveable { mutableStateOf(NeckTab.NOTAS) }
    var latin by rememberSaveable { mutableStateOf(true) }
    // Tom (para Acordes e Harmonizar) e acorde montado no braço.
    var keyRoot by rememberSaveable { mutableIntStateOf(rootPc(initialKey) ?: 7) }
    var minor by rememberSaveable { mutableStateOf(initialKey.endsWith("m")) }
    var chord by remember {
        mutableStateOf(LabChord(rootPc(initialKey) ?: 7, if (initialKey.endsWith("m")) ChordType.MINOR else ChordType.MAJOR))
    }
    val player = remember { ChordPlayer() }
    DisposableEffect(Unit) { onDispose { player.stop() } }

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
                Text("Notas, acordes e harmonia no braço", style = MaterialTheme.typography.labelLarge, color = Brass)
            }
        }

        // Abas do laboratório.
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Surface2)
                .border(1.dp, Hairline, RoundedCornerShape(12.dp))
                .padding(3.dp),
        ) {
            NeckTab.entries.forEach { t ->
                val on = t == tab
                Box(
                    Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (on) Brass else Surface2)
                        .clickable { tab = t; player.stop() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(t.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (on) Ink else TextBody, maxLines = 1)
                }
            }
        }

        when (tab) {
            NeckTab.NOTAS -> NotesTab(latin, onLatin = { latin = it })
            NeckTab.ACORDES -> ChordsTab(
                keyRoot, minor, chord, latin, player,
                onKey = { r, m -> keyRoot = r; minor = m },
                onChord = { chord = it },
            )
            NeckTab.HARMONIZAR -> HarmonizeTab(
                keyRoot, minor, player,
                onKey = { r, m -> keyRoot = r; minor = m },
                onOpenChord = { chord = it; tab = NeckTab.ACORDES },
            )
            NeckTab.DECORAR -> StudyTab(latin)
        }
    }
}

// ---------------------------------------------------------------------------
// Aba Notas
// ---------------------------------------------------------------------------

@Composable
private fun ColumnScope.NotesTab(latin: Boolean, onLatin: (Boolean) -> Unit) {
    var selected by rememberSaveable { mutableIntStateOf(0) }       // Dó
    var showAll by rememberSaveable { mutableStateOf(true) }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LabChip("Dó Ré Mi", latin, Modifier.weight(1f)) { onLatin(true) }
        LabChip("C D E", !latin, Modifier.weight(1f)) { onLatin(false) }
        LabChip(if (showAll) "Todas as notas" else "Só a escolhida", showAll, Modifier.weight(1.3f)) { showAll = !showAll }
    }
    (0 until 12).chunked(6).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            row.forEach { pc -> LabChip(noteName(pc, latin), pc == selected, Modifier.weight(1f)) { selected = pc } }
        }
    }

    val dots = buildList {
        for (s in 0 until Guitar.STRINGS) for (f in 0..NeckQuiz.LAST_FRET) {
            val pc = Guitar.pitchClassAt(s, f)
            when {
                pc == selected -> add(NeckDot(s, f, noteName(pc, latin), Brass, emphasis = f == 0 || f == 12))
                showAll -> add(NeckDot(s, f, noteName(pc, latin), if (pc in NATURALS) TextBody else TextMuted, DotStyle.GHOST))
            }
        }
    }
    LabCard {
        GuitarNeck(
            dots = dots,
            onTap = { s, f -> selected = Guitar.pitchClassAt(s, f) },
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Toque em qualquer casa do braço para ver todas as posições daquela nota.",
            style = MaterialTheme.typography.labelSmall, color = TextMuted,
        )
    }

    // Onde ela está, corda por corda.
    val positions = NeckQuiz.positions(selected)
    LabCard {
        Text(
            "${noteName(selected, latin)}: ${positions.size} lugares da casa 0 à 12",
            style = MaterialTheme.typography.titleMedium, color = TextStrong,
        )
        Spacer(Modifier.height(6.dp))
        (0 until Guitar.STRINGS).forEach { s ->
            val frets = positions.filter { it.first == s }.map { it.second }
            Text(
                "${6 - s}ª corda (${noteName(Guitar.pitchClassAt(s, 0), latin)}): casa " + frets.joinToString(" e "),
                style = MaterialTheme.typography.bodyMedium, color = TextBody,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Atalho da oitava: achou a nota na 6ª corda? A mesma nota está 2 cordas para o lado e 2 casas " +
                "para baixo (4ª corda). Da 5ª corda, mesma coisa até a 3ª. Da 4ª ou 3ª, são 3 casas " +
                "(por causa da afinação da 2ª corda). E a 1ª corda é igual à 6ª, na mesma casa.",
            style = MaterialTheme.typography.bodyMedium, color = TextMuted,
        )
    }
}

internal val NATURALS = setOf(0, 2, 4, 5, 7, 9, 11)

// ---------------------------------------------------------------------------
// Peças visuais em comum
// ---------------------------------------------------------------------------

@Composable
internal fun LabChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Brass else Surface2)
            .border(1.dp, if (selected) Brass else Hairline, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (selected) Ink else TextBody, maxLines = 1)
    }
}

@Composable
internal fun LabCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .padding(12.dp),
        content = content,
    )
}

/** Seletor de tom compacto: − / nome / + e Maior | Menor. */
@Composable
internal fun KeyPicker(root: Int, minor: Boolean, onKey: (Int, Boolean) -> Unit) {
    val key = pitchClassCiphers[root] + if (minor) "m" else ""
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        LabChip("−", false, Modifier.width(44.dp)) { onKey((root + 11) % 12, minor) }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text(
                "Tom: " + (HarmonicDatabase.ptNameByCipher[key] ?: key),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextStrong, maxLines = 1,
            )
        }
        LabChip("+", false, Modifier.width(44.dp)) { onKey((root + 1) % 12, minor) }
        LabChip("Maior", !minor, Modifier.width(66.dp)) { onKey(root, false) }
        LabChip("Menor", minor, Modifier.width(66.dp)) { onKey(root, true) }
    }
}

/** Acorde do campo (grau 0..6) como acorde do laboratório. */
internal fun fieldChord(cipher: String): LabChord? =
    parseCipher(cipher)?.let { LabChord(it.root, ChordType.of(it.quality)) }
