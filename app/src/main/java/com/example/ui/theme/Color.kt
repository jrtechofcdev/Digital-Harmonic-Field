package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Duas paletas com os MESMOS papéis (ver docs/DESIGN.md):
//
//  - Escura "estúdio": fundo quase preto, superfícies em camadas e um único
//    acento quente (latão). Para palco e igreja com pouca luz.
//  - Clara "abertura": as cores do final do vídeo de abertura (azul profundo e
//    bege quente) bem suavizadas no fundo, superfícies de vidro fosco e texto
//    escuro. O acento vira um latão escurecido (o azul já significa
//    Subdominante nos acordes, então não pode ser cor de botão).
//
// As telas usam os nomes abaixo (Ink, Surface1, TextStrong...). Eles leem a
// paleta atual, então trocar o tema troca o app inteiro na hora.
// ---------------------------------------------------------------------------

@Immutable
data class AppPalette(
    val isLight: Boolean,
    val ink: Color,              // fundo da tela; texto sobre o acento e sobre a seleção
    val surface1: Color,         // painéis e grupos
    val surface2: Color,         // chips, botões secundários
    val surface3: Color,         // pressionado / escolhido em grades
    val hairline: Color,         // bordas finas e divisórias
    val hairlineStrong: Color,   // bordas que precisam aparecer mais
    val textStrong: Color,       // títulos e valores
    val textBody: Color,         // texto corrido
    val textMuted: Color,        // rótulos e legendas (AA ≥ 4,5:1 em todas as superfícies)
    val brass: Color,            // acento: ação principal, aba ativa, ponto de foco
    val brassDim: Color,
    val selection: Color,        // item escolhido (chip/segmento); texto em cima = ink
    val favorite: Color,         // estrela de favorito
    val funcTonic: Color,        // Tônica: repouso
    val funcSubdominant: Color,  // Subdominante: preparação
    val funcDominant: Color,     // Dominante: tensão
    val funcNeutral: Color,      // passagem / fora do campo
)

val DarkPalette = AppPalette(
    isLight = false,
    ink = Color(0xFF0B0C0F),
    surface1 = Color(0xFF14161B),
    surface2 = Color(0xFF1C1F26),
    surface3 = Color(0xFF262A33),
    hairline = Color(0xFF2C313B),
    hairlineStrong = Color(0xFF3A414D),
    textStrong = Color(0xFFECEEF1),
    textBody = Color(0xFFA6ADBA),
    textMuted = Color(0xFF8C93A0),
    brass = Color(0xFFE0A94A),
    brassDim = Color(0xFF7A5E28),
    selection = Color(0xFFECEEF1),
    favorite = Color(0xFFE7B84B),
    funcTonic = Color(0xFF57B892),
    funcSubdominant = Color(0xFF6B97DF),
    funcDominant = Color(0xFFE07468),
    funcNeutral = Color(0xFF8B93A1),
)

val LightPalette = AppPalette(
    isLight = true,
    ink = Color(0xFFF6F4F0),
    surface1 = Color(0xE6FFFFFF),        // vidro fosco: branco a 90% sobre o fundo da abertura
    surface2 = Color(0xFFE8EDF5),
    surface3 = Color(0xFFDCE3EE),
    hairline = Color(0xFFD3DAE5),
    hairlineStrong = Color(0xFFB4BFCF),
    textStrong = Color(0xFF121826),
    textBody = Color(0xFF384254),
    textMuted = Color(0xFF566073),
    brass = Color(0xFF87591B),
    brassDim = Color(0xFFC9A877),
    selection = Color(0xFF1B2433),
    favorite = Color(0xFF9A6A12),
    funcTonic = Color(0xFF186B4C),
    funcSubdominant = Color(0xFF2B5DB5),
    funcDominant = Color(0xFFA83A31),
    funcNeutral = Color(0xFF566073),
)

/** A paleta em uso. Mudar aqui recompõe o app inteiro. */
object Palette {
    var current by mutableStateOf(DarkPalette)
}

val Ink: Color get() = Palette.current.ink
val Surface1: Color get() = Palette.current.surface1
val Surface2: Color get() = Palette.current.surface2
val Surface3: Color get() = Palette.current.surface3
val Hairline: Color get() = Palette.current.hairline
val HairlineStrong: Color get() = Palette.current.hairlineStrong

val TextStrong: Color get() = Palette.current.textStrong
val TextBody: Color get() = Palette.current.textBody
val TextMuted: Color get() = Palette.current.textMuted

// Acento da marca. Usado SÓ na ação principal de cada tela, na aba ativa e no
// ponto de foco. Seleção de chips/abas internas é neutra (Selection).
val Brass: Color get() = Palette.current.brass
val BrassDim: Color get() = Palette.current.brassDim
val Selection: Color get() = Palette.current.selection
val Favorite: Color get() = Palette.current.favorite

// Cores funcionais da harmonia: Tônica (repouso), Subdominante (preparação) e
// Dominante (tensão). Ensinam teoria enquanto o músico usa.
val FuncTonic: Color get() = Palette.current.funcTonic
val FuncSubdominant: Color get() = Palette.current.funcSubdominant
val FuncDominant: Color get() = Palette.current.funcDominant
val FuncNeutral: Color get() = Palette.current.funcNeutral

/** true quando o tema claro está ativo. */
val IsLightTheme: Boolean get() = Palette.current.isLight

// ---------------------------------------------------------------------------
// Braço do violão (simulação): madeira, trastes e cordas. Iguais nos dois temas
// (é o instrumento de verdade); as notas usam as cores das funções acima.
// ---------------------------------------------------------------------------

val NeckWood = Color(0xFF2B211C)      // escala (jacarandá escuro)
val NeckWoodEdge = Color(0xFF3A2D25)  // borda da escala
val NeckNut = Color(0xFFD8CFBE)       // rastilho (osso)
val FretWire = Color(0xFF8E9197)      // trastes
val StringPlain = Color(0xFFC9CCD1)   // cordas lisas (1ª e 2ª)
val StringWound = Color(0xFFB39466)   // cordas encapadas (3ª a 6ª)
val NeckInlay = Color(0xFF5E4E42)     // marcações de casa
