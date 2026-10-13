package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ChordInfo
import com.example.R
import com.example.audio.ChordPlayer
import com.example.music.Chord
import com.example.music.DominantFor
import com.example.music.Guitar
import com.example.music.HarmonicFunction
import com.example.music.parseCipher
import com.example.music.transposePtNote
import com.example.ui.components.ChordDiagram
import com.example.ui.components.NotationSettings
import com.example.ui.components.Panel
import com.example.ui.components.SecondaryButton
import com.example.ui.components.SectionLabel
import com.example.ui.theme.Hairline
import com.example.ui.theme.Surface1
import com.example.ui.theme.Surface2
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextStrong

/*
 * Dominantes secundárias no detalhe do campo (o "5 de cada grau").
 * A cor é a da Dominante: é a função que esses acordes têm (ver docs/DESIGN.md).
 */

/** Liga/desliga as dominantes no campo; fica salvo no aparelho. */
object DominantSettings {
    private const val PREFS = "app_prefs"
    private const val KEY = "mostrar_dominantes"

    var show by mutableStateOf(false)
        private set

    fun init(context: Context) {
        show = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false)
    }

    fun set(context: Context, value: Boolean) {
        show = value
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, value).apply()
    }
}

private val DominantColor: Color get() = HarmonicFunction.DOMINANT.color()

/** Nome da nota em português ("Dó#", "Ré"...), igual ao resto do app. */
private fun noteNamePt(pc: Int): String = transposePtNote("Dó", pc)

/** (principal, secundário) conforme Cifra | Grau: ("A7", "6⁷") ou ("6⁷", "A7"). */
private fun domLabels(d: DominantFor): Pair<String, String> =
    if (NotationSettings.byDegree) d.label to d.dominant.cipher else d.dominant.cipher to d.label

private fun targetName(d: DominantFor): String =
    if (NotationSettings.byDegree) d.targetLabel else d.target.cipher

/** Cor de um acorde do exemplo: a função dele no campo, ou Dominante. */
private fun exampleColor(chord: Chord, d: DominantFor, chords: List<ChordInfo>): Color {
    if (chord == d.dominant) return DominantColor
    val i = chords.indexOfFirst { parseCipher(it.cipher) == chord }
    return if (i >= 0) chords[i].function.color() else DominantColor
}

private fun midi(chord: Chord): List<Int> = Guitar.shape(chord).midiNotes

/**
 * Linha fina em cima de cada acorde do campo (retrato): "A7  6⁷   chama o Dm".
 * Tocar nela escolhe o acorde e mostra como a dominante entra.
 */
@Composable
internal fun DominantLine(d: DominantFor?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, onClickLabel = "Ver como a dominante entra") { onClick() }
            .padding(start = 14.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (d == null) {
            Text(
                "Sem dominante: o acorde diminuto não serve de casa",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
            return@Row
        }
        Icon(painterResource(R.drawable.ic_down), contentDescription = null, tint = DominantColor, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        val (main, secondary) = domLabels(d)
        Text(main, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DominantColor)
        Spacer(Modifier.width(6.dp))
        Text(secondary, style = MaterialTheme.typography.labelMedium, color = TextMuted)
        Spacer(Modifier.weight(1f))
        Text(
            if (d.primary) "a dominante do tom" else "chama o ${targetName(d)}",
            style = MaterialTheme.typography.bodySmall,
            color = TextBody,
            maxLines = 1,
        )
    }
}

/** Faixa com as 7 dominantes, alinhada em cima dos 7 acordes (paisagem). */
@Composable
internal fun DominantStrip(dominants: List<DominantFor?>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().height(36.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        dominants.forEachIndexed { i, d ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (i == selected && d != null) Surface2 else Color.Transparent)
                    .clickable(enabled = d != null, role = Role.Button) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                if (d != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.ic_down), contentDescription = null, tint = DominantColor, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(domLabels(d).first, style = MaterialTheme.typography.titleSmall, color = DominantColor, maxLines = 1)
                    }
                } else {
                    Text("sem", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                }
            }
        }
    }
}

/** Para onde vai a nota de fora (a 3ª da dominante) e a 7ª. */
private fun motionLines(d: DominantFor): List<String> {
    val t = d.target.cipher
    return listOf(
        "${noteNamePt(d.leadingTone)} (a 3ª do ${d.dominant.cipher}) sobe meio tom para ${noteNamePt(d.target.root)}, a fundamental do $t.",
        "${noteNamePt(d.seventh)} (a 7ª do ${d.dominant.cipher}) desce para ${noteNamePt(d.seventhLandsOn)}, a 3ª do $t.",
    )
}

/** A nota do campo que "briga" com a nota de fora (ex.: Dó natural contra o Dó# do A7). */
private fun clashLine(d: DominantFor, scale: List<Int>): String? {
    val out = d.outsideNotes.firstOrNull() ?: return null
    val natural = listOf((out + 11) % 12, (out + 1) % 12).firstOrNull { it in scale } ?: return null
    return "se a melodia estiver cantando ${noteNamePt(natural)} nessa hora, fique no acorde do campo: " +
        "o ${noteNamePt(out)} do ${d.dominant.cipher} vai brigar com a voz."
}

/**
 * O "como e onde" da dominante escolhida: notas (com a de fora marcada), para
 * onde elas andam, onde ela entra no hino, um caminho de exemplo e o som.
 */
