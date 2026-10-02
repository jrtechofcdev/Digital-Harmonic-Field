package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.HarmonicDatabase
import com.example.R
import com.example.audio.KeyListener
import com.example.music.KeyCandidate
import com.example.music.KeyResult
import com.example.music.KeyStatus
import com.example.music.ptPitchClass
import com.example.ui.components.SectionLabel
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
import kotlin.math.abs
import kotlin.math.ceil

private val ptKeyName = HarmonicDatabase.ptNameByCipher

@Composable
fun KeyFinderScreen(
    onOpenKey: (String) -> Unit,
    onBack: () -> Unit,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val listener = remember { KeyListener() }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) listener.start()
    }

    // Libera o microfone ao sair da tela ou quando o app vai para segundo plano.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) listener.cancel()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            listener.cancel()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp, end = 16.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
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
            Column {
                Text("Detectar tom", style = MaterialTheme.typography.headlineMedium, color = TextStrong)
                Text(
                    "Ouve ${KeyListener.SECONDS} segundos de canto e sugere o tom",
                    style = MaterialTheme.typography.labelSmall,
                    color = Brass,
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        if (!hasPermission) {
            PermissionPrompt { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
            return@Column
        }

        val listening = listener.phase == KeyListener.Phase.LISTENING
        ListenDial(
            listening = listening,
            done = listener.phase == KeyListener.Phase.DONE,
            progress = listener.progress,
            onClick = { if (listening) listener.cancel() else listener.start() },
        )

        Spacer(Modifier.height(16.dp))

        if (listening) {
            LiveStatus(
                level = listener.level,
                hearingVoice = listener.hearingVoice,
                liveGuess = listener.liveGuess,
            )
        }

        listener.error?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = FuncDominant)
            Spacer(Modifier.height(12.dp))
        }

        val result = listener.result
        if (!listening && result != null) {
            if (result.hasAnswer) {
                ConfidenceBanner(result)
                Spacer(Modifier.height(12.dp))
                result.candidates.forEachIndexed { i, c ->
                    CandidateCard(
                        rank = i + 1,
                        candidate = c,
                        top = result.candidates.first(),
                        result = result,
                        onOpen = { onOpenKey(c.keyCipher) },
                    )
                    Spacer(Modifier.height(10.dp))
                }
                EvidenceCard(result)
            } else {
                NoAnswerCard(result.status)
            }
            Spacer(Modifier.height(16.dp))
        }

        HowItWorksCard()
    }
}

// ---------------------------------------------------------------------------
// Botão de escuta com contagem regressiva
// ---------------------------------------------------------------------------

@Composable
private fun ListenDial(listening: Boolean, done: Boolean, progress: Float, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(196.dp)
                .clip(CircleShape)
                .background(Surface1)
                .clickable { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(196.dp)) {
                val stroke = 8.dp.toPx()
                val inset = stroke / 2
                drawArc(
                    color = Hairline,
                    startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke),
                )
                if (listening) {
                    drawArc(
                        color = Brass,
                        startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                        size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
            }
            if (listening) {
                val remaining = ceil((1f - progress) * KeyListener.SECONDS).toInt().coerceAtLeast(1)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$remaining", fontSize = 64.sp, fontWeight = FontWeight.Bold, color = TextStrong)
                    Text("ouvindo… toque para cancelar", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painterResource(R.drawable.ic_mic),
                        contentDescription = null,
                        tint = Brass,
                        modifier = Modifier.size(40.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (done) "Ouvir de novo" else "Ouvir ${KeyListener.SECONDS} s",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextStrong,
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveStatus(level: Float, hearingVoice: Boolean, liveGuess: KeyCandidate?) {
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Surface2),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(level.coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (hearingVoice) FuncTonic else TextMuted),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (hearingVoice) FuncTonic else Hairline)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (hearingVoice) "Voz captada" else "Procurando a voz…",
                style = MaterialTheme.typography.bodyMedium,
                color = if (hearingVoice) FuncTonic else TextMuted,
            )
            Spacer(Modifier.weight(1f))
            if (liveGuess != null) {
                Text(
                    "Parcial: ${ptKeyName[liveGuess.keyCipher] ?: liveGuess.keyCipher}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ---------------------------------------------------------------------------
// Resultado
// ---------------------------------------------------------------------------

@Composable
private fun ConfidenceBanner(result: KeyResult) {
    val top = result.candidates.first()
    val topName = ptKeyName[top.keyCipher] ?: top.keyCipher
    val (title, body, color) = when (result.status) {
        KeyStatus.ALTA -> Triple("Tom identificado", "Pode começar em $topName.", FuncTonic)
        KeyStatus.MEDIA -> Triple("Tom provável", "Confira com o primeiro acorde antes de entrar.", Brass)
        else -> Triple("Incerto", "Ouvi pouca diferença entre os tons. Ouça de novo com mais canto.", FuncDominant)
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(14.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = TextBody)
        if (result.betweenKeys) {
            Spacer(Modifier.height(6.dp))
            Text(
                "O grupo está cantando entre dois tons (%+d cents). Os dois vizinhos aparecem abaixo."
                    .format(result.tuningOffsetCents.toInt()),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
        }
    }
}

@Composable
private fun CandidateCard(
    rank: Int,
    candidate: KeyCandidate,
    top: KeyCandidate,
    result: KeyResult,
    onOpen: () -> Unit,
) {
    val isTop = rank == 1
    val cipher = candidate.keyCipher
    val field = remember(cipher) { HarmonicDatabase.getField(cipher) }
    val relation = when {
        isTop -> null
        candidate.isRelativeOf(top) -> "Relativa — mesmos acordes"
        result.betweenKeys && candidate.isMinor == top.isMinor &&
            abs(candidate.root - top.root) in setOf(1, 11) -> "Vizinho — grupo entre dois tons"
        else -> null
    }
    val accent = if (isTop && result.status == KeyStatus.ALTA) FuncTonic else Brass

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(if (isTop) 1.5.dp else 1.dp, if (isTop) accent else Hairline, RoundedCornerShape(14.dp))
            .clickable { onOpen() }
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isTop) accent else Surface2),
                contentAlignment = Alignment.Center,
            ) {
                Text("$rank", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (isTop) Ink else TextBody)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    ptKeyName[cipher] ?: cipher,
                    style = if (isTop) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                    color = TextStrong,
                )
                Text(
                    relation ?: if (isTop) "Mais provável" else "Alternativa",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isTop) accent else TextMuted,
                )
            }
            Text(
                "${(candidate.probability * 100).toInt().coerceAtLeast(1)}%",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isTop) accent else TextBody,
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Surface2),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(candidate.probability.toFloat().coerceIn(0.01f, 1f))
                    .height(4.dp)
                    .background(if (isTop) accent else TextMuted),
            )
        }
        if (field != null) {
            Spacer(Modifier.height(12.dp))
            Text("Acordes para começar", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 3, 4, 5).mapNotNull { field.chords.getOrNull(it) }.forEach { chord ->
                    val c = chord.function.color()
                    Column(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Surface2)
                            .border(1.dp, c.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(chord.cipher, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = c)
                        Text(chord.degree, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                }
                Spacer(Modifier.weight(1f))
                Text("Abrir ›", style = MaterialTheme.typography.labelLarge, color = Brass, modifier = Modifier.align(Alignment.CenterVertically))
            }
        }
    }
}

