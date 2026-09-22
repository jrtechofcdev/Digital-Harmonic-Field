package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.music.HarmonicFunction
import com.example.ui.theme.Brass
import com.example.ui.theme.FuncDominant
import com.example.ui.theme.FuncSubdominant
import com.example.ui.theme.FuncTonic
import com.example.ui.theme.Hairline
import com.example.ui.theme.Surface1
import com.example.ui.theme.TextMuted

/** Rótulo de seção discreto, em caixa alta e espaçado. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = TextMuted,
        modifier = modifier,
    )
}

/** Cartão padrão: superfície com hairline. Sem brilhos nem gradientes. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    borderColor: Color = Hairline,
    background: Color = Surface1,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(14.dp)),
    ) { content() }
}

/** Etiqueta pequena com ponto colorido — usada para a função tonal (T/SD/D). */
@Composable
fun FunctionTag(function: HarmonicFunction, modifier: Modifier = Modifier) {
    val color = function.color()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = function.label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

/** Barra vertical fina que sinaliza a função de um item de lista. */
@Composable
fun AccentBar(color: Color, height: Dp) {
    Box(
        modifier = Modifier
            .width(3.dp)
            .size(width = 3.dp, height = height)
            .clip(RoundedCornerShape(2.dp))
            .background(color)
    )
    Spacer(Modifier.width(12.dp))
}

/**
 * Marca do app: palheta de guitarra (identidade "guitarrista") com as três
 * barras das funções tonais dentro. Desenhada em Compose para casar com o ícone.
 */
@Composable
fun BrandMark(modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Canvas(modifier = modifier.size(size)) {
        val u = this.size.minDimension / 108f
        fun p(x: Float, y: Float) = Offset(x * u, y * u)

        // Contorno da palheta.
        val pick = Path().apply {
            moveTo(54f * u, 30f * u)
            cubicTo(63f * u, 30f * u, 72f * u, 34f * u, 72f * u, 46f * u)
            cubicTo(72f * u, 60f * u, 60f * u, 80f * u, 54f * u, 86f * u)
            cubicTo(48f * u, 80f * u, 36f * u, 60f * u, 36f * u, 46f * u)
            cubicTo(36f * u, 34f * u, 45f * u, 30f * u, 54f * u, 30f * u)
            close()
        }
        drawPath(pick, color = Surface1)
        drawPath(pick, color = Brass, style = Stroke(width = 3.5f * u))

        val barWidth = 5f * u
        drawLine(FuncTonic, p(48f, 64f), p(48f, 52f), strokeWidth = barWidth, cap = StrokeCap.Round)
        drawLine(FuncSubdominant, p(54f, 64f), p(54f, 44f), strokeWidth = barWidth, cap = StrokeCap.Round)
        drawLine(FuncDominant, p(60f, 64f), p(60f, 50f), strokeWidth = barWidth, cap = StrokeCap.Round)
    }
}
