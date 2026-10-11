package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.HarmonicDatabase
import com.example.audio.ChordPlayer
import com.example.music.ChordType
import com.example.music.Guitar
import com.example.music.Harmonization
import com.example.music.Harmonize
import com.example.music.HarmonicFunction
import com.example.music.LabChord
import com.example.music.ChordShapes
import com.example.music.Voicing
import com.example.music.Voicings
import com.example.music.functionForDegree
import com.example.music.pitchClassCiphers
import com.example.music.rootPc
import com.example.ui.components.ChordDiagram
import com.example.ui.components.DotStyle
import com.example.ui.components.GuitarNeck
import com.example.ui.components.NeckBarre
import com.example.ui.components.NeckDot
import com.example.ui.components.NotationSettings
import com.example.ui.components.NotationToggle
import com.example.ui.components.SectionLabel
import com.example.ui.components.chordLabels
import com.example.ui.theme.FuncNeutral
import com.example.ui.theme.Hairline
import com.example.ui.theme.Surface1
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextStrong

/** Notas "de base" (desenhadas cheias); as outras são notas de cor (anel). */
private val CORE_ROLES = setOf("1", "3", "3m", "5", "5b", "5#")

/** Grau (0..6) do acorde no tom, se a fundamental for da escala. */
private fun degreeOf(chordRoot: Int, keyRoot: Int, minor: Boolean): Int? {
    val scale = if (!minor) listOf(0, 2, 4, 5, 7, 9, 11) else listOf(0, 2, 3, 5, 7, 8, 10)
    return scale.indexOf(Math.floorMod(chordRoot - keyRoot, 12)).takeIf { it >= 0 }
}

/** Cor do acorde pela função tonal no tom (fora do campo: neutro). */
private fun chordColor(chord: LabChord, keyRoot: Int, minor: Boolean): Color {
    val d = degreeOf(chord.root, keyRoot, minor) ?: return FuncNeutral
    // Acorde maior com sétima fora do 5 é "dominante secundária": tem função de dominante.
    if (chord.type == ChordType.DOM7 && d != 4 && !(minor && d == 6)) return HarmonicFunction.DOMINANT.color()
    return functionForDegree(d, minor).color()
}

/** Rótulo por grau no tom: "1(9)", "4⁷M", "5sus4"… (fora do campo: a cifra). */
private fun degreeName(chord: LabChord, keyRoot: Int, minor: Boolean): String {
    val d = degreeOf(chord.root, keyRoot, minor) ?: return chord.cipher
    val bass = chord.bass?.let { b -> degreeOf(b, keyRoot, minor)?.let { "/${it + 1}" } ?: "/" + pitchClassCiphers[b] } ?: ""
    return "${d + 1}${chord.type.degreeSuffix}$bass"
}

private fun typeChipLabel(t: ChordType) = when (t) {
    ChordType.MAJOR -> "maior"
    else -> t.suffix
}

private fun Voicing.toDots(chord: LabChord, color: Color): List<NeckDot> = strings.map { s ->
    val pc = Guitar.pitchClassAt(s, frets[s])
    val role = chord.role(pc) ?: "b"            // só acontece com o baixo de "C/D"
    NeckDot(s, frets[s], role, color, if (role in CORE_ROLES || role == "b") DotStyle.FILLED else DotStyle.RING, emphasis = role == "1")
}

internal fun Voicing.barreOnNeck(color: Color): NeckBarre? = barre?.let { b ->
    NeckBarre(b, barreFrom.coerceAtLeast(0), strings.last(), color)
}

private fun play(player: ChordPlayer, chords: List<LabChord>, seconds: Double = 1.4) {
    val notes = chords.mapNotNull { Voicings.best(it)?.midiNotes }
    if (notes.isNotEmpty()) player.play(notes, secondsEach = seconds, strum = 0.035)
}

// ---------------------------------------------------------------------------
// Aba Acordes: montar o acorde no braço
// ---------------------------------------------------------------------------

