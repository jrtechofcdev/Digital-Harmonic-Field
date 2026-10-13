package com.example.ui.screens

import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.widget.VideoView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SweepGradientShader
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.AppInfo
import com.example.R
import com.example.settings.AppSettings
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean

/** Preto do logo e do começo do vídeo (o mesmo de res/values/colors.xml). */
private val OpeningBlack = Color(0xFF020303)
/** Fundo dos logos da JR TECH OFC. */
private val JrTechDark = Color(0xFF292929)

/**
 * Vídeo de abertura (res/raw/abertura.mp4), com som se o ajuste permitir.
 * Preenche a tela em retrato (corta as sobras) e cabe inteiro em paisagem.
 * "Pular" no canto, e se o app for para segundo plano a abertura acaba.
 */
@Composable
fun IntroVideo(onDone: () -> Unit) {
    val sound = AppSettings.introSound
    val finished = remember { AtomicBoolean(false) }
    val finish = { if (finished.compareAndSet(false, true)) onDone() }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) finish() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(OpeningBlack).clipToBounds()) {
        val ratio = 720f / 1280f                                // o vídeo é 9:16
        val portrait = maxHeight >= maxWidth
        val h = if (portrait) maxOf(maxHeight, maxWidth / ratio) else maxHeight
        val w = h * ratio
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    if (!sound && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE)
                    setVideoURI(Uri.parse("android.resource://${ctx.packageName}/${R.raw.abertura}"))
                    setOnPreparedListener { mp -> if (!sound) mp.setVolume(0f, 0f); start() }
                    setOnCompletionListener { finish() }
                    setOnErrorListener { _, _, _ -> finish(); true }
                }
            },
            onRelease = { it.stopPlayback() },
            modifier = Modifier.requiredSize(w, h).align(Alignment.Center),
        )
        val insets = WindowInsets.safeDrawing.asPaddingValues()
        GlassPill(
            "Pular",
            Modifier.align(Alignment.TopEnd).padding(top = insets.calculateTopPadding() + 8.dp, end = 12.dp),
        ) { finish() }
    }
}

/**
 * Tela da JR TECH OFC, logo depois do vídeo e na mesma estética do final dele:
 * fundo azul e bege (o último quadro, desfocado), ondas saindo do centro e o
 * logo numa placa de vidro escura com a borda iridescente da palheta.
 * Some sozinha em ~3 s (ou com um toque). No rodapé: direitos e "Sobre".
 */
@Composable
fun BrandScreen(onDone: () -> Unit, onAbout: () -> Unit, durationMs: Long = 3200) {
    val calm = AppSettings.reduceMotion
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        shown = true
        delay(durationMs)
        onDone()
    }
    val appear by animateFloatAsState(if (shown) 1f else 0f, tween(if (calm) 250 else 800, easing = FastOutSlowInEasing), label = "aparecer")
    val loop = rememberInfiniteTransition(label = "marca")
    val ripple by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(2800, easing = LinearEasing)), label = "ondas")
    val rim by loop.animateFloat(0f, 360f, infiniteRepeatable(tween(5200, easing = LinearEasing)), label = "borda")
    val shine = remember { Animatable(-0.6f) }
    LaunchedEffect(Unit) {
        if (!calm) {
            delay(450)
            shine.animateTo(1.6f, tween(1200, easing = FastOutSlowInEasing))
        }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF5E77A6))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDone() },
    ) {
        Image(
            painterResource(R.drawable.abertura_fundo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        val plaqueW = minOf(maxWidth * 0.64f, 280.dp)
        val center = Offset(0.5f, 0.42f)

        // Ondas de vidro saindo do centro, como na abertura.
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width * center.x, size.height * center.y)
            val minR = plaqueW.toPx() * 0.55f
            val maxR = size.maxDimension * 0.75f
            for (i in 0 until 3) {
                val phase = if (calm) i / 3f else (ripple + i / 3f) % 1f
                val r = minR + (maxR - minR) * phase
                drawCircle(Color.White.copy(alpha = (1f - phase) * 0.32f * appear), radius = r, center = c, style = Stroke(width = 2.dp.toPx()))
            }
            // Escurece um pouco embaixo para o texto do rodapé ficar legível.
            drawRect(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color(0x8C0B1222)))
        }

        // A placa com o logo.
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = maxHeight * center.y - plaqueW * (774f / 760f) / 2)
                .width(plaqueW)
                .aspectRatio(760f / 774f)
                .graphicsLayer {
                    alpha = appear
                    val s = 0.9f + 0.1f * appear
                    scaleX = s; scaleY = s
                },
        ) {
            val shape = RoundedCornerShape(32.dp)
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(JrTechDark)
                    .drawWithContent {
                        drawContent()
                        // Reflexo de vidro em cima.
                        drawRect(Brush.verticalGradient(0f to Color(0x24FFFFFF), 0.45f to Color.Transparent))
                        // Brilho que atravessa a placa (uma vez).
                        val x = size.width * shine.value
                        drawRect(
                            Brush.linearGradient(
                                0f to Color.Transparent, 0.5f to Color(0x30FFFFFF), 1f to Color.Transparent,
                                start = Offset(x - size.width * 0.35f, 0f), end = Offset(x + size.width * 0.05f, size.height),
                            )
                        )
                    },
            ) {
                Image(painterResource(R.drawable.jrtech_vertical), contentDescription = "Logo da JR TECH OFC", modifier = Modifier.fillMaxSize())
            }
            // Borda iridescente que gira devagar (a mesma da palheta da abertura).
            Canvas(Modifier.fillMaxSize()) {
                val shader = SweepGradientShader(
                    center = Offset(size.width / 2, size.height / 2),
                    colors = listOf(
                        Color(0xFF7FE7FF), Color(0xFF6C7BFF), Color(0xFFE38BFF), Color(0xFFFFB36B),
                        Color(0xFFFFF27A), Color(0xFF7DFFB2), Color(0xFF7FE7FF),
                    ),
                )
                shader.setLocalMatrix(android.graphics.Matrix().apply { setRotate(rim, size.width / 2, size.height / 2) })
                drawRoundRect(
                    brush = ShaderBrush(shader),
                    cornerRadius = CornerRadius(32.dp.toPx()),
                    style = Stroke(width = 2.5.dp.toPx()),
                    alpha = 0.9f,
                )
            }
        }

        // Rodapé: direitos do autor e o acesso ao Sobre.
        val insets = WindowInsets.safeDrawing.asPaddingValues()
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = insets.calculateBottomPadding() + 16.dp)
                .graphicsLayer { alpha = appear },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(AppInfo.copyright, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = Color.White, textAlign = TextAlign.Center)
            Text(
                "${AppInfo.AUTHOR_ROLE}. Desenvolvedor do ${AppInfo.APP_NAME}.",
                style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center,
            )
            Text(
                "Todos os direitos reservados. Proibida a cópia, modificação ou redistribuição sem autorização do autor.",
                style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.78f), textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            GlassPill("Sobre o app", onClick = onAbout)
        }
    }
}

/** Botão pequeno de vidro (área de toque de 44 dp, desenho de 30 dp). */
@Composable
private fun GlassPill(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .heightIn(min = 44.dp)
            .clickable(role = Role.Button, onClickLabel = label) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(Color(0x33000000))
                .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White)
        }
    }
}
