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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.components.SectionLabel
import com.example.ui.theme.Brass
import com.example.ui.theme.Hairline
import com.example.ui.theme.Ink
import com.example.ui.theme.Surface1
import com.example.ui.theme.Surface2
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextStrong

private val majorKeys = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
private val allKeysOrdered = majorKeys + majorKeys.map { it + "m" }
private val ptByCipher = HarmonicDatabase.allKeys().associate { it.keyCipher to it.keyNamePt }

@Composable
fun ProgressionsScreen(
    onOpenKey: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    var selectedKey by remember { mutableStateOf("G") }
    val field = remember(selectedKey) { HarmonicDatabase.getField(selectedKey) }
    val progressions = remember(selectedKey) {
        ProgressionLibrary.forField(field?.isMinor ?: false)
    }

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
                Text(
                    "Progressões",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextStrong,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Sequências consagradas já montadas no tom escolhido. Ótimas para compor, ensaiar e conduzir a ministração.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextBody,
                )
            }
        }

        item {
            Column {
                SectionLabel("Tom")
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(allKeysOrdered) { key ->
                        KeyChip(
                            label = key,
                            selected = key == selectedKey,
                            onClick = { selectedKey = key },
                        )
                    }
                }
            }
        }

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
                            "Campo de ${ptByCipher[selectedKey] ?: selectedKey}",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextStrong,
                        )
                        Text(
                            "Toque para ver todos os graus deste tom",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                        )
                    }
                    Text("Abrir ›", style = MaterialTheme.typography.labelLarge, color = Brass)
                }
            }
        }

        items(progressions) { prog ->
            ProgressionCard(prog = prog, keyCipher = selectedKey)
        }
    }
}

@Composable
private fun KeyChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Brass else Surface1)
            .border(1.dp, if (selected) Brass else Hairline, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (selected) Ink else TextBody,
        )
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
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(prog.name, style = MaterialTheme.typography.titleLarge, color = TextStrong)
            Spacer(Modifier.weight(1f))
            Text(prog.roman, style = MaterialTheme.typography.labelLarge, color = Brass)
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            prog.degrees.forEach { degreeIndex ->
                val chord = field?.chords?.getOrNull(degreeIndex)
                if (chord != null) {
                    ChordChip(cipher = chord.cipher, degree = chord.degree, color = chord.function.color())
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(prog.description, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
    }
}

@Composable
private fun ChordChip(cipher: String, degree: String, color: androidx.compose.ui.graphics.Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Surface2)
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(cipher, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        Spacer(Modifier.height(2.dp))
        Text(degree, style = MaterialTheme.typography.labelSmall, color = TextMuted)
    }
}
