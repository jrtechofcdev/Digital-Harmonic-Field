package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.music.HarmonicFunction
import com.example.ui.theme.Hairline
import com.example.ui.theme.Surface1
import com.example.ui.theme.TextMuted

/**
 * Rótulo de um grupo de controles ("Tom", "Tipo de acorde"…). Frase normal, sem
 * caixa alta espaçada: quem lê é iniciante e o rótulo precisa ser lido, não enfeitar.
 */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
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

/** Etiqueta da função tonal (Tônica / Subdominante / Dominante), na cor da função. */
@Composable
fun FunctionTag(function: HarmonicFunction, modifier: Modifier = Modifier) {
    val color = function.color()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = function.label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

/**
 * Marca do app: a mesma arte do ícone (braço do violão com as notas da
 * Tônica, Subdominante e Dominante, dentro do anel em latão). Vem de
 * res/drawable-nodpi/brand_mark.png, gerado por tools/icon/make_icon.py.
 */
@Composable
fun BrandMark(modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Image(
        painter = painterResource(R.drawable.brand_mark),
        contentDescription = null,
        modifier = modifier.size(size),
    )
}
