package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ChordInfo
import com.example.ui.theme.Surface2
import com.example.ui.theme.TextMuted

/**
 * Os acordes de uma progressão, na ordem de tocar, sempre DENTRO da largura da
 * tela — nada de arrastar a linha para o lado. Progressões longas quebram em
 * mais linhas (até 5 acordes cabem numa linha; acima disso, 4 por linha).
 *
 * Cada acorde é pintado pela sua função tonal (Tônica/Subdominante/Dominante).
 */
@Composable
fun ChordStrip(
    degrees: List<Int>,
    chords: List<ChordInfo>,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    perRow: Int = if (degrees.size <= 5) degrees.size.coerceAtLeast(1) else 4,
    showFunction: Boolean = !compact,
) {
    val steps = degrees.mapNotNull { chords.getOrNull(it) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)) {
        steps.chunked(perRow).forEachIndexed { rowIndex, row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                row.forEachIndexed { i, chord ->
                    val color = chord.function.color()
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(if (compact) 8.dp else 10.dp))
                            .background(Surface2)
                            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(if (compact) 8.dp else 10.dp))
                            .padding(vertical = if (compact) 6.dp else 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (!compact) {
                            Text(
                                "${rowIndex * perRow + i + 1}º",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                            )
                        }
                        Text(
                            chord.cipher,
                            style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = color,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                        )
                        if (showFunction) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                chord.function.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = color,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                // Completa a última linha para os acordes manterem o mesmo tamanho.
                repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
