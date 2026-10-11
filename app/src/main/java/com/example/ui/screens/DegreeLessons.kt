package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.music.DegreeExample
import com.example.music.DegreeTheory
import com.example.music.functionForDegree
import com.example.music.pitchClassCiphers
import com.example.ui.components.NotationSettings
import com.example.ui.components.NotationToggle
import com.example.ui.theme.Brass
import com.example.ui.theme.FuncDominant
import com.example.ui.theme.FuncNeutral
import com.example.ui.theme.FuncSubdominant
import com.example.ui.theme.FuncTonic
import com.example.ui.theme.Hairline
import com.example.ui.theme.Ink
import com.example.ui.theme.Surface2
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextStrong

/*
 * Lições de GRAUS da aba Aprender:
 *  - "Como pegar os graus" (passo a passo + treino);
 *  - "Graus a fundo" (sétimas, dominantes secundárias, diminutos, baixo
 *    caminhando, empréstimo, tom menor, transposição) com exemplos no tom escolhido;
 *  - atalho para o braço do violão.
 */

/** Cor da função do grau [index] num campo maior (para os exemplos). */
private fun degreeColor(index: Int) = functionForDegree(index, false).color()

@Composable
internal fun HowToFindDegreesLesson(onOpenTrainer: () -> Unit) {
    LessonCard("Como pegar os graus (passo a passo)", startExpanded = true) {
        Paragraph(
            "Tocar por grau é pensar em NÚMEROS em vez de nomes de acorde: \"1, 4, 5, 6m\". " +
                "Assim você toca a mesma música em qualquer tom. Ligue o modo Grau (abaixo) e o app " +
                "inteiro passa a mostrar os números."
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Mostrar acordes por:", style = MaterialTheme.typography.labelLarge, color = TextBody)
            Spacer(Modifier.width(10.dp))
            NotationToggle()
        }
        Spacer(Modifier.height(14.dp))
        StepLine("1", "Ache o 1 (a casa)", "É onde a música descansa: quase sempre o último acorde do hino e a última nota da melodia. Na dúvida, use o Detectar tom.")
        StepLine("2", "Monte a escada do tom", "A partir do 1, a escada do tom maior é sempre: 1 · 2m · 3m · 4 · 5 · 6m · 7°. Escolha um tom e veja:")
        KeyStepper { field ->
            DegreeLadder(DegreeTheory(field).triads, colors = (0..6).map { degreeColor(it) })
        }
        Spacer(Modifier.height(12.dp))
        StepLine("3", "Ouça a sensação de cada grau", "")
        FeelingLine(FuncTonic, "1 — a casa", "Resolvido, dá vontade de parar.")
        FeelingLine(FuncSubdominant, "4 — sai de casa", "Levanta, abre, sem tensão.")
        FeelingLine(FuncDominant, "5 — tensão", "Parece que pede para voltar ao 1.")
        FeelingLine(FuncTonic, "6m — a casa com saudade", "Repousa, mas triste. É a \"relativa menor\".")
        FeelingLine(FuncSubdominant, "2m — prepara", "Soa \"a caminho\"; quase sempre vem antes do 5.")
        FeelingLine(FuncTonic, "3m — passagem", "Parecido com o 1, mais suave. Aparece pouco.")
        Spacer(Modifier.height(6.dp))
        StepLine("4", "Siga o baixo", "O baixo costuma tocar a nota do grau. Se ele sai do 1 e sobe 4 notas da escala (ex.: Sol → Dó), o acorde é o 4.")
        StepLine("5", "Conheça os caminhos de hino", "1 – 4 – 1 – 5 – 1 (estrofe) · 1 – 6m – 2m – 5 – 1 (volta da Harpa) · 4 – 5 – 1 (fim de frase) · 4 – 1 (Amém).")
        StepLine("6", "Treine um pouco todo dia", "5 minutos no treino pelo ouvido já mudam o jeito de ouvir. Comece no 1 · 4 · 5.")
        ActionButton("Abrir o treino de graus", onOpenTrainer)
    }
}

