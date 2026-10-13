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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.example.ui.components.OptionChip
import com.example.ui.components.Panel
import com.example.ui.components.SecondaryButton
import com.example.ui.components.SectionTitle
import com.example.ui.components.Segmented
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
import com.example.ui.components.NotationSettings
import com.example.ui.components.NotationToggle
import com.example.ui.components.SectionLabel
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
    header: @Composable () -> Unit = {},
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
        item { header() }

        // Tom: maior/menor + 12 notas, a chave Cifra|Grau e o atalho para o campo.
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Segmented(listOf("Maior", "Menor"), if (isMinor) 1 else 0, { isMinor = it == 1 }, Modifier.weight(1f))
                    NotationToggle()
                }
                (0 until 12).chunked(6).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { r ->
                            OptionChip(pitchClassCiphers[r] + if (isMinor) "m" else "", r == root, Modifier.weight(1f)) { root = r }
                        }
                    }
                }
                SecondaryButton(
                    "Ver o campo de ${ptByCipher[selectedKey] ?: selectedKey}",
                    Modifier.fillMaxWidth(),
                ) { onOpenKey(selectedKey) }
            }
        }

        groups.forEach { (group, list) ->
            item {
                Column(Modifier.padding(top = 12.dp)) {
                    SectionTitle(group.title)
                    Spacer(Modifier.height(2.dp))
                    Text(group.subtitle, style = MaterialTheme.typography.bodyMedium, color = TextBody)
                }
            }
            items(list, key = { it.name }) { prog ->
                ProgressionCard(
                    prog = prog,
                    chords = field?.chords.orEmpty(),
                    sequence = field?.takeIf { NotationSettings.byDegree }
                        ?.let { com.example.music.degreeSequence(prog.degrees, it) } ?: prog.roman,
                    onPlay = { onPlayFullScreen(prog, selectedKey) },
                )
            }
        }
    }
}

@Composable
private fun ProgressionCard(prog: Progression, chords: List<com.example.ChordInfo>, sequence: String, onPlay: () -> Unit) {
    var expanded by rememberSaveable(prog.name) { mutableStateOf(false) }
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(prog.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextStrong)
                Text(sequence, style = MaterialTheme.typography.bodyMedium, color = TextBody)
            }
            Spacer(Modifier.width(8.dp))
            SecondaryButton("Tela cheia", onClick = onPlay)
        }
        Spacer(Modifier.height(12.dp))
        ChordStrip(degrees = prog.degrees, chords = chords)
        if (expanded) {
            Spacer(Modifier.height(12.dp))
            Text(prog.description, style = MaterialTheme.typography.bodyMedium, color = TextBody)
            if (prog.tip.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = TextStrong)) { append("Dica: ") }
                        append(prog.tip)
                    },
                    style = MaterialTheme.typography.bodyMedium, color = TextBody,
                )
            }
        }
        Text(
            if (expanded) "Fechar explicação" else "Ver explicação",
            style = MaterialTheme.typography.labelLarge,
            color = TextStrong,
            modifier = Modifier
                .padding(top = 4.dp)
                .heightIn(min = 44.dp)
                .clickable(role = Role.Button) { expanded = !expanded }
                .wrapContentHeight(Alignment.CenterVertically),
        )
    }
}
