package com.example.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.HarmonicDatabase
import com.example.R
import com.example.audio.ChordPlayer
import com.example.data.Live
import com.example.music.ColorChord
import com.example.music.LabChord
import com.example.music.PassageStep
import com.example.music.PreachMoment
import com.example.music.PreachSequence
import com.example.music.Preaching
import com.example.music.rootPc
import com.example.ui.components.NotationSettings
import com.example.ui.components.NotationToggle
import com.example.ui.components.OptionChip
import com.example.ui.components.Panel
import com.example.ui.components.PrimaryButton
import com.example.ui.components.RoundIconButton
import com.example.ui.components.SecondaryButton
import com.example.ui.components.Segmented
import com.example.ui.theme.Hairline
import com.example.ui.theme.Surface1
import com.example.ui.theme.Surface2
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextStrong
import kotlin.math.ceil
import kotlin.math.min

/** Toca uma sequência devagar, como um fundo (cada acorde soa um bom tempo). */
private fun playSlow(player: ChordPlayer, steps: List<PassageStep>, seconds: Double = 1.9) {
    val notes = steps.mapNotNull { shapeOf(it.chord)?.midiNotes }
    if (notes.isNotEmpty()) player.play(notes, secondsEach = seconds, strum = 0.07)
}

/**
 * Aba Pregação: fundos musicais para a hora da Palavra. Sequências por
 * momento (pregação, oração, apelo, Ceia, voltar ao hino), todas no tom do
 * momento, com a sensação, o jeito de tocar, o som e a tela cheia com
 * "passar sozinho". Na segunda parte, os acordes de som bonito explicados.
 */
@Composable
fun PreachScreen(
    contentPadding: PaddingValues,
    favorites: Set<String>,
    onOpenStage: (String) -> Unit,
    onOpenDictionary: (LabChord) -> Unit,
    onDetectKey: () -> Unit,
) {
    val player = remember { ChordPlayer() }
    DisposableEffect(player) { onDispose { player.stop() } }
    val haptic = LocalHapticFeedback.current

    val key = Live.key
    val minor = key.endsWith("m")
    val tonic = rootPc(key) ?: 7
    var view by rememberSaveable { mutableIntStateOf(0) }
    var momentName by rememberSaveable { mutableStateOf(PreachMoment.PREGACAO.name) }
    val moment = PreachMoment.valueOf(momentName)
    var onlySimple by rememberSaveable { mutableStateOf(false) }
    var keySheet by rememberSaveable { mutableStateOf(false) }
    val sequences = remember(key, moment, onlySimple) {
        Preaching.forMoment(moment, tonic, minor).filter { !onlySimple || !it.colored }
    }
    val colors = remember(key) { Preaching.colors(tonic, minor) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = contentPadding.calculateStartPadding(LayoutDirection.Ltr) + 16.dp,
            end = contentPadding.calculateEndPadding(LayoutDirection.Ltr) + 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Pregação", style = MaterialTheme.typography.headlineMedium, color = TextStrong)
                    Text("Fundos para a pregação, a oração e o apelo", style = MaterialTheme.typography.bodyMedium, color = TextBody)
                }
                NotationToggle()
            }
        }

        // O tom do momento (o mesmo da aba Tocar).
        item {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(role = Role.Button, onClickLabel = "Trocar o tom") { keySheet = true },
                    ) {
                        Text("No tom de", style = MaterialTheme.typography.labelLarge, color = TextMuted)
                        Text(
                            HarmonicDatabase.ptNameByCipher[key] ?: key,
                            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextStrong, maxLines = 1,
                        )
                        Text("Cifra $key, toque para trocar", style = MaterialTheme.typography.bodySmall, color = TextBody)
                    }
                    RoundIconButton(R.drawable.ic_minus, "Meio tom abaixo") { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); Live.transpose(-1) }
                    Spacer(Modifier.width(8.dp))
                    RoundIconButton(R.drawable.ic_plus, "Meio tom acima") { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); Live.transpose(+1) }
                }
            }
        }

        item { Segmented(listOf("Sequências", "Acordes bonitos"), view, { view = it }, Modifier.fillMaxWidth()) }

        if (view == 0) {
            // Momento do culto.
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PreachMoment.entries.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { m ->
                                OptionChip(m.title, m == moment, Modifier.weight(if (m.title.length > 10) 1.4f else 1f)) { momentName = m.name }
                            }
                        }
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(moment.hint, style = MaterialTheme.typography.bodyMedium, color = TextBody)
                    OptionChip("Só acordes simples", onlySimple, Modifier.fillMaxWidth()) { onlySimple = !onlySimple }
                }
            }
            if (sequences.isEmpty()) {
                item {
                    Text(
                        "Neste momento, todas as sequências usam alguma cor. Desligue \"Só acordes simples\" para ver.",
                        style = MaterialTheme.typography.bodyMedium, color = TextBody,
                    )
                }
            }
            items(sequences, key = { it.id }) { seq ->
                SequenceCard(seq, player, onStage = { onOpenStage(seq.id) }, onOpenDictionary = onOpenDictionary)
            }
        } else {
            item {
                Text(
                    "Acordes simples com uma nota a mais (ou trocada) que deixam o fundo mais bonito. " +
                        "Toque em \"Ouvir a diferença\": primeiro soa o acorde simples, depois o bonito. " +
                        "Toque num desenho para ver todas as formas no Dicionário.",
                    style = MaterialTheme.typography.bodyMedium, color = TextBody,
                )
            }
            items(colors, key = { it.title }) { c -> ColorChordCard(c, player, onOpenDictionary) }
        }
    }

    if (keySheet) KeySheet(favorites, onDetectKey = { keySheet = false; onDetectKey() }) { keySheet = false }
}

