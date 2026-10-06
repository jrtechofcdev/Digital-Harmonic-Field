package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.ui.components.ChordStrip
import com.example.ui.components.SectionLabel
import com.example.ui.theme.Brass
import com.example.ui.theme.Hairline
import com.example.ui.theme.Ink
import com.example.ui.theme.Surface1
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextStrong

private val ptByCipher = HarmonicDatabase.ptNameByCipher

/**
 * Progressões já montadas no tom escolhido. O seletor de tom fica no topo e é
 * compacto; cada progressão mostra os acordes ocupando a largura da tela (sem
 * rolagem lateral) e pode ser aberta em tela cheia para tocar.
 */
@Composable
fun ProgressionsScreen(
    onOpenKey: (String) -> Unit,
    onPlayFullScreen: (Progression, String) -> Unit,
    contentPadding: PaddingValues,
) {
    var root by rememberSaveable { mutableIntStateOf(7) }   // Sol
    var isMinor by rememberSaveable { mutableStateOf(false) }
    val selectedKey = pitchClassCiphers[root] + if (isMinor) "m" else ""
    val field = remember(selectedKey) { HarmonicDatabase.getField(selectedKey) }
    val groups = remember(isMinor) {
        ProgressionLibrary.forField(isMinor).groupBy { it.group }.toSortedMap(compareBy { it.ordinal })
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Progressões", style = MaterialTheme.typography.headlineMedium, color = TextStrong)
                    Text(
                        "Tom de ${ptByCipher[selectedKey] ?: selectedKey}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Brass,
                    )
                }
                Text(
                    "Ver campo ›",
                    style = MaterialTheme.typography.labelLarge,
                    color = Brass,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenKey(selectedKey) }
                        .padding(8.dp),
                )
            }
        }

        // Seletor de tom compacto: maior/menor + 12 notas.
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("Maior", !isMinor, Modifier.weight(1f)) { isMinor = false }
                    Pill("Menor", isMinor, Modifier.weight(1f)) { isMinor = true }
                }
                (0 until 12).chunked(6).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { r ->
                            Pill(
                                label = pitchClassCiphers[r] + if (isMinor) "m" else "",
                                selected = r == root,
                                modifier = Modifier.weight(1f),
                            ) { root = r }
                        }
                    }
                }
            }
        }

        groups.forEach { (group, list) ->
            item {
                Column(Modifier.padding(top = 8.dp)) {
                    SectionLabel(group.title)
                    Spacer(Modifier.height(4.dp))
                    Text(group.subtitle, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
            }
            items(list, key = { it.name }) { prog ->
                ProgressionCard(
                    prog = prog,
                    chords = field?.chords.orEmpty(),
                    onPlay = { onPlayFullScreen(prog, selectedKey) },
                )
            }
        }
    }
}

@Composable
private fun Pill(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Brass else Surface1)
            .border(1.dp, if (selected) Brass else Hairline, RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (selected) Ink else TextBody,
            maxLines = 1,
        )
    }
}

@Composable
private fun ProgressionCard(prog: Progression, chords: List<com.example.ChordInfo>, onPlay: () -> Unit) {
    var expanded by rememberSaveable(prog.name) { mutableStateOf(false) }
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
            .clickable { expanded = !expanded }
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(prog.name, style = MaterialTheme.typography.titleMedium, color = TextStrong)
                Text(prog.roman, style = MaterialTheme.typography.labelLarge, color = Brass)
            }
            // Tela cheia: acordes grandes para tocar.
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brass)
                    .clickable { onPlay() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text("Tela cheia", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Ink)
            }
        }
        Spacer(Modifier.height(12.dp))
        ChordStrip(degrees = prog.degrees, chords = chords)
        Spacer(Modifier.height(10.dp))
        if (expanded) {
            Text(prog.description, style = MaterialTheme.typography.bodyMedium, color = TextBody)
            if (prog.tip.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brass.copy(alpha = 0.10f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Text("Dica", style = MaterialTheme.typography.labelSmall, color = Brass)
                    Spacer(Modifier.width(8.dp))
                    Text(prog.tip, style = MaterialTheme.typography.bodyMedium, color = TextBody)
                }
            }
        } else {
            Text("Toque para ver a explicação", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
    }
}
