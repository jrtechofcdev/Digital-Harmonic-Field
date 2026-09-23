package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.audio.ChordListener
import com.example.music.pitchClassCiphers
import com.example.music.pitchClassOf
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

@Composable
fun ListenScreen(
    onOpenKey: (String) -> Unit,
    onBack: () -> Unit,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val listener = remember { ChordListener() }

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
            if (event == Lifecycle.Event.ON_STOP) listener.stop()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            listener.stop()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp, end = 16.dp,
                top = contentPadding.calculateTopPadding() + 12.dp,
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
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar",
                    tint = TextStrong,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Text("Ouvir", style = MaterialTheme.typography.headlineMedium, color = TextStrong)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Toque um acorde perto do celular. O app escuta e estima o acorde e o tom provável da música.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextBody,
        )
        Spacer(Modifier.height(16.dp))

        if (!hasPermission) {
            PermissionCard { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
        } else {
            ListenButton(listener.isListening) { listener.toggle() }

            listener.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = FuncDominant)
            }

            Spacer(Modifier.height(16.dp))
            CurrentChordCard(
                chord = listener.currentChord,
                chordPt = listener.currentChordPt,
                isListening = listener.isListening,
                level = listener.level,
            )

            Spacer(Modifier.height(12.dp))
            KeyCard(
                keyCipher = listener.keyCipher,
                keyPt = listener.keyPt,
                isListening = listener.isListening,
                onOpenKey = onOpenKey,
                onReset = { listener.resetKey() },
            )

            Spacer(Modifier.height(16.dp))
            SectionLabel("O que o app está ouvindo")
            Spacer(Modifier.height(10.dp))
            ChromaBars(chroma = listener.chroma, chord = listener.currentChord)

            Spacer(Modifier.height(16.dp))
            TipsCard()
        }
    }
}

@Composable
private fun PermissionCard(onRequest: () -> Unit) {
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
            "Para identificar o acorde e o tom, o app precisa ouvir pelo microfone. " +
                "O áudio é analisado no próprio aparelho e não é gravado nem enviado a lugar nenhum.",
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
private fun ListenButton(isListening: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isListening) FuncDominant else Brass)
            .clickable { onToggle() }
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (isListening) "Parar de ouvir" else "Começar a ouvir",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Ink,
        )
    }
}

@Composable
private fun CurrentChordCard(
    chord: String?,
    chordPt: String?,
    isListening: Boolean,
    level: Float,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SectionLabel("Acorde agora")
        Spacer(Modifier.height(10.dp))
        Text(
            text = chord ?: if (isListening) "—" else "···",
            style = MaterialTheme.typography.displaySmall,
            fontSize = 52.sp,
            fontWeight = FontWeight.Bold,
            color = if (chord != null) Brass else TextMuted,
        )
        Text(
            text = chordPt ?: if (isListening) "ouvindo…" else "toque em começar",
            style = MaterialTheme.typography.titleMedium,
            color = TextBody,
        )
        if (isListening) {
            Spacer(Modifier.height(14.dp))
            LevelBar(level)
            if (level < 0.08f) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Som muito baixo — aproxime o instrumento do celular.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun LevelBar(level: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Surface2),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(level.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(FuncTonic),
        )
    }
}

@Composable
private fun KeyCard(
    keyCipher: String?,
    keyPt: String?,
    isListening: Boolean,
    onOpenKey: (String) -> Unit,
    onReset: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SectionLabel("Tom provável da música")
                Spacer(Modifier.height(6.dp))
                Text(
                    keyPt ?: if (isListening) "estimando…" else "—",
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (keyCipher != null) TextStrong else TextMuted,
                )
            }
            if (keyCipher != null && isListening) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(Surface2)
                        .border(1.dp, Hairline, RoundedCornerShape(9.dp))
                        .clickable { onReset() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text("Recomeçar", style = MaterialTheme.typography.labelLarge, color = TextBody)
                }
            }
        }
        if (keyCipher != null) {
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brass)
                    .clickable { onOpenKey(keyCipher) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Abrir campo de $keyCipher",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
            }
        } else if (isListening) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Toque a música por alguns segundos para o app fechar o tom.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
        }
    }
}

@Composable
private fun ChromaBars(chroma: FloatArray, chord: String?) {
    val highlighted = remember(chord) { highlightedPitchClasses(chord) }
    val maxHeight = 110.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface1)
            .border(1.dp, Hairline, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        for (pc in 0 until 12) {
            val value = chroma.getOrElse(pc) { 0f }.coerceIn(0f, 1f)
            val isOn = highlighted.contains(pc)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(maxHeight),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(maxHeight * value.coerceAtLeast(0.02f))
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isOn) Brass else Surface2),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    pitchClassCiphers[pc],
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOn) Brass else TextMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun TipsCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Text("Para acertar melhor", style = MaterialTheme.typography.titleMedium, color = TextStrong)
        Spacer(Modifier.height(8.dp))
        Tip("Um instrumento de cada vez, em local silencioso.")
        Tip("Acordes claros e sustentados funcionam melhor que dedilhados rápidos.")
        Tip("Deixe tocando alguns segundos: o tom fica mais firme quanto mais o app ouve.")
        Tip("É uma estimativa para ajudar seu ouvido — confie sempre no que você escuta.")
    }
}

@Composable
private fun Tip(text: String) {
    Row(modifier = Modifier.padding(bottom = 8.dp)) {
        Box(
            Modifier
                .padding(top = 6.dp, end = 10.dp)
                .size(5.dp)
                .clip(CircleShape)
                .background(Brass)
        )
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextBody)
    }
}

/** Classes de altura (0..11) que compõem o acorde atual, para destacar no gráfico. */
private fun highlightedPitchClasses(chord: String?): Set<Int> {
    if (chord.isNullOrEmpty()) return emptySet()
    val isMinor = chord.endsWith("m") && !chord.endsWith("dim")
    val rootCipher = if (isMinor) chord.dropLast(1) else chord
    val root = pitchClassOf(rootCipher)
    if (root < 0) return emptySet()
    val third = if (isMinor) 3 else 4
    return setOf(root % 12, (root + third) % 12, (root + 7) % 12)
}
