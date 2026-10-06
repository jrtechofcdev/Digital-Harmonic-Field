package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.HarmonicDatabase
import com.example.music.Progression
import com.example.music.pitchClassCiphers
import com.example.ui.theme.Brass
import com.example.ui.theme.Hairline
import com.example.ui.theme.Ink
import com.example.ui.theme.Surface1
import com.example.ui.theme.Surface2
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextStrong
import kotlin.math.ceil
import kotlin.math.min

/**
 * Progressão em tela cheia, para tocar ao vivo: acordes gigantes que ocupam a
 * tela toda, sem rolagem. O acorde da vez fica destacado; "Próximo" avança (ou
 * toque direto num acorde). Dá para trocar o tom na hora (− / +) e a tela não
 * apaga enquanto estiver aberta.
 */
@Composable
fun ProgressionStageScreen(
    progression: Progression,
    keyCipher: String,
    onBack: () -> Unit,
    contentPadding: PaddingValues,
) {
    val minor = keyCipher.endsWith("m")
    val startRoot = pitchClassCiphers.indexOf(keyCipher.removeSuffix("m")).coerceAtLeast(0)
    var root by rememberSaveable { mutableIntStateOf(startRoot) }
    var step by rememberSaveable { mutableIntStateOf(0) }
    val cipher = pitchClassCiphers[root] + if (minor) "m" else ""
    val chords = remember(cipher) { HarmonicDatabase.getField(cipher)?.chords.orEmpty() }
    val steps = progression.degrees.mapNotNull { chords.getOrNull(it) }
    val n = steps.size.coerceAtLeast(1)

    // Tela sempre acesa enquanto toca.
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(
                start = 12.dp, end = 12.dp,
                top = contentPadding.calculateTopPadding() + 6.dp,
                bottom = contentPadding.calculateBottomPadding() + 10.dp,
            ),
    ) {
        // Topo: voltar, nome e troca de tom.
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = TextStrong, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    progression.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextStrong,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(progression.roman, style = MaterialTheme.typography.labelSmall, color = Brass, maxLines = 1)
            }
            RoundButton(onClick = { root = (root + 11) % 12 }) {
                Text("−", style = MaterialTheme.typography.titleLarge, color = TextStrong)
            }
            Text(
                HarmonicDatabase.ptNameByCipher[cipher] ?: cipher,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Brass,
                modifier = Modifier.padding(horizontal = 8.dp),
                maxLines = 1,
            )
            RoundButton(onClick = { root = (root + 1) % 12 }) {
                Text("+", style = MaterialTheme.typography.titleLarge, color = TextStrong)
            }
        }

        Spacer(Modifier.height(10.dp))

        // Grade que preenche todo o espaço disponível.
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val landscape = maxWidth > maxHeight
            val cols = when {
                landscape -> if (n <= 5) n else 4
                n <= 2 -> 1
                else -> 2
            }
            val rows = ceil(n / cols.toDouble()).toInt()
            val gap = 8.dp
            val cellW = (maxWidth - gap * (cols - 1)) / cols
            val cellH = (maxHeight - gap * (rows - 1)) / rows
            val density = LocalDensity.current
            val cipherSize = with(density) { (min(cellH.toPx() * 0.36f, cellW.toPx() * 0.30f)).toSp() }

            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                steps.withIndex().chunked(cols).forEach { row ->
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        row.forEach { (i, chord) ->
                            val color = chord.function.color()
                            val current = i == step
                            Column(
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (current) color.copy(alpha = 0.16f) else Surface1)
                                    .border(
                                        if (current) 3.dp else 1.dp,
                                        if (current) color else Hairline,
                                        RoundedCornerShape(16.dp),
                                    )
                                    .clickable { step = i },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text("${i + 1}º · ${chord.degree}", style = MaterialTheme.typography.labelLarge, color = TextMuted)
                                Text(
                                    chord.cipher,
                                    fontSize = cipherSize,
                                    fontWeight = FontWeight.Bold,
                                    color = if (current) color else color.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    softWrap = false,
                                )
                                Text(chord.function.label, style = MaterialTheme.typography.labelLarge, color = color)
                            }
                        }
                        repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Passo a passo.
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StepButton("‹ Anterior", primary = false, modifier = Modifier.weight(1f)) {
                step = (step - 1 + n) % n
            }
            StepButton(if (step == n - 1) "Recomeçar" else "Próximo ›", primary = true, modifier = Modifier.weight(2f)) {
                step = (step + 1) % n
            }
        }
    }
}

@Composable
private fun RoundButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Surface2)
            .border(1.dp, Hairline, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun StepButton(label: String, primary: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (primary) Brass else Surface1)
            .border(1.dp, if (primary) Brass else Hairline, RoundedCornerShape(14.dp))
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
