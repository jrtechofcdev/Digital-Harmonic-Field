package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.Brass
import com.example.ui.theme.Hairline
import com.example.ui.theme.HairlineStrong
import com.example.ui.theme.Ink
import com.example.ui.theme.Selection
import com.example.ui.theme.Surface1
import com.example.ui.theme.Surface2
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextStrong

/*
 * Peças do sistema visual (ver docs/DESIGN.md):
 *  - toque mínimo de 44 dp em tudo que é clicável;
 *  - raios: 16 (painéis), 12 (botões e grupos), 10 (chips), 6 (etiquetas);
 *  - seleção é NEUTRA (Selection, texto Ink); o latão (Brass) fica só para a
 *    ação principal da tela (PrimaryButton) e para a aba ativa.
 */

/** Botão redondo só com ícone (voltar, −, +). */
@Composable
fun RoundIconButton(@DrawableRes icon: Int, description: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .pressClick(label = description, onClick = onClick)
            .size(44.dp)
            .clip(CircleShape)
            .background(Surface1)
            .border(1.dp, Hairline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = description, tint = TextStrong, modifier = Modifier.size(20.dp))
    }
}

/**
 * Cabeçalho de tela: voltar (opcional), título e uma linha de apoio. O apoio é
 * texto comum (não latão): o acento fica para a ação da tela.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            RoundIconButton(R.drawable.ic_back, "Voltar", onClick = onBack)
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = TextStrong, maxLines = 2, overflow = TextOverflow.Ellipsis)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = TextBody, maxLines = 2) }
        }
        trailing()
    }
}

/** Título de uma seção da tela (frase normal, sem caixa alta). */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleLarge, color = TextStrong, modifier = modifier)
}

/** Chip de escolha (nota, tipo, nível…). Escolhido = claro; resto = superfície. */
@Composable
fun OptionChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    val bg by animateColorAsState(if (selected) Selection else Surface2, tween(Motion.ms(180)), label = "chip")
    val fg by animateColorAsState(if (selected) Ink else TextBody, tween(Motion.ms(180)), label = "chipTexto")
    Box(
        modifier
            .pressClick(role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, if (selected) Selection else Hairline, shape)
            .semantics { this.selected = selected }
            .padding(horizontal = 6.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Controle segmentado: alterna entre 2 a 4 visões da mesma tela. O destaque desliza até a escolha. */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Surface2)
            .border(1.dp, Hairline, RoundedCornerShape(12.dp))
            .padding(3.dp),
    ) {
        val segW = maxWidth / options.size.coerceAtLeast(1)
        val x by animateDpAsState(segW * selected, tween(Motion.ms(240), easing = FastOutSlowInEasing), label = "segmento")
        // O destaque (desenhado atrás dos textos).
        Box(Modifier.matchParentSize()) {
            Box(
                Modifier
                    .offset(x = x)
                    .width(segW)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(9.dp))
                    .background(Selection),
            )
        }
        Row(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, label ->
                val on = i == selected
                val fg by animateColorAsState(if (on) Ink else TextBody, tween(Motion.ms(200)), label = "segTexto")
                Box(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .semantics { this.selected = on }
                        .clickable(role = Role.Tab) { onSelect(i) }
                        .padding(horizontal = if (options.size >= 4) 2.dp else 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        // Com 4 opções o espaço é curto: um ponto menor evita cortar ("Harmonizar").
                        style = if (options.size >= 4) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                        color = fg,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** A ação principal da tela (uma por tela): latão. */
@Composable
fun PrimaryButton(label: String, modifier: Modifier = Modifier, @DrawableRes icon: Int? = null, onClick: () -> Unit) {
    Row(
        modifier
            .pressClick(onClick = onClick)
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brass)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Icon(painterResource(it), contentDescription = null, tint = Ink, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, style = MaterialTheme.typography.titleSmall, color = Ink, maxLines = 1)
    }
}

/** Ação secundária: superfície com contorno, texto claro. */
@Composable
fun SecondaryButton(label: String, modifier: Modifier = Modifier, @DrawableRes icon: Int? = null, onClick: () -> Unit) {
    Row(
        modifier
            .pressClick(onClick = onClick)
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Surface2)
            .border(1.dp, Hairline, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Icon(painterResource(it), contentDescription = null, tint = TextStrong, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = TextStrong, maxLines = 1)
    }
}

/** Linha que leva a outra tela: título, explicação curta e a seta de navegação. */
@Composable
fun NavRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    @DrawableRes icon: Int? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = Role.Button) { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Icon(painterResource(it), contentDescription = null, tint = TextBody, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextStrong)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = TextBody) }
        }
        Spacer(Modifier.width(8.dp))
        Icon(painterResource(R.drawable.ic_chevron), contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
    }
}

/** Grupo de linhas (NavRow) com divisórias finas entre elas. */
@Composable
fun RowGroup(modifier: Modifier = Modifier, rows: List<@Composable () -> Unit>) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface1)
            .border(1.dp, Hairline, RoundedCornerShape(16.dp)),
    ) {
        rows.forEachIndexed { i, row ->
            if (i > 0) Box(Modifier.fillMaxWidth().padding(start = 16.dp).height(1.dp).background(Hairline))
            row()
        }
    }
}

/** Painel: agrupa um bloco de conteúdo que precisa de fundo próprio. */
@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface1)
            .border(1.dp, Hairline, RoundedCornerShape(16.dp))
            .padding(16.dp),
        content = content,
    )
}

/** Linha com chave liga/desliga: título, explicação curta e o interruptor. */
@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    framed: Boolean = true,          // false = sem moldura (dentro de um RowGroup)
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    val frame = if (framed) Modifier
        .clip(RoundedCornerShape(16.dp))
        .background(Surface1)
        .border(1.dp, Hairline, RoundedCornerShape(16.dp)) else Modifier
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = if (framed) 56.dp else 64.dp)
            .then(frame)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextStrong)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = TextBody) }
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Ink, checkedTrackColor = Selection, checkedBorderColor = Selection,
                uncheckedThumbColor = TextMuted, uncheckedTrackColor = Surface2, uncheckedBorderColor = HairlineStrong,
            ),
        )
    }
}
