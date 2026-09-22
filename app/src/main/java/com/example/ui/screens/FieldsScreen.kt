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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.HarmonicDatabase
import com.example.ui.components.BrandMark
import com.example.ui.components.SectionLabel
import com.example.ui.theme.Brass
import com.example.ui.theme.Favorite
import com.example.ui.theme.Hairline
import com.example.ui.theme.Surface1
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextStrong

private val majorOrder = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
private val minorOrder = majorOrder.map { it + "m" }

private val ptNameByCipher: Map<String, String> = HarmonicDatabase.ptNameByCipher

@Composable
fun FieldsScreen(
    favorites: Set<String>,
    onOpenKey: (String) -> Unit,
    onSupport: () -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { BrandHeader(onSupport = onSupport) }

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
private fun BrandHeader(onSupport: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandMark(size = 46.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Campo Harmônico",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextStrong,
                )
                Text(
                    text = "JR TECH · guitarrista e developer",
                    style = MaterialTheme.typography.labelSmall,
                    color = Brass,
                )
            }
            // Botão de apoio (palheta).
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Surface1)
                    .border(1.dp, Hairline, RoundedCornerShape(10.dp))
                    .clickable { onSupport() }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BrandMark(size = 18.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("Apoiar", style = MaterialTheme.typography.labelLarge, color = TextBody)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Escolha um tom para ver os sete graus, suas funções e a formação de cada acorde.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextBody,
        )
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
            .height(66.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Surface1)
            .border(
                1.dp,
                if (isFavorite) Favorite.copy(alpha = 0.55f) else Hairline,
                RoundedCornerShape(12.dp),
            )
            .clickable { onClick() },
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
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isFavorite) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = Favorite,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(11.dp),
            )
        }
    }
}
