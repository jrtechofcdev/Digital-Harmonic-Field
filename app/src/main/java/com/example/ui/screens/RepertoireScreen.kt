package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.HarmonicDatabase
import com.example.R
import com.example.data.Repertoire
import com.example.data.SetItem
import com.example.music.pitchClassCiphers
import com.example.music.rootPc
import com.example.ui.components.OptionChip
import com.example.ui.components.Panel
import com.example.ui.components.PrimaryButton
import com.example.ui.components.RoundIconButton
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SecondaryButton
import com.example.ui.components.SectionLabel
import com.example.ui.components.Segmented
import com.example.ui.theme.HairlineStrong
import com.example.ui.theme.Selection
import com.example.ui.theme.Surface2
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextStrong

/**
 * Repertório do culto: os hinos do dia, na ordem, com o tom de cada um.
 * Toque em "Campo" para abrir os acordes no tom, ou em "Passagens" para as
 * graças daquele tom. Editar e remover ficam dentro do formulário do hino,
 * longe do toque acidental. Tudo fica salvo só no aparelho.
 */
@Composable
fun RepertoireScreen(
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    onOpenField: (String) -> Unit = {},
    onOpenPassages: (String) -> Unit = {},
) {
    var editing by remember { mutableStateOf<SetItem?>(null) }     // item no formulário (novo ou existente)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp, end = 16.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader("Repertório do culto", subtitle = "Os hinos do culto e o tom de cada um", onBack = onBack)

        val form = editing
        if (form != null) {
            SetItemForm(
                form,
                onCancel = { editing = null },
                onRemove = { Repertoire.remove(form.id); editing = null },
            ) { saved ->
                if (Repertoire.items.any { it.id == saved.id }) Repertoire.update(saved) else Repertoire.add(saved)
                editing = null
            }
        } else {
            PrimaryButton("Adicionar hino", Modifier.fillMaxWidth(), icon = R.drawable.ic_plus) {
                editing = SetItem(Repertoire.nextId(), null, "", "G")
            }
        }

        if (Repertoire.items.isEmpty() && form == null) {
            Panel {
                Text("Monte a lista do culto", style = MaterialTheme.typography.titleMedium, color = TextStrong)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Adicione os hinos na ordem em que vão ser cantados, com o número da Harpa e o tom. " +
                        "Na hora, é só tocar no hino para abrir os acordes no tom certo, ou as passagens para enfeitar.",
                    style = MaterialTheme.typography.bodyMedium, color = TextBody,
                )
            }
        }

        if (Repertoire.items.isNotEmpty()) SectionLabel("Ordem do culto (${Repertoire.items.size})")
        Repertoire.items.forEachIndexed { i, item ->
            val keyName = HarmonicDatabase.ptNameByCipher[item.key] ?: item.key
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(Surface2),
                        contentAlignment = Alignment.Center,
                    ) { Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = TextBody) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.heading, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextStrong, maxLines = 2)
                        Text(
                            keyName + if (item.capo > 0) ", capotraste na ${item.capo}ª casa" else "",
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
                    SecondaryButton("Campo", Modifier.weight(1f)) { onOpenField(item.key) }
                    SecondaryButton("Passagens", Modifier.weight(1.3f)) { onOpenPassages(item.key) }
                    RoundIconButton(R.drawable.ic_up, "Subir na ordem") { Repertoire.move(item.id, -1) }
                    RoundIconButton(R.drawable.ic_down, "Descer na ordem") { Repertoire.move(item.id, +1) }
                }
                Text(
                    "Editar ou remover",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextBody,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .heightIn(min = 44.dp)
                        .clickable(role = Role.Button) { editing = item }
                        .padding(vertical = 12.dp),
                )
            }
        }
    }
}

/** Formulário de um hino: número, nome, tom, capotraste e observação. */
@Composable
private fun SetItemForm(start: SetItem, onCancel: () -> Unit, onRemove: () -> Unit, onSave: (SetItem) -> Unit) {
    var number by rememberSaveable(start.id) { mutableStateOf(start.number?.toString() ?: "") }
    var title by rememberSaveable(start.id) { mutableStateOf(start.title) }
    var root by rememberSaveable(start.id) { mutableIntStateOf(rootPc(start.key) ?: 7) }
    var minor by rememberSaveable(start.id) { mutableStateOf(start.key.endsWith("m")) }
    var capo by rememberSaveable(start.id) { mutableIntStateOf(start.capo) }
    var note by rememberSaveable(start.id) { mutableStateOf(start.note) }
    var confirmRemove by remember(start.id) { mutableStateOf(false) }
    val existing = Repertoire.items.any { it.id == start.id }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Selection, unfocusedBorderColor = HairlineStrong,
        focusedLabelColor = TextStrong, unfocusedLabelColor = TextMuted,
        cursorColor = TextStrong, focusedTextColor = TextStrong, unfocusedTextColor = TextStrong,
    )
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
                colors = fieldColors,
                modifier = Modifier.width(90.dp),
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(60) },
                label = { Text("Nome do hino") },
                singleLine = true,
                colors = fieldColors,
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
        OutlinedTextField(
            value = note,
            onValueChange = { note = it.take(140) },
            label = { Text("Observação (ex.: subir meio tom no fim)") },
            colors = fieldColors,
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
