package com.example.ui.screens

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.HarmonicDatabase
import com.example.music.Progression
import com.example.music.ProgressionLibrary
import com.example.music.pitchClassCiphers
import com.example.ui.components.SectionLabel
import com.example.ui.theme.Brass
import com.example.ui.theme.Hairline
import com.example.ui.theme.Ink
import com.example.ui.theme.Surface1
import com.example.ui.theme.Surface2
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextStrong

private val rootPtNames = listOf("Dó", "Dó#", "Ré", "Ré#", "Mi", "Fá", "Fá#", "Sol", "Sol#", "Lá", "Lá#", "Si")
private val ptByCipher = HarmonicDatabase.ptNameByCipher

@Composable
fun ProgressionsScreen(
    onOpenKey: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    var root by remember { mutableIntStateOf(7) }   // Sol
    var isMinor by remember { mutableStateOf(false) }
    val selectedKey = pitchClassCiphers[root] + if (isMinor) "m" else ""
    val progressions = remember(isMinor) { ProgressionLibrary.forField(isMinor) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text("Progressões", style = MaterialTheme.typography.headlineMedium, color = TextStrong)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Escolha o tom e siga a ordem dos acordes. Comece pelas marcadas como essenciais.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextBody,
                )
            }
        }

        // Passo 1: tipo do tom
        item {
            Column {
                SectionLabel("1. Tipo do tom")
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ToggleButton("Maior", !isMinor, Modifier.weight(1f)) { isMinor = false }
                    ToggleButton("Menor", isMinor, Modifier.weight(1f)) { isMinor = true }
                }
            }
        }

        // Passo 2: nota do tom
        item {
            Column {
                SectionLabel("2. Nota do tom")
                Spacer(Modifier.height(10.dp))
                NoteGrid(selectedRoot = root, onSelect = { root = it })
            }
        }

        // Atalho para o campo completo
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface1)
                    .border(1.dp, Hairline, RoundedCornerShape(12.dp))
                    .clickable { onOpenKey(selectedKey) }
                    .padding(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Tom de ${ptByCipher[selectedKey] ?: selectedKey}",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextStrong,
                        )
                        Text(
                            "Ver todos os acordes deste tom",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                        )
                    }
                    Text("Abrir ›", style = MaterialTheme.typography.labelLarge, color = Brass)
                }
            }
        }

        item { SectionLabel("3. Progressões neste tom") }

        items(progressions) { prog ->
            ProgressionCard(prog = prog, keyCipher = selectedKey)
        }
    }
}

@Composable
private fun ToggleButton(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(48.dp)
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
private fun NoteGrid(selectedRoot: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        (0 until 12).chunked(6).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { r ->
                    val selected = r == selectedRoot
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) Brass else Surface1)
                            .border(1.dp, if (selected) Brass else Hairline, RoundedCornerShape(10.dp))
                            .clickable { onSelect(r) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            pitchClassCiphers[r],
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Ink else TextStrong,
                        )
                        Text(
                            rootPtNames[r],
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) Ink else TextMuted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressionCard(prog: Progression, keyCipher: String) {
    val field = remember(keyCipher) { HarmonicDatabase.getField(keyCipher) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(
                if (prog.featured) 1.5.dp else 1.dp,
                if (prog.featured) Brass.copy(alpha = 0.55f) else Hairline,
                RoundedCornerShape(14.dp),
            )
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                if (prog.featured) {
                    Text("ESSENCIAL", style = MaterialTheme.typography.labelSmall, color = Brass)
                    Spacer(Modifier.height(2.dp))
                }
                Text(prog.name, style = MaterialTheme.typography.titleLarge, color = TextStrong)
            }
            Text(prog.roman, style = MaterialTheme.typography.labelLarge, color = Brass)
        }
        Spacer(Modifier.height(12.dp))
        // Acordes grandes, na ordem de tocar.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            prog.degrees.forEachIndexed { position, degreeIndex ->
                val chord = field?.chords?.getOrNull(degreeIndex) ?: return@forEachIndexed
                if (position > 0) {
                    Text("→", style = MaterialTheme.typography.titleLarge, color = TextMuted)
                }
                ChordChip(cipher = chord.cipher, functionLabel = chord.function.label, color = chord.function.color())
            }
        }
        if (prog.tip.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brass.copy(alpha = 0.10f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text("Dica  ", style = MaterialTheme.typography.labelSmall, color = Brass)
                Text(prog.tip, style = MaterialTheme.typography.bodyMedium, color = TextBody)
            }
        }
    }
}

@Composable
private fun ChordChip(cipher: String, functionLabel: String, color: androidx.compose.ui.graphics.Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Surface2)
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(cipher, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
        Spacer(Modifier.height(2.dp))
        Text(functionLabel, style = MaterialTheme.typography.labelSmall, color = color)
    }
}
