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
import com.example.R
import com.example.ui.components.RoundIconButton
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SecondaryButton
import com.example.ui.components.Segmented
import com.example.ui.theme.Selection
import com.example.ui.theme.Surface3
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.ChordPlayer
import com.example.music.ChordShapes
import com.example.music.ChordType
import com.example.music.Guitar
import com.example.music.LabChord
import com.example.music.NamedVoicing
import com.example.music.pitchClassCiphers
import com.example.music.ptPitchClass
import com.example.ui.components.ChordDiagram
import com.example.ui.components.DotStyle
import com.example.ui.components.GuitarNeck
import com.example.ui.components.NeckDot
import com.example.ui.components.SectionLabel
import com.example.ui.theme.Brass
import com.example.ui.theme.Hairline
import com.example.ui.theme.Surface1
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextStrong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Filtros da lista "Todas". */
private enum class ShapeFilter(val title: String) {
    TODAS("Todas"), SEM_PESTANA("Sem pestana"), SOLTAS("Corda solta"),
    BAIXO6("Baixo na 6ª"), BAIXO5("Baixo na 5ª"), BAIXO4("Baixo na 4ª");

    fun accepts(n: NamedVoicing): Boolean {
        val v = n.voicing
        return when (this) {
            TODAS -> true
            SEM_PESTANA -> v.barre == null
            SOLTAS -> v.usesOpen
            BAIXO6 -> v.strings.first() == 0
            BAIXO5 -> v.strings.first() == 1
            BAIXO4 -> v.strings.first() == 2
        }
    }
}

private const val PAGE = 24

/**
 * Dicionário de acordes: escolha a nota e o tipo e veja as formas MAIS USADAS
 * (as 5 do sistema CAGED, as das revistinhas) ou TODAS as formas tocáveis no
 * braço. A forma escolhida aparece grande no braço, com o número do dedo em
 * cada casa e o passo a passo de como montar.
 */
@Composable
fun ChordDictionaryScreen(initial: LabChord, onBack: () -> Unit, contentPadding: PaddingValues) {
    var root by rememberSaveable { mutableIntStateOf(initial.root) }
    var typeName by rememberSaveable { mutableStateOf(initial.type.name) }
    val type = ChordType.valueOf(typeName)
    val chord = LabChord(root, type)
    var showAll by rememberSaveable { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf(ShapeFilter.TODAS) }
    var fingers by rememberSaveable { mutableStateOf(true) }
    var page by remember(chord, filter) { mutableIntStateOf(1) }
    var selected by remember(chord, showAll, filter) { mutableIntStateOf(0) }

    val common = remember(chord) { ChordShapes.common(chord) }
    // "Todas" dá mais trabalho para calcular: roda fora da tela.
    val all by produceState<List<NamedVoicing>?>(null, chord) {
        value = null
        value = withContext(Dispatchers.Default) { ChordShapes.all(chord) }
    }
    val list = if (showAll) all.orEmpty().filter { filter.accepts(it) } else common
    val current = list.getOrNull(selected.coerceAtMost((list.size - 1).coerceAtLeast(0)))

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
        ScreenHeader("Dicionário de acordes", subtitle = "Todas as formas de tocar cada acorde", onBack = onBack)

        // Nota e tipo.
        SectionLabel("Nota")
        (0 until 12).chunked(6).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { pc -> LabChip(pitchClassCiphers[pc], pc == root, Modifier.weight(1f)) { root = pc } }
            }
        }
        SectionLabel("Tipo")
        ChordType.entries.chunked(5).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                row.forEach { t ->
                    LabChip(if (t == ChordType.MAJOR) "maior" else t.suffix, t == type, Modifier.weight(1f)) { typeName = t.name }
                }
                repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        // O acorde.
        LabCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(chord.cipher, style = MaterialTheme.typography.displaySmall, color = TextStrong)
                    Text("${ptPitchClass(chord.root)} ${chord.type.ptName}", style = MaterialTheme.typography.bodyMedium, color = TextBody)
                }
                SecondaryButton(if (player.isPlaying) "Tocando…" else "Ouvir", icon = R.drawable.ic_play) {
                    current?.let { player.play(listOf(it.voicing.midiNotes), secondsEach = 2.4, strum = 0.04) }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Notas: " + chord.type.intervals.joinToString(", ") { iv ->
                    ptPitchClass((chord.root + iv) % 12) + " (" + chord.type.role(iv) + ")"
                },
                style = MaterialTheme.typography.bodyMedium, color = TextStrong,
            )
        }

        // Mais usadas × todas.
        Segmented(
            listOf("Mais usadas (${common.size})", "Todas (${all?.size ?: "…"})"),
            if (showAll) 1 else 0, { showAll = it == 1 }, Modifier.fillMaxWidth(),
        )
        if (showAll) {
            ShapeFilter.entries.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { f -> LabChip(f.title, f == filter, Modifier.weight(1f)) { filter = f } }
                }
            }
        }

        if (showAll && all == null) {
            Text("Procurando todas as formas no braço…", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        } else if (current == null) {
            Text("Nenhuma forma com esse filtro.", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        } else {
            ShapeDetail(chord, current, selected, list.size, fingers, onFingers = { fingers = it }) { selected = it }
            SectionLabel(if (showAll) "Todas as formas no braço. Toque numa para ver grande." else "As formas mais usadas. Toque numa para ver grande.")
            val shown = if (showAll) list.take(page * PAGE) else list
            shown.withIndex().chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (i, nv) ->
                        val on = nv === current
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (on) Surface3 else Surface1)
                                .border(if (on) 2.dp else 1.dp, if (on) Selection else Hairline, RoundedCornerShape(12.dp))
                                .clickable { selected = i }
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            ChordDiagram(nv.voicing.shape, if (on) Brass else TextBody)
                            Text(nv.name.removePrefix("Forma de ").removePrefix("Baixo na "), style = MaterialTheme.typography.labelSmall, color = TextBody, maxLines = 1)
                            Text(nv.voicing.position, style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 1)
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            if (showAll && list.size > shown.size) {
                SecondaryButton("Mostrar mais ${minOf(PAGE, list.size - shown.size)} de ${list.size - shown.size}", Modifier.fillMaxWidth()) { page++ }
            }
        }

        LabCard {
            Text("As 5 formas (CAGED)", style = MaterialTheme.typography.titleMedium, color = TextStrong)
            Spacer(Modifier.height(4.dp))
            Text(
                "Os desenhos abertos de Dó, Lá, Sol, Mi e Ré podem \"andar\" pelo braço. Por isso todo acorde tem " +
                    "pelo menos 5 lugares para ser tocado, um emendado no outro, nessa ordem: C-A-G-E-D. " +
                    "Aprenda primeiro a forma de Mi e a de Lá (as pestanas): com elas você toca qualquer acorde maior ou menor.",
                style = MaterialTheme.typography.bodyMedium, color = TextBody,
            )
        }
    }
}