/** Selo pequeno ("Simples" / "Com cor"). */
@Composable
private fun Tag(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Surface2)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) { Text(text, style = MaterialTheme.typography.labelSmall, color = TextBody) }
}

/** "Como tocar: ..." com o rótulo em destaque. */
@Composable
private fun LabeledText(label: String, text: String) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = TextStrong)) { append("$label ") }
            append(text)
        },
        style = MaterialTheme.typography.bodySmall, color = TextBody,
    )
}

@Composable
private fun SequenceCard(seq: PreachSequence, player: ChordPlayer, onStage: () -> Unit, onOpenDictionary: (LabChord) -> Unit) {
    var showShapes by rememberSaveable(seq.id) { mutableStateOf(false) }
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(seq.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextStrong, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Tag(if (seq.colored) "Com cor" else "Simples")
        }
        Spacer(Modifier.height(10.dp))
        StepRow(seq.steps)
        Spacer(Modifier.height(10.dp))
        Text(seq.feel, style = MaterialTheme.typography.bodyMedium, color = TextBody)
        if (seq.outsideCount > 0) {
            Text(
                if (seq.outsideCount == 1) "Usa 1 acorde de fora do campo (marcado como \"fora\")." else "Usa ${seq.outsideCount} acordes de fora do campo.",
                style = MaterialTheme.typography.labelSmall, color = TextMuted,
            )
        }
        Spacer(Modifier.height(6.dp))
        LabeledText("Como tocar:", seq.howTo)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SecondaryButton("Tela cheia", Modifier.weight(1.3f), icon = R.drawable.ic_play_circle, onClick = onStage)
            SecondaryButton("Ouvir", Modifier.weight(1f), icon = R.drawable.ic_play) { playSlow(player, seq.steps) }
            OptionChip("Formas", showShapes, Modifier.weight(0.9f)) { showShapes = !showShapes }
        }
        if (showShapes) {
            Spacer(Modifier.height(8.dp))
            StepShapes(seq.steps, onOpenDictionary)
        }
    }
}

