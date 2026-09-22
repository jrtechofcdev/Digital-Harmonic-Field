package com.example.ui.screens

import android.content.res.Configuration
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
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.ChordInfo
import com.example.HarmonicDatabase
import com.example.HarmonicField
import com.example.music.Progression
import com.example.music.ProgressionLibrary
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

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    var transpose by remember { mutableIntStateOf(0) }
    var selected by remember { mutableIntStateOf(0) }

    val field = remember(keyCipher) { HarmonicDatabase.getField(keyCipher) }
    if (field == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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

    if (isLandscape) {
        LandscapeLayout(
            field = field,
            keyNamePt = keyNamePt,
            chords = chords,
            selected = selected,
            onSelect = { selected = it },
            selectedChord = selectedChord,
            scaleNotes = scaleNotes,
            transpose = transpose,
            onTranspose = { transpose = it.coerceIn(-11, 11) },
            isFavorite = isFavorite,
            onToggleFavorite = onToggleFavorite,
            onBack = onBack,
            contentPadding = contentPadding,
        )
    } else {
        PortraitLayout(
            field = field,
            keyNamePt = keyNamePt,
            chords = chords,
            selected = selected,
            onSelect = { selected = it },
            selectedChord = selectedChord,
            scaleNotes = scaleNotes,
            transpose = transpose,
            onTranspose = { transpose = it.coerceIn(-11, 11) },
            isFavorite = isFavorite,
            onToggleFavorite = onToggleFavorite,
            onBack = onBack,
            contentPadding = contentPadding,
        )
    }
}

// ---------------------------------------------------------------------------
// Retrato — rolagem confortável
// ---------------------------------------------------------------------------

@Composable
private fun PortraitLayout(
    field: HarmonicField,
    keyNamePt: String,
    chords: List<ChordInfo>,
    selected: Int,
    onSelect: (Int) -> Unit,
    selectedChord: ChordInfo,
    scaleNotes: String,
    transpose: Int,
    onTranspose: (Int) -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
    contentPadding: PaddingValues,
) {
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
        DetailHeader(keyNamePt, isFavorite, onBack, onToggleFavorite)

        Spacer(Modifier.height(16.dp))
        TransposeBar(transpose, onTranspose)

        Spacer(Modifier.height(16.dp))
        Text("Graus do campo", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            chords.forEachIndexed { index, chord ->
                ChordRow(chord, index == selected) { onSelect(index) }
            }
        }

        Spacer(Modifier.height(16.dp))
        FormulaCard(selectedChord)

        Spacer(Modifier.height(16.dp))
        CommonPathsCard(chords = chords, isMinor = field.isMinor)

        Spacer(Modifier.height(16.dp))
        ScaleFooter(scaleNotes)
    }
}

// ---------------------------------------------------------------------------
// Paisagem — tudo na tela, sem rolagem
// ---------------------------------------------------------------------------

@Composable
private fun LandscapeLayout(
    field: HarmonicField,
    keyNamePt: String,
    chords: List<ChordInfo>,
    selected: Int,
    onSelect: (Int) -> Unit,
    selectedChord: ChordInfo,
    scaleNotes: String,
    transpose: Int,
    onTranspose: (Int) -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
    contentPadding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = contentPadding.calculateStartPadding(LayoutDirection.Ltr) + 14.dp,
                end = contentPadding.calculateEndPadding(LayoutDirection.Ltr) + 14.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 8.dp,
            ),
    ) {
        // Cabeçalho compacto com transposição embutida
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", onBack)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Campo de".uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Brass,
                )
                Text(
                    keyNamePt,
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextStrong,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TransposeCompact(transpose, onTranspose)
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    Icons.Filled.Star, "Favoritar",
                    tint = if (isFavorite) Favorite else Hairline,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Fileira dos 7 graus, ocupando a largura toda
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            chords.forEachIndexed { index, chord ->
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    ChordMini(chord, index == selected) { onSelect(index) }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Parte de baixo: formação (esquerda) + caminho principal (direita)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.weight(1f).fillMaxHeight()) {
                FormulaCard(selectedChord, compact = true)
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                CommonPathsCard(chords = chords, isMinor = field.isMinor, compact = true, maxPaths = 1)
            }
        }

        Spacer(Modifier.height(8.dp))
        // Escala em uma linha só, no rodapé
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Escala:  ",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
            )
            Text(
                scaleNotes,
                style = MaterialTheme.typography.bodyMedium,
                color = TextBody,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Componentes compartilhados
// ---------------------------------------------------------------------------

@Composable
private fun DetailHeader(
    keyNamePt: String,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        CircleIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", onBack)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Campo harmônico".uppercase(), style = MaterialTheme.typography.labelSmall, color = Brass)
            Text(
                keyNamePt,
                style = MaterialTheme.typography.displaySmall,
                color = TextStrong,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onToggleFavorite) {
            Icon(
                Icons.Filled.Star, "Favoritar",
                tint = if (isFavorite) Favorite else Hairline,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

@Composable
private fun CircleIconButton(
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

@Composable
private fun TransposeBar(transpose: Int, onChange: (Int) -> Unit) {
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
                transposeLabel(transpose),
                style = MaterialTheme.typography.titleMedium,
                color = if (transpose == 0) TextBody else Brass,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (transpose != 0) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onChange(0) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) { Text("Zerar", style = MaterialTheme.typography.labelLarge, color = TextMuted) }
            }
            StepButton("−", "Diminuir") { onChange(transpose - 1) }
            StepButton("+", "Aumentar") { onChange(transpose + 1) }
        }
    }
}

