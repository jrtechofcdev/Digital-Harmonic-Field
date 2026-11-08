package com.example.ui.screens

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.HarmonicDatabase
import com.example.R
import com.example.data.Live
import com.example.data.Repertoire
import com.example.data.SetItem
import com.example.data.Setlist
import com.example.music.pitchClassCiphers
import com.example.music.rootPc
import com.example.ui.components.OptionChip
import com.example.ui.components.Panel
import com.example.ui.components.PrimaryButton
import com.example.ui.components.RoundIconButton
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SecondaryButton
import com.example.ui.components.SectionLabel
import com.example.ui.components.SectionTitle
import com.example.ui.components.Segmented
import com.example.ui.components.pressClick
import com.example.ui.theme.Hairline
import com.example.ui.theme.HairlineStrong
import com.example.ui.theme.Selection
import com.example.ui.theme.Surface1
import com.example.ui.theme.Surface2
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextStrong
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Coluna rolável das telas de repertório (respeita a barra lateral na paisagem). */
@Composable
private fun RepertoireColumn(contentPadding: PaddingValues, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = contentPadding.calculateStartPadding(LayoutDirection.Ltr) + 16.dp,
                end = contentPadding.calculateEndPadding(LayoutDirection.Ltr) + 16.dp,
                top = contentPadding.calculateTopPadding() + 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) { content() }
}

// ---------------------------------------------------------------------------
// Aba Repertório: as listas
// ---------------------------------------------------------------------------

/**
 * Aba Repertório: as listas de hinos ("Culto de domingo", "Santa ceia"…).
 * "Começar" segue a lista na aba Tocar: hino anterior/próximo e o tom muda
 * sozinho. Um toque na lista abre para editar.
 */
@Composable
fun SetlistsScreen(
    contentPadding: PaddingValues,
    onOpenList: (Long) -> Unit,
    onPlay: () -> Unit,
) {
    val context = LocalContext.current
    RepertoireColumn(contentPadding) {
        ScreenHeader("Repertório", subtitle = "As listas de hinos do culto, com o tom de cada um")

        // O que está tocando agora (se houver).
        Live.setlist?.let { playing ->
            Panel {
                Text("Tocando agora", style = MaterialTheme.typography.labelLarge, color = TextMuted)
                Text(playing.name, style = MaterialTheme.typography.titleLarge, color = TextStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "Hino ${Live.position + 1} de ${playing.items.size}: ${Live.currentItem?.heading ?: ""}",
                    style = MaterialTheme.typography.bodyMedium, color = TextBody, maxLines = 2,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton("Ir para o Tocar", Modifier.weight(1f), icon = R.drawable.ic_play_circle, onClick = onPlay)
                    SecondaryButton("Parar", Modifier.weight(1f), icon = R.drawable.ic_stop) { Live.stop() }
                }
            }
        }

        PrimaryButton("Novo repertório", Modifier.fillMaxWidth(), icon = R.drawable.ic_plus) {
            onOpenList(Repertoire.createList(defaultListName()).id)
        }

        if (Repertoire.lists.isEmpty()) {
            Panel {
                Text("Monte a lista do culto", style = MaterialTheme.typography.titleMedium, color = TextStrong)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Coloque os hinos na ordem do culto, com o número da Harpa, o tom e o andamento. " +
                        "Na hora, toque em Começar: a aba Tocar mostra um hino de cada vez e o tom troca sozinho.",
                    style = MaterialTheme.typography.bodyMedium, color = TextBody,
                )
            }
        } else {
            SectionLabel("Suas listas (${Repertoire.lists.size})")
            Repertoire.lists.forEach { list ->
                SetlistCard(
                    list = list,
                    playing = Live.setlistId == list.id,
                    onOpen = { onOpenList(list.id) },
                    onStart = { Live.start(list.id); onPlay() },
                    onShare = { shareSetlist(context, list) },
                )
            }
        }
    }
}

