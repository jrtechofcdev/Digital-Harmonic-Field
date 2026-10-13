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
import com.example.ui.components.OptionChip
import com.example.ui.components.PrimaryButton
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SecondaryButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.HarmonicDatabase
import com.example.audio.ChordPlayer
import com.example.music.DegreeQuiz
import com.example.music.chordMidiNotes
import com.example.music.parseCipher
import com.example.music.functionForDegree
import com.example.ui.theme.Brass
import com.example.ui.theme.FuncDominant
import com.example.ui.theme.FuncTonic
import com.example.ui.theme.Hairline
import com.example.ui.theme.Ink
import com.example.ui.theme.Surface1
import com.example.ui.theme.Surface2
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextStrong

/**
 * Treino de graus: pelo OUVIDO (o app toca o 1 e depois um acorde) ou pela
 * CIFRA (qual grau é tal acorde / qual acorde é tal grau). Começa fácil (1·4·5)
 * e vai abrindo.
 */
@Composable
fun DegreeTrainerScreen(onBack: () -> Unit, contentPadding: PaddingValues) {
    val player = remember { ChordPlayer() }
    DisposableEffect(Unit) { onDispose { player.stop() } }

    var kind by remember { mutableStateOf(DegreeQuiz.Kind.EAR) }
    var level by remember { mutableStateOf(DegreeQuiz.Level.BASICO) }
    var question by remember { mutableStateOf(DegreeQuiz.next(kind, level)) }
    var answered by remember { mutableStateOf<String?>(null) }
    var right by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }

    fun newQuestion() {
        question = DegreeQuiz.next(kind, level, avoid = question.degree)
        answered = null
    }
    // No treino de ouvido, toca sozinho a cada pergunta nova.
    LaunchedEffect(question) {
        if (question.kind == DegreeQuiz.Kind.EAR) player.play(question.playback)
    }

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
        ScreenHeader(
            "Treino de graus",
            subtitle = "Acertos: $right de $total" + if (streak >= 3) ", $streak seguidos" else "",
            onBack = onBack,
        )

        // Modo e nível.
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                DegreeQuiz.Kind.EAR to "Pelo ouvido",
                DegreeQuiz.Kind.CIPHER_TO_DEGREE to "Cifra → grau",
                DegreeQuiz.Kind.DEGREE_TO_CIPHER to "Grau → cifra",
            ).forEach { (k, label) ->
                Pill(label, kind == k, Modifier.weight(1f)) {
                    kind = k; question = DegreeQuiz.next(k, level); answered = null
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DegreeQuiz.Level.entries.forEach { l ->
                Pill(l.title, level == l, Modifier.weight(1f)) {
                    level = l; question = DegreeQuiz.next(kind, l); answered = null
                }
            }
        }

        // Pergunta.
        val keyName = HarmonicDatabase.ptNameByCipher[question.key] ?: question.key
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Surface1)
                .border(1.dp, Hairline, RoundedCornerShape(16.dp))
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (question.kind) {
                DegreeQuiz.Kind.EAR -> {
                    Text("Tom de $keyName", style = MaterialTheme.typography.labelLarge, color = TextBody)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "O app toca o 1 (a casa) e depois outro acorde.\nQual é o grau do segundo?",
                        style = MaterialTheme.typography.bodyLarge, color = TextBody, textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrimaryButton(if (player.isPlaying) "Tocando…" else "Ouvir de novo", Modifier.weight(1.4f), icon = R.drawable.ic_play) {
                            player.play(question.playback)
                        }
                        SecondaryButton("Só o 1", Modifier.weight(1f).height(52.dp)) {
                            parseCipher(question.key)?.let { player.play(listOf(chordMidiNotes(it))) }
                        }
                    }
                }
                DegreeQuiz.Kind.CIPHER_TO_DEGREE -> {
                    Text("Tom de $keyName", style = MaterialTheme.typography.labelLarge, color = TextBody)
                    Spacer(Modifier.height(6.dp))
                    Text(question.chordCipher, fontSize = 56.sp, fontWeight = FontWeight.Bold, color = TextStrong)
                    Text("Qual é o grau deste acorde?", style = MaterialTheme.typography.bodyLarge, color = TextBody)
                }
                DegreeQuiz.Kind.DEGREE_TO_CIPHER -> {
                    Text("Tom de $keyName", style = MaterialTheme.typography.labelLarge, color = TextBody)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        com.example.music.degreeLabel(question.degree, question.chordCipher),
                        fontSize = 56.sp, fontWeight = FontWeight.Bold, color = TextStrong,
                    )
                    Text("Qual acorde é este grau?", style = MaterialTheme.typography.bodyLarge, color = TextBody)
                }
            }
        }

        // Respostas.
        question.options.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { opt ->
                    val isAnswer = opt == question.answer
                    val bg = when {
                        answered == null -> Surface2
                        isAnswer -> FuncTonic.copy(alpha = 0.25f)
                        opt == answered -> FuncDominant.copy(alpha = 0.25f)
                        else -> Surface2
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bg)
                            .border(1.dp, if (answered != null && isAnswer) FuncTonic else Hairline, RoundedCornerShape(12.dp))
                            .clickable(enabled = answered == null) {
                                answered = opt
                                total++
                                if (isAnswer) { right++; streak++ } else streak = 0
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(opt, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextStrong)
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        // Resultado + explicação.
        answered?.let { a ->
            val ok = a == question.answer
            val color = functionForDegree(question.degree, false).color()
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background((if (ok) FuncTonic else FuncDominant).copy(alpha = 0.10f))
                    .border(1.dp, (if (ok) FuncTonic else FuncDominant).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(14.dp),
            ) {
                Text(
                    if (ok) "Acertou!" else "Era ${question.answer}",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                    color = if (ok) FuncTonic else FuncDominant,
                )
                Text(
                    "No tom de $keyName, o grau " +
                        com.example.music.degreeLabel(question.degree, question.chordCipher) +
                        " é ${question.chordCipher}.",
                    style = MaterialTheme.typography.bodyMedium, color = TextBody,
                )
                Spacer(Modifier.height(4.dp))
                Text(DegreeQuiz.feeling(question.degree), style = MaterialTheme.typography.bodyMedium, color = color)
            }
            PrimaryButton("Próxima pergunta", Modifier.fillMaxWidth()) { newQuestion() }
        }

        Text(
            "Dica: antes de responder, cante a nota do 1 por dentro. O 5 \"pede\" para voltar a ela; " +
                "o 4 parece que \"saiu de casa\"; o 6m é a casa com saudade.",
            style = MaterialTheme.typography.bodyMedium, color = TextMuted,
        )
    }
}

@Composable
private fun Pill(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) =
    OptionChip(label, selected, modifier, onClick)
