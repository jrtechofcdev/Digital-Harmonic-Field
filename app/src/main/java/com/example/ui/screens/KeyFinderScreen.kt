package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
import androidx.compose.runtime.LaunchedEffect
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
import com.example.dev.DevStore
import com.example.ui.dev.DevLabelForm
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

private val ptKeyName = HarmonicDatabase.ptNameByCipher

@Composable
fun KeyFinderScreen(
    onOpenKey: (String) -> Unit,
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    onOpenDevPanel: () -> Unit = {},
) {
    val context = LocalContext.current
    // Na versão DEV, o gravador de sessões acompanha o detector (no app normal: nada).
    val listener = remember { KeyListener().apply { if (DevStore.enabled) tap = DevStore.recorder } }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Ao mudar para true, o efeito abaixo é refeito e liga o microfone.
        hasPermission = granted
    }

    // Com a tela visível, o microfone fica pronto (últimos 5 s só na memória).
    // Ao sair da tela ou ir para segundo plano, desliga e apaga tudo.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, hasPermission) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (hasPermission) listener.open()
                Lifecycle.Event.ON_STOP -> listener.close()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            listener.close()
        }
    }

    // Vibra ao terminar: dá para saber o resultado sem olhar a tela.
    LaunchedEffect(listener.result) {
        listener.result?.let { vibrateFor(context, it.status) }
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
                    "Ouve em rodadas de ${KeyListener.PREROLL_SECONDS} s até ter certeza",
                    style = MaterialTheme.typography.labelSmall,
                    color = Brass,
                )
            }
        }

        if (DevStore.enabled) {
            Spacer(Modifier.height(12.dp))
            DevBanner(onOpenDevPanel)
        }

        Spacer(Modifier.height(20.dp))

        if (!hasPermission) {
            PermissionPrompt { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
            return@Column
        }

        val listening = listener.phase == KeyListener.Phase.ANALYZING
        ListenDial(
            phase = listener.phase,
            buffered = listener.buffered,
            analyzedSeconds = listener.analyzedSeconds,
            onClick = { if (listening) listener.stopAndUse() else listener.detect() },
        )

        Spacer(Modifier.height(16.dp))

        if (listening) {
            LiveStatus(
                level = listener.level,
                hearingVoice = listener.hearingVoice,
                liveResult = listener.liveResult,
                analyzedSeconds = listener.analyzedSeconds,
                gainDb = listener.gainDb,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionButton("Parar e usar", primary = true, modifier = Modifier.weight(1f)) { listener.stopAndUse() }
                ActionButton("Cancelar", primary = false, modifier = Modifier.weight(1f)) { listener.cancelDetection() }
            }
            Spacer(Modifier.height(16.dp))
        } else if (listener.phase != KeyListener.Phase.OFF && listener.result == null) {
            MemoryNote()
        }

        listener.error?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = FuncDominant)
            Spacer(Modifier.height(12.dp))
        }

        val result = listener.result
        if (!listening && result != null) {
            // Mais certeza no mesmo hino, ou começar outro.
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (listener.canListenMore) {
                    ActionButton("Ouvir mais 5 s", primary = true, modifier = Modifier.weight(1f)) { listener.listenMore() }
                }
                ActionButton("Novo hino", primary = !listener.canListenMore, modifier = Modifier.weight(1f)) { listener.detect() }
            }
            Spacer(Modifier.height(14.dp))
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
                if (DevStore.enabled) DevLabelSlot(result)
                EvidenceCard(result, listener.analyzedSeconds)
            } else {
                NoAnswerCard(result.status)
                if (DevStore.enabled) {
                    Spacer(Modifier.height(12.dp))
                    DevLabelSlot(result)
                }
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
private fun ListenDial(
    phase: KeyListener.Phase,
    buffered: Float,
    analyzedSeconds: Float,
    onClick: () -> Unit,
) {
    val listening = phase == KeyListener.Phase.ANALYZING
    val roundLen = KeyListener.PREROLL_SECONDS.toFloat()
    val round = (analyzedSeconds / roundLen).toInt().coerceAtLeast(0) + 1
    // Ouvindo: o arco mostra o andamento da rodada atual. Pronto: o quanto já está guardado.
    val arc = if (listening) ((analyzedSeconds % roundLen) / roundLen).coerceIn(0f, 1f) else buffered
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
                val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
                val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
                drawArc(
                    color = Hairline,
                    startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    topLeft = topLeft, size = arcSize,
                    style = Stroke(width = stroke),
                )
                if (arc > 0f && phase != KeyListener.Phase.OFF) {
                    drawArc(
                        color = if (listening) Brass else TextMuted,
                        startAngle = -90f, sweepAngle = 360f * arc, useCenter = false,
                        topLeft = topLeft, size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
            }
            if (listening) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Rodada", style = MaterialTheme.typography.labelLarge, color = TextMuted)
                    Text("$round", fontSize = 56.sp, fontWeight = FontWeight.Bold, color = TextStrong)
                    Text(
                        "%.1f s ouvidos".format(analyzedSeconds),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text("toque para parar e usar", style = MaterialTheme.typography.labelSmall, color = TextMuted)
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
                        if (phase == KeyListener.Phase.DONE) "Novo hino" else "Detectar tom",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextStrong,
                    )
                    val seconds = (buffered * KeyListener.PREROLL_SECONDS).toInt()
                    Text(
                        when {
                            phase == KeyListener.Phase.OFF -> "toque para ouvir"
                            buffered >= 1f -> "últimos ${KeyListener.PREROLL_SECONDS} s prontos"
                            else -> "guardando… $seconds s"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                    )
                }
            }
        }
    }
}