@Composable
private fun SetlistCard(list: Setlist, playing: Boolean, onOpen: () -> Unit, onStart: () -> Unit, onShare: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .pressClick(label = "Abrir ${list.name}", onClick = onOpen)
            .clip(RoundedCornerShape(16.dp))
            .background(Surface1)
            .border(1.dp, Hairline, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(list.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    when (list.items.size) {
                        0 -> "Nenhum hino ainda"
                        1 -> "1 hino"
                        else -> "${list.items.size} hinos"
                    } + if (playing) ", tocando agora" else "",
                    style = MaterialTheme.typography.bodyMedium, color = TextBody,
                )
            }
            if (list.items.isNotEmpty()) RoundIconButton(R.drawable.ic_share, "Compartilhar ${list.name}", onClick = onShare)
        }
        if (list.items.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            // Os tons em ordem, como uma prévia rápida do culto.
            Text(
                list.items.joinToString("  ") { it.key },
                style = MaterialTheme.typography.titleSmall, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
            SecondaryButton(if (playing) "Continuar" else "Começar", Modifier.fillMaxWidth(), icon = R.drawable.ic_play, onClick = onStart)
        }
    }
}

/** "Culto 08/11" (o dia de hoje), para não precisar digitar nada. */
private fun defaultListName(): String =
    "Culto " + SimpleDateFormat("dd/MM", Locale.forLanguageTag("pt-BR")).format(Date())

/** Abre o "Compartilhar" do Android com a lista formatada para o WhatsApp. */
internal fun shareSetlist(context: Context, list: Setlist) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, Repertoire.shareText(list.items, list.name))
    }
    runCatching { context.startActivity(Intent.createChooser(send, "Enviar o repertório")) }
}

// ---------------------------------------------------------------------------
// Uma lista: editar os hinos e tocar
// ---------------------------------------------------------------------------

/**
 * Uma lista do repertório: os hinos na ordem, com tom, capotraste, BPM e
 * observação. "Tocar daqui" começa a seguir a lista a partir daquele hino.
 * Renomear, duplicar e apagar ficam no fim, longe do toque acidental.
 */
@Composable
fun SetlistScreen(
    listId: Long,
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    onPlay: () -> Unit,
    onOpenList: (Long) -> Unit = {},
) {
    val context = LocalContext.current
    val list = Repertoire.get(listId)
    var editing by remember { mutableStateOf<SetItem?>(null) }
    var renaming by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    RepertoireColumn(contentPadding) {
        if (list == null) {
            ScreenHeader("Repertório", subtitle = "Esta lista foi apagada", onBack = onBack)
            return@RepertoireColumn
        }
        ScreenHeader(
            list.name,
            subtitle = when (list.items.size) {
                0 -> "Nenhum hino ainda"
                1 -> "1 hino"
                else -> "${list.items.size} hinos na ordem do culto"
            },
            onBack = onBack,
        ) {
            if (list.items.isNotEmpty()) RoundIconButton(R.drawable.ic_share, "Compartilhar") { shareSetlist(context, list) }
        }

        if (renaming) {
            RenameForm(list.name, onCancel = { renaming = false }) { Repertoire.renameList(list.id, it); renaming = false }
        }

        val form = editing
        if (form != null) {
            SetItemForm(
                form,
                existing = list.items.any { it.id == form.id },
                onCancel = { editing = null },
                onRemove = { Repertoire.removeItem(list.id, form.id); editing = null },
            ) { saved ->
                if (list.items.any { it.id == saved.id }) Repertoire.updateItem(list.id, saved) else Repertoire.addItem(list.id, saved)
                editing = null
            }
        } else if (list.items.isEmpty()) {
            PrimaryButton("Adicionar hino", Modifier.fillMaxWidth(), icon = R.drawable.ic_plus) {
                editing = SetItem(Repertoire.nextItemId(list.id), null, "", Live.key)
            }
            Panel {
                Text("Adicione os hinos na ordem", style = MaterialTheme.typography.titleMedium, color = TextStrong)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Para cada hino: o número da Harpa (ou o nome), o tom e, se quiser, o capotraste, o BPM e uma observação.",
                    style = MaterialTheme.typography.bodyMedium, color = TextBody,
                )
            }
        } else {
            PrimaryButton(
                if (Live.setlistId == list.id) "Continuar tocando" else "Começar a tocar",
                Modifier.fillMaxWidth(), icon = R.drawable.ic_play,
            ) {
                if (Live.setlistId != list.id) Live.start(list.id)
                onPlay()
            }
            SecondaryButton("Adicionar hino", Modifier.fillMaxWidth(), icon = R.drawable.ic_plus) {
                editing = SetItem(Repertoire.nextItemId(list.id), null, "", list.items.last().key)
            }
        }

        list.items.forEachIndexed { i, item ->
            SetItemCard(
                index = i,
                item = item,
                isCurrent = Live.setlistId == list.id && Live.position == i,
                onPlay = { Live.start(list.id, i); onPlay() },
                onUp = { Repertoire.moveItem(list.id, item.id, -1) },
                onDown = { Repertoire.moveItem(list.id, item.id, +1) },
                onEdit = { editing = item },
            )
        }

        // Ações da lista (no fim, longe do dedo).
        if (editing == null) {
            SectionTitle("Esta lista", Modifier.padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton("Renomear", Modifier.weight(1f), icon = R.drawable.ic_edit) { renaming = true }
                SecondaryButton("Duplicar", Modifier.weight(1f), icon = R.drawable.ic_copy) {
                    Repertoire.duplicateList(list.id)?.let { onOpenList(it.id) }
                }
            }
            SecondaryButton(
                if (confirmDelete) "Toque de novo para apagar a lista" else "Apagar a lista",
                Modifier.fillMaxWidth(), icon = R.drawable.ic_trash,
            ) {
                if (confirmDelete) {
                    if (Live.setlistId == list.id) Live.stop()
                    Repertoire.deleteList(list.id)
                    onBack()
                } else confirmDelete = true
            }
        }
    }
}