@Composable
internal fun ColumnScope.ChordsTab(
    keyRoot: Int,
    minor: Boolean,
    chord: LabChord,
    latin: Boolean,
    player: ChordPlayer,
    onKey: (Int, Boolean) -> Unit,
    onChord: (LabChord) -> Unit,
    onOpenDictionary: (LabChord) -> Unit = {},
) {
    val key = pitchClassCiphers[keyRoot] + if (minor) "m" else ""
    val field = remember(key) { HarmonicDatabase.getField(key) }
    var assembled by rememberSaveable { mutableStateOf(true) }
    val shownRoles = remember(chord.type) { mutableStateOf(chord.type.intervals.map { chord.type.role(it) }.toSet()) }
    // As formas mais usadas (CAGED), as mesmas do Dicionário de acordes.
    val voicings = remember(chord) { ChordShapes.common(chord).map { it.voicing } }
    var vIdx by remember(chord) { mutableIntStateOf(0) }
    val color = chordColor(chord, keyRoot, minor)

    KeyPicker(keyRoot, minor) { r, m ->
        onKey(r, m)
        HarmonicDatabase.getField(pitchClassCiphers[r] + if (m) "m" else "")?.chords?.firstOrNull()
            ?.let { fieldChord(it.cipher) }?.let(onChord)
    }

    // Graus do campo (atalho para os acordes do tom).
    Row(verticalAlignment = Alignment.CenterVertically) {
        SectionLabel("Acordes do tom", Modifier.weight(1f))
        NotationToggle()
    }
    field?.let { f ->
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            f.chords.forEachIndexed { i, c ->
                val fc = c.function.color()
                val on = rootPc(c.cipher) == chord.root
                val (main, sub) = chordLabels(i, c.cipher)
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (on) fc.copy(alpha = 0.18f) else Surface1)
                        .border(if (on) 2.dp else 1.dp, if (on) fc else Hairline, RoundedCornerShape(10.dp))
                        .clickable { fieldChord(c.cipher)?.let(onChord) }
                        .padding(vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(main, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = fc, maxLines = 1)
                    Text(sub, style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 1)
                }
            }
        }
    }

    // Tipo do acorde (o "tempero").
    SectionLabel("Tipo de acorde")
    ChordType.entries.chunked(5).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            row.forEach { t -> LabChip(typeChipLabel(t), t == chord.type && chord.bass == null, Modifier.weight(1f)) { onChord(LabChord(chord.root, t)) } }
            repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }

    // O acorde escolhido.
    LabCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                val byDegree = NotationSettings.byDegree
                val dn = degreeName(chord, keyRoot, minor)
                Text(if (byDegree) dn else chord.cipher, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = color)
                Text(
                    (if (byDegree) chord.cipher else "Grau $dn") + " · " + noteName(chord.root, true) + " " + chord.type.ptName,
                    style = MaterialTheme.typography.bodyMedium, color = TextBody,
                )
            }
            LabChip(if (player.isPlaying) "Tocando…" else "▶ Ouvir", true, Modifier.width(104.dp)) {
                voicings.getOrNull(vIdx)?.let { player.play(listOf(it.midiNotes), secondsEach = 2.4, strum = 0.04) }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Notas: " + chord.type.intervals.joinToString("  ·  ") { iv ->
                noteName((chord.root + iv) % 12, latin) + " (" + chord.type.role(iv) + ")"
            } + (chord.bass?.let { "  ·  baixo " + noteName(it, latin) } ?: ""),
            style = MaterialTheme.typography.bodyMedium, color = TextStrong,
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LabChip("Montado (forma pronta)", assembled, Modifier.weight(1.2f)) { assembled = true }
        LabChip("Onde estão 1 · 3 · 5", !assembled, Modifier.weight(1f)) { assembled = false }
    }

    if (assembled) {
        val v = voicings.getOrNull(vIdx)
        if (v == null) {
            Text("Não achei uma forma confortável para este acorde.", color = TextMuted)
            return
        }
        // Variações espalhadas pelo braço.
        voicings.withIndex().chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { (i, vv) -> LabChip("${i + 1} · ${vv.position}", i == vIdx, Modifier.weight(1f)) { vIdx = i } }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        LabCard {
            // Zoom no trecho do braço onde a forma está (6 casas).
            val first = if (v.maxFret <= 5) 1 else (v.minFret - 1).coerceAtLeast(1)
            GuitarNeck(
                dots = v.toDots(chord, color),
                firstFret = first,
                lastFret = first + 5,
                muted = v.frets.indices.filter { v.frets[it] < 0 }.toSet(),
                barre = v.barreOnNeck(color),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Do grave ao agudo: " + v.strings.joinToString(" · ") { s ->
                    val pc = Guitar.pitchClassAt(s, v.frets[s])
                    noteName(pc, latin) + " (" + (chord.role(pc) ?: "baixo") + ")"
                },
                style = MaterialTheme.typography.bodyMedium, color = TextBody,
            )
            Text(
                "${v.fingers} dedo(s)" + (v.barre?.let { " · pestana na casa $it" } ?: "") +
                    (if (v.usesOpen) " · usa corda solta" else "") +
                    " · bolinha cheia = base (1, 3, 5) · anel = nota de cor (7, 9, 6, 4)",
                style = MaterialTheme.typography.labelSmall, color = TextMuted,
            )
        }
        // Todas as variações em desenho, para comparar.
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Formas mais usadas", Modifier.weight(1f))
            LabChip("Todas as formas ›", false, Modifier.width(150.dp)) { onOpenDictionary(chord) }
        }
        voicings.withIndex().chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (i, vv) ->
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (i == vIdx) color.copy(alpha = 0.14f) else Surface1)
                            .border(if (i == vIdx) 2.dp else 1.dp, if (i == vIdx) color else Hairline, RoundedCornerShape(12.dp))
                            .clickable { vIdx = i }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ChordDiagram(vv.shape, color)
                        Text("${i + 1} · ${vv.position}", style = MaterialTheme.typography.labelSmall, color = TextBody, maxLines = 1)
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    } else {
        // Montar sozinho: mostra cada papel do acorde pelo braço.
        val roles = chord.type.intervals.map { chord.type.role(it) }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            roles.forEach { r ->
                val on = r in shownRoles.value
                LabChip(r, on, Modifier.weight(1f)) {
                    shownRoles.value = if (on) shownRoles.value - r else shownRoles.value + r
                }
            }
        }
        val dots = buildList {
            for (s in 0 until Guitar.STRINGS) for (f in 0..12) {
                val pc = Guitar.pitchClassAt(s, f)
                val role = chord.role(pc) ?: continue
                if (role !in shownRoles.value) continue
                add(NeckDot(s, f, role, color, if (role in CORE_ROLES) DotStyle.FILLED else DotStyle.RING, emphasis = role == "1"))
            }
        }
        LabCard {
            GuitarNeck(dots = dots)
        }
        LabCard {
            Text("Como montar sozinho", style = MaterialTheme.typography.titleMedium, color = TextStrong)
            Spacer(Modifier.height(6.dp))
            listOf(
                "1. Deixe só o \"1\" ligado e escolha um na 6ª ou na 5ª corda — ele é o baixo do acorde.",
                "2. Ligue a \"3\" (ou \"3m\") e procure uma nas cordas de cima, perto do 1 (até 3 casas). É ela que diz se o acorde é maior ou menor.",
                "3. Ligue a \"5\" e complete. Pode repetir o 1 e a 5 em outras cordas — repetir a 3 deixa o som pesado.",
                "4. Quer mais cor? Troque um 1 ou uma 5 repetida pela 9, 7M ou 6. Na 1ª corda (a mais aguda) ela aparece mais.",
            ).forEach { Text(it, style = MaterialTheme.typography.bodyMedium, color = TextBody, modifier = Modifier.padding(vertical = 2.dp)) }
        }
    }
}

