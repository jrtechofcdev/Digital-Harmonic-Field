package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.audio.TonePlayer
import com.example.audio.TunerListener
import com.example.data.TunerSettings
import com.example.music.Tuning
import com.example.music.TuningLibrary
import com.example.music.centsToTargetFolded
import com.example.music.ptPitchClass
import com.example.music.readingForFrequency
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TunerScreen(
    settings: TunerSettings,
    onSettingsChange: (TunerSettings) -> Unit,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listener = remember { TunerListener() }
    val tone = remember { TonePlayer() }
    val toneJob = remember { mutableStateOf<Job?>(null) }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(settings.noiseFilter) {
        listener.minClarity = settings.noiseFilter.minClarity
        listener.silenceRms = settings.noiseFilter.silenceRms
    }
    // Silencia a detecção enquanto o tom de referência toca.
    LaunchedEffect(tone.playingKey) { listener.muted = tone.playingKey != null }
    // Começa a ouvir sozinho ao abrir a aba (como o CifraClub).
    LaunchedEffect(hasPermission) { if (hasPermission) listener.start() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> { listener.stop(); tone.stop() }
                Lifecycle.Event.ON_START -> if (hasPermission) listener.start()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            listener.stop(); tone.stop()
        }
    }

    // As Configurações são hospedadas AQUI (não como tela separada) para o
    // afinador não desmontar — a corda travada e o microfone continuam ao voltar.
    var showSettings by remember { mutableStateOf(false) }
    BackHandler(enabled = showSettings) { showSettings = false }
    if (showSettings) {
        SettingsScreen(
            settings = settings,
            onChange = onSettingsChange,
            onBack = { showSettings = false },
            contentPadding = contentPadding,
        )
        return
    }

    var tuningId by remember { mutableStateOf(TuningLibrary.padrao.id) }
    var guided by remember { mutableStateOf(true) }         // true = corda por corda
    var selectedString by remember { mutableIntStateOf(0) } // 0 = 6ª corda (travada)
    val tuning = TuningLibrary.byId(tuningId)
    val refA = settings.refA.toDouble()
    val tol = settings.precision.cents.toFloat()

    fun pickString(index: Int) {
        selectedString = index
        guided = true
        if (settings.sounds) {
            val s = tuning.strings[index]
            toneJob.value?.cancel()
            tone.playOnce(s.noteName, s.targetFreq(refA))
            toneJob.value = scope.launch {
                delay(1300)
                if (tone.playingKey == s.noteName) tone.stop()
            }
        }
    }

    val freq = listener.frequency
    val analysis = if (freq != null) {
        analyze(freq, guided, tuning, selectedString, refA, tol)
    } else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp, end = 16.dp,
                top = contentPadding.calculateTopPadding() + 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
    ) {
        // Cabeçalho
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Afinador", style = MaterialTheme.typography.headlineMedium, color = TextStrong)
                Text(
                    "Lá = %.1f Hz · %s".format(settings.refA, tuning.name),
                    style = MaterialTheme.typography.labelSmall,
                    color = Brass,
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Surface1)
                    .border(1.dp, Hairline, CircleShape)
                    .clickable { showSettings = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Settings, "Configurações", tint = TextStrong, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(Modifier.height(14.dp))

        if (!hasPermission) {
            PermissionBox { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
            return@Column
        }

        // Modo
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ModeChip("Corda por corda", guided, Modifier.weight(1f)) { guided = true }
            ModeChip("Livre", !guided, Modifier.weight(1f)) { guided = false }
        }

        Spacer(Modifier.height(14.dp))

        // Medidor de ponteiro
        GaugeCard(analysis = analysis, tolerance = tol, guided = guided, listening = listener.isListening)

        Spacer(Modifier.height(14.dp))

        // Cordas (no modo corda por corda, tocar trava na corda)
        Text(
            if (guided) "Toque na corda que vai afinar" else "Cordas da afinação",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
        )
        Spacer(Modifier.height(8.dp))
        StringsRow(
            tuning = tuning,
            selectedIndex = if (guided) selectedString else analysis?.activeIndex,
            guided = guided,
            inTune = analysis?.inTune == true,
            onSelect = { pickString(it) },
        )

        Spacer(Modifier.height(14.dp))

        // Seleção de afinação
        Text("Afinação", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TuningLibrary.all.forEach { t ->
                TuningChip(t, t.id == tuningId) {
                    tuningId = t.id
                    if (selectedString > t.strings.lastIndex) selectedString = 0
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(tuning.description, style = MaterialTheme.typography.bodyMedium, color = TextMuted)

        listener.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = FuncDominant)
        }

        Spacer(Modifier.height(16.dp))
        GuideCard()
    }
}

// ---------------------------------------------------------------------------
// Lógica
// ---------------------------------------------------------------------------

private data class TuneAnalysis(
    val bigNote: String,     // nota mostrada em destaque
    val ptNote: String,
    val cents: Double,
    val activeIndex: Int?,
    val inTune: Boolean,
)

private fun analyze(
    freq: Double,
    guided: Boolean,
    tuning: Tuning,
    selectedString: Int,
    refA: Double,
    tolerance: Float,
): TuneAnalysis {
    if (guided) {
        val s = tuning.strings[selectedString]
        val cents = centsToTargetFolded(freq, s.targetFreq(refA))
        return TuneAnalysis(s.letter, s.ptName, cents, selectedString, abs(cents) <= tolerance)
    }
    val r = readingForFrequency(freq, refA)
    val nearest = nearestStringIndex(freq, tuning, refA)
    return TuneAnalysis(r.name, ptPitchClass(r.midi), r.cents, nearest, abs(r.cents) <= tolerance)
}

private fun nearestStringIndex(freq: Double, tuning: Tuning, refA: Double): Int {
    var best = 0
    var bestCents = Double.MAX_VALUE
    tuning.strings.forEachIndexed { i, s ->
        val c = abs(centsToTargetFolded(freq, s.targetFreq(refA)))
        if (c < bestCents) { bestCents = c; best = i }
    }
    return best
}

// ---------------------------------------------------------------------------
// Medidor de ponteiro (velocímetro)
// ---------------------------------------------------------------------------

@Composable
private fun GaugeCard(analysis: TuneAnalysis?, tolerance: Float, guided: Boolean, listening: Boolean) {
    val inTune = analysis?.inTune == true
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface1)
            .border(
                1.dp,
                if (inTune) FuncTonic.copy(alpha = 0.6f) else Hairline,
                RoundedCornerShape(16.dp),
            )
            .padding(vertical = 18.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Gauge(cents = analysis?.cents?.toFloat(), tolerance = tolerance, inTune = inTune)
        Spacer(Modifier.height(6.dp))
        Text(
            text = analysis?.bigNote ?: if (listening) "—" else "···",
            fontSize = 60.sp,
            fontWeight = FontWeight.Bold,
            color = if (inTune) FuncTonic else TextStrong,
            style = MaterialTheme.typography.displaySmall,
        )
        Text(
            text = analysis?.ptNote ?: if (listening) "toque uma corda" else "iniciando…",
            style = MaterialTheme.typography.titleMedium,
            color = TextBody,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = directionText(analysis, guided),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = when {
                analysis == null -> TextMuted
                inTune -> FuncTonic
                else -> Brass
            },
            textAlign = TextAlign.Center,
        )
    }
}