@Composable
private fun SetItemCard(
    index: Int,
    item: SetItem,
    isCurrent: Boolean,
    onPlay: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onEdit: () -> Unit,
) {
    val keyName = HarmonicDatabase.ptNameByCipher[item.key] ?: item.key
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(if (isCurrent) Selection else Surface2),
                contentAlignment = Alignment.Center,
            ) {
                Text("${index + 1}", style = MaterialTheme.typography.labelLarge, color = if (isCurrent) com.example.ui.theme.Ink else TextBody)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.heading, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextStrong, maxLines = 2)
                Text(
                    listOfNotNull(
                        keyName,
                        item.capo.takeIf { it > 0 }?.let { "capo na ${it}ª casa" },
                        item.bpm?.let { "$it BPM" },
                    ).joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium, color = TextBody,
                )
            }
            Text(item.key, style = MaterialTheme.typography.headlineLarge, color = TextStrong)
        }
        if (item.note.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(item.note, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SecondaryButton("Tocar daqui", Modifier.weight(1f), icon = R.drawable.ic_play, onClick = onPlay)
            RoundIconButton(R.drawable.ic_up, "Subir na ordem", onClick = onUp)
            RoundIconButton(R.drawable.ic_down, "Descer na ordem", onClick = onDown)
            RoundIconButton(R.drawable.ic_edit, "Editar ou remover", onClick = onEdit)
        }
    }
}

@Composable
private fun fieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Selection, unfocusedBorderColor = HairlineStrong,
    focusedLabelColor = TextStrong, unfocusedLabelColor = TextMuted,
    cursorColor = TextStrong, focusedTextColor = TextStrong, unfocusedTextColor = TextStrong,
)

@Composable
private fun RenameForm(current: String, onCancel: () -> Unit, onSave: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(current) }
    Panel {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(40) },
            label = { Text("Nome da lista") },
            singleLine = true,
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton("Cancelar", Modifier.weight(1f), onClick = onCancel)
            SecondaryButton("Salvar nome", Modifier.weight(1f)) { onSave(name) }
        }
    }
}

