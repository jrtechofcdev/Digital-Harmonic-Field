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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import com.example.music.centsBetween
import com.example.music.midiToName
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
import kotlin.math.abs

@Composable
fun TunerScreen(
    settings: TunerSettings,
    onSettingsChange: (TunerSettings) -> Unit,
    onBack: () -> Unit,
    contentPadding: PaddingValues,
) {
    // IMPORTANTE: o microfone e os effects são criados ANTES do desvio para as
    // configurações. Assim, abrir os ajustes não desmonta a escuta (o afinador
    // continua ativo ao voltar).
    val context = LocalContext.current
    val listener = remember { TunerListener() }
    val tone = remember { TonePlayer() }

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

    // Aplica o filtro de ruído às leituras.
    LaunchedEffect(settings.noiseFilter) {
        listener.minClarity = settings.noiseFilter.minClarity
        listener.silenceRms = settings.noiseFilter.silenceRms
    }

    // Silencia a detecção enquanto o tom de referência toca, para o afinador não
    // "ouvir" o próprio alto-falante e marcar como afinado sem motivo.
    LaunchedEffect(tone.playingKey) {
        listener.muted = tone.playingKey != null
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) { listener.stop(); tone.stop() }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            listener.stop(); tone.stop()
        }
    }

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
    var chromatic by remember { mutableStateOf(false) }
    var manualString by remember { mutableStateOf<Int?>(null) }
    val tuning = TuningLibrary.byId(tuningId)
    val refA = settings.refA.toDouble()

    val freq = listener.frequency
    val analysis = if (freq != null) {
        analyze(freq, chromatic, tuning, manualString, refA, settings.precision.cents)
    } else null

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
        // Cabeçalho
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundIcon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", onBack)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Afinador", style = MaterialTheme.typography.headlineMedium, color = TextStrong)
                Text(
                    "Lá = %.1f Hz · %s".format(settings.refA, tuning.name),
                    style = MaterialTheme.typography.labelSmall,
                    color = Brass,
                )
            }
            RoundIcon(Icons.Filled.Settings, "Configurações") { showSettings = true }
        }

        Spacer(Modifier.height(16.dp))

        if (!hasPermission) {
            PermissionBox { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
        } else {
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
                        manualString = null
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(tuning.description, style = MaterialTheme.typography.bodyMedium, color = TextMuted)

            Spacer(Modifier.height(16.dp))

            // Medidor principal
            MeterCard(
                analysis = analysis,
                listening = listener.isListening,
                tolerance = settings.precision.cents.toFloat(),
            )

            Spacer(Modifier.height(12.dp))

            // Cordas da afinação
            StringsRow(
                tuning = tuning,
                refA = refA,
                activeIndex = analysis?.activeIndex,
                inTune = analysis?.inTune == true,
                manualIndex = manualString,
                onSelect = { idx ->
                    manualString = if (manualString == idx) null else idx
                    chromatic = false
                    if (settings.sounds) {
                        val s = tuning.strings[idx]
                        tone.toggle(s.noteName, s.targetFreq(refA))
                    }
                },
            )

            Spacer(Modifier.height(12.dp))

            // Modo cromático x por corda
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ModeChip("Por corda", !chromatic, Modifier.weight(1f)) { chromatic = false }
                ModeChip("Cromático", chromatic, Modifier.weight(1f)) {
                    chromatic = true
                    manualString = null
                }
            }

            Spacer(Modifier.height(16.dp))

            // Botão ouvir/parar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (listener.isListening) FuncDominant else Brass)
                    .clickable { listener.toggle() }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (listener.isListening) "Parar" else "Começar a afinar",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
            }

            listener.error?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = FuncDominant)
            }

            Spacer(Modifier.height(16.dp))
            GuideCard()
        }
    }
}

// ---------------------------------------------------------------------------
// Lógica de afinação
// ---------------------------------------------------------------------------

private data class TuneAnalysis(
    val targetName: String,   // ex.: "E2"
    val targetPt: String,     // ex.: "Mi"
    val cents: Double,        // desvio em relação ao alvo
    val activeIndex: Int?,    // corda ativa (modo por corda)
    val inTune: Boolean,
)

private fun analyze(
    freq: Double,
    chromatic: Boolean,
    tuning: Tuning,
    manualString: Int?,
    refA: Double,
    tolerance: Double,
): TuneAnalysis {
    if (chromatic) {
        val r = readingForFrequency(freq, refA)
        return TuneAnalysis(r.name, ptPitchClass(r.midi), r.cents, null, abs(r.cents) <= tolerance)
    }
    val idx = manualString ?: nearestStringIndex(freq, tuning, refA)
    val s = tuning.strings[idx]
    val cents = centsBetween(freq, s.targetFreq(refA))
    return TuneAnalysis(s.noteName, s.ptName, cents, idx, abs(cents) <= tolerance)
}

