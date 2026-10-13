package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.music.Guitar
import com.example.music.NeckQuiz
import com.example.ui.components.DotStyle
import com.example.ui.components.GuitarNeck
import com.example.ui.components.NeckDot
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SectionLabel
import com.example.ui.components.Segmented
import com.example.ui.theme.Brass
import com.example.ui.theme.FuncDominant
import com.example.ui.theme.FuncTonic
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextStrong

/**
 * Aba Decorar: o método (em etapas curtas) e dois jogos rápidos para fixar as
 * notas no braço. Baseado no que mais funciona para iniciantes: uma corda de
 * cada vez, falar o nome em voz alta, poucos minutos por dia e testar a memória
 * (em vez de só olhar o mapa).
 */
@Composable
internal fun ColumnScope.StudyTab(latin: Boolean) {
    NeckGame(latin)

    SectionLabel("O método, passo a passo")
    METHOD.forEach { (title, body) ->
        LabCard {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextStrong)
            Spacer(Modifier.height(4.dp))
            body.forEach { Text(it, style = MaterialTheme.typography.bodyMedium, color = TextBody, modifier = Modifier.padding(vertical = 2.dp)) }
        }
    }

    SectionLabel("Para decorar os acordes")
    LabCard {
        CHORD_TIPS.forEach { (title, body) ->
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextStrong)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = TextBody, modifier = Modifier.padding(bottom = 8.dp))
        }
    }
}

private enum class Game(val title: String) { QUAL("Que nota é?"), ACHE("Ache a nota") }

@Composable
private fun ColumnScope.NeckGame(latin: Boolean) {
    var game by remember { mutableStateOf(Game.QUAL) }
    var stage by remember { mutableStateOf(NeckQuiz.Stage.SIXTH) }
    var spot by remember { mutableStateOf(NeckQuiz.spot(NeckQuiz.Stage.SIXTH)) }
    var answer by remember { mutableStateOf<Int?>(null) }              // nota respondida (Que nota é?)
    var tapped by remember { mutableStateOf<NeckQuiz.Spot?>(null) }    // casa tocada (Ache a nota)
    var right by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }

    fun next(s: NeckQuiz.Stage = stage) {
        spot = NeckQuiz.spot(s, avoid = spot)
        answer = null
        tapped = null
    }
    val done = answer != null || tapped != null
    val correct = when (game) {
        Game.QUAL -> answer == spot.pc
        Game.ACHE -> tapped?.let { it.pc == spot.pc && (stage == NeckQuiz.Stage.ALL || it.string == spot.string) } == true
    }
    val name = noteName(spot.pc, latin)
    val stringName = "${6 - spot.string}ª corda"

    SectionLabel("Treino rápido (acertos: $right de $total)")
    Segmented(
        Game.entries.map { it.title },
        game.ordinal,
        { game = Game.entries[it]; next() },
        Modifier.fillMaxWidth(),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        NeckQuiz.Stage.entries.forEach { s -> LabChip(s.title, s == stage, Modifier.weight(1f)) { stage = s; next(s) } }
    }

    // Pergunta.
    Text(
        when (game) {
            Game.QUAL -> "Que nota é esta? ($stringName, casa ${spot.fret})"
            Game.ACHE -> if (stage == NeckQuiz.Stage.ALL) "Toque num $name (qualquer corda)" else "Toque no $name da $stringName"
        },
        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextStrong,
    )

    // Braço do jogo.
    val dots = buildList {
        when (game) {
            Game.QUAL -> add(
                NeckDot(spot.string, spot.fret, if (done) name else "?", if (!done) Brass else if (correct) FuncTonic else FuncDominant),
            )
            Game.ACHE -> {
                tapped?.let { t ->
                    add(NeckDot(t.string, t.fret, noteName(t.pc, latin), if (correct) FuncTonic else FuncDominant))
                    if (!correct) {
                        val strings = if (stage == NeckQuiz.Stage.ALL) (0 until Guitar.STRINGS).toList() else listOf(spot.string)
                        NeckQuiz.positions(spot.pc).filter { it.first in strings }.forEach { (s, f) ->
                            add(NeckDot(s, f, name, Brass, DotStyle.RING))
                        }
                    }
                }
            }
        }
    }
    LabCard {
        GuitarNeck(
            dots = dots,
            onTap = if (game == Game.ACHE && !done) { s, f ->
                tapped = NeckQuiz.Spot(s, f)
                total++
                val ok = Guitar.pitchClassAt(s, f) == spot.pc && (stage == NeckQuiz.Stage.ALL || s == spot.string)
                if (ok) right++
            } else null,
        )
    }

    // Respostas (Que nota é?).
    if (game == Game.QUAL) {
        NeckQuiz.choices(stage).chunked(if (stage.naturalsOnly) 7 else 6).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                row.forEach { pc ->
                    LabChip(noteName(pc, latin), done && pc == spot.pc, Modifier.weight(1f)) {
                        if (!done) {
                            answer = pc
                            total++
                            if (pc == spot.pc) right++
                        }
                    }
                }
            }
        }
    }

    if (done) {
        Text(
            if (correct) "Isso! Diga em voz alta \"$name\". Falar ajuda a gravar."
            else "Era $name ($stringName, casa ${spot.fret}). Procure o marco mais perto e conte as casas.",
            style = MaterialTheme.typography.bodyMedium,
            color = if (correct) FuncTonic else FuncDominant,
        )
        PrimaryButton("Próxima", Modifier.padding(top = 2.dp).fillMaxWidth()) { next() }
    } else {
        Text(
            "Marcos da 6ª corda: 0 Mi, 3 Sol, 5 Lá, 7 Si, 8 Dó, 10 Ré, 12 Mi.",
            style = MaterialTheme.typography.labelSmall, color = TextMuted,
        )
    }
}