// ---------------------------------------------------------------------------
// Aba Harmonizar: sair do acorde "seco"
// ---------------------------------------------------------------------------

@Composable
internal fun ColumnScope.HarmonizeTab(
    keyRoot: Int,
    minor: Boolean,
    player: ChordPlayer,
    onKey: (Int, Boolean) -> Unit,
    onOpenChord: (LabChord) -> Unit,
) {
    var degree by rememberSaveable { mutableIntStateOf(0) }
    val key = pitchClassCiphers[keyRoot] + if (minor) "m" else ""
    val field = remember(key) { HarmonicDatabase.getField(key) }
    val (dry, rich) = remember(key) { Harmonize.beforeAfter(minor, keyRoot) }
    val ideas = remember(key, degree) { Harmonize.forDegree(minor, keyRoot, degree) }
    val byDegree = NotationSettings.byDegree

    KeyPicker(keyRoot, minor, onKey)

    // Antes e depois: o mesmo trecho, seco e harmonioso.
    LabCard {
        Text("Ouça a diferença", style = MaterialTheme.typography.titleMedium, color = TextStrong)
        Spacer(Modifier.height(8.dp))
        BeforeAfterRow("Seco", dry, keyRoot, minor, byDegree) { play(player, dry.map { it.chord }) }
        Spacer(Modifier.height(8.dp))
        BeforeAfterRow("Harmonioso", rich, keyRoot, minor, byDegree) { play(player, rich.map { it.chord }) }
        Spacer(Modifier.height(6.dp))
        Text(
            "Mesma função, mesmo caminho — só mudou o tempero de cada acorde.",
            style = MaterialTheme.typography.labelSmall, color = TextMuted,
        )
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        SectionLabel("Escolha o grau", Modifier.weight(1f))
        NotationToggle()
    }
    field?.let { f ->
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            f.chords.forEachIndexed { i, c ->
                val fc = c.function.color()
                val on = i == degree
                val (main, sub) = chordLabels(i, c.cipher)
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (on) fc.copy(alpha = 0.18f) else Surface1)
                        .border(if (on) 2.dp else 1.dp, if (on) fc else Hairline, RoundedCornerShape(10.dp))
                        .clickable { degree = i }
                        .padding(vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(main, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = fc, maxLines = 1)
                    Text(sub, style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 1)
                }
            }
        }
    }

    ideas.forEach { idea -> IdeaCard(idea, keyRoot, minor, byDegree, player, onOpenChord) }

    LabCard {
        Text("Regras de ouro no culto", style = MaterialTheme.typography.titleMedium, color = TextStrong)
        Spacer(Modifier.height(6.dp))
        listOf(
            "Não troque tudo de uma vez: 1 ou 2 acordes \"temperados\" por estrofe já mudam o hino.",
            "O (9) é o curinga: cabe no 1, no 4 e no 6m quase sempre, sem brigar com a melodia.",
            "Cuidado com o 7M quando a voz canta a nota do 1 — as duas notas ficam coladas e soa errado.",
            "sus4 é tensão: segure e resolva no acorde normal (5sus4 → 5 → 1).",
            "Com teclado e baixo junto, prefira formas mais agudas (casa 5 em diante): o violão não embola com eles.",
            "No final do hino (\"Amém\"), o 1⁶⁽⁹⁾ ou o 4m → 1 dão aquele fechamento de igreja.",
        ).forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, color = TextBody, modifier = Modifier.padding(vertical = 2.dp)) }
    }
}

