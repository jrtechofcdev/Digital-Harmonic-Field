package com.example.ui.dev

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.HarmonicDatabase
import com.example.dev.DevStore
import com.example.music.pitchClassCiphers
import com.example.ui.theme.Brass
import com.example.ui.theme.FuncDominant
import com.example.ui.theme.FuncTonic
import com.example.ui.theme.Hairline
import com.example.ui.theme.Ink
import com.example.ui.theme.Surface1
import com.example.ui.theme.Surface2
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextStrong
import org.json.JSONArray
import org.json.JSONObject

/** Etiquetas de feedback (gravadas no JSON com estes códigos). */
private val TAGS = listOf(
    "acertou_de_primeira" to "Acertou de 1ª",
    "acertou_ouvindo_mais" to "Acertou ouvindo mais",
    "demorou" to "Demorou",
    "errou" to "Errou o tom",
    "confundiu_relativa" to "Confundiu com a relativa",
    "confundiu_vizinho" to "Confundiu com tom vizinho",
    "muito_ruido" to "Muito ruído/conversa",
    "a_capela" to "A capela",
    "com_teclado" to "Com teclado",
    "banda_completa" to "Banda completa",
    "solo_ministro" to "Solo/ministro",
    "hino_lento" to "Hino lento",
    "hino_rapido" to "Hino rápido",
)