@Composable
internal fun DegreesDeepDiveLesson() {
    LessonCard("Graus a fundo (aprendizado estendido)") {
        Paragraph("Tudo abaixo aparece no tom que você escolher — os números são os mesmos em qualquer tom.")
        KeyStepper { field ->
            val t = remember(field.keyCipher) { DegreeTheory(field) }
            val relMinorKey = pitchClassCiphers[(com.example.music.rootPc(field.keyCipher)!! + 9) % 12] + "m"
            val minorField = remember(relMinorKey) { HarmonicDatabase.getField(relMinorKey) }

            Topic("1. Campo maior × campo menor")
            Paragraph("Maior: 1 · 2m · 3m · 4 · 5 · 6m · 7°.  Menor: 1m · 2° · 3 · 4m · 5m · 6 · 7.")
            if (minorField != null) {
                val tm = remember(relMinorKey) { DegreeTheory(minorField) }
                Paragraph("Nos hinos em tom menor, o 5 quase sempre vira MAIOR (5 ou 5⁷) — é ele que faz a volta forte para o 1m. Em ${HarmonicDatabase.ptNameByCipher[relMinorKey]}:")
                DegreeLadder(
                    tm.minorWithMajorFive,
                    colors = listOf(FuncTonic, FuncSubdominant, FuncDominant, FuncDominant, FuncTonic),
                )
            }

            Topic("2. Graus com sétima")
            Paragraph("Acrescentar a sétima deixa o acorde mais \"cheio\". O mais importante é o 5⁷: aumenta a tensão e a volta para o 1 fica mais forte. O caminho 2m⁷ – 5⁷ – 1⁷M é o mais elegante.")
            DegreeLadder(t.sevenths, colors = (0..6).map { degreeColor(it) })

            Topic("3. Dominantes secundárias — o \"5\" de cada grau")
            Paragraph("Qualquer grau pode ganhar o seu próprio 5: um acorde MAIOR com sétima, uma quinta acima dele. O mais comum em hino é o 1⁷ indo para o 4.")
            t.secondaryDominants.forEach { (dom, target) ->
                val ti = t.triads.indexOf(target)
                DegreeLadder(listOf(dom, target), colors = listOf(FuncDominant, degreeColor(ti)), caption = "${dom.label} = ${dom.note}")
            }

            if (t.passingDiminished.isNotEmpty()) {
                Topic("4. Diminutos de passagem")
                Paragraph("Sobem meio tom ligando dois graus — dão movimento sem sair do tom.")
                t.passingDiminished.forEach { (a, d, b) ->
                    DegreeLadder(
                        listOf(a, d, b),
                        colors = listOf(degreeColor(t.triads.indexOf(a)), FuncNeutral, degreeColor(t.triads.indexOf(b))),
                    )
                }
            }

            if (t.walkingBass.isNotEmpty()) {
                Topic("5. Baixo caminhando (inversões)")
                Paragraph("Escreve-se acorde/baixo: 5/7 = acorde do 5 com o baixo na nota do 7. O baixo anda passo a passo e a música fica \"ligada\".")
                t.walkingBass.forEach { seq ->
                    DegreeLadder(seq, colors = seq.map { ex ->
                        when {
                            ex.label.startsWith("5") -> FuncDominant
                            ex.label.startsWith("1") -> FuncTonic
                            else -> degreeColor(t.triads.indexOf(ex).coerceAtLeast(0))
                        }
                    })
                }
            }

            if (t.borrowedFourMinor.isNotEmpty()) {
                Topic("6. Empréstimo: o 4m")
                Paragraph("O 4 vira menor por um instante antes de voltar ao 1 — o \"Amém\" com emoção. É um acorde \"emprestado\" do tom menor.")
                DegreeLadder(t.borrowedFourMinor, colors = listOf(FuncSubdominant, FuncSubdominant, FuncTonic))
            }

            Topic("7. Transpor pelos graus")
            Paragraph(
                "Os números não mudam: 1 – 4 – 5 em Sol é Sol – Dó – Ré; em Ré é Ré – Sol – Lá. Pensou em " +
                    "grau, toca em qualquer tom — e com capotraste, os números continuam os mesmos."
            )
        }
    }
}