/** O método em etapas. Cada uma leva de 3 a 7 dias com 5–10 minutos por dia. */
private val METHOD = listOf(
    "1. Os nomes das cordas soltas" to listOf(
        "Da mais grossa para a mais fina: Mi, Lá, Ré, Sol, Si, Mi (E A D G B E).",
        "Frase para lembrar: \"Eu Amo Deus, Glória Bendita Eternamente\". A primeira letra é a cifra de cada corda.",
    ),
    "2. A regra das casas" to listOf(
        "Cada casa sobe meio tom. Entre duas notas naturais existe uma casa no meio (o sustenido)…",
        "…menos entre Mi e Fá, e entre Si e Dó: elas são vizinhas, não tem casa no meio.",
        "Na casa 12 tudo recomeça (mesma nota, uma oitava acima). Então você só precisa decorar da casa 0 à 11.",
    ),
    "3. Uma corda de cada vez (6ª, depois 5ª)" to listOf(
        "Comece só pelas notas naturais da 6ª corda: 0 Mi, 1 Fá, 3 Sol, 5 Lá, 7 Si, 8 Dó, 10 Ré, 12 Mi.",
        "Depois a 5ª corda: 0 Lá, 2 Si, 3 Dó, 5 Ré, 7 Mi, 8 Fá, 10 Sol, 12 Lá.",
        "Por que essas duas primeiro? Porque o baixo dos acordes com pestana mora nelas: sabendo a 6ª e a 5ª, você acha qualquer acorde.",
        "Toque a nota e FALE o nome em voz alta. Suba e desça a corda. Use o jogo acima nas fases \"6ª corda\" e \"5ª corda\".",
    ),
    "4. Marcos e atalhos" to listOf(
        "Use as bolinhas do braço como marcos (casas 3, 5, 7, 9 e 12). Decore a nota de cada marco e conte a partir dele.",
        "Atalho da oitava: da 6ª corda para a 4ª, mesma nota 2 casas à frente; da 5ª para a 3ª, também 2 casas.",
        "A 1ª corda é igual à 6ª (mesmas notas, mesmas casas).",
    ),
    "5. Uma nota por dia" to listOf(
        "Escolha a \"nota do dia\" (ex.: Ré) e ache TODAS no braço, corda por corda, devagar e depois com o metrônomo.",
        "Na aba Notas, toque nela para conferir as posições.",
    ),
    "6. Pouco e sempre" to listOf(
        "10 minutos por dia valem mais que 1 hora no sábado. Revise no dia seguinte, depois de 3 dias e depois de 1 semana.",
        "Testar a memória (jogos) grava mais do que só olhar o mapa. Erre à vontade: o erro mostra o que revisar.",
        "Leve para o hino: ao tocar, diga o nome da nota do baixo de cada acorde.",
    ),
)

private val CHORD_TIPS = listOf(
    "Pense em notas, não só em desenho" to
        "Todo acorde é 1 + 3 + 5. Sol = Sol, Si, Ré. Ao montar, diga as notas. Na aba Acordes, use \"Onde estão 1, 3, 5\".",
    "As 5 formas que andam pelo braço (CAGED)" to
        "Os desenhos de Dó, Lá, Sol, Mi e Ré abertos se repetem pelo braço, um emendado no outro, nessa ordem. " +
            "Por isso o mesmo acorde aparece em vários lugares: são as \"variações\" da aba Acordes.",
    "Tríades nas cordas de cima" to
        "Só 3 notas (1ª, 2ª e 3ª cordas) já formam o acorde. São leves, não embolam com o teclado e o baixo, e ficam lindas em hino lento.",
    "Dedo guia" to
        "Na troca de acordes, mantenha preso o dedo que se repete (Dó → Lám: dedos 1 e 2 ficam). Menos movimento, troca mais limpa.",
    "Do seco ao harmonioso" to
        "Primeiro decore o acorde básico. Depois aprenda UMA cor por semana (o (9), depois o 7M, depois o sus4) e use no hino da semana.",
)
