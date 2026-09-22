package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.HarmonicDatabase
import com.example.ui.theme.Brass
import com.example.ui.theme.FuncDominant
import com.example.ui.theme.FuncSubdominant
import com.example.ui.theme.FuncTonic
import com.example.ui.theme.Hairline
import com.example.ui.theme.Ink
import com.example.ui.theme.Surface1
import com.example.ui.theme.Surface2
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextStrong
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LearnScreen(
    onOpenKey: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text("Aprender", style = MaterialTheme.typography.headlineMedium, color = TextStrong)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Um guia direto para quem está começando na igreja. Toque em cada tópico para abrir.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextBody,
                )
            }
        }

        item {
            LessonCard("O que é campo harmônico?", startExpanded = true) {
                Paragraph(
                    "Campo harmônico é o \"time\" de acordes que combinam dentro de um tom. " +
                        "Cada tom (Sol, Dó, Ré...) tem 7 acordes que soam bem juntos — são eles que " +
                        "aparecem na maioria das músicas daquele tom."
                )
                Paragraph(
                    "Descobrindo o tom da música, você já sabe quais 7 acordes provavelmente vão " +
                        "aparecer. É como ter o mapa antes de começar a tocar."
                )
                Paragraph(
                    "Na aba Campos, escolha um tom e veja os 7 acordes com a cor da função de cada um."
                )
            }
        }

        item {
            LessonCard("As três funções: para onde cada acorde puxa") {
                FunctionLine(FuncTonic, "Tônica (T)", "É a casa, o repouso. A música começa e termina aqui. Dá sensação de \"chegou\".")
                FunctionLine(FuncSubdominant, "Subdominante (SD)", "A preparação. Tira você do lugar sem criar muita tensão — geralmente aponta para a dominante.")
                FunctionLine(FuncDominant, "Dominante (D)", "A tensão. Cria aquele \"segura que vai voltar\" e pede para resolver na tônica.")
                Spacer(Modifier.height(6.dp))
                Paragraph(
                    "Todo movimento de uma música é um vai e volta entre essas três sensações. " +
                        "No app, cada acorde já vem pintado com a cor da sua função."
                )
            }
        }

        item {
            LessonCard("A Progressão da Harpa (a mais usada)", startExpanded = true) {
                Paragraph(
                    "É a \"volta\" que aparece em quase todo hino. No tom de Sol ela fica assim:"
                )
                ChordFlow(listOf("Sol" to FuncTonic, "Mi m" to FuncTonic, "Lá m" to FuncSubdominant, "Ré" to FuncDominant, "Sol" to FuncTonic))
                Paragraph(
                    "Em graus: I – vi – ii – V – I. Ou seja: tônica, relativa menor, subdominante, " +
                        "dominante e volta pra tônica. Em qualquer tom é só usar os acordes daquele campo."
                )
                Paragraph(
                    "Sacada: Mi → Lá → Ré → Sol descem de quinta em quinta — é o círculo das quintas " +
                        "girando (veja o tópico do círculo abaixo)."
                )
                Paragraph(
                    "Na aba Progressões você vê essa sequência já montada em qualquer tonalidade."
                )
            }
        }

        item {
            LessonCard("Como tirar música de ouvido") {
                StepLine("1", "Ache a tônica", "Cante a nota onde a música \"descansa\" (normalmente o último acorde). Esse é o tom.")
                StepLine("2", "Abra o campo desse tom", "Na aba Campos. Ali estão os 7 acordes candidatos da música.")
                StepLine("3", "Comece pelo 1, 4 e 5", "A maioria dos trechos usa tônica (1), subdominante (4) e dominante (5). Teste esses primeiro.")
                StepLine("4", "Ouça a tensão", "Quando sentir que \"vai voltar pra casa\", provavelmente é a dominante (5) resolvendo na tônica (1).")
                StepLine("5", "Use a Harpa", "Em hinos, teste a volta I – vi – ii – V – I. Encaixa na maioria.")
            }
        }

        item {
            LessonCard("Sistema de números (1 – 4 – 5)") {
                Paragraph(
                    "Na igreja é comum alguém dizer \"vai no 4\" ou \"faz o 5\". Os números são os graus " +
                        "do campo, contando a partir da tônica."
                )
                Paragraph(
                    "No tom de Sol: 1 = Sol, 2 = Lá m, 3 = Si m, 4 = Dó, 5 = Ré, 6 = Mi m, 7 = Fá# dim."
                )
                Paragraph(
                    "A vantagem: o número não muda de tom para tom. \"1 – 4 – 5\" é sempre a mesma ideia; " +
                        "só trocam as notas. Aprendendo por números, você toca em qualquer tom."
                )
            }
        }

        item {
            LessonCard("Círculo das quintas: o que é e como usar", startExpanded = true) {
                Paragraph(
                    "O círculo das quintas é um mapa dos 12 tons. Tons vizinhos são \"parentes\": " +
                        "compartilham quase todos os acordes, então combinam muito bem."
                )
                Paragraph(
                    "A regra prática mais importante: para qualquer tom, o vizinho da direita é a sua " +
                        "dominante (o 5) e o da esquerda é a subdominante (o 4). O par do anel de dentro " +
                        "é a relativa menor. Ou seja, os acordes mais usados de um tom estão coladinhos nele."
                )
                Paragraph("Toque em um tom para ver isso na prática:")
                Spacer(Modifier.height(10.dp))
                CircleLesson(onOpenKey = onOpenKey)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Cartão de lição expansível
// ---------------------------------------------------------------------------

@Composable
private fun LessonCard(
    title: String,
    startExpanded: Boolean = false,
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(startExpanded) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(1.dp, Hairline, RoundedCornerShape(14.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = TextStrong,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (expanded) "–" else "+",
                style = MaterialTheme.typography.headlineMedium,
                color = Brass,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun Paragraph(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = TextBody,
        modifier = Modifier.padding(bottom = 10.dp),
    )
}

@Composable
private fun FunctionLine(color: Color, title: String, text: String) {
    Row(modifier = Modifier.padding(bottom = 10.dp)) {
        Box(
            Modifier
                .padding(top = 5.dp, end = 12.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = color)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = TextBody)
        }
    }
}

@Composable
private fun StepLine(number: String, title: String, text: String) {
    Row(modifier = Modifier.padding(bottom = 12.dp)) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(Brass),
            contentAlignment = Alignment.Center,
        ) {
            Text(number, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Ink)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextStrong)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = TextBody)
        }
    }
}

