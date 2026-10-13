package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

// Space Grotesk — grotesca contemporânea com formas de dígitos marcantes,
// ideal para cifras e números romanos. Substitui a Roboto padrão para dar
// identidade própria ao app (um dos maiores fatores de "cara de template").
val SpaceGrotesk = FontFamily(
    Font(R.font.sg_400, FontWeight.Normal),
    Font(R.font.sg_500, FontWeight.Medium),
    Font(R.font.sg_600, FontWeight.SemiBold),
    Font(R.font.sg_700, FontWeight.Bold),
)

// Escala tipográfica completa (todos os estilos do Material 3 definidos, para
// nenhum texto cair na fonte padrão do sistema). Tamanhos pensados para leitura
// no culto: corpo de 14 sp, legenda mínima de 12 sp, títulos com tracking
// levemente negativo. Rótulos NÃO usam caixa alta espaçada.
private fun style(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
    fontFamily = SpaceGrotesk,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp,
)

val AppTypography = Typography(
    displayLarge = style(44, 48, FontWeight.Bold, -1.0),
    displayMedium = style(36, 40, FontWeight.Bold, -0.7),
    displaySmall = style(30, 34, FontWeight.Bold, -0.5),
    headlineLarge = style(26, 30, FontWeight.SemiBold, -0.4),
    headlineMedium = style(22, 28, FontWeight.SemiBold, -0.3),
    headlineSmall = style(20, 24, FontWeight.SemiBold, -0.2),
    titleLarge = style(18, 24, FontWeight.SemiBold),
    titleMedium = style(16, 22, FontWeight.Medium),
    titleSmall = style(14, 20, FontWeight.SemiBold),
    bodyLarge = style(16, 24, FontWeight.Normal),
    bodyMedium = style(14, 20, FontWeight.Normal),
    bodySmall = style(12, 16, FontWeight.Normal),
    labelLarge = style(14, 18, FontWeight.Medium),
    labelMedium = style(12, 16, FontWeight.Medium),
    labelSmall = style(12, 16, FontWeight.Medium, 0.1),
)
