package com.example.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.ChordInfo
import com.example.HarmonicDatabase
import com.example.HarmonicField
import com.example.R
import com.example.audio.ChordPlayer
import com.example.data.Live
import com.example.data.Repertoire
import com.example.music.Capo
import com.example.music.DominantFor
import com.example.music.Guitar
import com.example.music.LabChord
import com.example.music.Progression
import com.example.music.ProgressionLibrary
import com.example.music.SecondaryDominants
import com.example.music.getChordFormulaNotes
import com.example.music.parseCipher
import com.example.music.pitchClassCiphers
import com.example.music.transposePtNote
import com.example.ui.components.BrandMark
import com.example.ui.components.ChordDiagram
import com.example.ui.components.ChordStrip
import com.example.ui.components.FunctionTag
import com.example.ui.components.Motion
import com.example.ui.components.NotationToggle
import com.example.ui.components.OptionChip
import com.example.ui.components.Panel
import com.example.ui.components.PrimaryButton
import com.example.ui.components.RoundIconButton
import com.example.ui.components.SecondaryButton
import com.example.ui.components.SectionTitle
import com.example.ui.components.Segmented
import com.example.ui.components.chordLabels
import com.example.ui.components.pressClick
import com.example.ui.theme.Favorite
import com.example.ui.theme.Hairline
import com.example.ui.theme.Ink
import com.example.ui.theme.SheetSurface
import com.example.ui.theme.Surface1
import com.example.ui.theme.Surface2
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextStrong

/**
 * Aba Tocar: o painel do músico no culto. Tudo gira em volta do tom do momento
 * (Live): o repertório em andamento, o tom em destaque (transpor, trocar,
 * detectar, capotraste), os 7 acordes (um toque abre a forma no violão), as
 * ferramentas no mesmo tom e os caminhos comuns. A tela fica acesa.
 */
@Composable
fun PlayScreen(
    contentPadding: PaddingValues,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onDetectKey: () -> Unit,
    onOpenRepertoire: () -> Unit,
    onOpenTool: (Tool) -> Unit,
    onPlayFullScreen: (Progression, String) -> Unit,
    onOpenDictionary: (LabChord) -> Unit,
) {
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = com.example.settings.AppSettings.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }
    val player = remember { ChordPlayer() }
    DisposableEffect(player) { onDispose { player.stop() } }

    val key = Live.key
    val field = remember(key) { HarmonicDatabase.getField(key) } ?: return
    val dominants = remember(key) { SecondaryDominants.forField(field) }
    var selected by rememberSaveable(key) { mutableIntStateOf(0) }
    var sheetFor by remember { mutableStateOf<Int?>(null) }
    var keySheet by rememberSaveable { mutableStateOf(false) }

    val actions = PlayActions(
        onChangeKey = { keySheet = true },
        onDetectKey = onDetectKey,
        onOpenRepertoire = onOpenRepertoire,
        onOpenTool = onOpenTool,
        onPlayFullScreen = { onPlayFullScreen(it, key) },
        onChord = { selected = it; sheetFor = it },
        favorite = key in favorites,
        onToggleFavorite = { onToggleFavorite(key) },
    )

    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        PlayLandscape(field, dominants, selected, contentPadding, actions, player)
    } else {
        PlayPortrait(field, dominants, contentPadding, actions)
    }

    sheetFor?.let { i ->
        ChordSheet(field.chords[i], i, dominants.getOrNull(i), field.chords, player, onOpenDictionary = onOpenDictionary) { sheetFor = null }
    }
    if (keySheet) KeySheet(favorites, onDetectKey = { keySheet = false; onDetectKey() }) { keySheet = false }
}

/** O que a tela Tocar pode pedir para fora dela. */
private class PlayActions(
    val onChangeKey: () -> Unit,
    val onDetectKey: () -> Unit,
    val onOpenRepertoire: () -> Unit,
    val onOpenTool: (Tool) -> Unit,
    val onPlayFullScreen: (Progression) -> Unit,
    val onChord: (Int) -> Unit,
    val favorite: Boolean,
    val onToggleFavorite: () -> Unit,
)

// ---------------------------------------------------------------------------
// Retrato
// ---------------------------------------------------------------------------

