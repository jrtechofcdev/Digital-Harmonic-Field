package com.example.ui.dev

import android.media.MediaPlayer
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dev.DevStore
import com.example.music.KeyTrainer
import com.example.ui.components.SectionLabel
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val ERROR_NAMES = mapOf(
    "relativa" to "Relativa (ex.: Sol ↔ Mi menor)",
    "quinta_acima" to "Disse a dominante (ex.: Ré em vez de Sol)",
    "quarta_acima" to "Disse a subdominante (ex.: Dó em vez de Sol)",
    "homonima" to "Maior ↔ menor (ex.: Sol ↔ Sol menor)",
    "semitom" to "Meio tom ao lado (grupo entre dois tons)",
    "sem_resposta" to "Não respondeu",
    "outro" to "Outro",
)

/**
 * Painel da versão DEV: quanto o detector acertou NOS SEUS HINOS, treinador do
 * aparelho, lista de sessões (ouvir, rotular, excluir) e envio da pasta em .zip.
 */
@Composable
fun DevPanelScreen(onBack: () -> Unit, contentPadding: PaddingValues) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Recalcula sempre que algo é gravado na pasta (DevStore.version muda).
    val stats = remember(DevStore.version.intValue) { DevStore.stats() }
    val list = remember(DevStore.version.intValue) { DevStore.summaries() }
    var editing by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf<String?>(null) }
    var outcome by remember { mutableStateOf<KeyTrainer.Outcome?>(null) }
    var playing by remember { mutableStateOf<String?>(null) }
    val player = remember { arrayOfNulls<MediaPlayer>(1) }
    DisposableEffect(Unit) { onDispose { player[0]?.release() } }

    fun play(s: DevStore.Summary) {
        player[0]?.release(); player[0] = null
        if (playing == s.id) { playing = null; return }
        runCatching {
            player[0] = MediaPlayer().apply {
                setDataSource(s.audioFile.absolutePath)
                setOnCompletionListener { playing = null }
                prepare(); start()
            }
            playing = s.id
        }.onFailure { Toast.makeText(context, "Não foi possível tocar o trecho.", Toast.LENGTH_SHORT).show() }
    }

    // Rotular/editar uma sessão: ocupa a tela.
    editing?.let { id ->
        val s = remember(id) { DevStore.session(id) }
        val detected = remember(id) {
            s?.optJSONObject("resultado_final")?.optJSONArray("top")?.let { a ->
                (0 until a.length()).map { a.getJSONObject(it).optString("tom") }
            } ?: emptyList()
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Header("Rotular sessão", onBack = { editing = null }) }
            item {
                DevLabelForm(
                    sessionId = id,
                    detected = detected,
                    existing = s?.optJSONObject("rotulo"),
                    onSaved = { editing = null },
                    onDiscarded = { editing = null },
                )
            }
        }
        return
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Header("Treino real", onBack = onBack) }

        item {
            Card {
                Text("Pasta de treino", style = MaterialTheme.typography.titleMedium, color = TextStrong)
                Text(DevStore.dir.absolutePath, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Spacer(Modifier.height(8.dp))
                Fact("Hinos captados", "${stats.total}")
                Fact("Rotulados / pendentes", "${stats.labeled} / ${stats.pending}")
                Fact("Tamanho", "%.1f MB".format(stats.megabytes))
            }
        }

        item {
            Card {
                Text("Desempenho nos seus hinos", style = MaterialTheme.typography.titleMedium, color = TextStrong)
                Text(
                    "Conta só os hinos em que você marcou o tom certo.",
                    style = MaterialTheme.typography.labelSmall, color = TextMuted,
                )
                Spacer(Modifier.height(8.dp))
                if (stats.roundAccuracy.isEmpty()) {
                    Text("Rotule alguns hinos para ver o acerto real.", style = MaterialTheme.typography.bodyMedium, color = TextBody)
                } else {
                    stats.roundAccuracy.forEachIndexed { i, (r, acc) ->
                        Fact("Acerto na ${r}ª rodada (${r * 5} s)", pct(acc) + "  (${stats.roundCounts[i]} hinos)")
                    }
                    stats.finalAccuracy?.let { Fact("Acerto no resultado final", pct(it)) }
                    stats.top3Accuracy?.let { Fact("Tom certo entre os 3", pct(it)) }
                }
                stats.avgStars?.let { Fact("Nota média do app", "%.1f ★".format(it)) }
                if (stats.errors.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text("Erros mais comuns", style = MaterialTheme.typography.labelLarge, color = TextBody)
                    stats.errors.entries.sortedByDescending { it.value }.forEach { (k, n) ->
                        Fact(ERROR_NAMES[k] ?: k, "$n")
                    }
                }
            }
        }

        item {
            Card {
                Text("Treinador do app", style = MaterialTheme.typography.titleMedium, color = TextStrong)
                Text(
                    "Ajusta a última camada do modelo com os seus rótulos e mede, com validação cruzada " +
                        "(cada hino é testado sem ter sido usado no ajuste), se o acerto melhora de verdade. " +
                        "O treino completo eu faço com o material que você enviar.",
                    style = MaterialTheme.typography.bodyMedium, color = TextBody,
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    busy ?: "Treinar com meus rótulos",
                    primary = true, enabled = busy == null && stats.labeled >= 5, modifier = Modifier.fillMaxWidth(),
                ) {
                    busy = "Treinando…"
                    scope.launch {
                        val r = withContext(Dispatchers.Default) { runCatching { DevStore.runTrainer() }.getOrNull() }
                        outcome = r
                        busy = null
                        if (r == null) Toast.makeText(context, "Não foi possível treinar.", Toast.LENGTH_SHORT).show()
                    }
                }
                if (stats.labeled < 5) {
                    Text("Precisa de pelo menos 5 hinos rotulados (ideal: 30+).", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
                outcome?.let { o ->
                    val r = o.report
                    Spacer(Modifier.height(8.dp))
                    Fact("Momentos usados", "${r.samples} (de ${r.hymns} hinos)")
                    Fact("Modelo original", pct(r.baseAccuracy))
                    Fact("Melhor ajuste (validação cruzada)", pct(r.adaptedAccuracy))
                    Text(
                        if (o.adapted) "O ajuste melhorou nos seus hinos e foi salvo. Pode testar ligando abaixo."
                        else "Nenhum ajuste superou o original ainda — o original continua. Siga rotulando.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (o.adapted) FuncTonic else FuncDominant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Usar modelo ajustado", style = MaterialTheme.typography.titleSmall, color = TextStrong)
                        Text(
                            if (DevStore.hasAdaptedModel) "Fica registrado em cada sessão qual modelo foi usado."
                            else "Treine primeiro.",
                            style = MaterialTheme.typography.labelSmall, color = TextMuted,
                        )
                    }
                    Switch(
                        checked = DevStore.useAdapted,
                        enabled = DevStore.hasAdaptedModel,
                        onCheckedChange = { DevStore.setUseAdapted(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Ink, checkedTrackColor = Brass),
                    )
                }
            }
        }

        item {
            Card {
                Text("Enviar o material", style = MaterialTheme.typography.titleMedium, color = TextStrong)
                Text(
                    "Compacta a pasta inteira (áudios, sessoes.json, log e relatórios) num único .zip.",
                    style = MaterialTheme.typography.bodyMedium, color = TextBody,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(busy ?: "Compartilhar .zip", primary = true, enabled = busy == null, modifier = Modifier.weight(1f)) {
                        busy = "Compactando…"
                        scope.launch {
                            val zip = withContext(Dispatchers.IO) { runCatching { DevStore.exportZip() }.getOrNull() }
                            busy = null
                            if (zip != null) context.startActivity(DevStore.shareIntent(zip))
                            else Toast.makeText(context, "Falha ao compactar.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    Button("Salvar em Downloads", primary = false, enabled = busy == null, modifier = Modifier.weight(1f)) {
                        busy = "Compactando…"
                        scope.launch {
                            val uri = withContext(Dispatchers.IO) {
                                runCatching { DevStore.saveToDownloads(DevStore.exportZip()) }.getOrNull()
                            }
                            busy = null
                            Toast.makeText(
                                context,
                                if (uri != null) "Salvo em Downloads/DHF-treino" else "Use \"Compartilhar\" neste aparelho.",
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    }
                }
            }
        }

        item { SectionLabel("Sessões (mais recentes primeiro)") }
        if (list.isEmpty()) {
            item {
                Text(
                    "Nenhuma ainda. Use \"Detectar tom\" no culto: cada hino detectado aparece aqui.",
                    style = MaterialTheme.typography.bodyMedium, color = TextMuted,
                )
            }
        }
        items(list, key = { it.id }) { s ->
            SessionRow(
                s = s,
                playing = playing == s.id,
                onPlay = { play(s) },
                onLabel = { editing = s.id },
            )
        }
    }
}

@Composable
private fun SessionRow(s: DevStore.Summary, playing: Boolean, onPlay: () -> Unit, onLabel: () -> Unit) {
    val badgeColor = when (s.correct) { true -> FuncTonic; false -> FuncDominant; null -> TextMuted }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface1)
            .border(1.dp, Hairline, RoundedCornerShape(12.dp))
            .clickable { onLabel() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (playing) Brass else Surface2)
                .clickable { onPlay() },
            contentAlignment = Alignment.Center,
        ) {
            Text(if (playing) "■" else "▶", color = if (playing) Ink else TextStrong, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "${s.startedAt} · %.0f s".format(s.seconds),
                style = MaterialTheme.typography.labelSmall, color = TextMuted,
            )
            Text(
                "App: ${s.detected ?: "—"}" + (s.detectedStatus?.let { " ($it)" } ?: ""),
                style = MaterialTheme.typography.titleSmall, color = TextStrong,
            )
            Text(
                when (s.labelKind) {
                    null -> "Pendente — toque para rotular"
                    "nao_sei" -> "Rótulo: não sei o tom"
                    "nao_era_hino" -> "Rótulo: não era hino"
                    else -> "Certo: ${s.label}" + (s.stars?.let { " · ${"★".repeat(it)}" } ?: "")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (s.labelKind == null) Brass else TextBody,
            )
        }
        Text(
            when (s.correct) { true -> "✓"; false -> "✗"; null -> "" },
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = badgeColor,
        )
    }
}

@Composable
private fun Header(title: String, onBack: () -> Unit) {
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
        Column {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = TextStrong)
            Text("Versão DEV · dados reais para treinar o detector", style = MaterialTheme.typography.labelSmall, color = FuncDominant)
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) { content() }
}

@Composable
private fun Fact(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextMuted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = TextBody)
    }
}

private fun pct(x: Double) = "${Math.round(x * 100)}%"
