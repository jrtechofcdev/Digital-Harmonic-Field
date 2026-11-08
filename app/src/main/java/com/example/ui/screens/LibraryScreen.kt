package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.HarmonicDatabase
import com.example.R
import com.example.data.Live
import com.example.music.LabChord
import com.example.music.pitchClassCiphers
import com.example.ui.components.Motion
import com.example.ui.components.NavRow
import com.example.ui.components.RowGroup
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SectionLabel
import com.example.ui.components.SectionTitle
import com.example.ui.components.pressClick
import com.example.ui.theme.Favorite
import com.example.ui.theme.Hairline
import com.example.ui.theme.HairlineStrong
import com.example.ui.theme.Ink
import com.example.ui.theme.Selection
import com.example.ui.theme.Surface1
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextStrong

private val majorOrder = pitchClassCiphers
private val minorOrder = pitchClassCiphers.map { it + "m" }

/**
 * Aba Biblioteca: tudo o que o app tem, num lugar só. No topo, a busca (tom,
 * acorde ou ferramenta). Abaixo, as ferramentas no tom do momento, os 24 tons
 * (um toque e o tom vai para a aba Tocar) e o dicionário.
 */
@Composable
fun LibraryScreen(
    contentPadding: PaddingValues,
    favorites: Set<String>,
    onPlayKey: (String) -> Unit,
    onOpenTool: (Tool) -> Unit,
    onOpenChord: (LabChord) -> Unit,
    onOpenDevPanel: () -> Unit = {},
) {
    var query by rememberSaveable { mutableStateOf("") }
    val hits = remember(query) { LibrarySearch.search(query) }
    val keyName = HarmonicDatabase.ptNameByCipher[Live.key] ?: Live.key

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = contentPadding.calculateStartPadding(LayoutDirection.Ltr) + 16.dp,
            end = contentPadding.calculateEndPadding(LayoutDirection.Ltr) + 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ScreenHeader("Biblioteca", subtitle = "Tons, acordes e ferramentas") }
        item { SearchField(query) { query = it } }

        if (query.isNotBlank()) {
            item {
                if (hits.isEmpty()) {
                    Text(
                        "Nada encontrado para \"${query.trim()}\". Tente um tom (Sol, Am), um acorde (D7, Bm7) ou uma ferramenta (metrônomo).",
                        style = MaterialTheme.typography.bodyMedium, color = TextBody,
                    )
                } else {
                    RowGroup(rows = hits.take(16).map { hit ->
                        @Composable {
                            when (hit) {
                                is SearchHit.Key -> NavRow(
                                    "${hit.name} (${hit.cipher})",
                                    subtitle = "Tocar neste tom",
                                    icon = R.drawable.ic_play_circle,
                                ) { onPlayKey(hit.cipher) }
                                is SearchHit.Chord -> NavRow(
                                    "${hit.chord.cipher}: ${hit.name}",
                                    subtitle = "Ver as formas no violão",
                                    icon = R.drawable.ic_book,
                                ) { onOpenChord(hit.chord) }
                                is SearchHit.ToolHit -> NavRow(hit.tool.label, subtitle = hit.tool.subtitle, icon = hit.tool.icon) { onOpenTool(hit.tool) }
                            }
                        }
                    })
                }
            }
            return@LazyColumn
        }

        // Ferramentas que abrem no tom do momento.
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("No tom de $keyName")
                Text("Abrem já no tom que está na aba Tocar.", style = MaterialTheme.typography.bodyMedium, color = TextBody)
            }
        }
        item {
            RowGroup(rows = listOf(Tool.PROGRESSOES, Tool.PASSAGENS, Tool.ACORDES, Tool.HARMONIZAR).map { t ->
                @Composable { NavRow(t.label, subtitle = t.subtitle, icon = t.icon) { onOpenTool(t) } }
            })
        }

        item { SectionTitle("Violão") }
        item {
            RowGroup(rows = listOf(Tool.BRACO, Tool.DICIONARIO, Tool.DECORAR).map { t ->
                @Composable { NavRow(t.label, subtitle = t.subtitle, icon = t.icon) { onOpenTool(t) } }
            })
        }

        item { SectionTitle("Na hora de tocar") }
        item {
            val rows = listOf(Tool.DETECTAR, Tool.METRONOMO, Tool.CAPO).map { t ->
                @Composable { NavRow(t.label, subtitle = t.subtitle, icon = t.icon) { onOpenTool(t) } }
            }.toMutableList()
            if (com.example.BuildConfig.DEV_TOOLS) rows += @Composable {
                NavRow("Treino real (versão DEV)", subtitle = "Trechos captados, rótulos e envio do .zip", onClick = onOpenDevPanel)
            }
            RowGroup(rows = rows)
        }

        // Os 24 tons.
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Os 24 tons")
                Text("Toque num tom para tocar nele.", style = MaterialTheme.typography.bodyMedium, color = TextBody)
            }
        }
        if (favorites.isNotEmpty()) {
            item {
                Column {
                    SectionLabel("Favoritos")
                    Spacer(Modifier.height(10.dp))
                    KeyGrid((majorOrder + minorOrder).filter { it in favorites }, favorites, onPlayKey)
                }
            }
        }
        item {
            Column {
                SectionLabel("Tons maiores")
                Spacer(Modifier.height(10.dp))
                KeyGrid(majorOrder, favorites, onPlayKey)
            }
        }
        item {
            Column {
                SectionLabel("Tons menores")
                Spacer(Modifier.height(10.dp))
                KeyGrid(minorOrder, favorites, onPlayKey)
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    val focus = LocalFocusManager.current
    OutlinedTextField(
        value = query,
        onValueChange = { onChange(it.take(40)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text("Buscar tom, acorde ou ferramenta", color = TextMuted) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null, tint = TextBody, modifier = Modifier.size(20.dp)) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) {
                    Icon(painterResource(R.drawable.ic_close), contentDescription = "Limpar a busca", tint = TextBody, modifier = Modifier.size(20.dp))
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Selection, unfocusedBorderColor = HairlineStrong,
            focusedContainerColor = Surface1, unfocusedContainerColor = Surface1,
            cursorColor = TextStrong, focusedTextColor = TextStrong, unfocusedTextColor = TextStrong,
        ),
    )
}

/** Grade de tons (4 por linha). O tom do momento aparece escolhido. */
@Composable
internal fun KeyGrid(ciphers: List<String>, favorites: Set<String>, onPick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ciphers.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { cipher ->
                    KeyTile(cipher, cipher in favorites, cipher == Live.key, Modifier.weight(1f)) { onPick(cipher) }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun KeyTile(cipher: String, isFavorite: Boolean, isCurrent: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val name = HarmonicDatabase.ptNameByCipher[cipher] ?: cipher
    val bg by animateColorAsState(if (isCurrent) Selection else Surface1, tween(Motion.ms(180)), label = "tom")
    Box(
        modifier
            .pressClick(role = Role.Button, label = "Tocar em $name", onClick = onClick)
            .height(68.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, if (isCurrent) Selection else Hairline, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(cipher, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (isCurrent) Ink else TextStrong, maxLines = 1)
            Text(name, style = MaterialTheme.typography.labelSmall, color = if (isCurrent) Ink else TextBody, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (isFavorite) {
            Icon(
                painterResource(R.drawable.ic_star_fill),
                contentDescription = "Favorito",
                tint = if (isCurrent) Ink else Favorite,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(12.dp),
            )
        }
    }
}