@Composable
private fun TransposeCompact(transpose: Int, onChange: (Int) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StepButton("−", "Diminuir") { onChange(transpose - 1) }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
            Text("Transpor", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(
                if (transpose == 0) "Original" else (if (transpose > 0) "+$transpose" else "$transpose"),
                style = MaterialTheme.typography.titleMedium,
                color = if (transpose == 0) TextBody else Brass,
            )
        }
        StepButton("+", "Aumentar") { onChange(transpose + 1) }
    }
}

private fun transposeLabel(transpose: Int): String = when {
    transpose == 0 -> "Tom original"
    transpose > 0 -> "+$transpose semitom${if (transpose > 1) "s" else ""}"
    else -> "$transpose semitom${if (transpose < -1) "s" else ""}"
}

@Composable
private fun StepButton(symbol: String, desc: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(Surface2)
            .border(1.dp, Hairline, RoundedCornerShape(9.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextStrong)
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
            .padding(end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
                Text(chord.degree, style = MaterialTheme.typography.labelLarge, color = TextMuted)
                FunctionTag(chord.function)
            }
            Spacer(Modifier.height(2.dp))
            Text(
                chord.portugueseName,
                style = MaterialTheme.typography.bodyMedium,
                color = TextBody,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            chord.cipher,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun ChordMini(chord: ChordInfo, selected: Boolean, onClick: () -> Unit) {
    val color = chord.function.color()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Surface2 else Surface1)
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) color else Hairline,
                RoundedCornerShape(10.dp),
            )
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(chord.degree, style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 1)
        Text(
            chord.cipher,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(chord.function.short, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1)
    }
}

@Composable
private fun FormulaCard(chord: ChordInfo, compact: Boolean = false) {
    val color = chord.function.color()
    val notes = remember(chord.cipher) { getChordFormulaNotes(chord.cipher) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (compact) Modifier.fillMaxHeight() else Modifier)
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .padding(if (compact) 12.dp else 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Formação de ${chord.cipher}",
                style = MaterialTheme.typography.titleLarge,
                color = TextStrong,
            )
            Spacer(Modifier.weight(1f))
            FunctionTag(chord.function)
        }
        if (!compact) {
            Text(chord.function.tendency, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        }
        Spacer(Modifier.height(if (compact) 8.dp else 14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            notes.forEach { fn ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Ink)
                        .border(1.dp, Hairline, RoundedCornerShape(10.dp))
                        .padding(vertical = if (compact) 8.dp else 12.dp, horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        fn.note,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = color,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(fn.interval, style = MaterialTheme.typography.labelSmall, color = TextStrong, textAlign = TextAlign.Center)
                    if (!compact) {
                        Spacer(Modifier.height(3.dp))
                        Text(fn.detail, style = MaterialTheme.typography.bodyMedium, color = TextMuted, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun CommonPathsCard(
    chords: List<ChordInfo>,
    isMinor: Boolean,
    compact: Boolean = false,
    maxPaths: Int = Int.MAX_VALUE,
) {
    val paths = remember(isMinor, maxPaths) { ProgressionLibrary.featuredFor(isMinor).take(maxPaths) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .padding(if (compact) 12.dp else 16.dp),
    ) {
        Text("Caminhos comuns neste tom", style = MaterialTheme.typography.titleMedium, color = TextStrong)
        if (!compact) {
            Text(
                "Sequências prontas para acompanhar — é só seguir a ordem.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
        }
        Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)) {
            paths.forEach { prog ->
                PathRow(prog, chords)
            }
        }
    }
}

@Composable
private fun PathRow(prog: Progression, chords: List<ChordInfo>) {
    Column {
        Text(prog.name, style = MaterialTheme.typography.labelSmall, color = Brass)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            prog.degrees.forEachIndexed { position, degreeIndex ->
                val chord = chords.getOrNull(degreeIndex) ?: return@forEachIndexed
                if (position > 0) {
                    Text("→", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
                val color = chord.function.color()
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Surface2)
                        .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Text(
                        chord.cipher,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = color,
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
        Text(scaleNotes, style = MaterialTheme.typography.titleMedium, color = TextBody)
    }
}
