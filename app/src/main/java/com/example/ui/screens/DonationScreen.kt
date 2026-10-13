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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import com.example.R
import com.example.ui.components.PrimaryButton
import com.example.ui.components.RowGroup
import com.example.ui.components.ScreenHeader
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.BrandMark
import com.example.ui.theme.Hairline
import com.example.ui.theme.Ink
import com.example.ui.theme.Surface1
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextStrong

// Valor, descrição e link do Mercado Pago. Para editar valores/links, mude aqui.
private data class Tier(val amount: String, val note: String, val url: String)

private val tiers = listOf(
    Tier("R$ 5", "Uma palheta", "https://mpago.li/19n4bhK"),
    Tier("R$ 10", "Um cafezinho", "https://mpago.li/1EjJbwT"),
    Tier("R$ 25", "Um jogo de cordas", "https://mpago.li/2dP3Yb1"),
    Tier("R$ 50", "Um super apoio", "https://mpago.li/1Ui9AVu"),
)
private const val FREE_URL = "https://link.mercadopago.com.br/jrtechofc"

@Composable
fun DonationScreen(
    onBack: () -> Unit,
    contentPadding: PaddingValues,
) {
    val uriHandler = LocalUriHandler.current
    fun open(url: String) = runCatching { uriHandler.openUri(url) }

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
        ScreenHeader("Apoiar o projeto", onBack = onBack)
        Spacer(Modifier.height(20.dp))

        // Marca + assinatura
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandMark(size = 56.dp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text("JR TECH", style = MaterialTheme.typography.titleLarge, color = TextStrong)
                Text("Guitarrista e developer", style = MaterialTheme.typography.bodyMedium, color = TextBody)
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Este app é gratuito e sempre vai ser. Se ele te ajudou no louvor ou nos estudos, " +
                "você pode contribuir com qualquer valor: é o que me ajuda a manter e melhorar o projeto. " +
                "Muito obrigado.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextBody,
        )

        Spacer(Modifier.height(20.dp))
        RowGroup(rows = tiers.map { tier -> @Composable { TierRow(tier) { open(tier.url) } } })
        Spacer(Modifier.height(12.dp))
        PrimaryButton("Escolher outro valor", Modifier.fillMaxWidth()) { open(FREE_URL) }

        Spacer(Modifier.height(16.dp))
        Text(
            "O pagamento abre no Mercado Pago, com Pix ou cartão. Nada é cobrado dentro do app.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
        )
    }
}

@Composable
private fun TierRow(tier: Tier, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clickable(role = Role.Button, onClickLabel = "Apoiar com ${tier.amount}") { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            tier.amount,
            style = MaterialTheme.typography.titleLarge,
            color = TextStrong,
            modifier = Modifier.width(88.dp),
        )
        Text(tier.note, style = MaterialTheme.typography.bodyLarge, color = TextBody, modifier = Modifier.weight(1f))
        Icon(painterResource(R.drawable.ic_chevron), contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
    }
}