@Composable
private fun ColorChordCard(c: ColorChord, player: ChordPlayer, onOpenDictionary: (LabChord) -> Unit) {
    Panel {
        Text(c.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextStrong)
        Spacer(Modifier.height(8.dp))
        StepShapes(listOf(c.plain, c.colored), onOpenDictionary, hint = false)
        Spacer(Modifier.height(4.dp))
        Text(c.sound, style = MaterialTheme.typography.bodyMedium, color = TextBody)
        Spacer(Modifier.height(6.dp))
        LabeledText("Como montar:", c.how)
        Spacer(Modifier.height(4.dp))
        LabeledText("Onde usar:", c.where)
        Spacer(Modifier.height(10.dp))
        SecondaryButton("Ouvir a diferença (${c.plain.chord.cipher} e ${c.colored.chord.cipher})", Modifier.fillMaxWidth(), icon = R.drawable.ic_play) {
            playSlow(player, listOf(c.plain, c.colored), seconds = 2.2)
        }
    }
}

// ---------------------------------------------------------------------------
// Tela cheia: acordes grandes e "passar sozinho"
// ---------------------------------------------------------------------------

private val PACES = listOf(0, 8, 6, 4)   // segundos por acorde (0 = eu passo)

/**
 * Uma sequência de fundo em tela cheia: acordes grandes, o da vez destacado.
 * "Passar sozinho" troca de acorde a cada 8, 6 ou 4 segundos, para o músico
 * tocar olhando a tela sem tirar a mão do instrumento. A tela fica acesa.
 */
