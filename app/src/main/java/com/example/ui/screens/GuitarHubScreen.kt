package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.music.ChordShapes
import com.example.music.ChordType
import com.example.music.LabChord
import com.example.ui.components.ChordDiagram
import com.example.ui.components.NavRow
import com.example.ui.components.Panel
import com.example.ui.components.PrimaryButton
import com.example.ui.components.RowGroup
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SectionTitle
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextStrong

/** Abas internas do Braço do violão (mesma ordem de GuitarScreen). */
object GuitarTab {
    const val NOTAS = 0
    const val ACORDES = 1
    const val HARMONIZAR = 2
    const val DECORAR = 3
}

/**
 * Aba Violão: tudo o que acontece no braço. Um destaque (o braço do violão,
 * com um desenho de acorde de verdade) e, abaixo, as ferramentas agrupadas pelo
 * que o músico quer fazer: achar a forma do acorde ou enfeitar entre acordes.
 */
@Composable
fun GuitarHubScreen(
    contentPadding: PaddingValues,
    onOpenLab: (tab: Int) -> Unit,
    onOpenDictionary: () -> Unit,
    onOpenPassages: () -> Unit,
) {
    // O Sol aberto (forma de Sol), a 1ª forma do dicionário: desenho real, não ilustração.
    val g = ChordShapes.common(LabChord(7, ChordType.MAJOR)).firstOrNull()?.voicing

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ScreenHeader("Violão", subtitle = "Notas, formas e enfeites no braço") }

        item {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Braço do violão", style = MaterialTheme.typography.titleLarge, color = TextStrong)
                        Text(
                            "Toque numa nota e veja onde ela está em todo o braço. Escolha um acorde do tom e ele se monta sozinho, com 1, 3 e 5 marcados.",
                            style = MaterialTheme.typography.bodyMedium, color = TextBody,
                        )
                    }
                    g?.let {
                        Spacer(Modifier.width(12.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(width = 84.dp, height = 102.dp)) { ChordDiagram(it.shape, TextStrong) }
                            Text("Sol (G)", style = MaterialTheme.typography.labelMedium, color = TextBody)
                        }
                    }
                }
                Spacer(Modifier.size(14.dp))
                PrimaryButton("Abrir o braço do violão", Modifier.fillMaxWidth(), icon = R.drawable.ic_guitar) { onOpenLab(GuitarTab.NOTAS) }
            }
        }

        item { SectionTitle("Acordes", Modifier) }
        item {
            RowGroup(rows = listOf(
                @Composable { NavRow("Dicionário de acordes", subtitle = "As formas mais usadas e todas as formas, com os dedos", onClick = onOpenDictionary) },
                @Composable { NavRow("Acordes do tom no braço", subtitle = "Escolha o tom e o grau: o acorde se monta com variações", onClick = { onOpenLab(GuitarTab.ACORDES) }) },
                @Composable { NavRow("Harmonizar", subtitle = "Trocar o acorde seco por um com mais cor: (9), 7M, sus4", onClick = { onOpenLab(GuitarTab.HARMONIZAR) }) },
            ))
        }

        item { SectionTitle("Entre um acorde e outro") }
        item {
            RowGroup(rows = listOf(
                @Composable { NavRow("Passagens e enfeites", subtitle = "Caídas para o 1, baixo descendo, diminutos e finais de Amém", onClick = onOpenPassages) },
            ))
        }
    }
}