/** Explica por que o microfone fica ligado com a tela aberta. */
@Composable
private fun MemoryNote() {
    Text(
        "Com esta tela aberta, o microfone fica ligado e só os últimos " +
            "${KeyListener.PREROLL_SECONDS} segundos ficam na memória, para a resposta sair na hora. " +
            "Nada é gravado nem enviado; ao sair da tela, tudo é apagado.",
        style = MaterialTheme.typography.bodyMedium,
        color = TextMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
    )
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun LiveStatus(level: Float, hearingVoice: Boolean, liveResult: KeyResult?, analyzedSeconds: Float, gainDb: Float) {
    Column(Modifier.fillMaxWidth()) {
        // Rodadas: 1, 2, 3 (cada uma com 5 s).
        val roundLen = KeyListener.PREROLL_SECONDS.toFloat()
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (r in 0 until maxOf(3, (analyzedSeconds / roundLen).toInt() + 1)) {
                val fill = ((analyzedSeconds - r * roundLen) / roundLen).coerceIn(0f, 1f)
                Box(
                    Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Surface2),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(fill)
                            .height(6.dp)
                            .background(if (fill >= 1f) Brass else Brass.copy(alpha = 0.6f)),
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
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
            // Ganho automático: no culto o celular capta baixo; o app aumenta sozinho.
            if (gainDb >= 3f) {
                Text(
                    "ganho +${gainDb.toInt()} dB",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
                Spacer(Modifier.width(8.dp))
            }
            Box(
                Modifier
                    .width(60.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Surface2),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(level.coerceIn(0f, 1f))
                        .height(4.dp)
                        .background(if (hearingVoice) FuncTonic else TextMuted),
                )
            }
        }
        // Palpite ao vivo: muda conforme a evidência chega.
        val top = liveResult?.candidates?.firstOrNull()
        if (top != null) {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface1)
                    .border(1.dp, Hairline, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Palpite até agora", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(
                        ptKeyName[top.keyCipher] ?: top.keyCipher,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextStrong,
                    )
                }
                Text(
                    "${(top.probability * 100).toInt()}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (liveResult.status == KeyStatus.ALTA) FuncTonic else Brass,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun ActionButton(label: String, primary: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (primary) Brass else Surface1)
            .border(1.dp, if (primary) Brass else Hairline, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (primary) Ink else TextBody,
        )
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
        KeyStatus.MEDIA -> Triple("Tom provável", "Confira com o primeiro acorde, ou toque em \"Ouvir mais 5 s\" para confirmar.", Brass)
        else -> Triple("Incerto", "Os tons abaixo estão parecidos. \"Ouvir mais 5 s\" costuma desempatar.", FuncDominant)
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
private fun EvidenceCard(result: KeyResult, analyzedSeconds: Float) {
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
        FactLine("Canto aproveitado", "%.1f s de %.1f s ouvidos".format(result.voicedSeconds, analyzedSeconds))
        if (result.bassSeconds > 0) {
            FactLine("Linha do baixo", "%.1f s (baixo/teclado)".format(result.bassSeconds))
        }
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
        Tip("Aponte o celular para quem está cantando ou para a caixa do vocal — perto do canto, ele ouve muito melhor.")
        Tip("Comece a ouvir no meio de uma frase cantada, não na introdução.")
        Tip("Um trecho com o fim de uma frase ajuda a achar a tônica. Se puder, ouça 2 ou 3 rodadas.")
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
        Tip("Ouve em rodadas de 5 s. A 1ª sai na hora (os últimos 5 s ficam guardados na memória); a 2ª e a 3ª somam mais canto e deixam o palpite mais seguro.")
        Tip("Você pode parar quando quiser (\"Parar e usar\") ou pedir mais 5 s no mesmo hino. Ele só para sozinho quando tem certeza.")
        Tip("Ganho automático com limitador: se o celular capta o canto baixo, o app aumenta sozinho (até +30 dB) sem distorcer.")
        Tip("Separa a voz do resto do som, tira o ruído constante e só usa notas cantadas e sustentadas — conversa, tosse e palmas ficam de fora.")
        Tip("Um modelo treinado com milhares de trechos de hinos analisa o que um músico percebe de ouvido: as notas mais cantadas, onde a frase respira, a sensível subindo para a tônica e o baixo fazendo 5 → 1.")
        Tip("Com banda, ouve o baixo e o teclado à parte: ajudam a separar tons vizinhos e a relativa menor.")
        Tip("Dica de uso: 5 s às vezes servem para dois tons. Com 10 a 15 s (2 ou 3 rodadas), o acerto sobe bastante.")
        Tip("Sem evidência suficiente, ele não chuta: pede para ouvir de novo.")
        Tip("Vibra ao terminar: duas vezes quando tem certeza, uma vez quando é provável e uma longa quando não deu.")
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
            "Para detectar o tom, o app ouve pelo microfone enquanto esta tela está aberta. O áudio " +
                "é analisado no aparelho, só os últimos 5 segundos ficam na memória e nada é gravado " +
                "nem enviado.",
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


/** Duas vibrações curtas = certeza; uma = provável; uma longa = sem resposta. */
private fun vibrateFor(context: Context, status: KeyStatus) {
    val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    if (vibrator == null || !vibrator.hasVibrator()) return
    val pattern = when (status) {
        KeyStatus.ALTA -> longArrayOf(0, 60, 90, 60)
        KeyStatus.MEDIA, KeyStatus.BAIXA -> longArrayOf(0, 90)
        KeyStatus.SEM_VOZ, KeyStatus.INSUFICIENTE -> longArrayOf(0, 300)
    }
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }
}

// ---------------------------------------------------------------------------
// Versão DEV (treino real)
// ---------------------------------------------------------------------------

@Composable
private fun DevBanner(onOpen: () -> Unit) {
    val count = androidx.compose.runtime.remember(DevStore.version.intValue) { DevStore.stats().total }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(FuncDominant.copy(alpha = 0.12f))
            .border(1.dp, FuncDominant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .clickable { onOpen() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("MODO DEV · gravando trechos para treino", style = MaterialTheme.typography.labelLarge, color = FuncDominant)
            Text("$count hinos na pasta de treino", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
        Text("Painel ›", style = MaterialTheme.typography.labelLarge, color = FuncDominant)
    }
}

/** Cartão de rótulo do hino atual (tom certo, estrelas, etiquetas). */
@Composable
private fun DevLabelSlot(result: KeyResult) {
    val id = DevStore.recorder.currentId ?: return
    val existing = androidx.compose.runtime.remember(id) { DevStore.session(id)?.optJSONObject("rotulo") }
    DevLabelForm(
        sessionId = id,
        detected = result.candidates.map { it.keyCipher },
        existing = existing,
    )
    Spacer(Modifier.height(12.dp))
}
