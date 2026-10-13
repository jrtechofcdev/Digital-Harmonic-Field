package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.HarmonicDatabase
import com.example.R
import com.example.data.Repertoire
import com.example.ui.components.BrandMark
import com.example.ui.components.NavRow
import com.example.ui.components.Panel
import com.example.ui.components.PrimaryButton
import com.example.ui.components.RowGroup
import com.example.ui.components.SecondaryButton
import com.example.ui.components.SectionTitle
import com.example.ui.theme.Surface2
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextStrong

/**
 * Aba Culto: o que o músico precisa NA HORA de tocar. Um único foco (Detectar o
 * tom), o repertório do dia logo abaixo (cada hino abre o campo no tom certo) e
 * as ferramentas de palco (metrônomo e capotraste).
 */
@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    onDetectKey: () -> Unit,
    onOpenKey: (String) -> Unit,
    onOpenRepertoire: () -> Unit,
    onOpenMetronome: () -> Unit,
    onOpenCapo: () -> Unit,
    onSupport: () -> Unit,
    onOpenDevPanel: () -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Marca + nome da aba.
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandMark(size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Culto", style = MaterialTheme.typography.headlineMedium, color = TextStrong)
                    Text("Digital Harmonic Field", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
            }
        }

        // O foco da tela: descobrir o tom do hino.
        item {
            Panel {
                Text("Em que tom estão cantando?", style = MaterialTheme.typography.titleLarge, color = TextStrong)
                Spacer(Modifier.size(4.dp))
                Text(
                    "Aponte o celular para quem canta. Em 5 a 15 segundos aparecem os tons mais prováveis e os acordes para começar.",
                    style = MaterialTheme.typography.bodyMedium, color = TextBody,
                )
                Spacer(Modifier.size(14.dp))
                PrimaryButton("Detectar o tom", Modifier.fillMaxWidth(), icon = R.drawable.ic_mic, onClick = onDetectKey)
            }
        }

        // Repertório do dia.
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Repertório", Modifier.weight(1f))
                if (Repertoire.items.isNotEmpty()) SecondaryButton("Editar lista", onClick = onOpenRepertoire)
            }
        }
        if (Repertoire.items.isEmpty()) {
            item {
                Panel {
                    Text("Nenhum hino na lista ainda", style = MaterialTheme.typography.titleMedium, color = TextStrong)
                    Text(
                        "Coloque os hinos do culto na ordem, com o número da Harpa e o tom. Na hora, um toque abre os acordes no tom certo.",
                        style = MaterialTheme.typography.bodyMedium, color = TextBody,
                    )
                    Spacer(Modifier.size(12.dp))
                    SecondaryButton("Montar a lista", Modifier.fillMaxWidth(), icon = R.drawable.ic_plus, onClick = onOpenRepertoire)
                }
            }
        } else {
            item {
                RowGroup(rows = Repertoire.items.mapIndexed { i, hymn ->
                    @Composable {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 64.dp)
                                .clickable(role = Role.Button) { onOpenKey(hymn.key) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(Surface2), contentAlignment = Alignment.Center) {
                                Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = TextBody)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(hymn.heading, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    (HarmonicDatabase.ptNameByCipher[hymn.key] ?: hymn.key) + if (hymn.capo > 0) ", capo na ${hymn.capo}ª casa" else "",
                                    style = MaterialTheme.typography.bodyMedium, color = TextBody, maxLines = 1,
                                )
                            }
                            Text(hymn.key, style = MaterialTheme.typography.headlineSmall, color = TextStrong)
                        }
                    }
                })
            }
        }

        // Ferramentas de palco.
        item { SectionTitle("Na hora de tocar", Modifier.padding(top = 4.dp)) }
        item {
            val rows = mutableListOf<@Composable () -> Unit>(
                { NavRow("Metrônomo", subtitle = "Tempo e compasso, com batida visual", onClick = onOpenMetronome) },
                { NavRow("Capotraste", subtitle = "Em que casa colocar para usar acordes abertos", onClick = onOpenCapo) },
            )
            if (com.example.BuildConfig.DEV_TOOLS) rows += {
                NavRow("Treino real (versão DEV)", subtitle = "Trechos captados, rótulos e envio do .zip", onClick = onOpenDevPanel)
            }
            RowGroup(rows = rows)
        }

        // Rodapé: autoria e apoio.
        item {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Feito por JR TECH, guitarrista e developer", style = MaterialTheme.typography.bodySmall, color = TextMuted, modifier = Modifier.weight(1f))
                SecondaryButton("Apoiar", onClick = onSupport)
            }
        }
    }
}