@Composable
private fun EvidenceCard(result: KeyResult) {
    val top = result.candidates.first()
    val scale = if (top.isMinor) intArrayOf(0, 2, 3, 5, 7, 8, 10) else intArrayOf(0, 2, 4, 5, 7, 9, 11)
    val inScale = scale.map { (top.root + it) % 12 }.toSet()
    val maxW = (result.noteWeights.maxOrNull() ?: 1f).coerceAtLeast(0.0001f)

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Text("No que o resultado se baseou", style = MaterialTheme.typography.titleMedium, color = TextStrong)
        Spacer(Modifier.height(4.dp))
        Text(
            "Notas que a voz sustentou (em latão, as do tom mais provável).",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
            for (pc in 0 until 12) {
                val w = result.noteWeights[pc] / maxW
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.BottomCenter) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(80.dp * w.coerceAtLeast(0.03f))
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (pc in inScale && w > 0.05f) Brass else Surface2),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        ptPitchClass(pc),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (pc in inScale) TextBody else TextMuted,
                        maxLines = 1,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        FactLine("Canto analisado", "%.1f s".format(result.voicedSeconds))
        result.lastNote?.let { FactLine("Última nota sustentada", ptPitchClass(it)) }
        val off = result.tuningOffsetCents.toInt()
        if (abs(off) >= 10) {
            FactLine("Afinação do grupo", "%+d cents ${if (off > 0) "acima" else "abaixo"} do padrão".format(off))
        }
    }
}

@Composable
private fun FactLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextMuted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = TextBody)
    }
}

@Composable
private fun NoAnswerCard(status: KeyStatus) {
    val (title, body) = if (status == KeyStatus.SEM_VOZ) {
        "Não ouvi canto" to "Só peguei ruído ou conversa. Para não sugerir um tom errado, o app só " +
            "responde quando ouve notas cantadas e sustentadas."
    } else {
        "Não deu para ter certeza" to "Ouvi pouca melodia clara, ou notas que não formam um tom. " +
            "Tente de novo quando a congregação já estiver cantando."
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(1.dp, FuncDominant.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = TextStrong)
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = TextBody)
        Spacer(Modifier.height(10.dp))
        Tip("Aponte o celular para quem está cantando ou para a caixa do vocal.")
        Tip("Comece a ouvir no meio de uma frase cantada, não na introdução.")
        Tip("Um trecho com o fim de uma frase ajuda a achar a tônica.")
    }
}

@Composable
private fun HowItWorksCard() {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        SectionLabel("Como funciona")
        Spacer(Modifier.height(8.dp))
        Tip("Filtra só a faixa da voz e tira o ruído constante do ambiente.")
        Tip("Considera apenas notas cantadas e sustentadas — conversa, tosse e palmas ficam de fora.")
        Tip("Compensa se o grupo estiver um pouco acima ou abaixo do tom.")
        Tip("Compara as notas com o padrão de milhares de melodias e mostra os 3 tons mais compatíveis.")
        Tip("Sem evidência suficiente, ele não chuta: pede para ouvir de novo.")
    }
}

@Composable
private fun Tip(text: String) {
    Row(Modifier.padding(bottom = 6.dp)) {
        Box(
            Modifier
                .padding(top = 7.dp, end = 10.dp)
                .size(5.dp)
                .clip(CircleShape)
                .background(Brass)
        )
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextBody)
    }
}

@Composable
private fun PermissionPrompt(onRequest: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Text("Permissão do microfone", style = MaterialTheme.typography.titleLarge, color = TextStrong)
        Spacer(Modifier.height(6.dp))
        Text(
            "Para detectar o tom, o app ouve 5 segundos pelo microfone. O áudio é analisado no " +
                "aparelho e não é gravado nem enviado.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextBody,
        )
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Brass)
                .clickable { onRequest() }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Permitir microfone", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink)
        }
    }
}