@Composable
private fun PlayPortrait(
    field: HarmonicField,
    dominants: List<DominantFor?>,
    contentPadding: PaddingValues,
    a: PlayActions,
) {
    val showDominants = DominantSettings.show
    val context = LocalContext.current
    val scroll = rememberScrollState()
    // Trocou o hino, a lista ou o tom: volta ao topo (o hino e o tom ficam à vista).
    val now = "${Live.setlistId}/${Live.position}/${Live.key}"
    var lastSeen by rememberSaveable { mutableStateOf(now) }
    LaunchedEffect(now) {
        if (lastSeen != now) { lastSeen = now; scroll.animateScrollTo(0) }
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(
                start = 16.dp, end = 16.dp,
                top = contentPadding.calculateTopPadding() + 10.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Topo: marca, título e a chave Cifra | Grau (muda o app inteiro).
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandMark(size = 34.dp)
            Spacer(Modifier.width(10.dp))
            Text("Tocar", style = MaterialTheme.typography.headlineMedium, color = TextStrong, modifier = Modifier.weight(1f))
            NotationToggle()
        }

        SetlistBar(a.onOpenRepertoire)
        KeyHero(field, a)

        // Os 7 acordes.
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    SectionTitle("Os 7 acordes")
                    Text("Toque num acorde para ver a forma no violão", style = MaterialTheme.typography.bodySmall, color = TextBody)
                }
                OptionChip("Dominantes", showDominants, Modifier.width(124.dp)) { DominantSettings.set(context, !showDominants) }
            }
            ChordGrid(field, dominants, showDominants, a.onChord)
        }

        // Ferramentas no mesmo tom.
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("Neste tom")
            ToolGrid(a.onOpenTool)
        }

        // Caminhos comuns (toque para tocar em tela cheia).
        PathsPanel(field, a.onPlayFullScreen)
    }
}

/** Repertório em andamento: hino anterior / próximo, com o tom mudando junto. */
@Composable
private fun SetlistBar(onOpenRepertoire: () -> Unit) {
    val list = Live.setlist
    val haptic = LocalHapticFeedback.current
    if (list == null) {
        if (Repertoire.lists.any { it.items.isNotEmpty() }) {
            SecondaryButton("Seguir um repertório", Modifier.fillMaxWidth(), icon = R.drawable.ic_playlist, onClick = onOpenRepertoire)
        }
        return
    }
    val item = Live.currentItem
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(R.drawable.ic_chevron_left, "Hino anterior") {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); Live.previous()
            }
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button, onClickLabel = "Abrir o repertório") { onOpenRepertoire() }
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("${list.name}: hino ${Live.position + 1} de ${list.items.size}", style = MaterialTheme.typography.labelMedium, color = TextBody, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item?.heading ?: "", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextStrong, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                val extra = listOfNotNull(
                    item?.capo?.takeIf { it > 0 }?.let { "capo na ${it}ª casa" },
                    item?.bpm?.let { "$it BPM" },
                    item?.note?.takeIf { it.isNotBlank() },
                ).joinToString(", ")
                if (extra.isNotEmpty()) Text(extra, style = MaterialTheme.typography.bodySmall, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            RoundIconButton(R.drawable.ic_chevron, "Próximo hino") {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); Live.next()
            }
        }
        // Cantaram em outro tom: guardar no hino para o próximo culto já abrir certo.
        if (Live.differsFromItem) {
            Text(
                "Salvar ${Live.key} neste hino",
                style = MaterialTheme.typography.labelLarge, color = TextStrong,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button) { Live.saveKeyToItem() }
                    .padding(vertical = 12.dp),
            )
        }
    }
}

