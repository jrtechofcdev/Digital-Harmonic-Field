package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import com.example.music.Guitar
import com.example.ui.theme.FretWire
import com.example.ui.theme.Hairline
import com.example.ui.theme.HairlineStrong
import com.example.ui.theme.Ink
import com.example.ui.theme.NeckInlay
import com.example.ui.theme.NeckNut
import com.example.ui.theme.NeckWood
import com.example.ui.theme.NeckWoodEdge
import com.example.ui.theme.StringPlain
import com.example.ui.theme.StringWound
import com.example.ui.theme.Surface3
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextStrong
import kotlin.math.min
import kotlin.math.pow

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
            val x0 = left + (shape.barreFrom ?: played.first()) * dx
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
                else -> if (fr != shape.barre || s < (shape.barreFrom ?: 0)) {
                    drawCircle(color, r, Offset(x, top + (fr - base + 0.5f) * dy))
                }
            }
        }
    }
}

/** Como uma nota aparece no braço. */
enum class DotStyle { FILLED, RING, GHOST }

/**
 * Uma bolinha no braço: corda (0 = 6ª, Mi grave), casa (0 = solta), rótulo e cor.
 * [emphasis] desenha um contorno claro (usado na fundamental).
 */
data class NeckDot(
    val string: Int,
    val fret: Int,
    val label: String,
    val color: Color,
    val style: DotStyle = DotStyle.FILLED,
    val emphasis: Boolean = false,
)

/** Pestana desenhada no braço: casa e cordas (da mais grave à mais aguda). */
data class NeckBarre(val fret: Int, val fromString: Int, val toString: Int, val color: Color)

/**
 * Geometria do braço: as casas seguem a proporção de um violão de verdade
 * (cada casa é ~5,6% menor que a anterior), com a pestana do braço em cima e a
 * 6ª corda (Mi grave) à esquerda — como quando você olha o violão no colo.
 */
private class NeckGeometry(w: Float, h: Float, val firstFret: Int, val lastFret: Int) {
    val left = w * 0.10f                 // espaço para o número da casa
    val right = w * 0.985f
    val namesY = h * 0.016f
    val nutY = h * if (lastFret - firstFret <= 6) 0.15f else 0.10f   // pestana do braço (rastilho)
    val bottom = h * 0.995f
    val gap = (right - left) / Guitar.STRINGS
    private val openTop = namesY * 2.3f
    private fun pos(n: Int) = 1f - 2f.pow(-n / 12f)

    fun stringX(s: Int) = left + gap * (s + 0.5f)
    fun fretY(n: Int): Float {
        if (n < firstFret) return nutY
        val a = pos(firstFret - 1)
        return nutY + (pos(n) - a) / (pos(lastFret) - a) * (bottom - nutY)
    }

    /** Centro da bolinha de uma casa (0 = acima da pestana, corda solta). */
    fun dotY(f: Int) = if (f == 0) (openTop + nutY) / 2f else (fretY(f - 1) + fretY(f)) / 2f

    fun radius(f: Int): Float {
        val h = if (f == 0) nutY - openTop else fretY(f) - fretY(f - 1)
        return min(gap * 0.40f, h * 0.43f)
    }

    fun visible(f: Int) = f == 0 || f in firstFret..lastFret

    /** Onde o dedo tocou → (corda, casa), ou null fora do braço. */
    fun hit(o: Offset): Pair<Int, Int>? {
        if (o.x < left || o.x > right) return null
        val s = ((o.x - left) / gap).toInt().coerceIn(0, Guitar.STRINGS - 1)
        if (o.y < namesY) return null
        if (o.y < nutY) return s to 0
        val f = (firstFret..lastFret).firstOrNull { o.y <= fretY(it) } ?: return null
        return s to f
    }
}

/**
 * Braço de violão "de verdade", na vertical. Mostra as bolinhas que a tela
 * mandar ([dots]) e avisa onde o dedo tocou ([onTap]) — para escolher notas
 * direto no braço. [muted] = cordas que não devem soar (×).
 * [firstFret]..[lastFret] = trecho mostrado (um "zoom" quando firstFret > 1;
 * as cordas soltas continuam aparecendo em cima).
 */
