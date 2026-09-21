package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ChordInfo
import com.example.HarmonicDatabase
import com.example.music.getChordFormulaNotes
import com.example.music.transposeCipher
import com.example.music.transposePtChord
import com.example.music.transposePtNote
import com.example.ui.components.FunctionTag
import com.example.ui.theme.Brass
import com.example.ui.theme.Favorite
import com.example.ui.theme.Hairline
import com.example.ui.theme.Ink
import com.example.ui.theme.Surface1
import com.example.ui.theme.Surface2
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextStrong

@Composable
fun FieldDetailScreen(
    keyCipher: String,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
    contentPadding: PaddingValues,
) {
    // Mantém a tela ligada — ninguém quer o celular apagando no meio do louvor.
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    var transpose by remember { mutableIntStateOf(0) }
    var selected by remember { mutableIntStateOf(0) }

    val field = remember(keyCipher) { HarmonicDatabase.getField(keyCipher) }
    if (field == null) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("Tonalidade não encontrada.", color = TextStrong)
        }
        return
    }

    val keyNamePt = remember(field, transpose) { transposePtChord(field.keyNamePt, transpose) }
    val scaleNotes = remember(field, transpose) {
        field.scaleNotes.split(" — ").joinToString(" · ") { transposePtNote(it, transpose) }
    }
    val chords = remember(field, transpose) {
        field.chords.map {
            it.copy(
                cipher = transposeCipher(it.cipher, transpose),
                portugueseName = transposePtChord(it.portugueseName, transpose),
            )
        }
    }
    val selectedChord = chords.getOrElse(selected) { chords.first() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp, end = 16.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
    ) {
        DetailHeader(
            keyNamePt = keyNamePt,
            keyCipher = field.keyCipher,
            isFavorite = isFavorite,
            onBack = onBack,
            onToggleFavorite = onToggleFavorite,
        )

        Spacer(Modifier.height(16.dp))
        TransposeBar(
            transpose = transpose,
            onChange = { transpose = it.coerceIn(-11, 11) },
        )

        Spacer(Modifier.height(16.dp))
        Text(
            text = "Graus do campo",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
        )
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            chords.forEachIndexed { index, chord ->
                ChordRow(
                    chord = chord,
                    selected = index == selected,
                    onClick = { selected = index },
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        FormulaCard(chord = selectedChord)

        Spacer(Modifier.height(16.dp))
        ScaleFooter(scaleNotes = scaleNotes)
    }
}

@Composable
private fun DetailHeader(
    keyNamePt: String,
    keyCipher: String,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Voltar",
                tint = TextStrong,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "Campo harmônico".uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Brass,
            )
            Text(
                text = keyNamePt,
                style = MaterialTheme.typography.displaySmall,
                color = TextStrong,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "Favoritar",
                tint = if (isFavorite) Favorite else Hairline,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

@Composable
private fun TransposeBar(transpose: Int, onChange: (Int) -> Unit) {
    val label = when {
        transpose == 0 -> "Tom original"
        transpose > 0 -> "+$transpose semitom${if (transpose > 1) "s" else ""}"
        else -> "$transpose semitom${if (transpose < -1) "s" else ""}"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface1)
            .border(1.dp, Hairline, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text("Transpor", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = if (transpose == 0) TextBody else Brass,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (transpose != 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onChange(0) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text("Zerar", style = MaterialTheme.typography.labelLarge, color = TextMuted)
                }
            }
            StepButton("−", "Diminuir") { onChange(transpose - 1) }
            StepButton("+", "Aumentar") { onChange(transpose + 1) }
        }
    }
}

@Composable
private fun StepButton(
    symbol: String,
    desc: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(Surface2)
            .border(1.dp, Hairline, RoundedCornerShape(9.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextStrong,
        )
    }
}

@Composable
private fun ChordRow(chord: ChordInfo, selected: Boolean, onClick: () -> Unit) {
    val color = chord.function.color()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Surface2 else Surface1)
            .border(
                if (selected) 1.5.dp else 1.dp,
                if (selected) color.copy(alpha = 0.6f) else Hairline,
                RoundedCornerShape(12.dp),
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp)
            .padding(start = 0.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Barra de função
        Box(
            Modifier
                .padding(start = 12.dp, end = 12.dp)
                .width(4.dp)
                .height(38.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = chord.degree,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMuted,
                )
                FunctionTag(chord.function)
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = chord.portugueseName,
                style = MaterialTheme.typography.bodyMedium,
                color = TextBody,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = chord.cipher,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun FormulaCard(chord: ChordInfo) {
    val color = chord.function.color()
    val notes = remember(chord.cipher) { getChordFormulaNotes(chord.cipher) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Formação de ${chord.cipher}",
                style = MaterialTheme.typography.titleLarge,
                color = TextStrong,
            )
            Spacer(Modifier.weight(1f))
            FunctionTag(chord.function)
        }
        Text(
            text = chord.function.feel,
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            notes.forEach { fn ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Ink)
                        .border(1.dp, Hairline, RoundedCornerShape(10.dp))
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = fn.note,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = color,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = fn.interval,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextStrong,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = fn.detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun ScaleFooter(scaleNotes: String) {
    Column(Modifier.fillMaxWidth()) {
        Text("Notas da escala", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Spacer(Modifier.height(6.dp))
        Text(
            text = scaleNotes,
            style = MaterialTheme.typography.titleMedium,
            color = TextBody,
        )
    }
}
