package com.example.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.example.settings.AppSettings

/**
 * Tema do app. As cores vêm da paleta atual (escura ou clara, ver Color.kt) e o
 * tamanho do texto segue o ajuste "Tamanho do texto" (multiplica o do celular).
 */
@Composable
fun HarmonicTheme(content: @Composable () -> Unit) {
    val p = Palette.current
    val scheme = if (p.isLight) lightColorScheme(
        primary = p.brass, onPrimary = p.ink,
        primaryContainer = p.surface2, onPrimaryContainer = p.textStrong,
        secondary = p.funcSubdominant, onSecondary = p.ink,
        tertiary = p.funcTonic,
        background = p.ink, onBackground = p.textStrong,
        surface = p.surface1, onSurface = p.textStrong,
        surfaceVariant = p.surface2, onSurfaceVariant = p.textBody,
        outline = p.hairline, outlineVariant = p.hairline,
        error = p.funcDominant,
    ) else darkColorScheme(
        primary = p.brass, onPrimary = p.ink,
        primaryContainer = p.surface2, onPrimaryContainer = p.textStrong,
        secondary = p.funcSubdominant, onSecondary = p.ink,
        tertiary = p.funcTonic,
        background = p.ink, onBackground = p.textStrong,
        surface = p.surface1, onSurface = p.textStrong,
        surfaceVariant = p.surface2, onSurfaceVariant = p.textBody,
        outline = p.hairline, outlineVariant = p.hairline,
        error = p.funcDominant,
    )
    val density = LocalDensity.current
    val scale = AppSettings.textSize.scale
    CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale * scale)) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
    }
}

/**
 * Fundo de tela inteira. No escuro: chapado (Ink). No claro: as texturas do
 * final da abertura, bem suaves para não atrapalhar a leitura: azul em cima à
 * esquerda, bege quente na diagonal oposta e círculos de vidro bem fracos.
 */
@Composable
fun AppBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    if (!IsLightTheme) {
        Box(modifier.fillMaxSize().background(Ink), content = content)
        return
    }
    Box(modifier.fillMaxSize().background(Ink)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(
                Brush.linearGradient(
                    0f to Color(0xFFE3EAF6),
                    0.55f to Color(0xFFF2F1EF),
                    1f to Color(0xFFF4EBDD),
                    start = Offset(0f, 0f), end = Offset(w, h),
                )
            )
            // Brilhos suaves: azul em cima à esquerda, bege em baixo à direita.
            drawCircle(Brush.radialGradient(listOf(Color(0x335B7FC0), Color.Transparent), center = Offset(w * 0.1f, h * 0.08f), radius = w * 0.9f),
                radius = w * 0.9f, center = Offset(w * 0.1f, h * 0.08f))
            drawCircle(Brush.radialGradient(listOf(Color(0x33D9B98E), Color.Transparent), center = Offset(w * 0.95f, h * 0.85f), radius = w),
                radius = w, center = Offset(w * 0.95f, h * 0.85f))
            // Círculos de vidro sobrepostos, como no final da abertura.
            val ring = Color(0x2EFFFFFF)
            listOf(Offset(w * 0.78f, h * 0.12f) to w * 0.55f, Offset(w * 0.2f, h * 0.3f) to w * 0.48f, Offset(w * 0.6f, h * 0.62f) to w * 0.62f)
                .forEach { (c, r) -> drawCircle(ring, radius = r, center = c, style = Stroke(width = 2f)) }
        }
        content()
    }
}
