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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.audio.ChordPlayer
import com.example.music.ChordShapes
import com.example.music.LabChord
import com.example.music.Passage
import com.example.music.PassageLevel
import com.example.music.PassageMove
import com.example.music.PassageStep
import com.example.music.Passages
import com.example.music.rootPc
import com.example.ui.components.ChordDiagram
import com.example.ui.components.NotationSettings
import com.example.ui.components.NotationToggle
import com.example.ui.components.SectionLabel
import com.example.ui.theme.Brass
import com.example.ui.theme.Hairline
import com.example.ui.theme.Surface1
import com.example.ui.theme.Surface2
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextStrong

/** A forma usada para tocar e desenhar cada acorde (a mais usada). */
private fun shapeOf(chord: LabChord) = ChordShapes.common(chord).firstOrNull()?.voicing

private fun play(player: ChordPlayer, steps: List<PassageStep>) {
    val notes = steps.mapNotNull { shapeOf(it.chord)?.midiNotes }
    if (notes.isNotEmpty()) player.play(notes, secondsEach = 1.15, strum = 0.03)
}

/**
 * Passagens e enfeites: as "graças" que ligam um acorde ao outro no hino — a
 * caída para o 1, o baixo descendo, o diminuto subindo, o final de Amém.
 * Escolha o tom e o momento da música; cada ideia vem com os acordes no tom
 * (cifra ou grau), o porquê, o som de antes e depois e o desenho no braço.
 */
@Composable
fun PassagesScreen(
    initialKey: String,
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    onOpenDictionary: (LabChord) -> Unit = {},
) {
    var root by rememberSaveable { mutableIntStateOf(rootPc(initialKey) ?: 7) }
    var minor by rememberSaveable { mutableStateOf(initialKey.endsWith("m")) }
    var moveName by rememberSaveable { mutableStateOf(PassageMove.VOLTAR.name) }
    val moves = Passages.moves(minor)
    val move = PassageMove.valueOf(moveName).takeIf { it in moves } ?: moves.first()
    val plain = remember(root, minor, move) { Passages.plain(move, root, minor) }
    val passages = remember(root, minor, move) { Passages.forMove(move, root, minor) }

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
                Text("Passagens e enfeites", style = MaterialTheme.typography.headlineMedium, color = TextStrong)
                Text("As \"graças\" que ligam um acorde ao outro", style = MaterialTheme.typography.labelLarge, color = Brass)
            }
        }

        KeyPicker(root, minor) { r, m -> root = r; minor = m; player.stop() }

        // Em que momento do hino?
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Em que momento do hino?", Modifier.weight(1f))
            NotationToggle()
        }
        moves.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { m ->
                    LabChip(if (minor) m.minorTitle else m.title, m == move, Modifier.weight(1f)) { moveName = m.name; player.stop() }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        // O caminho direto, para comparar.
        LabCard {
            Text(move.hint, style = MaterialTheme.typography.bodyMedium, color = TextBody)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Direto", style = MaterialTheme.typography.labelLarge, color = TextMuted, modifier = Modifier.width(56.dp))
                Box(Modifier.weight(1f)) { StepRow(plain) }
                Spacer(Modifier.width(6.dp))
                LabChip("▶", false, Modifier.width(44.dp)) { play(player, plain) }
            }
        }

        passages.forEach { p -> PassageCard(p, plain, player, onOpenDictionary) }

        LabCard {
            Text("Como usar no culto", style = MaterialTheme.typography.titleMedium, color = TextStrong)
            Spacer(Modifier.height(6.dp))
            listOf(
                "Use a passagem onde a melodia segura uma nota longa (fim de frase): é o espaço do enfeite.",
                "Os acordes de passagem duram pouco — meio tempo ou um tempo cada. O acorde de chegada dura mais.",
                "Comece pelas de nível Básico. Quando ficar natural, troque por uma Intermediária no mesmo lugar.",
                "Acordes marcados \"fora\" saem do campo: soam especiais, então use com moderação (1 por estrofe).",
                "Tocando com baixo e teclado, combine antes: a passagem fica bonita quando todos fazem juntos.",
            ).forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, color = TextBody, modifier = Modifier.padding(vertical = 2.dp)) }
        }
    }
}

/** Os acordes de uma sequência, na largura da tela (quebra a linha, nunca rola para o lado). */
@Composable
private fun StepRow(steps: List<PassageStep>) {
    val perRow = if (steps.size <= 5) steps.size.coerceAtLeast(1) else 4
    val byDegree = NotationSettings.byDegree
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        steps.chunked(perRow).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                row.forEach { st ->
                    val c = st.function.color()
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Surface2)
                            .border(1.dp, c.copy(alpha = if (st.outside) 0.9f else 0.45f), RoundedCornerShape(9.dp))
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            if (byDegree) st.label else st.chord.cipher,
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = c,
                            maxLines = 1, softWrap = false, overflow = TextOverflow.Clip,
                        )
                        Text(
                            (if (byDegree) st.chord.cipher else st.label) + if (st.outside) " · fora" else "",
                            style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 1,
                        )
                    }
                }
                repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PassageCard(p: Passage, plain: List<PassageStep>, player: ChordPlayer, onOpenDictionary: (LabChord) -> Unit) {
    var showShapes by remember(p) { mutableStateOf(false) }
    LabCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(p.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextStrong, modifier = Modifier.weight(1f))
            val levelColor = when (p.level) {
                PassageLevel.BASICO -> TextBody
                PassageLevel.MEDIO -> Brass
                PassageLevel.PRO -> TextStrong
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, levelColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(p.level.title, style = MaterialTheme.typography.labelSmall, color = levelColor)
            }
        }
        Spacer(Modifier.height(8.dp))
        StepRow(p.steps)
        Spacer(Modifier.height(8.dp))
        Text(p.why, style = MaterialTheme.typography.bodyMedium, color = TextBody)
        if (p.outsideCount > 0) {
            Text(
                if (p.outsideCount == 1) "Usa 1 acorde de fora do campo." else "Usa ${p.outsideCount} acordes de fora do campo.",
                style = MaterialTheme.typography.labelSmall, color = TextMuted,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LabChip(if (player.isPlaying) "Tocando…" else "▶ Ouvir", true, Modifier.weight(1.1f)) { play(player, p.steps) }
            LabChip("▶ Direto", false, Modifier.weight(1f)) { play(player, plain) }
            LabChip(if (showShapes) "Braço ⌃" else "No braço ⌄", showShapes, Modifier.weight(1f)) { showShapes = !showShapes }
        }
        if (showShapes) {
            Spacer(Modifier.height(8.dp))
            val per = if (p.steps.size <= 4) 4 else 5
            p.steps.chunked(per).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { st ->
                        val c = st.function.color()
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Surface2)
                                .clickable(enabled = st.chord.bass == null) { onOpenDictionary(st.chord) }
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            shapeOf(st.chord)?.let { ChordDiagram(it.shape, c) }
                            Text(st.chord.cipher, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = c, maxLines = 1)
                        }
                    }
                    repeat(per - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(6.dp))
            }
            Text(
                "Toque num desenho para ver todas as formas desse acorde no Dicionário.",
                style = MaterialTheme.typography.labelSmall, color = TextMuted,
            )
        }
    }
}