@Composable
fun PreachStageScreen(id: String, onBack: () -> Unit, contentPadding: PaddingValues) {
    val key = Live.key
    val minor = key.endsWith("m")
    val tonic = rootPc(key) ?: 7
    val seq = remember(id, key) { Preaching.byId(id, tonic, minor) }
    var step by rememberSaveable(id) { mutableIntStateOf(0) }
    var pace by rememberSaveable { mutableIntStateOf(0) }
    val progress = remember { Animatable(0f) }

    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = com.example.settings.AppSettings.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    if (seq == null) {
        // O tom mudou de maior para menor (ou vice-versa): a sequência não existe aqui.
        Column(Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding() + 12.dp, start = 16.dp, end = 16.dp)) {
            RoundIconButton(R.drawable.ic_back, "Voltar", onClick = onBack)
            Spacer(Modifier.height(12.dp))
            Text("Esta sequência é de outro tipo de tom. Volte e escolha outra.", style = MaterialTheme.typography.bodyLarge, color = TextBody)
        }
        return
    }
    val n = seq.steps.size.coerceAtLeast(1)
    val seconds = PACES[pace]
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Passar sozinho: a barra enche e o acorde troca. O tempo é real (não encurta com "Menos animação").
    LaunchedEffect(pace, step, n) {
        progress.snapTo(0f)
        if (seconds > 0) {
            progress.animateTo(1f, tween(seconds * 1000, easing = LinearEasing))
            step = (step + 1) % n
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(
                start = contentPadding.calculateStartPadding(LayoutDirection.Ltr) + 12.dp,
                end = contentPadding.calculateEndPadding(LayoutDirection.Ltr) + 12.dp,
                top = contentPadding.calculateTopPadding() + 6.dp,
                bottom = contentPadding.calculateBottomPadding() + 10.dp,
            ),
    ) {
        val title: @Composable (Modifier) -> Unit = { m ->
            Column(m) {
                Text(seq.name, style = MaterialTheme.typography.titleMedium, color = TextStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    seq.steps.joinToString(" - ") { if (NotationSettings.byDegree) it.label else it.chord.cipher },
                    style = MaterialTheme.typography.labelMedium, color = TextBody, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        val keyControls: @Composable () -> Unit = {
            NotationToggle()
            Spacer(Modifier.width(8.dp))
            RoundIconButton(R.drawable.ic_minus, "Meio tom abaixo") { Live.transpose(-1) }
            Text(
                key,
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextStrong,
                modifier = Modifier.padding(horizontal = 8.dp), maxLines = 1,
            )
            RoundIconButton(R.drawable.ic_plus, "Meio tom acima") { Live.transpose(+1) }
        }
        if (landscape) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(R.drawable.ic_back, "Voltar", onClick = onBack)
                Spacer(Modifier.width(10.dp))
                title(Modifier.weight(1f))
                keyControls()
            }
        } else {
            // Em pé: nome inteiro em cima, controles do tom embaixo.
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(R.drawable.ic_back, "Voltar", onClick = onBack)
                Spacer(Modifier.width(10.dp))
                title(Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tom", style = MaterialTheme.typography.labelLarge, color = TextMuted, modifier = Modifier.weight(1f))
                keyControls()
            }
        }

        Spacer(Modifier.height(10.dp))

        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val wide = maxWidth > maxHeight
            val cols = when {
                wide -> if (n <= 4) n else 4
                n <= 2 -> 1
                else -> 2
            }
            val rows = ceil(n / cols.toDouble()).toInt()
            val gap = 8.dp
            val cellW = (maxWidth - gap * (cols - 1)) / cols
            val cellH = (maxHeight - gap * (rows - 1)) / rows
            // Cifras curtas (G, Em) ficam bem grandes; as longas (Dsus4) cabem na largura.
            val longest = seq.steps.maxOf { (if (NotationSettings.byDegree) it.label else it.chord.cipher).length }.coerceAtLeast(2)
            val size = with(LocalDensity.current) { min(cellH.toPx() * 0.36f, cellW.toPx() * 0.95f / longest).toSp() }
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                seq.steps.withIndex().chunked(cols).forEach { row ->
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        row.forEach { (i, st) ->
                            val color = st.function.color()
                            val current = i == step
                            val main = if (NotationSettings.byDegree) st.label else st.chord.cipher
                            val secondary = if (NotationSettings.byDegree) st.chord.cipher else st.label
                            Column(
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (current) color.copy(alpha = 0.16f) else Surface1)
                                    .border(if (current) 3.dp else 1.dp, if (current) color else Hairline, RoundedCornerShape(16.dp))
                                    .clickable(onClickLabel = "Ir para ${st.chord.cipher}") { step = i },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text("${i + 1}º · $secondary", style = MaterialTheme.typography.labelLarge, color = TextMuted, maxLines = 1)
                                Text(
                                    main, fontSize = size, fontWeight = FontWeight.Bold,
                                    color = if (current) color else color.copy(alpha = 0.8f),
                                    maxLines = 1, softWrap = false,
                                )
                                Text(st.function.label, style = MaterialTheme.typography.labelLarge, color = color)
                            }
                        }
                        repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        // Barra do tempo até o próximo acorde (só com "passar sozinho").
        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Surface2)) {
            if (seconds > 0) {
                Box(
                    Modifier
                        .fillMaxWidth(progress.value)
                        .fillMaxHeight()
                        .background(seq.steps[step.coerceIn(0, n - 1)].function.color()),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        val paceControl: @Composable (Modifier) -> Unit = { m ->
            Segmented(listOf("Eu passo", "8 s", "6 s", "4 s"), pace, { pace = it }, m)
        }
        if (landscape) {
            // Deitado: tudo numa linha, para os acordes ficarem grandes.
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                paceControl(Modifier.weight(1.6f))
                SecondaryButton("Anterior", Modifier.weight(0.9f).heightIn(min = 52.dp), icon = R.drawable.ic_chevron_left) {
                    step = (step - 1 + n) % n
                }
                PrimaryButton("Próximo", Modifier.weight(1.2f)) { step = (step + 1) % n }
            }
        } else {
            Text("Passar sozinho", style = MaterialTheme.typography.labelLarge, color = TextMuted)
            Spacer(Modifier.height(4.dp))
            paceControl(Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("Anterior", Modifier.weight(1f).heightIn(min = 56.dp), icon = R.drawable.ic_chevron_left) {
                    step = (step - 1 + n) % n
                }
                PrimaryButton("Próximo", Modifier.weight(2f).heightIn(min = 56.dp)) { step = (step + 1) % n }
            }
        }
    }
}
