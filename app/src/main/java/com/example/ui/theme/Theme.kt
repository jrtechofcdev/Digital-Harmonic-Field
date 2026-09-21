package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// App é dark por natureza (uso em palco/culto com pouca luz). Mantemos um único
// esquema escuro, consistente, em vez de depender de cores dinâmicas do sistema
// — que descaracterizariam a identidade.
private val AppDarkScheme = darkColorScheme(
    primary = Brass,
    onPrimary = Ink,
    primaryContainer = Surface2,
    onPrimaryContainer = TextStrong,
    secondary = FuncSubdominant,
    onSecondary = Ink,
    tertiary = FuncTonic,
    background = Ink,
    onBackground = TextStrong,
    surface = Surface1,
    onSurface = TextStrong,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextBody,
    outline = Hairline,
    outlineVariant = Hairline,
    error = FuncDominant,
)

@Composable
fun HarmonicTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = AppDarkScheme,
        typography = AppTypography,
        content = content,
    )
}