private fun nearestStringIndex(freq: Double, tuning: Tuning, refA: Double): Int {
    var best = 0
    var bestCents = Double.MAX_VALUE
    tuning.strings.forEachIndexed { i, s ->
        val c = abs(centsBetween(freq, s.targetFreq(refA)))
        if (c < bestCents) { bestCents = c; best = i }
    }
    return best
}

// ---------------------------------------------------------------------------
// Componentes
// ---------------------------------------------------------------------------

@Composable
private fun MeterCard(analysis: TuneAnalysis?, listening: Boolean, tolerance: Float) {
    val inTune = analysis?.inTune == true
    val noteColor = if (inTune) FuncTonic else TextStrong
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
            .padding(vertical = 20.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = analysis?.targetName ?: if (listening) "—" else "···",
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
            color = noteColor,
            style = MaterialTheme.typography.displaySmall,
        )
        Text(
            text = analysis?.targetPt ?: if (listening) "toque uma corda" else "toque em começar",
            style = MaterialTheme.typography.titleMedium,
            color = TextBody,
        )
        Spacer(Modifier.height(16.dp))
        CentsMeter(cents = analysis?.cents?.toFloat(), tolerance = tolerance, inTune = inTune)
        Spacer(Modifier.height(12.dp))
        Text(
            text = directionText(analysis),
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

private fun directionText(a: TuneAnalysis?): String = when {
    a == null -> "Aguardando o som…"
    a.inTune -> "Afinado ✓"
    a.cents < 0 -> "Está grave — aperte a corda"
    else -> "Está agudo — afrouxe a corda"
}

@Composable
private fun CentsMeter(cents: Float?, tolerance: Float, inTune: Boolean) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val baseY = h * 0.62f
        val sidePad = 16.dp.toPx()
        val halfW = cx - sidePad
        fun xForCents(c: Float) = cx + (c.coerceIn(-50f, 50f) / 50f) * halfW

        // Zona verde de tolerância no centro.
        val zoneHalf = (tolerance / 50f) * halfW
        drawRoundRect(
            color = FuncTonic.copy(alpha = 0.16f),
            topLeft = Offset(cx - zoneHalf, baseY - 26.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(zoneHalf * 2f, 52.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
        )

        // Régua de marcas a cada 10 cents.
        var c = -50
        while (c <= 50) {
            val x = xForCents(c.toFloat())
            val tall = c == 0
            drawLine(
                color = if (tall) Brass else Hairline,
                start = Offset(x, baseY - if (tall) 22.dp.toPx() else 12.dp.toPx()),
                end = Offset(x, baseY + if (tall) 22.dp.toPx() else 12.dp.toPx()),
                strokeWidth = if (tall) 3.dp.toPx() else 1.5.dp.toPx(),
                cap = StrokeCap.Round,
            )
            c += 10
        }

        // Indicador de leitura.
        if (cents != null) {
            val x = xForCents(cents)
            val color = if (inTune) FuncTonic else Brass
            drawLine(
                color = color,
                start = Offset(x, baseY - 30.dp.toPx()),
                end = Offset(x, baseY + 30.dp.toPx()),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawCircle(color = color, radius = 7.dp.toPx(), center = Offset(x, baseY - 34.dp.toPx()))
        }
    }
}

@Composable
private fun StringsRow(
    tuning: Tuning,
    refA: Double,
    activeIndex: Int?,
    inTune: Boolean,
    manualIndex: Int?,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tuning.strings.forEachIndexed { i, s ->
            val active = i == activeIndex
            val done = active && inTune
            val borderColor = when {
                done -> FuncTonic
                active -> Brass
                manualIndex == i -> Brass.copy(alpha = 0.7f)
                else -> Hairline
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) Surface2 else Surface1)
                    .border(if (active) 2.dp else 1.dp, borderColor, RoundedCornerShape(10.dp))
                    .clickable { onSelect(i) }
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
            .height(44.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) Surface2 else Surface1)
            .border(1.dp, if (selected) Brass else Hairline, RoundedCornerShape(11.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) Brass else TextBody,
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
        Text("Como afinar (passo a passo)", style = MaterialTheme.typography.titleMedium, color = TextStrong)
        Spacer(Modifier.height(8.dp))
        GuideStep("1", "Escolha a afinação (comece na Padrão) e toque em Começar.")
        GuideStep("2", "Toque UMA corda solta. O app mostra a nota mais próxima.")
        GuideStep("3", "Gire a tarraxa: se o app disser \"grave\", aperte; se \"agudo\", afrouxe.")
        GuideStep("4", "Deixe o ponteiro no centro (verde). Aí a corda está afinada.")
        GuideStep("5", "Repita nas outras cordas, da 6ª (mais grossa) à 1ª (mais fina).")
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

@Composable
private fun RoundIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Surface1)
            .border(1.dp, Hairline, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = desc, tint = TextStrong, modifier = Modifier.size(18.dp))
    }
}