/** A forma escolhida, grande no braço, com os dedos e o passo a passo. */
@Composable
private fun ShapeDetail(
    chord: LabChord,
    nv: NamedVoicing,
    index: Int,
    count: Int,
    fingers: Boolean,
    onFingers: (Boolean) -> Unit,
    onSelect: (Int) -> Unit,
) {
    val v = nv.voicing
    LabCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(R.drawable.ic_chevron_left, "Forma anterior") { onSelect((index - 1 + count) % count) }
            Column(Modifier.weight(1f).padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(nv.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextStrong, maxLines = 2)
                Text("${index + 1} de $count", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
            RoundIconButton(R.drawable.ic_chevron, "Próxima forma") { onSelect((index + 1) % count) }
        }
        Spacer(Modifier.height(8.dp))
        Segmented(listOf("Dedos (1 a 4)", "Notas (1, 3, 5)"), if (fingers) 0 else 1, { onFingers(it == 0) }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        val fg = v.fingering
        val dots = v.strings.map { s ->
            val role = chord.role(Guitar.pitchClassAt(s, v.frets[s])) ?: "b"
            when {
                fingers && v.frets[s] == 0 -> NeckDot(s, 0, "0", Brass, DotStyle.RING)
                fingers -> NeckDot(s, v.frets[s], "${fg[s]}", Brass, emphasis = role == "1")
                else -> NeckDot(
                    s, v.frets[s], role, Brass,
                    if (role in setOf("1", "3", "3m", "5", "5b", "5#")) DotStyle.FILLED else DotStyle.RING,
                    emphasis = role == "1",
                )
            }
        }
        val first = if (v.maxFret <= 5) 1 else (v.minFret - 1).coerceAtLeast(1)
        GuitarNeck(
            dots = dots,
            firstFret = first,
            lastFret = first + 5,
            muted = v.frets.indices.filter { v.frets[it] < 0 }.toSet(),
            barre = v.barreOnNeck(Brass),
        )
        Spacer(Modifier.height(8.dp))
        Text("Como montar", style = MaterialTheme.typography.titleMedium, color = TextStrong)
        ChordShapes.howTo(v).forEach {
            Text("• $it", style = MaterialTheme.typography.bodyMedium, color = TextBody, modifier = Modifier.padding(vertical = 1.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            if (fingers) "1 indicador, 2 médio, 3 anelar, 4 mindinho, 0 corda solta.\nO contorno claro marca a fundamental."
            else "Bolinha cheia: base do acorde (1, 3, 5). Anel: nota de cor (7, 9, 6, 4).\nO contorno claro marca a fundamental.",
            style = MaterialTheme.typography.labelSmall, color = TextMuted,
        )
    }
}
