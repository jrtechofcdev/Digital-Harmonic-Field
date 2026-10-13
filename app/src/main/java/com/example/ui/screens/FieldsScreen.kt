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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import com.example.ui.components.ScreenHeader
import com.example.ui.components.Segmented
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.HarmonicDatabase
import com.example.R
import com.example.ui.components.BrandMark
import com.example.ui.components.SectionLabel
import com.example.ui.theme.Brass
import com.example.ui.theme.Favorite
import com.example.ui.theme.Hairline
import com.example.ui.theme.Ink
import com.example.ui.theme.Surface1
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextStrong

private val majorOrder = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
private val minorOrder = majorOrder.map { it + "m" }

private val ptNameByCipher: Map<String, String> = HarmonicDatabase.ptNameByCipher

/**
 * Aba Campos: os 24 tons (Tons) e as sequências prontas (Progressões), num
 * controle segmentado. O cabeçalho rola junto com o conteúdo.
 */
@Composable
fun CamposScreen(
    favorites: Set<String>,
    onOpenKey: (String) -> Unit,
    onPlayFullScreen: (com.example.music.Progression, String) -> Unit,
    contentPadding: PaddingValues,
) {
    var segment by rememberSaveable { mutableIntStateOf(0) }
    val header: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ScreenHeader(
                "Campos",
                subtitle = if (segment == 0) "Toque num tom para ver os 7 acordes e a função de cada um"
                else "Sequências prontas para tocar no tom escolhido",
            )
            Segmented(listOf("Tons", "Progressões"), segment, { segment = it }, Modifier.fillMaxWidth())
        }
    }
    if (segment == 0) FieldsScreen(favorites, onOpenKey, contentPadding, header)
    else ProgressionsScreen(onOpenKey, onPlayFullScreen, contentPadding, header)
}

/** Os 24 tons em grade (maiores e menores), com os favoritos no topo. */
@Composable
fun FieldsScreen(
    favorites: Set<String>,
    onOpenKey: (String) -> Unit,
    contentPadding: PaddingValues,
    header: @Composable () -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { header() }

        if (favorites.isNotEmpty()) {
            item {
                Column {
                    SectionLabel("Favoritos")
                    Spacer(Modifier.height(10.dp))
                    KeyGrid(
                        ciphers = (majorOrder + minorOrder).filter { favorites.contains(it) },
                        favorites = favorites,
                        onOpenKey = onOpenKey,
                    )
                }
            }
        }

        item {
            Column {
                SectionLabel("Tons maiores")
                Spacer(Modifier.height(10.dp))
                KeyGrid(ciphers = majorOrder, favorites = favorites, onOpenKey = onOpenKey)
            }
        }

        item {
            Column {
                SectionLabel("Tons menores")
                Spacer(Modifier.height(10.dp))
                KeyGrid(ciphers = minorOrder, favorites = favorites, onOpenKey = onOpenKey)
            }
        }
    }
}

@Composable
private fun KeyGrid(
    ciphers: List<String>,
    favorites: Set<String>,
    onOpenKey: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ciphers.chunked(4).forEach { rowCiphers ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowCiphers.forEach { cipher ->
                    KeyTile(
                        cipher = cipher,
                        namePt = ptNameByCipher[cipher] ?: cipher,
                        isFavorite = favorites.contains(cipher),
                        onClick = { onOpenKey(cipher) },
                        modifier = Modifier.weight(1f),
                    )
                }
                // Preenche colunas faltantes para manter o alinhamento.
                repeat(4 - rowCiphers.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun KeyTile(
    cipher: String,
    namePt: String,
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(68.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Surface1)
            .border(1.dp, Hairline, RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClickLabel = "Abrir o campo de $namePt") { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = cipher,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextStrong,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = namePt,
                style = MaterialTheme.typography.labelSmall,
                color = TextBody,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isFavorite) {
            Icon(
                painterResource(R.drawable.ic_star_fill),
                contentDescription = "Favorito",
                tint = Favorite,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(12.dp),
            )
        }
    }
}