/** Formulário de um hino: número, nome, tom, capotraste, BPM e observação. */
@Composable
private fun SetItemForm(
    start: SetItem,
    existing: Boolean,
    onCancel: () -> Unit,
    onRemove: () -> Unit,
    onSave: (SetItem) -> Unit,
) {
    var number by rememberSaveable(start.id) { mutableStateOf(start.number?.toString() ?: "") }
    var title by rememberSaveable(start.id) { mutableStateOf(start.title) }
    var root by rememberSaveable(start.id) { mutableIntStateOf(rootPc(start.key) ?: 7) }
    var minor by rememberSaveable(start.id) { mutableStateOf(start.key.endsWith("m")) }
    var capo by rememberSaveable(start.id) { mutableIntStateOf(start.capo) }
    var bpm by rememberSaveable(start.id) { mutableStateOf(start.bpm?.toString() ?: "") }
    var note by rememberSaveable(start.id) { mutableStateOf(start.note) }
    var confirmRemove by remember(start.id) { mutableStateOf(false) }
    val colors = fieldColors()

    Panel {
        Text(if (existing) "Editar hino" else "Novo hino", style = MaterialTheme.typography.titleLarge, color = TextStrong)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = number,
                onValueChange = { v -> number = v.filter { it.isDigit() }.take(3) },
                label = { Text("Nº") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = colors,
                modifier = Modifier.width(90.dp),
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(60) },
                label = { Text("Nome do hino") },
                singleLine = true,
                colors = colors,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        SectionLabel("Tom")
        Spacer(Modifier.height(6.dp))
        Segmented(listOf("Maior", "Menor"), if (minor) 1 else 0, { minor = it == 1 }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        (0 until 12).chunked(6).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { r -> OptionChip(pitchClassCiphers[r] + if (minor) "m" else "", r == root, Modifier.weight(1f)) { root = r } }
            }
            Spacer(Modifier.height(6.dp))
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Capotraste", style = MaterialTheme.typography.bodyLarge, color = TextBody, modifier = Modifier.weight(1f))
            RoundIconButton(R.drawable.ic_minus, "Uma casa a menos") { capo = (capo - 1).coerceAtLeast(0) }
            Text(
                if (capo == 0) "sem" else "${capo}ª casa",
                style = MaterialTheme.typography.titleMedium, color = TextStrong,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(84.dp),
                maxLines = 1,
            )
            RoundIconButton(R.drawable.ic_plus, "Uma casa a mais") { capo = (capo + 1).coerceAtMost(9) }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = bpm,
                onValueChange = { v -> bpm = v.filter { it.isDigit() }.take(3) },
                label = { Text("BPM") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = colors,
                modifier = Modifier.width(110.dp),
            )
            Text(
                "Andamento (opcional): o metrônomo já abre nele.",
                style = MaterialTheme.typography.bodySmall, color = TextMuted, modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = note,
            onValueChange = { note = it.take(140) },
            label = { Text("Observação (ex.: subir meio tom no fim)") },
            colors = colors,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SecondaryButton("Cancelar", Modifier.weight(1f).heightIn(min = 52.dp), onClick = onCancel)
            PrimaryButton("Salvar", Modifier.weight(1f)) {
                onSave(
                    start.copy(
                        number = number.toIntOrNull(),
                        title = title.trim(),
                        key = pitchClassCiphers[root] + if (minor) "m" else "",
                        capo = capo,
                        note = note.trim(),
                        bpm = bpm.toIntOrNull()?.coerceIn(30, 240),
                    )
                )
            }
        }
        // Remover fica aqui dentro, com confirmação: na lista ele seria tocado sem querer.
        if (existing) {
            Spacer(Modifier.height(8.dp))
            Text(
                if (confirmRemove) "Toque de novo para remover da lista" else "Remover da lista",
                style = MaterialTheme.typography.labelLarge,
                color = if (confirmRemove) TextStrong else TextBody,
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .clickable(role = Role.Button) { if (confirmRemove) onRemove() else confirmRemove = true }
                    .padding(vertical = 12.dp),
            )
        }
    }
}