/** O tom em destaque e tudo o que muda o tom. */
@Composable
private fun KeyHero(field: HarmonicField, a: PlayActions) {
    val haptic = LocalHapticFeedback.current
    val moved = Live.transposedBy
    Panel {
        Row(verticalAlignment = Alignment.Top) {
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button, onClickLabel = "Trocar o tom") { a.onChangeKey() },
            ) {
                Text("Tom", style = MaterialTheme.typography.labelLarge, color = TextMuted)
                Text(field.keyNamePt, style = MaterialTheme.typography.displaySmall, color = TextStrong, maxLines = 1)
                Text(
                    when {
                        moved == 0 -> "Cifra ${field.keyCipher}"
                        else -> "Cifra ${field.keyCipher}, ${if (moved > 0) "+" else ""}$moved do original (${Live.originalKey})"
                    },
                    style = MaterialTheme.typography.bodyMedium, color = TextBody,
                )
            }
            FavoriteStar(a.favorite, a.onToggleFavorite)
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RoundIconButton(R.drawable.ic_minus, "Meio tom abaixo") { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); Live.transpose(-1) }
            SecondaryButton("Trocar tom", Modifier.weight(1f), icon = R.drawable.ic_music_notes, onClick = a.onChangeKey)
            RoundIconButton(R.drawable.ic_plus, "Meio tom acima") { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); Live.transpose(+1) }
        }
        if (moved != 0) {
            Text(
                "Voltar ao original (${Live.originalKey})",
                style = MaterialTheme.typography.labelLarge, color = TextStrong,
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .clickable(role = Role.Button) { Live.resetTranspose() }
                    .padding(vertical = 12.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        PrimaryButton("Detectar o tom", Modifier.fillMaxWidth(), icon = R.drawable.ic_mic, onClick = a.onDetectKey)
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable(role = Role.Button, onClickLabel = "Abrir o capotraste") { a.onOpenTool(Tool.CAPO) }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_capo), contentDescription = null, tint = TextBody, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(Capo.advice(field.keyCipher), style = MaterialTheme.typography.bodySmall, color = TextBody, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun FavoriteStar(isFavorite: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        Icon(
            painterResource(if (isFavorite) R.drawable.ic_star_fill else R.drawable.ic_star),
            contentDescription = if (isFavorite) "Tirar dos favoritos" else "Favoritar este tom",
            tint = if (isFavorite) Favorite else TextMuted,
            modifier = Modifier.size(26.dp),
        )
    }
}

/** Os 7 acordes em grade de 2 colunas (o 8º lugar mostra a escala). */
@Composable
private fun ChordGrid(field: HarmonicField, dominants: List<DominantFor?>, showDominants: Boolean, onChord: (Int) -> Unit) {
    val scale = remember(field) { field.scaleNotes.split(", ").joinToString(", ") { transposePtNote(it, 0) } }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        (0 until 8).chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { i ->
                    if (i < 7) ChordTile(field.chords[i], i, dominants.getOrNull(i), showDominants, Modifier.weight(1f)) { onChord(i) }
                    else ScaleTile(scale, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ChordTile(chord: ChordInfo, index: Int, dominant: DominantFor?, showDominant: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val color = chord.function.color()
    val (main, secondary) = chordLabels(index, chord.cipher)
    Column(
        modifier
            .pressClick(label = "Ver ${chord.cipher}", onClick = onClick)
            .heightIn(min = 96.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        AnimatedVisibility(showDominant, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                if (dominant != null) {
                    Icon(painterResource(R.drawable.ic_down), contentDescription = null, tint = com.example.music.HarmonicFunction.DOMINANT.color(), modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (com.example.ui.components.NotationSettings.byDegree) dominant.label else dominant.dominant.cipher,
                        style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
                        color = com.example.music.HarmonicFunction.DOMINANT.color(),
                    )
                } else {
                    Text("sem dominante", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                }
            }
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(main, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = color, maxLines = 1, modifier = Modifier.weight(1f))
            Text(secondary, style = MaterialTheme.typography.titleMedium, color = TextMuted, maxLines = 1)
        }
        Spacer(Modifier.height(4.dp))
        FunctionTag(chord.function)
    }
}

@Composable
private fun ScaleTile(scale: String, modifier: Modifier) {
    Column(
        modifier
            .heightIn(min = 96.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Surface2)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text("Escala", style = MaterialTheme.typography.labelLarge, color = TextMuted)
        Spacer(Modifier.height(2.dp))
        Text(scale, style = MaterialTheme.typography.bodyMedium, color = TextBody)
    }
}

/** 6 atalhos com ícone, em 3 colunas. */
@Composable
private fun ToolGrid(onOpenTool: (Tool) -> Unit) {
    val bpm = Live.currentItem?.bpm
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Tool.playGrid.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { tool ->
                    Column(
                        Modifier
                            .weight(1f)
                            .pressClick(label = tool.label) { onOpenTool(tool) }
                            .heightIn(min = 76.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Surface1)
                            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
                            .padding(vertical = 12.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(painterResource(tool.icon), contentDescription = null, tint = TextStrong, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (tool == Tool.METRONOMO && bpm != null) "$bpm BPM" else tool.short,
                            style = MaterialTheme.typography.labelLarge, color = TextBody, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/** Caminhos comuns no tom: um toque abre em tela cheia para tocar. */
@Composable
private fun PathsPanel(field: HarmonicField, onPlay: (Progression) -> Unit, compact: Boolean = false, max: Int = Int.MAX_VALUE) {
    val paths = remember(field.isMinor, max) { ProgressionLibrary.featuredFor(field.isMinor).take(max) }
    Panel(if (compact) Modifier.fillMaxHeight() else Modifier) {
        Text("Caminhos comuns", style = MaterialTheme.typography.titleMedium, color = TextStrong)
        if (!compact) Text("Toque num caminho para tocar em tela cheia.", style = MaterialTheme.typography.bodySmall, color = TextBody)
        Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp)) {
            paths.forEach { prog ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button, onClickLabel = "Tocar ${prog.name} em tela cheia") { onPlay(prog) }
                        .padding(vertical = 4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(prog.name, style = MaterialTheme.typography.labelLarge, color = TextBody, modifier = Modifier.weight(1f))
                        Icon(painterResource(R.drawable.ic_play), contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    ChordStrip(degrees = prog.degrees, chords = field.chords, compact = true)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Paisagem: tudo na tela, sem rolagem
// ---------------------------------------------------------------------------

@Composable
private fun PlayLandscape(
    field: HarmonicField,
    dominants: List<DominantFor?>,
    selected: Int,
    contentPadding: PaddingValues,
    a: PlayActions,
    player: ChordPlayer,
) {
    val showDominants = DominantSettings.show
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    Column(
        Modifier
            .fillMaxSize()
            .padding(
                start = contentPadding.calculateStartPadding(LayoutDirection.Ltr) + 12.dp,
                end = contentPadding.calculateEndPadding(LayoutDirection.Ltr) + 12.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 8.dp,
            ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Tom (tocar troca) + transpor.
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button, onClickLabel = "Trocar o tom") { a.onChangeKey() },
            ) {
                Text(field.keyNamePt, style = MaterialTheme.typography.headlineMedium, color = TextStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val moved = Live.transposedBy
                Text(
                    if (moved == 0) "Toque para trocar o tom" else "${if (moved > 0) "+" else ""}$moved do original (${Live.originalKey})",
                    style = MaterialTheme.typography.bodySmall, color = TextBody, maxLines = 1,
                )
            }
            RoundIconButton(R.drawable.ic_minus, "Meio tom abaixo") { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); Live.transpose(-1) }
            Spacer(Modifier.width(6.dp))
            RoundIconButton(R.drawable.ic_plus, "Meio tom acima") { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); Live.transpose(+1) }
            Live.setlist?.let { list ->
                Spacer(Modifier.width(12.dp))
                RoundIconButton(R.drawable.ic_chevron_left, "Hino anterior") { Live.previous() }
                Text(
                    "${Live.position + 1} de ${list.items.size}",
                    style = MaterialTheme.typography.labelLarge, color = TextBody,
                    modifier = Modifier.padding(horizontal = 8.dp).clickable { a.onOpenRepertoire() },
                )
                RoundIconButton(R.drawable.ic_chevron, "Próximo hino") { Live.next() }
            }
            Spacer(Modifier.width(12.dp))
            NotationToggle()
            Spacer(Modifier.width(8.dp))
            RoundIconButton(R.drawable.ic_mic, "Detectar o tom", onClick = a.onDetectKey)
            FavoriteStar(a.favorite, a.onToggleFavorite)
        }

        Spacer(Modifier.height(8.dp))
        if (showDominants) {
            DominantStrip(dominants, selected) { a.onChord(it) }
            Spacer(Modifier.height(4.dp))
        }
        Row(Modifier.fillMaxWidth().height(if (showDominants) 76.dp else 88.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            field.chords.forEachIndexed { i, chord ->
                Box(Modifier.weight(1f).fillMaxHeight()) { ChordMini(chord, i, i == selected) { a.onChord(i) } }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f).fillMaxHeight()) {
                if (showDominants) DominantCard(dominants.getOrNull(selected), field.chords, player, compact = true)
                else FormulaCard(field.chords[selected], compact = true)
            }
            Box(Modifier.weight(1f).fillMaxHeight()) { PathsPanel(field, a.onPlayFullScreen, compact = true, max = 2) }
        }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(Capo.advice(field.keyCipher), style = MaterialTheme.typography.bodySmall, color = TextBody, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            OptionChip("Dominantes", showDominants, Modifier.width(124.dp)) { DominantSettings.set(context, !showDominants) }
            SecondaryButton("Braço", icon = R.drawable.ic_guitar) { a.onOpenTool(Tool.BRACO) }
            SecondaryButton("Passagens", icon = R.drawable.ic_path) { a.onOpenTool(Tool.PASSAGENS) }
            SecondaryButton("Progressões", icon = R.drawable.ic_list_numbers) { a.onOpenTool(Tool.PROGRESSOES) }
        }
    }
}

@Composable
internal fun ChordMini(chord: ChordInfo, index: Int, selected: Boolean, onClick: () -> Unit) {
    val color = chord.function.color()
    val (main, secondary) = chordLabels(index, chord.cipher)
    val bg by animateColorAsState(if (selected) Surface2 else Surface1, tween(Motion.ms(180)), label = "mini")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .pressClick(label = "Ver ${chord.cipher}", onClick = onClick)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(if (selected) 2.dp else 1.dp, if (selected) color else Hairline, RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(secondary, style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 1)
        Text(main, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(chord.function.short, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1)
    }
}

/** Formação do acorde: fundamental, terça e quinta. */
@Composable
internal fun FormulaCard(chord: ChordInfo, compact: Boolean = false) {
    val color = chord.function.color()
    val notes = remember(chord.cipher) { getChordFormulaNotes(chord.cipher) }
    Panel(if (compact) Modifier.fillMaxHeight() else Modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Formação de ${chord.cipher}", style = MaterialTheme.typography.titleMedium, color = TextStrong)
            Spacer(Modifier.weight(1f))
            FunctionTag(chord.function)
        }
        if (!compact) Text(chord.function.tendency, style = MaterialTheme.typography.bodyMedium, color = TextBody)
        Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            notes.forEach { fn ->
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Ink)
                        .border(1.dp, Hairline, RoundedCornerShape(10.dp))
                        .padding(vertical = if (compact) 8.dp else 10.dp, horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(fn.note, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
                    Text(fn.interval, style = MaterialTheme.typography.labelSmall, color = TextStrong, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Folhas (sobem de baixo)
// ---------------------------------------------------------------------------

/** O acorde tocado: forma no violão, notas, quem chama ele e o som. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChordSheet(
    chord: ChordInfo,
    index: Int,
    dominant: DominantFor?,
    chords: List<ChordInfo>,
    player: ChordPlayer,
    onOpenDictionary: (LabChord) -> Unit,
    onDismiss: () -> Unit,
) {
    val color = chord.function.color()
    val (main, secondary) = chordLabels(index, chord.cipher)
    val parsed = remember(chord.cipher) { parseCipher(chord.cipher) }
    val shape = remember(chord.cipher) { parsed?.let { Guitar.shape(it) } }
    val notes = remember(chord.cipher) { getChordFormulaNotes(chord.cipher) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = SheetSurface) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(main, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = color)
                        Spacer(Modifier.width(10.dp))
                        Text(secondary, style = MaterialTheme.typography.titleLarge, color = TextMuted, modifier = Modifier.padding(bottom = 6.dp))
                    }
                    Text(chord.portugueseName, style = MaterialTheme.typography.titleMedium, color = TextStrong)
                    Spacer(Modifier.height(4.dp))
                    FunctionTag(chord.function)
                }
                shape?.let { Box(Modifier.width(96.dp)) { ChordDiagram(it, color) } }
            }
            Text(chord.function.tendency, style = MaterialTheme.typography.bodyMedium, color = TextBody)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                notes.forEach { fn ->
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Surface2).padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(fn.note, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
                        Text(fn.interval, style = MaterialTheme.typography.labelMedium, color = TextBody)
                    }
                }
            }
            // Dominantes ligadas: o cartão completo (notas, como entra, exemplo e som).
            if (DominantSettings.show && dominant != null) {
                DominantCard(dominant, chords, player)
            } else dominant?.let { d ->
                val dColor = com.example.music.HarmonicFunction.DOMINANT.color()
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface2).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (d.primary) "Quem chama: ${d.dominant.cipher} (a dominante do tom)" else "Quem chama: ${d.dominant.cipher} (${d.label})",
                            style = MaterialTheme.typography.titleSmall, color = dColor,
                        )
                        Text(
                            "${com.example.music.transposePtNote("Dó", d.leadingTone)} sobe meio tom para ${com.example.music.transposePtNote("Dó", d.target.root)}.",
                            style = MaterialTheme.typography.bodySmall, color = TextBody,
                        )
                    }
                    SecondaryButton("Ouvir", icon = R.drawable.ic_play) {
                        player.play(listOf(Guitar.shape(d.dominant).midiNotes, shape?.midiNotes.orEmpty()), secondsEach = 1.5, strum = 0.035)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton("Ouvir ${chord.cipher}", Modifier.weight(1f), icon = R.drawable.ic_play) {
                    shape?.let { player.play(listOf(it.midiNotes), secondsEach = 2.2, strum = 0.04) }
                }
                SecondaryButton("Mais formas", Modifier.weight(1f), icon = R.drawable.ic_book) {
                    fieldChord(chord.cipher)?.let { onDismiss(); onOpenDictionary(it) }
                }
            }
        }
    }
}

/** Trocar o tom: maior/menor, as 12 notas, os favoritos e o atalho para detectar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KeySheet(favorites: Set<String>, onDetectKey: () -> Unit, onDismiss: () -> Unit) {
    val current = Live.key
    var minor by rememberSaveable { mutableStateOf(current.endsWith("m")) }
    val haptic = LocalHapticFeedback.current
    fun choose(k: String) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); Live.setKey(k); onDismiss() }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = SheetSurface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Trocar tom", style = MaterialTheme.typography.titleLarge, color = TextStrong)
            if (favorites.isNotEmpty()) {
                Text("Favoritos", style = MaterialTheme.typography.labelLarge, color = TextMuted)
                favorites.sortedBy { HarmonicDatabase.allKeys().indexOfFirst { f -> f.keyCipher == it } }.chunked(6).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { k -> OptionChip(k, k == current, Modifier.weight(1f)) { choose(k) } }
                        repeat(6 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            Segmented(listOf("Maior", "Menor"), if (minor) 1 else 0, { minor = it == 1 }, Modifier.fillMaxWidth())
            (0 until 12).chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { pc ->
                        val k = pitchClassCiphers[pc] + if (minor) "m" else ""
                        KeyChoice(k, k == current, Modifier.weight(1f)) { choose(k) }
                    }
                }
            }
            SecondaryButton("Detectar pelo microfone", Modifier.fillMaxWidth(), icon = R.drawable.ic_mic, onClick = onDetectKey)
        }
    }
}

@Composable
private fun KeyChoice(cipher: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val name = HarmonicDatabase.ptNameByCipher[cipher] ?: cipher
    val bg by animateColorAsState(if (selected) com.example.ui.theme.Selection else Surface2, tween(Motion.ms(160)), label = "tom")
    Column(
        modifier
            .pressClick(role = Role.RadioButton, label = name, onClick = onClick)
            .heightIn(min = 60.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(cipher, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (selected) Ink else TextStrong)
        Text(name.substringBefore(" "), style = MaterialTheme.typography.labelSmall, color = if (selected) Ink else TextBody, maxLines = 1)
    }
}


