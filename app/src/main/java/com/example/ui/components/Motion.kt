package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import com.example.settings.AppSettings

/*
 * Movimento do app (ver docs/DESIGN.md): curto, com propósito e sempre
 * respeitando o ajuste "Menos animação" (Ajustes > Aparência).
 */
object Motion {
    /** Duração em ms, encurtada quando o usuário pede menos animação. */
    fun ms(base: Int): Int = if (AppSettings.reduceMotion) base / 3 else base
    val calm: Boolean get() = AppSettings.reduceMotion
}

/**
 * Clique com um leve "afundar" (escala 0,96) enquanto o dedo está em cima, além
 * do efeito padrão do Android. Use no lugar de clickable nos controles.
 * Vai no começo da cadeia de modificadores para escalar o fundo junto.
 */
@Composable
fun Modifier.pressClick(
    role: Role = Role.Button,
    label: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed && !Motion.calm) 0.96f else 1f,
        spring(dampingRatio = 0.7f, stiffness = 900f),
        label = "toque",
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(source, LocalIndication.current, enabled = enabled, onClickLabel = label, role = role, onClick = onClick)
}

/** Só a escala do toque (para quem já tem o próprio clickable). */
@Composable
fun Modifier.pressScale(source: MutableInteractionSource): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && !Motion.calm) 0.96f else 1f, spring(0.7f, 900f), label = "toque")
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}

/**
 * Tela que entra deslizando da direita (com fade) e sai do mesmo jeito.
 * Guarda o último valor para a saída ainda mostrar o conteúdo certo.
 */
@Composable
fun <T : Any> SlideOverlay(value: T?, content: @Composable (T) -> Unit) {
    var last by remember { mutableStateOf(value) }
    if (value != null) last = value
    AnimatedVisibility(
        visible = value != null,
        enter = slideInHorizontally(tween(Motion.ms(320), easing = FastOutSlowInEasing)) { it / 5 } + fadeIn(tween(Motion.ms(220))),
        exit = slideOutHorizontally(tween(Motion.ms(240), easing = FastOutSlowInEasing)) { it / 5 } + fadeOut(tween(Motion.ms(200))),
    ) {
        last?.let { content(it) }
    }
}