@Composable
private fun BeforeAfterRow(title: String, items: List<Harmonization>, keyRoot: Int, minor: Boolean, byDegree: Boolean, onPlay: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = TextMuted, modifier = Modifier.width(84.dp))
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items.forEach { h ->
                val c = chordColor(h.chord, keyRoot, minor)
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(c.copy(alpha = 0.14f))
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (byDegree) h.label else h.chord.cipher, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = c, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.width(6.dp))
        LabChip("▶", false, Modifier.width(44.dp), onPlay)
    }
}

@Composable
private fun IdeaCard(
    idea: Harmonization,
    keyRoot: Int,
    minor: Boolean,
    byDegree: Boolean,
    player: ChordPlayer,
    onOpenChord: (LabChord) -> Unit,
) {
    val color = chordColor(idea.chord, keyRoot, minor)
    val v = remember(idea.chord) { Voicings.best(idea.chord) }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(78.dp)) { v?.let { ChordDiagram(it.shape, color) } }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(if (byDegree) idea.label else idea.chord.cipher, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
                Spacer(Modifier.width(8.dp))
                Text(if (byDegree) idea.chord.cipher else idea.label, style = MaterialTheme.typography.labelLarge, color = TextMuted)
            }
            Text(idea.why, style = MaterialTheme.typography.bodyMedium, color = TextBody)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LabChip("▶ Ouvir", false, Modifier.weight(1f)) {
                    v?.let { player.play(listOf(it.midiNotes), secondsEach = 2.2, strum = 0.04) }
                }
                LabChip("No braço ›", false, Modifier.weight(1f)) { onOpenChord(idea.chord) }
            }
        }
    }
}