private fun directionText(a: TuneAnalysis?, guided: Boolean): String = when {
    a == null -> "Aguardando o som…"
    a.inTune -> "Afinada ✓"
    guided && a.cents < 0 -> "Frouxa — aperte a corda"
    guided -> "Apertada — afrouxe a corda"
    a.cents < 0 -> "Abaixo — suba um pouco"
    else -> "Acima — desça um pouco"
}

@Composable
private fun Gauge(cents: Float?, tolerance: Float, inTune: Boolean) {
    val maxDeg = 70f
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val pivotY = h * 0.92f
        val r = minOf(cx - 24.dp.toPx(), pivotY - 12.dp.toPx())

        val rect = androidx.compose.ui.geometry.Rect(
            Offset(cx - r, pivotY - r),
            Size(2 * r, 2 * r),
        )

        // Arco base
        drawArc(
            color = Hairline,
            startAngle = 270f - maxDeg,
            sweepAngle = 2 * maxDeg,
            useCenter = false,
            topLeft = rect.topLeft,
            size = rect.size,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
        )
        // Zona verde de tolerância no centro
        val tolDeg = (tolerance / 50f) * maxDeg
        drawArc(
            color = FuncTonic,
            startAngle = 270f - tolDeg,
            sweepAngle = 2 * tolDeg,
            useCenter = false,
            topLeft = rect.topLeft,
            size = rect.size,
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
        )

        // Marcas
        var a = -maxDeg
        while (a <= maxDeg + 0.1f) {
            val d = Math.toRadians((270f + a).toDouble())
            val cosD = cos(d).toFloat()
            val sinD = sin(d).toFloat()
            val big = a == 0f
            val tickLen = if (big) 16.dp.toPx() else 9.dp.toPx()
            val outer = Offset(cx + r * cosD, pivotY + r * sinD)
            val inner = Offset(cx + (r - tickLen) * cosD, pivotY + (r - tickLen) * sinD)
            drawLine(
                color = if (big) Brass else Hairline,
                start = inner, end = outer,
                strokeWidth = if (big) 3.dp.toPx() else 1.5.dp.toPx(),
                cap = StrokeCap.Round,
            )
            a += 17.5f
        }

        // Ponteiro
        if (cents != null) {
            val clamped = cents.coerceIn(-50f, 50f)
            val d = Math.toRadians((270f + (clamped / 50f) * maxDeg).toDouble())
            val tip = Offset(cx + (r * 0.82f) * cos(d).toFloat(), pivotY + (r * 0.82f) * sin(d).toFloat())
            val color = if (inTune) FuncTonic else Brass
            drawLine(color = color, start = Offset(cx, pivotY), end = tip, strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
        }
        // Eixo do ponteiro
        drawCircle(color = if (inTune) FuncTonic else Brass, radius = 7.dp.toPx(), center = Offset(cx, pivotY))
        drawCircle(color = Ink, radius = 3.dp.toPx(), center = Offset(cx, pivotY))
    }
}