@Composable
fun GuitarNeck(
    dots: List<NeckDot>,
    modifier: Modifier = Modifier,
    lastFret: Int = 12,
    firstFret: Int = 1,
    stringNames: List<String> = listOf("6ª", "5ª", "4ª", "3ª", "2ª", "1ª"),
    muted: Set<Int> = emptySet(),
    barre: NeckBarre? = null,
    onTap: ((string: Int, fret: Int) -> Unit)? = null,
) {
    val measurer = rememberTextMeasurer()
    val span = lastFret - firstFret
    val ratio = when {
        span <= 6 -> 0.92f
        lastFret <= 12 -> 0.56f
        else -> 0.50f
    }
    val tap = if (onTap == null) Modifier else Modifier.pointerInput(firstFret, lastFret, onTap) {
        detectTapGestures { o ->
            NeckGeometry(size.width.toFloat(), size.height.toFloat(), firstFret, lastFret).hit(o)?.let { (s, f) -> onTap(s, f) }
        }
    }
    Canvas(modifier.fillMaxWidth().aspectRatio(ratio).then(tap)) {
        val g = NeckGeometry(size.width, size.height, firstFret, lastFret)
        val unit = size.width / 360f

        // Nome das cordas soltas (6ª à esquerda).
        for (s in 0 until Guitar.STRINGS) {
            text(measurer, stringNames[s], Offset(g.stringX(s), g.namesY), TextMuted, (11 * unit).toSp())
        }
        // Escala de madeira.
        drawRect(NeckWood, Offset(g.left, g.nutY), Size(g.right - g.left, g.bottom - g.nutY))
        drawLine(NeckWoodEdge, Offset(g.left, g.nutY), Offset(g.left, g.bottom), 2 * unit)
        drawLine(NeckWoodEdge, Offset(g.right, g.nutY), Offset(g.right, g.bottom), 2 * unit)
        // Marcações (3, 5, 7, 9, 12 duplo, 15) e o número da casa ao lado.
        val mid = (g.left + g.right) / 2f
        for (m in listOf(3, 5, 7, 9, 12, 15)) {
            if (m !in firstFret..lastFret) continue
            val y = g.dotY(m)
            val r = g.radius(m) * 0.32f
            if (m == 12) {
                drawCircle(NeckInlay, r, Offset(g.stringX(1) + g.gap / 2, y))
                drawCircle(NeckInlay, r, Offset(g.stringX(3) + g.gap / 2, y))
            } else {
                drawCircle(NeckInlay, r, Offset(mid, y))
            }
            text(measurer, "$m", Offset(g.left * 0.45f, y), TextMuted, (11 * unit).toSp(), bold = true)
        }
        // Trastes e rastilho.
        for (n in firstFret..lastFret) {
            val y = g.fretY(n)
            drawLine(FretWire, Offset(g.left, y), Offset(g.right, y), 2.2f * unit)
        }
        if (firstFret <= 1) {
            drawRect(NeckNut, Offset(g.left - unit, g.nutY - 5 * unit), Size(g.right - g.left + 2 * unit, 6 * unit))
        } else {
            // Zoom: o braço continua para cima; mostra em que casa o trecho começa.
            drawLine(FretWire, Offset(g.left, g.nutY), Offset(g.right, g.nutY), 2.2f * unit)
            if (firstFret !in listOf(3, 5, 7, 9, 12, 15)) {
                text(measurer, "$firstFret", Offset(g.left * 0.45f, g.dotY(firstFret)), TextMuted, (11 * unit).toSp(), bold = true)
            }
        }
        // Cordas: grossas e encapadas no grave, finas e lisas no agudo.
        val widths = floatArrayOf(3.0f, 2.6f, 2.2f, 1.7f, 1.3f, 1.0f)
        for (s in 0 until Guitar.STRINGS) {
            val x = g.stringX(s)
            drawLine(if (s <= 3) StringWound else StringPlain, Offset(x, g.nutY - 5 * unit), Offset(x, g.bottom), widths[s] * unit)
        }
        // Pestana (dedo 1 deitado).
        barre?.let { b ->
            val y = g.dotY(b.fret)
            val r = g.radius(b.fret)
            val x0 = g.stringX(b.fromString)
            val x1 = g.stringX(b.toString)
            drawRoundRect(
                b.color.copy(alpha = 0.55f),
                Offset(x0 - r, y - r * 0.8f), Size(x1 - x0 + 2 * r, r * 1.6f), CornerRadius(r),
            )
        }
        // Cordas abafadas.
        for (s in muted) {
            text(measurer, "×", Offset(g.stringX(s), g.dotY(0)), TextBody, (16 * unit).toSp(), bold = true)
        }
        // Notas.
        for (d in dots) {
            if (!g.visible(d.fret)) continue
            val c = Offset(g.stringX(d.string), g.dotY(d.fret))
            val r = g.radius(d.fret)
            val fs = (r * if (d.label.length >= 3) 0.62f else 0.78f).toSp()
            when (d.style) {
                DotStyle.FILLED -> {
                    drawCircle(d.color, r, c)
                    text(measurer, d.label, c, Ink, fs, bold = true)
                }
                DotStyle.RING -> {
                    drawCircle(Ink, r, c)
                    drawCircle(d.color, r - unit, c, style = Stroke(2.4f * unit))
                    text(measurer, d.label, c, d.color, fs, bold = true)
                }
                DotStyle.GHOST -> {
                    drawCircle(Surface3.copy(alpha = 0.85f), r * 0.86f, c)
                    text(measurer, d.label, c, d.color, fs * 0.92f)
                }
            }
            if (d.emphasis) drawCircle(TextStrong, r + 1.5f * unit, c, style = Stroke(2.2f * unit))
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