@Composable
internal fun DominantCard(
    d: DominantFor?,
    chords: List<ChordInfo>,
    player: ChordPlayer,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (d == null) {
        Panel(modifier) {
            Text("Este grau não tem dominante", style = MaterialTheme.typography.titleMedium, color = TextStrong)
            Text(
                "O acorde diminuto é tensão pura: ele não é uma \"casa\", então nenhum acorde resolve nele. Escolha outro grau.",
                style = MaterialTheme.typography.bodyMedium, color = TextBody,
            )
        }
        return
    }
    val scale = chords.mapNotNull { parseCipher(it.cipher)?.root }
    val (main, _) = domLabels(d)
    val title = "$main chama o ${targetName(d)}"
    val shape = Guitar.shape(d.dominant)

    if (compact) {
        Panel(modifier.fillMaxHeight()) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${noteNamePt(d.leadingTone)} sobe para ${noteNamePt(d.target.root)}; ${noteNamePt(d.seventh)} desce para ${noteNamePt(d.seventhLandsOn)}.",
                style = MaterialTheme.typography.bodySmall, color = TextBody, maxLines = 2,
            )
            Text(
                if (d.outsideNotes.isEmpty()) "Todas as notas são do campo."
                else "Fora do campo: " + d.outsideNotes.joinToString(" e ") { noteNamePt(it) } + ".",
                style = MaterialTheme.typography.bodySmall, color = TextMuted, maxLines = 1,
            )
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton("Ouvir", Modifier.weight(1f), icon = R.drawable.ic_play) {
                    player.play(listOf(midi(d.dominant), midi(d.target)), secondsEach = 1.6, strum = 0.035)
                }
                SecondaryButton("Exemplo", Modifier.weight(1f), icon = R.drawable.ic_play) {
                    player.play(d.example.map { midi(it.second) }, secondsEach = 1.3, strum = 0.035)
                }
            }
        }
        return
    }

    Panel(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = TextStrong)
                Text(
                    if (d.primary) "A dominante do próprio tom: o 5 que leva de volta para casa."
                    else "Dominante secundária: o 5 do ${d.targetLabel}. Fora do campo, mas no tom da música.",
                    style = MaterialTheme.typography.bodyMedium, color = TextBody,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.width(72.dp)) { ChordDiagram(shape, DominantColor) }
                Text(d.dominant.cipher, style = MaterialTheme.typography.labelMedium, color = TextBody)
            }
        }

        Spacer(Modifier.height(14.dp))
        SectionLabel("Notas do ${d.dominant.cipher}")
        Spacer(Modifier.height(6.dp))
        val roles = listOf("1", "3", "5", "7")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            d.dominant.pitchClasses.forEachIndexed { i, pc ->
                val outside = pc in d.outsideNotes
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Surface1)
                        .border(if (outside) 1.5.dp else 1.dp, if (outside) DominantColor else Hairline, RoundedCornerShape(10.dp))
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(noteNamePt(pc), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        color = if (outside) DominantColor else TextStrong)
                    Text(if (outside) "${roles[i]}, fora" else roles[i], style = MaterialTheme.typography.labelMedium, color = TextMuted)
                }
            }
        }
        if (d.outsideNotes.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                "A nota de fora do campo é o que dá o \"puxão\": o ouvido sente que ela precisa andar.",
                style = MaterialTheme.typography.bodySmall, color = TextBody,
            )
        }

        Spacer(Modifier.height(14.dp))
        SectionLabel("Como entra")
        Spacer(Modifier.height(4.dp))
        motionLines(d).forEach { Bullet(it) }

        Spacer(Modifier.height(10.dp))
        SectionLabel("Onde entra")
        Spacer(Modifier.height(4.dp))
        Bullet("Logo antes do ${d.target.cipher}: no lugar do acorde que vem antes dele, ou na segunda metade do compasso. Dura 1 ou 2 tempos e resolve no ${d.target.cipher}.")
        if (!d.primary) Bullet("Use quando a música for para o ${d.targetLabel}. Uma ou duas por estrofe já mudam o hino; não troque tudo de uma vez.")
        clashLine(d, scale)?.let { Bullet("Cuidado: $it") }

        Spacer(Modifier.height(12.dp))
        SectionLabel("Exemplo no tom")
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            d.example.forEach { (label, chord) ->
                val c = exampleColor(chord, d, chords)
                val byDegree = NotationSettings.byDegree
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(c.copy(alpha = if (chord == d.dominant) 0.18f else 0.10f))
                        .border(if (chord == d.dominant) 1.5.dp else 1.dp, c.copy(alpha = if (chord == d.dominant) 1f else 0.5f), RoundedCornerShape(10.dp))
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(if (byDegree) label else chord.cipher, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        color = c, maxLines = 1, textAlign = TextAlign.Center)
                    Text(if (byDegree) chord.cipher else label, style = MaterialTheme.typography.labelMedium, color = TextMuted, maxLines = 1)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton("Ouvir ${d.dominant.cipher} → ${d.target.cipher}", Modifier.weight(1f), icon = R.drawable.ic_play) {
                player.play(listOf(midi(d.dominant), midi(d.target)), secondsEach = 1.6, strum = 0.035)
            }
            SecondaryButton("Exemplo", icon = R.drawable.ic_play) {
                player.play(d.example.map { midi(it.second) }, secondsEach = 1.3, strum = 0.035)
            }
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Row(Modifier.padding(vertical = 3.dp)) {
        Box(Modifier.padding(top = 8.dp).size(5.dp).clip(RoundedCornerShape(3.dp)).background(TextMuted))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextBody)
    }
}
