package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import com.example.AppInfo
import com.example.BuildConfig
import com.example.FAVORITES_KEY
import com.example.R
import com.example.data.Repertoire
import com.example.dataStore
import com.example.legal.LegalDoc
import com.example.settings.AppSettings
import com.example.settings.IconMode
import com.example.settings.TextSize
import com.example.settings.ThemeMode
import com.example.ui.components.Notation
import com.example.ui.components.NotationSettings
import com.example.ui.components.NavRow
import com.example.ui.components.Panel
import com.example.ui.components.RowGroup
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SecondaryButton
import com.example.ui.components.SectionTitle
import com.example.ui.components.Segmented
import com.example.ui.components.SwitchRow
import com.example.ui.theme.Hairline
import com.example.ui.theme.Selection
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextStrong
import kotlinx.coroutines.launch

/**
 * Aba Ajustes: tudo o que dá para configurar, num lugar só, por assunto.
 * Cada mudança vale na hora e fica salva no aparelho.
 */
@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    onReplayIntro: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenLegal: (LegalDoc) -> Unit,
    onSupport: () -> Unit,
    onOpenDevPanel: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenHeader("Ajustes", subtitle = "Do seu jeito: aparência, abertura, música e dados") }

        // ------------------------------------------------------------ Aparência
        item { SectionTitle("Aparência", Modifier.padding(top = 4.dp)) }
        item {
            Panel {
                Label("Tema", "O claro usa as cores do final da abertura; o escuro é para palco e igreja com pouca luz.")
                Segmented(
                    ThemeMode.entries.map { it.label },
                    AppSettings.theme.ordinal,
                    { AppSettings.setTheme(context, ThemeMode.entries[it]) },
                    Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                Label("Tamanho do texto", "Para ler de longe, na estante.")
                Segmented(
                    TextSize.entries.map { it.label },
                    AppSettings.textSize.ordinal,
                    { AppSettings.setTextSize(TextSize.entries[it]) },
                    Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            Panel {
                Label("Ícone do app", "Troca o ícone na tela inicial do celular, sem reinstalar. Muda quando você sai do app.")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconChoice(R.drawable.icon_preview_dark, "Escuro", AppSettings.icon == IconMode.ESCURO, Modifier.weight(1f)) { AppSettings.setIcon(IconMode.ESCURO) }
                    IconChoice(R.drawable.icon_preview_light, "Claro", AppSettings.icon == IconMode.CLARO, Modifier.weight(1f)) { AppSettings.setIcon(IconMode.CLARO) }
                    IconChoice(null, "Pelo tema", AppSettings.icon == IconMode.AUTOMATICO, Modifier.weight(1f)) { AppSettings.setIcon(IconMode.AUTOMATICO) }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Pelo tema: tema escuro, ícone escuro; tema claro, ícone claro. Em alguns celulares o atalho arrastado para a tela inicial precisa ser colocado de novo.",
                    style = MaterialTheme.typography.bodySmall, color = TextMuted,
                )
            }
        }
        item {
            SwitchRow("Menos animação", checked = AppSettings.reduceMotion, subtitle = "Transições mais curtas e sem efeitos contínuos") {
                AppSettings.setReduceMotion(it)
            }
        }

        // ------------------------------------------------------------ Abertura
        item { SectionTitle("Abertura", Modifier.padding(top = 8.dp)) }
        item {
            RowGroup(rows = listOf(
                @Composable { SettingSwitch("Vídeo de abertura", "A palheta DHF ao abrir o app", AppSettings.introVideo) { AppSettings.setIntroVideo(it) } },
                @Composable { SettingSwitch("Som do vídeo", "Desligue para abrir em silêncio no culto", AppSettings.introSound, enabled = AppSettings.introVideo) { AppSettings.setIntroSound(it) } },
                @Composable { SettingSwitch("Tela JR TECH OFC", "O logo do desenvolvedor por 3 segundos", AppSettings.brandScreen) { AppSettings.setBrandScreen(it) } },
                @Composable { NavRow("Ver a abertura agora", subtitle = "Toca o vídeo e a tela do logo de novo", onClick = onReplayIntro) },
            ))
        }

        // ------------------------------------------------------------ Música
        item { SectionTitle("Música", Modifier.padding(top = 8.dp)) }
        item {
            Panel {
                Label("Acordes por", "Cifra (C, Am, G) ou grau (1, 6m, 5): muda o app inteiro.")
                Segmented(
                    listOf("Cifra", "Grau"),
                    if (NotationSettings.byDegree) 1 else 0,
                    { NotationSettings.set(context, if (it == 1) Notation.GRAU else Notation.CIFRA) },
                    Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                Label("Nomes das notas", "No braço do violão e nos treinos.")
                Segmented(
                    listOf("Dó Ré Mi", "C D E"),
                    if (AppSettings.latinNames) 0 else 1,
                    { AppSettings.setLatinNames(it == 0) },
                    Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            SwitchRow("Dominantes secundárias no campo", checked = DominantSettings.show, subtitle = "Mostra em cima de cada acorde o acorde que chama ele") {
                DominantSettings.set(context, it)
            }
        }

        // ------------------------------------------------------------ No culto
        item { SectionTitle("No culto", Modifier.padding(top = 8.dp)) }
        item {
            RowGroup(rows = listOf(
                @Composable { SettingSwitch("Manter a tela acesa", "No campo harmônico e na tela cheia das progressões", AppSettings.keepScreenOn) { AppSettings.setKeepScreenOn(it) } },
                @Composable { SettingSwitch("Vibrar ao terminar a detecção", "Para saber o resultado sem olhar a tela", AppSettings.vibrate) { AppSettings.setVibrate(it) } },
            ))
        }

        // ------------------------------------------------------------ Dados
        item { SectionTitle("Dados", Modifier.padding(top = 8.dp)) }
        item {
            Panel {
                Text("Tudo fica só neste aparelho. Estas ações não podem ser desfeitas.", style = MaterialTheme.typography.bodyMedium, color = TextBody)
                Spacer(Modifier.height(10.dp))
                ConfirmButton("Apagar os tons favoritos") {
                    scope.launch { context.dataStore.edit { it.remove(FAVORITES_KEY) } }
                }
                Spacer(Modifier.height(8.dp))
                ConfirmButton("Apagar o repertório do culto") { Repertoire.clear() }
                Spacer(Modifier.height(8.dp))
                ConfirmButton("Voltar todos os ajustes ao padrão") { AppSettings.resetAll(context) }
            }
        }

        // ------------------------------------------------------------ Sobre e legal
        item { SectionTitle("Sobre e legal", Modifier.padding(top = 8.dp)) }
        item {
            val rows = mutableListOf<@Composable () -> Unit>(
                { NavRow("Sobre o app", subtitle = "Versão, autor e direitos", onClick = onOpenAbout) },
                { NavRow(LegalDoc.PRIVACIDADE.title, subtitle = LegalDoc.PRIVACIDADE.subtitle) { onOpenLegal(LegalDoc.PRIVACIDADE) } },
                { NavRow(LegalDoc.TERMOS.title, subtitle = LegalDoc.TERMOS.subtitle) { onOpenLegal(LegalDoc.TERMOS) } },
                { NavRow(LegalDoc.PERMISSOES.title, subtitle = LegalDoc.PERMISSOES.subtitle) { onOpenLegal(LegalDoc.PERMISSOES) } },
                { NavRow(LegalDoc.LICENCAS.title, subtitle = LegalDoc.LICENCAS.subtitle) { onOpenLegal(LegalDoc.LICENCAS) } },
                { NavRow("Apoiar o projeto", subtitle = "Doação voluntária pelo Mercado Pago", onClick = onSupport) },
            )
            if (BuildConfig.DEV_TOOLS) rows += { NavRow("Treino real (versão DEV)", subtitle = "Trechos captados e rótulos", onClick = onOpenDevPanel) }
            RowGroup(rows = rows)
        }
        item {
            Text(
                "${AppInfo.APP_NAME} ${AppInfo.version}\n${AppInfo.copyright}",
                style = MaterialTheme.typography.bodySmall, color = TextMuted, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun Label(title: String, help: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = TextStrong)
    Text(help, style = MaterialTheme.typography.bodySmall, color = TextBody)
    Spacer(Modifier.height(8.dp))
}

/** Interruptor dentro de um grupo de linhas (sem moldura própria). */
@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    SwitchRow(
        title, checked = checked, subtitle = subtitle, framed = false, enabled = enabled,
        onChange = onChange,
    )
}

/** Opção de ícone: a miniatura do ícone e o nome. */
@Composable
private fun IconChoice(icon: Int?, label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .border(if (selected) 2.dp else 1.dp, if (selected) Selection else Hairline, RoundedCornerShape(14.dp))
            .clickable(role = Role.RadioButton) { onClick() }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (icon != null) {
            Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(48.dp))
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy((-14).dp)) {
                Image(painterResource(R.drawable.icon_preview_dark), contentDescription = null, modifier = Modifier.size(36.dp))
                Image(painterResource(R.drawable.icon_preview_light), contentDescription = null, modifier = Modifier.size(36.dp).padding(top = 12.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) TextStrong else TextBody)
    }
}

/** Ação de apagar: o primeiro toque pede confirmação, o segundo executa. */
@Composable
private fun ConfirmButton(label: String, onConfirm: () -> Unit) {
    var armed by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    SecondaryButton(
        when {
            done -> "Feito"
            armed -> "Toque de novo para confirmar"
            else -> label
        },
        Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) {
        if (done) return@SecondaryButton
        if (armed) { onConfirm(); done = true } else armed = true
    }
}
