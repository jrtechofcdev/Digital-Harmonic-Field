package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import com.example.music.Guitar
import com.example.ui.theme.Hairline
import com.example.ui.theme.HairlineStrong
import com.example.ui.theme.Ink
import com.example.ui.theme.Surface3
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextStrong
import kotlin.math.min

/**
 * Desenho de acorde (como nas revistinhas de cifra): cordas na vertical (6ª à
 * esquerda), casas na horizontal, bolinhas onde apertar, "×" = não tocar a
 * corda, "○" = corda solta. Pestana desenhada como uma barra.
 */
@Composable
fun ChordDiagram(shape: Guitar.Shape, color: Color, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    Canvas(modifier.fillMaxWidth().aspectRatio(0.82f)) {
        val frets = 5
        val top = size.height * 0.16f
        val left = size.width * 0.18f
        val right = size.width * 0.92f
        val bottom = size.height * 0.96f
        val dx = (right - left) / (Guitar.STRINGS - 1)
        val dy = (bottom - top) / frets
        val base = shape.baseFret
        val line = min(size.width, size.height) * 0.012f

        // Casas e cordas.
        for (f in 0..frets) {
            val y = top + f * dy
            val w = if (f == 0 && base == 1) line * 5 else line
            drawLine(if (f == 0 && base == 1) TextStrong else HairlineStrong, Offset(left, y), Offset(right, y), w)
        }
        for (s in 0 until Guitar.STRINGS) {
            val x = left + s * dx
            drawLine(HairlineStrong, Offset(x, top), Offset(x, bottom), line)
        }
        if (base > 1) {
            text(measurer, "${base}ª", Offset(left * 0.42f, top + dy / 2), TextMuted, (dy * 0.32f).toSp())
        }
        // Pestana.
        shape.barre?.let { b ->
            val played = shape.frets.indices.filter { shape.frets[it] >= 0 }
            val y = top + (b - base + 0.5f) * dy
            val x0 = left + played.first() * dx
            val x1 = left + played.last() * dx
            drawRoundRect(color, Offset(x0 - dx * 0.28f, y - dy * 0.22f), Size(x1 - x0 + dx * 0.56f, dy * 0.44f), CornerRadius(dy * 0.22f))
        }
        // Dedos, cordas soltas e abafadas.
        val r = min(dx, dy) * 0.32f
        for (s in 0 until Guitar.STRINGS) {
            val x = left + s * dx
            when (val fr = shape.frets[s]) {
                -1 -> text(measurer, "×", Offset(x, top * 0.5f), TextMuted, (top * 0.55f).toSp())
                0 -> drawCircle(TextBody, r * 0.7f, Offset(x, top * 0.5f), style = Stroke(line * 2))
                else -> if (fr != shape.barre) {
                    drawCircle(color, r, Offset(x, top + (fr - base + 0.5f) * dy))
                }
            }
        }
    }
}

/**
 * Mapa do braço (casas 0 a 12): cada nota da escala do tom aparece com o NÚMERO
 * do grau (ou o nome da nota). As notas do acorde escolhido ficam cheias, na cor
 * da função dele; as outras notas da escala, discretas. 1ª corda em cima.
 */
@Composable
fun Fretboard(
    scaleLabels: Map<Int, String>,   // classe de altura → rótulo (grau ou nota)
    chordTones: Set<Int>,
    chordRoot: Int,
    chordColor: Color,
    modifier: Modifier = Modifier,
    lastFret: Int = 12,
) {
    val measurer = rememberTextMeasurer()
    Canvas(modifier.fillMaxWidth().aspectRatio(2.6f)) {
        val strings = Guitar.STRINGS
        val left = size.width * 0.07f
        val right = size.width * 0.99f
        val top = size.height * 0.10f
        val bottom = size.height * 0.84f
        val fw = (right - left) / lastFret              // largura de cada casa
        val sy = (bottom - top) / (strings - 1)
        val line = size.height * 0.008f

        // Marcações de casa (3, 5, 7, 9, 12).
        for (m in listOf(3, 5, 7, 9, 12)) {
            if (m > lastFret) continue
            val x = left + (m - 0.5f) * fw
            text(measurer, "$m", Offset(x, size.height * 0.95f), TextMuted, (size.height * 0.07f).toSp())
        }
        // Casas.
        for (f in 0..lastFret) {
            val x = left + f * fw
            drawLine(if (f == 0) TextStrong else Hairline, Offset(x, top), Offset(x, bottom), if (f == 0) line * 4 else line)
        }
        // Cordas (1ª em cima).
        for (i in 0 until strings) {
            val y = top + i * sy
            drawLine(HairlineStrong, Offset(left, y), Offset(right, y), line * (1f + i * 0.35f))
        }
        // Notas.
        val r = min(fw, sy) * 0.42f
        for (i in 0 until strings) {
            val string = strings - 1 - i              // linha de cima = 1ª corda
            val y = top + i * sy
            for (f in 0..lastFret) {
                val pc = Guitar.pitchClassAt(string, f)
                val label = scaleLabels[pc] ?: continue
                val x = if (f == 0) left - fw * 0.42f else left + (f - 0.5f) * fw
                val inChord = pc in chordTones
                if (inChord) {
                    drawCircle(chordColor, r, Offset(x, y))
                    if (pc == chordRoot) drawCircle(TextStrong, r * 1.12f, Offset(x, y), style = Stroke(line * 2.4f))
                    text(measurer, label, Offset(x, y), Ink, (r * 0.95f).toSp(), bold = true)
                } else {
                    drawCircle(Surface3, r * 0.78f, Offset(x, y))
                    text(measurer, label, Offset(x, y), TextMuted, (r * 0.78f).toSp())
                }
            }
        }
    }
}

private fun DrawScope.text(
    measurer: TextMeasurer,
    value: String,
    center: Offset,
    color: Color,
    size: androidx.compose.ui.unit.TextUnit,
    bold: Boolean = false,
) {
    val layout = measurer.measure(
        value,
        TextStyle(color = color, fontSize = size, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal),
    )
    drawText(layout, topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f))
}