@Composable
private fun ChordFlow(chords: List<Pair<String, Color>>) {
    Row(
        modifier = Modifier.padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        chords.forEachIndexed { i, (label, color) ->
            if (i > 0) Text("→", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Surface2)
                    .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Círculo das quintas interativo com explicação dos vizinhos
// ---------------------------------------------------------------------------

private val majorsCW = listOf("C", "G", "D", "A", "E", "B", "F#", "C#", "G#", "D#", "A#", "F")
private val minorsCW = listOf("Am", "Em", "Bm", "F#m", "C#m", "G#m", "D#m", "A#m", "Fm", "Cm", "Gm", "Dm")
private val ptByCipher = HarmonicDatabase.ptNameByCipher

@Composable
private fun CircleLesson(onOpenKey: (String) -> Unit) {
    var selected by remember { mutableStateOf("G") }

    val selIndexMajor = majorsCW.indexOf(selected)
    val selIndexMinor = minorsCW.indexOf(selected)
    val isMinorSel = selIndexMinor >= 0
    val ringIndex = if (isMinorSel) selIndexMinor else selIndexMajor

    val dominant = majorsCW[(ringIndex + 1) % 12]
    val subdominant = majorsCW[(ringIndex + 11) % 12]
    val relative = if (isMinorSel) majorsCW[ringIndex] else minorsCW[ringIndex]

    CircleWheel(
        selected = selected,
        highlightDominant = if (isMinorSel) "" else dominant,
        highlightSubdominant = if (isMinorSel) "" else subdominant,
        highlightRelative = relative,
        onTap = { selected = it },
    )

    Spacer(Modifier.height(12.dp))

    // Painel explicativo do tom selecionado
    val field = HarmonicDatabase.getField(selected)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Ink)
            .border(1.dp, Hairline, RoundedCornerShape(12.dp))
            .padding(14.dp),
    ) {
        Text(
            "Tom de ${ptByCipher[selected] ?: selected}",
            style = MaterialTheme.typography.titleMedium,
            color = TextStrong,
        )
        Spacer(Modifier.height(8.dp))
        if (field != null) {
            NeighborLine(FuncTonic, "Tônica (1)", field.chords[0].cipher)
            NeighborLine(FuncSubdominant, "Subdominante (4)", field.chords[3].cipher)
            NeighborLine(FuncDominant, "Dominante (5)", field.chords[4].cipher)
        }
        if (!isMinorSel) {
            Spacer(Modifier.height(8.dp))
            Text(
                "No círculo: à direita fica $dominant (a dominante) e à esquerda $subdominant " +
                    "(a subdominante). O par de dentro, $relative, é a relativa menor.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Brass)
                .clickable { onOpenKey(selected) }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Abrir campo completo de $selected",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Ink,
            )
        }
    }
}

@Composable
private fun NeighborLine(color: Color, label: String, cipher: String) {
    Row(
        modifier = Modifier.padding(bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextBody, modifier = Modifier.width(140.dp))
        Text(cipher, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun CircleWheel(
    selected: String,
    highlightDominant: String,
    highlightSubdominant: String,
    highlightRelative: String,
    onTap: (String) -> Unit,
) {
    val boxSize = 320f
    val center = boxSize / 2f
    val outerR = 120f
    val innerR = 78f
    val majorSize = 52f
    val minorSize = 40f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(boxSize.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.size(boxSize.dp)) {
            Canvas(modifier = Modifier.size(boxSize.dp)) {
                val c = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    color = Hairline,
                    radius = (outerR + majorSize / 2f).dp.toPx(),
                    center = c,
                    style = Stroke(width = 1.dp.toPx()),
                )
                drawCircle(
                    color = Hairline,
                    radius = (innerR - minorSize / 2f).dp.toPx(),
                    center = c,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Surface1)
                    .border(1.dp, Hairline, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("5as", style = MaterialTheme.typography.titleMedium, color = TextMuted)
            }

            majorsCW.forEachIndexed { i, cipher ->
                val angle = -PI / 2.0 + i * (PI / 6.0)
                val x = center + outerR * cos(angle).toFloat() - majorSize / 2f
                val y = center + outerR * sin(angle).toFloat() - majorSize / 2f
                Node(
                    label = cipher,
                    sizeDp = majorSize, xDp = x, yDp = y,
                    fill = Surface2,
                    ring = ringColor(cipher, selected, highlightDominant, highlightSubdominant, highlightRelative),
                    textColor = TextStrong,
                    onClick = { onTap(cipher) },
                )
            }
            minorsCW.forEachIndexed { i, cipher ->
                val angle = -PI / 2.0 + i * (PI / 6.0)
                val x = center + innerR * cos(angle).toFloat() - minorSize / 2f
                val y = center + innerR * sin(angle).toFloat() - minorSize / 2f
                Node(
                    label = cipher,
                    sizeDp = minorSize, xDp = x, yDp = y,
                    fill = Ink,
                    ring = ringColor(cipher, selected, highlightDominant, highlightSubdominant, highlightRelative),
                    textColor = TextBody,
                    onClick = { onTap(cipher) },
                )
            }
        }
    }
}

private fun ringColor(
    cipher: String,
    selected: String,
    dominant: String,
    subdominant: String,
    relative: String,
): Color = when (cipher) {
    selected -> Brass
    dominant -> FuncDominant
    subdominant -> FuncSubdominant
    relative -> FuncTonic
    else -> Hairline
}

@Composable
private fun Node(
    label: String,
    sizeDp: Float,
    xDp: Float,
    yDp: Float,
    fill: Color,
    ring: Color,
    textColor: Color,
    onClick: () -> Unit,
) {
    val isHighlighted = ring != Hairline
    Box(
        modifier = Modifier
            .offset(x = xDp.dp, y = yDp.dp)
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(fill)
            .border(if (isHighlighted) 2.dp else 1.dp, ring, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isHighlighted && ring != Brass) ring else textColor,
            textAlign = TextAlign.Center,
        )
    }
}