// ---------------------------------------------------------------------------
// Cordas
// ---------------------------------------------------------------------------

@Composable
private fun StringsRow(
    tuning: Tuning,
    selectedIndex: Int?,
    guided: Boolean,
    inTune: Boolean,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tuning.strings.forEachIndexed { i, s ->
            val active = i == selectedIndex
            val done = active && inTune && guided
            val border = when {
                done -> FuncTonic
                active -> Brass
                else -> Hairline
            }
            val base = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(if (active) Surface2 else Surface1)
                .border(if (active) 2.dp else 1.dp, border, RoundedCornerShape(10.dp))
            Column(
                modifier = (if (guided) base.clickable { onSelect(i) } else base)
                    .padding(vertical = 10.dp, horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("${s.order}ª", style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 1)
                Text(
                    s.letter,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (done) FuncTonic else TextStrong,
                    maxLines = 1,
                )
                Text(if (done) "✓" else s.ptName, style = MaterialTheme.typography.labelSmall, color = if (done) FuncTonic else TextMuted, maxLines = 1)
            }
        }
    }
}

@Composable
private fun TuningChip(tuning: Tuning, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Brass else Surface1)
            .border(1.dp, if (selected) Brass else Hairline, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            tuning.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (selected) Ink else TextBody,
            maxLines = 1,
        )
        Text(
            tuning.letters,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) Ink.copy(alpha = 0.7f) else TextMuted,
            maxLines = 1,
        )
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Brass else Surface1)
            .border(1.dp, if (selected) Brass else Hairline, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (selected) Ink else TextBody,
        )
    }
}

@Composable
private fun GuideCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Text("Como afinar", style = MaterialTheme.typography.titleMedium, color = TextStrong)
        Spacer(Modifier.height(8.dp))
        GuideStep("1", "No modo Corda por corda, toque na 6ª corda (a mais grossa) para travá-la.")
        GuideStep("2", "Toque a corda solta e olhe o ponteiro.")
        GuideStep("3", "Se disser \"frouxa\", aperte a tarraxa; se \"apertada\", afrouxe.")
        GuideStep("4", "Centralize o ponteiro (verde). Aí a corda está afinada.")
        GuideStep("5", "Toque na próxima corda e repita, até a 1ª (a mais fina).")
    }
}

@Composable
private fun GuideStep(n: String, text: String) {
    Row(modifier = Modifier.padding(bottom = 8.dp)) {
        Box(
            Modifier.size(22.dp).clip(CircleShape).background(Brass),
            contentAlignment = Alignment.Center,
        ) {
            Text(n, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Ink)
        }
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextBody)
    }
}

@Composable
private fun PermissionBox(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Text("Permissão do microfone", style = MaterialTheme.typography.titleLarge, color = TextStrong)
        Spacer(Modifier.height(6.dp))
        Text(
            "O afinador ouve a corda pelo microfone. O áudio é analisado no aparelho e não é gravado.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextBody,
        )
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
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
