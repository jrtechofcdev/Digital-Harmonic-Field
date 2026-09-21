package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.Brass
import com.example.ui.theme.FuncSubdominant
import com.example.ui.theme.FuncTonic
import com.example.ui.theme.Hairline
import com.example.ui.theme.Ink
import com.example.ui.theme.Surface1
import com.example.ui.theme.Surface2
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextStrong
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Sequência de quintas (sentido horário), com os relativos menores no anel interno.
private val majorsCW = listOf("C", "G", "D", "A", "E", "B", "F#", "C#", "G#", "D#", "A#", "F")
private val minorsCW = listOf("Am", "Em", "Bm", "F#m", "C#m", "G#m", "D#m", "A#m", "Fm", "Cm", "Gm", "Dm")

@Composable
fun CircleScreen(
    onOpenKey: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp, end = 16.dp,
                top = contentPadding.calculateTopPadding() + 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
    ) {
        Text("Círculo das Quintas", style = MaterialTheme.typography.headlineMedium, color = TextStrong)
        Spacer(Modifier.height(6.dp))
        Text(
            "Tons vizinhos ficam lado a lado — quanto mais perto, mais acordes em comum. Toque em qualquer tom para abrir o campo dele.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextBody,
        )
        Spacer(Modifier.height(20.dp))

        CircleWheel(onOpenKey = onOpenKey)

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            LegendDot(FuncTonic, "Anel externo · tons maiores")
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            LegendDot(FuncSubdominant, "Anel interno · relativos menores")
        }
    }
}

@Composable
private fun CircleWheel(onOpenKey: (String) -> Unit) {
    val boxSize = 328f      // dp
    val center = boxSize / 2f
    val outerR = 122f
    val innerR = 78f
    val majorSize = 54f
    val minorSize = 42f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(boxSize.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.size(boxSize.dp)) {
            // Anéis-guia sutis.
            Canvas(modifier = Modifier.size(boxSize.dp)) {
                val c = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                drawCircle(color = Hairline, radius = (outerR + majorSize / 2f).dp.toPx().coerceAtMost(size.minDimension / 2f), center = c, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()))
                drawCircle(color = Hairline, radius = (innerR - minorSize / 2f).dp.toPx(), center = c, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()))
            }

            // Centro
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(66.dp)
                    .clip(CircleShape)
                    .background(Surface1)
                    .border(1.dp, Hairline, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("5as", style = MaterialTheme.typography.titleMedium, color = TextMuted)
            }

            majorsCW.forEachIndexed { i, cipher ->
                val angle = -PI / 2.0 + i * (PI / 6.0)
                val x = center + outerR * cos(angle).toFloat() - majorSize / 2f
                val y = center + outerR * sin(angle).toFloat() - majorSize / 2f
                NodeButton(
                    label = cipher,
                    sizeDp = majorSize,
                    xDp = x, yDp = y,
                    fill = Surface2,
                    borderColor = Hairline,
                    textColor = TextStrong,
                    onClick = { onOpenKey(cipher) },
                )
            }

            minorsCW.forEachIndexed { i, cipher ->
                val angle = -PI / 2.0 + i * (PI / 6.0)
                val x = center + innerR * cos(angle).toFloat() - minorSize / 2f
                val y = center + innerR * sin(angle).toFloat() - minorSize / 2f
                NodeButton(
                    label = cipher,
                    sizeDp = minorSize,
                    xDp = x, yDp = y,
                    fill = Ink,
                    borderColor = Hairline,
                    textColor = TextBody,
                    onClick = { onOpenKey(cipher) },
                )
            }
        }
    }
}

@Composable
private fun NodeButton(
    label: String,
    sizeDp: Float,
    xDp: Float,
    yDp: Float,
    fill: Color,
    borderColor: Color,
    textColor: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .offset(x = xDp.dp, y = yDp.dp)
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(fill)
            .border(1.dp, borderColor, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = textColor,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LegendDot(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
    }
}