/**
 * Rótulo de uma sessão (versão DEV): qual era o tom CERTO, o quanto você tem
 * certeza, nota de 1 a 5 para o app e etiquetas. Grava em sessoes.json.
 *
 * [detected] = tons que o app sugeriu (1º, 2º, 3º) — um toque já marca.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DevLabelForm(
    sessionId: String,
    detected: List<String>,
    existing: JSONObject? = null,
    onSaved: () -> Unit = {},
    onDiscarded: () -> Unit = {},
) {
    var key by remember(sessionId) { mutableStateOf(existing?.optString("tom")?.ifEmpty { null }) }
    var kind by remember(sessionId) {
        mutableStateOf(
            when {
                existing?.optBoolean("nao_era_hino") == true -> "nao_era_hino"
                existing != null && existing.optString("tom").isEmpty() -> "nao_sei"
                else -> "tom"
            }
        )
    }
    var minor by remember(sessionId) { mutableStateOf(key?.endsWith("m") ?: false) }
    var sure by remember(sessionId) { mutableStateOf(existing?.optString("certeza")?.ifEmpty { null } ?: "certeza") }
    var stars by remember(sessionId) { mutableIntStateOf(existing?.optInt("estrelas", 0) ?: 0) }
    var tags by remember(sessionId) {
        mutableStateOf(existing?.optJSONArray("tags")?.let { a -> (0 until a.length()).map { a.getString(it) }.toSet() } ?: emptySet())
    }
    var hymn by remember(sessionId) { mutableStateOf(existing?.optInt("harpa_numero", 0)?.takeIf { it > 0 }?.toString() ?: "") }
    var comment by remember(sessionId) { mutableStateOf(existing?.optString("comentario") ?: "") }
    var saved by remember(sessionId) { mutableStateOf(false) }
    var confirmDiscard by remember(sessionId) { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(1.dp, FuncDominant.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("TREINO", style = MaterialTheme.typography.labelSmall, color = FuncDominant)
            Spacer(Modifier.size(8.dp))
            Text("Qual era o tom certo?", style = MaterialTheme.typography.titleMedium, color = TextStrong)
        }
        Text("Sessão $sessionId — o áudio já está salvo como pendente.", style = MaterialTheme.typography.labelSmall, color = TextMuted)

        // 1) O app acertou? Um toque no tom sugerido.
        if (detected.isNotEmpty()) {
            Text("O app sugeriu (toque no certo):", style = MaterialTheme.typography.labelLarge, color = TextBody)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                detected.take(3).forEachIndexed { i, c ->
                    Pill(
                        "${i + 1}º · $c",
                        selected = kind == "tom" && key == c,
                        modifier = Modifier.weight(1f),
                    ) { kind = "tom"; key = c; minor = c.endsWith("m") }
                }
            }
        }

        // 2) Ou escolhe outro tom.
        Text("Ou escolha o tom:", style = MaterialTheme.typography.labelLarge, color = TextBody)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("Maior", !minor, Modifier.weight(1f)) {
                minor = false; key?.let { if (kind == "tom") key = it.removeSuffix("m") }
            }
            Pill("Menor", minor, Modifier.weight(1f)) {
                minor = true; key?.let { if (kind == "tom") key = it.removeSuffix("m") + "m" }
            }
        }
        (0 until 12).chunked(6).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { r ->
                    val c = pitchClassCiphers[r] + if (minor) "m" else ""
                    Pill(c, kind == "tom" && key == c, Modifier.weight(1f)) { kind = "tom"; key = c }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("Não sei o tom", kind == "nao_sei", Modifier.weight(1f)) { kind = "nao_sei"; key = null }
            Pill("Não era hino", kind == "nao_era_hino", Modifier.weight(1f)) { kind = "nao_era_hino"; key = null }
        }

        if (kind == "tom") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("Tenho certeza", sure == "certeza", Modifier.weight(1f)) { sure = "certeza" }
                Pill("Acho que é", sure == "acho", Modifier.weight(1f)) { sure = "acho" }
            }
        }

        // 3) Nota para o app.
        Text("Como o app se saiu neste hino?", style = MaterialTheme.typography.labelLarge, color = TextBody)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (i in 1..5) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = "$i estrela${if (i > 1) "s" else ""}",
                    tint = if (i <= stars) Brass else Surface2,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { stars = i }
                        .padding(2.dp),
                )
            }
        }

        // 4) Etiquetas.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TAGS.forEach { (code, label) ->
                val on = code in tags
                Box(
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (on) Brass.copy(alpha = 0.18f) else Surface2)
                        .border(1.dp, if (on) Brass else Hairline, RoundedCornerShape(16.dp))
                        .clickable { tags = if (on) tags - code else tags + code }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(label, style = MaterialTheme.typography.labelLarge, color = if (on) Brass else TextBody)
                }
            }
        }

        val fieldColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Brass, unfocusedBorderColor = Hairline,
            focusedTextColor = TextStrong, unfocusedTextColor = TextStrong, cursorColor = Brass,
            focusedLabelColor = Brass, unfocusedLabelColor = TextMuted,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = hymn,
                onValueChange = { v -> hymn = v.filter { it.isDigit() }.take(3) },
                label = { Text("Harpa nº") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = fieldColors,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it.take(300) },
                label = { Text("Comentário") },
                singleLine = true,
                colors = fieldColors,
                modifier = Modifier.weight(2f),
            )
        }

        val canSave = kind != "tom" || key != null
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                if (saved) "Salvo ✓ (salvar de novo)" else "Salvar rótulo",
                primary = true, enabled = canSave, modifier = Modifier.weight(2f),
            ) {
                val label = JSONObject()
                    .put("tom", if (kind == "tom") key else "")
                    .put("nome", if (kind == "tom") HarmonicDatabase.ptNameByCipher[key] ?: key else "")
                    .put("nao_sei", kind == "nao_sei")
                    .put("nao_era_hino", kind == "nao_era_hino")
                    .put("certeza", if (kind == "tom") sure else "")
                    .put("estrelas", stars)
                    .put("tags", JSONArray(tags.toList()))
                    .put("harpa_numero", hymn.toIntOrNull() ?: JSONObject.NULL)
                    .put("comentario", comment)
                    .put("app_sugeriu", JSONArray(detected))
                DevStore.saveLabel(sessionId, label)
                saved = true
                onSaved()
            }
            Button(
                if (confirmDiscard) "Confirmar" else "Descartar",
                primary = false, enabled = true, modifier = Modifier.weight(1f),
            ) {
                if (confirmDiscard) { DevStore.discard(sessionId); onDiscarded() } else confirmDiscard = true
            }
        }
        if (!canSave) Text("Escolha o tom (ou \"Não sei\") para salvar.", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        if (saved) Text("Gravado em sessoes.json.", style = MaterialTheme.typography.labelSmall, color = FuncTonic)
    }
}

@Composable
internal fun Pill(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Brass else Surface2)
            .border(1.dp, if (selected) Brass else Hairline, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (selected) Ink else TextBody,
            maxLines = 1,
        )
    }
}

@Composable
internal fun Button(label: String, primary: Boolean, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (primary && enabled) Brass else Surface2)
            .border(1.dp, if (primary && enabled) Brass else Hairline, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (primary && enabled) Ink else if (enabled) TextBody else TextMuted,
            maxLines = 1,
        )
    }
}