@Composable
internal fun GuitarLesson(onOpenGuitar: () -> Unit) {
    LessonCard("Os graus no braço do violão") {
        Paragraph(
            "Um braço de violão na tela: toque numa nota e veja todas as posições dela; escolha um grau e " +
                "o acorde se monta sozinho, com variações pelo braço e versões mais harmoniosas (9, 7M, sus4…) " +
                "para os hinos. Tem também o método e jogos para decorar as notas."
        )
        Paragraph(
            "Sacada (formas com pestana): se o 1 tem a fundamental na 6ª corda, o 4 está na MESMA casa " +
                "com a fundamental na 5ª corda, e o 5 duas casas depois, também na 5ª corda. Aprenda esse " +
                "desenho e ache 1 · 4 · 5 em qualquer tom."
        )
        ActionButton("Abrir o braço do violão", onOpenGuitar)
    }
}

@Composable
internal fun PassagesLesson(onOpenPassages: () -> Unit, onOpenDictionary: () -> Unit) {
    LessonCard("As \"graças\": passagens entre acordes") {
        Paragraph(
            "O tocador experiente não vai \"seco\" de um acorde para o outro: ele coloca um ou dois acordes " +
                "rápidos no caminho. É a caída antes do 1, o baixo que desce degrau por degrau, o diminuto que " +
                "sobe meio tom, o 4m no Amém."
        )
        Paragraph(
            "Regra para começar: a passagem vai onde a melodia segura uma nota longa (fim de frase). Os acordes " +
                "de passagem duram pouco; o de chegada dura mais. Comece pelo 2m – 5 – 1 e pelo 1 – 5/7 – 6m."
        )
        Paragraph(
            "Alguns acordes saem do campo harmônico (o app marca como \"fora\"). Eles soam especiais justamente " +
                "por isso — use um por estrofe, não em todo lugar."
        )
        ActionButton("Abrir Passagens e enfeites", onOpenPassages)
        ActionButton("Dicionário de acordes (todas as formas)", onOpenDictionary)
    }
}

// ---------------------------------------------------------------------------
// Peças
// ---------------------------------------------------------------------------

/** Seletor de tom maior compacto (‹ Tom de Sol ›) + conteúdo calculado para ele. */
@Composable
private fun KeyStepper(content: @Composable (com.example.HarmonicField) -> Unit) {
    var root by rememberSaveable { mutableIntStateOf(7) }
    val key = pitchClassCiphers[root]
    val field = remember(key) { HarmonicDatabase.getField(key) } ?: return
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton("‹") { root = (root + 11) % 12 }
        Text(
            "Tom de ${HarmonicDatabase.ptNameByCipher[key] ?: key}",
            style = MaterialTheme.typography.titleMedium,
            color = Brass,
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
        )
        StepButton("›") { root = (root + 1) % 12 }
    }
    content(field)
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Surface2)
            .border(1.dp, Hairline, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge, color = TextStrong)
    }
}

/**
 * Fileira de acordes de exemplo (sem rolagem lateral: dividem a largura).
 * No modo Grau o número vem em destaque; no modo Cifra, a cifra.
 */
@Composable
private fun DegreeLadder(items: List<DegreeExample>, colors: List<Color>, caption: String? = null) {
    Column(Modifier.padding(bottom = 8.dp)) {
        items.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { ex ->
                    val i = items.indexOf(ex)
                    val c = colors.getOrElse(i) { TextBody }
                    val main = if (NotationSettings.byDegree) ex.label else ex.chord.cipher
                    val sub = if (NotationSettings.byDegree) ex.chord.cipher else ex.label
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Surface2)
                            .border(1.dp, c.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(main, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = c, maxLines = 1)
                        Text(sub, style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 1)
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(6.dp))
        }
        caption?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = TextMuted) }
    }
}

@Composable
private fun Topic(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = TextStrong,
        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
    )
}

@Composable
private fun FeelingLine(color: Color, title: String, text: String) {
    Row(Modifier.padding(start = 38.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = color)
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextBody)
    }
}

@Composable
private fun ActionButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brass)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Ink)
    }
}
