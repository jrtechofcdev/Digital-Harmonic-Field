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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.NoiseFilter
import com.example.data.Precision
import com.example.data.TunerSettings
import com.example.ui.theme.Brass
import com.example.ui.theme.Hairline
import com.example.ui.theme.Ink
import com.example.ui.theme.Surface1
import com.example.ui.theme.Surface2
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextStrong

@Composable
fun SettingsScreen(
    settings: TunerSettings,
    onChange: (TunerSettings) -> Unit,
    onBack: () -> Unit,
    contentPadding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp, end = 16.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
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
            Text("Configurações", style = MaterialTheme.typography.headlineMedium, color = TextStrong)
        }

        Spacer(Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // Tema (informativo — o app é dark por natureza)
            SettingRow(
                title = "Tema",
                subtitle = "Otimizado para palco e culto",
                valueText = "Escuro",
                onClick = null,
            )

            // Filtro de ruído (cicla Baixo → Padrão → Alto)
            SettingRow(
                title = "Filtro de ruído",
                subtitle = "Quanto exige de sinal limpo para ler a nota",
                valueText = settings.noiseFilter.label,
                onClick = {
                    val next = when (settings.noiseFilter) {
                        NoiseFilter.BAIXO -> NoiseFilter.PADRAO
                        NoiseFilter.PADRAO -> NoiseFilter.ALTO
                        NoiseFilter.ALTO -> NoiseFilter.BAIXO
                    }
                    onChange(settings.copy(noiseFilter = next))
                },
            )

            // Precisão (cicla Relaxada → Padrão → Alta)
            SettingRow(
                title = "Precisão",
                subtitle = "Tolerância para marcar como afinado (± ${settings.precision.cents.toInt()} cents)",
                valueText = settings.precision.label,
                onClick = {
                    val next = when (settings.precision) {
                        Precision.RELAXADA -> Precision.PADRAO
                        Precision.PADRAO -> Precision.ALTA
                        Precision.ALTA -> Precision.RELAXADA
                    }
                    onChange(settings.copy(precision = next))
                },
            )

            // Frequência de referência (stepper 430–450)
            StepperRow(
                title = "Frequência de referência",
                subtitle = "Lá padrão do afinador",
                valueText = "%.1f Hz".format(settings.refA),
                onMinus = { onChange(settings.copy(refA = (settings.refA - 1f).coerceIn(430f, 450f))) },
                onPlus = { onChange(settings.copy(refA = (settings.refA + 1f).coerceIn(430f, 450f))) },
            )

            // Sons (toggle)
            SwitchRow(
                title = "Sons",
                subtitle = "Tocar o tom de referência ao escolher uma corda",
                checked = settings.sounds,
                onCheckedChange = { onChange(settings.copy(sounds = it)) },
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Dica: se estiver num lugar barulhento, aumente o Filtro de ruído. Para afinar mais " +
                "fino, use Precisão Alta. O padrão de fábrica é Lá = 440 Hz.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
        )
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, valueText: String, onClick: (() -> Unit)?) {
    val base = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(Surface1)
        .border(1.dp, Hairline, RoundedCornerShape(12.dp))
    Row(
        modifier = (if (onClick != null) base.clickable { onClick() } else base)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextStrong)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        }
        Text(valueText, style = MaterialTheme.typography.titleMedium, color = Brass)
    }
}

@Composable
private fun StepperRow(
    title: String,
    subtitle: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface1)
            .border(1.dp, Hairline, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextStrong)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        }
        StepBox("−", onMinus)
        Text(
            valueText,
            style = MaterialTheme.typography.titleMedium,
            color = Brass,
            modifier = Modifier.width(84.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        StepBox("+", onPlus)
    }
}

@Composable
private fun StepBox(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(Surface2)
            .border(1.dp, Hairline, RoundedCornerShape(9.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextStrong)
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface1)
            .border(1.dp, Hairline, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextStrong)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Ink,
                checkedTrackColor = Brass,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = Surface2,
                uncheckedBorderColor = Hairline,
            ),
        )
    }
}
