package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.HarmonicDatabase
import com.example.data.Repertoire
import com.example.data.SetItem
import com.example.music.pitchClassCiphers
import com.example.music.rootPc
import com.example.ui.components.SectionLabel
import com.example.ui.theme.Brass
import com.example.ui.theme.Hairline
import com.example.ui.theme.Surface1
import com.example.ui.theme.Surface2
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextStrong

/**
 * Repertório do culto: os hinos do dia, na ordem, com o tom de cada um.
 * Toque em "Campo" para abrir os acordes no tom, ou em "Passagens" para as
 * graças daquele tom. Tudo fica salvo só no aparelho.
 */
@Composable
fun RepertoireScreen(
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    onOpenField: (String) -> Unit = {},
    onOpenPassages: (String) -> Unit = {},
) {
    var editing by remember { mutableStateOf<SetItem?>(null) }     // item no formulário (novo ou existente)
    var confirmRemove by remember { mutableStateOf<Long?>(null) }

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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Surface1)
                    .border(1.dp, Hairline, CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = TextStrong, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Repertório do culto", style = MaterialTheme.typography.headlineMedium, color = TextStrong)
                Text("Os hinos do culto e o tom de cada um", style = MaterialTheme.typography.labelLarge, color = Brass)
            }
        }

        val form = editing
        if (form != null) {
            SetItemForm(form, onCancel = { editing = null }) { saved ->
                if (Repertoire.items.any { it.id == saved.id }) Repertoire.update(saved) else Repertoire.add(saved)
                editing = null
            }
        } else {
            LabChip("+ Adicionar hino", true, Modifier.fillMaxWidth()) {
                editing = SetItem(Repertoire.nextId(), null, "", "G")
            }
        }

        if (Repertoire.items.isEmpty() && form == null) {
            LabCard {
                Text("Monte a lista do culto", style = MaterialTheme.typography.titleMedium, color = TextStrong)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Adicione os hinos na ordem em que vão ser cantados, com o número da Harpa e o tom. " +
                        "Na hora, é só tocar no hino para abrir os acordes no tom certo — ou as passagens para enfeitar.",
                    style = MaterialTheme.typography.bodyMedium, color = TextBody,
                )
            }
        }

        if (Repertoire.items.isNotEmpty()) SectionLabel("Ordem do culto · ${Repertoire.items.size} hino(s)")
        Repertoire.items.forEachIndexed { i, item ->
            val keyName = HarmonicDatabase.ptNameByCipher[item.key] ?: item.key
            LabCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(30.dp).clip(CircleShape).background(Surface2),
                        contentAlignment = Alignment.Center,
                    ) { Text("${i + 1}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = TextBody) }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.heading, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextStrong, maxLines = 2)
                        Text(
                            keyName + if (item.capo > 0) " · capotraste na ${item.capo}ª casa" else "",
                            style = MaterialTheme.typography.bodyMedium, color = TextBody,
                        )
                    }
                    Text(item.key, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = Brass)
                }
                if (item.note.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(item.note, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LabChip("Campo ›", false, Modifier.weight(1f)) { onOpenField(item.key) }
                    LabChip("Passagens ›", false, Modifier.weight(1.2f)) { onOpenPassages(item.key) }
                    LabChip("Editar", false, Modifier.weight(1f)) { editing = item }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LabChip("↑ Subir", false, Modifier.weight(1f)) { Repertoire.move(item.id, -1) }
                    LabChip("↓ Descer", false, Modifier.weight(1f)) { Repertoire.move(item.id, +1) }
                    if (confirmRemove == item.id) {
                        LabChip("Confirmar", true, Modifier.weight(1f)) { Repertoire.remove(item.id); confirmRemove = null }
                    } else {
                        LabChip("Remover", false, Modifier.weight(1f)) { confirmRemove = item.id }
                    }
                }
            }
        }
    }
}

/** Formulário de um hino: número, nome, tom, capotraste e observação. */
@Composable
private fun SetItemForm(start: SetItem, onCancel: () -> Unit, onSave: (SetItem) -> Unit) {
    var number by rememberSaveable(start.id) { mutableStateOf(start.number?.toString() ?: "") }
    var title by rememberSaveable(start.id) { mutableStateOf(start.title) }
    var root by rememberSaveable(start.id) { mutableIntStateOf(rootPc(start.key) ?: 7) }
    var minor by rememberSaveable(start.id) { mutableStateOf(start.key.endsWith("m")) }
    var capo by rememberSaveable(start.id) { mutableIntStateOf(start.capo) }
    var note by rememberSaveable(start.id) { mutableStateOf(start.note) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Brass, unfocusedBorderColor = Hairline,
        focusedLabelColor = Brass, unfocusedLabelColor = TextMuted,
        cursorColor = Brass, focusedTextColor = TextStrong, unfocusedTextColor = TextStrong,
    )
    LabCard {
        Text(if (Repertoire.items.any { it.id == start.id }) "Editar hino" else "Novo hino", style = MaterialTheme.typography.titleMedium, color = TextStrong)
        Spacer(Modifier.height(8.dp))
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
        Spacer(Modifier.height(10.dp))
        SectionLabel("Tom")
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LabChip("Maior", !minor, Modifier.weight(1f)) { minor = false }
            LabChip("Menor", minor, Modifier.weight(1f)) { minor = true }
        }
        Spacer(Modifier.height(6.dp))
        (0 until 12).chunked(6).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                row.forEach { r -> LabChip(pitchClassCiphers[r] + if (minor) "m" else "", r == root, Modifier.weight(1f)) { root = r } }
            }
            Spacer(Modifier.height(5.dp))
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Capotraste", style = MaterialTheme.typography.bodyMedium, color = TextBody, modifier = Modifier.weight(1f))
            LabChip("−", false, Modifier.width(44.dp)) { capo = (capo - 1).coerceAtLeast(0) }
            Text(
                if (capo == 0) "sem" else "${capo}ª casa",
                style = MaterialTheme.typography.titleMedium, color = TextStrong,
                modifier = Modifier.width(90.dp).padding(horizontal = 8.dp),
                maxLines = 1,
            )
            LabChip("+", false, Modifier.width(44.dp)) { capo = (capo + 1).coerceAtMost(9) }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = note,
            onValueChange = { note = it.take(140) },
            label = { Text("Observação (ex.: subir meio tom no fim)") },
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LabChip("Cancelar", false, Modifier.weight(1f), onCancel)
            LabChip("Salvar", true, Modifier.weight(1f)) {
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
    }
}
