package com.example.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AppInfo
import com.example.R
import com.example.legal.LEGAL_UPDATED
import com.example.legal.LegalDoc
import com.example.legal.sectionsFor
import com.example.legal.thirdParties
import com.example.ui.components.BrandMark
import com.example.ui.components.NavRow
import com.example.ui.components.Panel
import com.example.ui.components.RowGroup
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SecondaryButton
import com.example.ui.components.SectionTitle
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextStrong

/** Placa escura com o logo da JR TECH OFC (o fundo do próprio logo). */
@Composable
internal fun JrTechPlaque(horizontal: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF292929)),
    ) {
        if (horizontal) {
            Image(painterResource(R.drawable.jrtech_horizontal), "Logo da JR TECH OFC", Modifier.fillMaxWidth().aspectRatio(900f / 249f))
        } else {
            Image(painterResource(R.drawable.jrtech_vertical), "Logo da JR TECH OFC", Modifier.fillMaxWidth().aspectRatio(760f / 774f))
        }
    }
}

/** Sobre o app: o que é, quem fez, direitos e os documentos legais. */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    onOpenLegal: (LegalDoc) -> Unit,
    onSupport: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ScreenHeader("Sobre", subtitle = "O app, o autor e os seus direitos", onBack = onBack) }

        item {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BrandMark(size = 64.dp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(AppInfo.APP_NAME, style = MaterialTheme.typography.titleLarge, color = TextStrong)
                        Text("Versão ${AppInfo.version}", style = MaterialTheme.typography.bodyMedium, color = TextBody)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Campos harmônicos, graus, acordes no violão e um detector de tom para o músico de igreja. Gratuito, sem anúncios, sem conta e sem internet.",
                    style = MaterialTheme.typography.bodyMedium, color = TextBody,
                )
            }
        }

        item { SectionTitle("Quem fez") }
        item {
            Panel {
                JrTechPlaque(horizontal = true)
                Spacer(Modifier.height(14.dp))
                Text(AppInfo.AUTHOR, style = MaterialTheme.typography.titleLarge, color = TextStrong)
                Text("${AppInfo.AUTHOR_ROLE}. Desenvolvedor do app.", style = MaterialTheme.typography.bodyMedium, color = TextBody)
                if (AppInfo.CONTACT.isNotBlank()) Text(AppInfo.CONTACT, style = MaterialTheme.typography.bodyMedium, color = TextBody)
                Spacer(Modifier.height(12.dp))
                SecondaryButton("Apoiar o projeto", Modifier.fillMaxWidth(), onClick = onSupport)
            }
        }

        item { SectionTitle("Direitos") }
        item {
            Panel {
                Text(AppInfo.copyright, style = MaterialTheme.typography.titleSmall, color = TextStrong)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Todos os direitos reservados. O nome, a marca DHF, os logos, o vídeo de abertura, o código e os textos são do autor (Lei 9.610/1998 e Lei 9.609/1998). Proibida a cópia, modificação ou redistribuição sem autorização.",
                    style = MaterialTheme.typography.bodyMedium, color = TextBody,
                )
            }
        }

        item { SectionTitle("Documentos") }
        item {
            RowGroup(rows = LegalDoc.entries.map { doc ->
                @Composable { NavRow(doc.title, subtitle = doc.subtitle) { onOpenLegal(doc) } }
            })
        }
        item {
            Text(
                "Atualizado em $LEGAL_UPDATED.",
                style = MaterialTheme.typography.bodySmall, color = TextMuted,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

/** Um documento legal, em seções. Licenças: cada componente com o texto completo. */
@Composable
fun LegalScreen(doc: LegalDoc, onBack: () -> Unit, contentPadding: PaddingValues) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenHeader(doc.title, subtitle = "Atualizado em $LEGAL_UPDATED", onBack = onBack) }
        if (doc == LegalDoc.LICENCAS) {
            item {
                Text(
                    "O app usa estes componentes de terceiros. Cada um tem a sua licença, que permite o uso desde que os créditos e o texto da licença acompanhem o app:",
                    style = MaterialTheme.typography.bodyMedium, color = TextBody,
                )
            }
            thirdParties.forEach { tp ->
                item(key = tp.name) {
                    var open by rememberSaveable(tp.name) { mutableStateOf(false) }
                    val text = remember(tp.rawText, open) {
                        if (open) context.resources.openRawResource(tp.rawText).bufferedReader().use { it.readText() } else ""
                    }
                    Panel(Modifier.animateContentSize()) {
                        Text(tp.name, style = MaterialTheme.typography.titleMedium, color = TextStrong)
                        Text("${tp.owner}. Licença: ${tp.license}.", style = MaterialTheme.typography.bodyMedium, color = TextBody)
                        Spacer(Modifier.height(10.dp))
                        SecondaryButton(if (open) "Fechar o texto da licença" else "Ver o texto da licença") { open = !open }
                        if (open) {
                            Spacer(Modifier.height(10.dp))
                            Text(text, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 15.sp), color = TextBody)
                        }
                    }
                }
            }
        } else {
            sectionsFor(doc).forEach { section ->
                item(key = section.title) {
                    Panel {
                        Text(section.title, style = MaterialTheme.typography.titleMedium, color = TextStrong)
                        Spacer(Modifier.height(4.dp))
                        section.paragraphs.forEach {
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = TextBody, modifier = Modifier.padding(vertical = 3.dp))
                        }
                    }
                }
            }
        }
    }
}
