package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Paleta base — dark "estúdio". Fundo quase-preto neutro, superfícies em
// camadas e um único acento quente (latão) usado com parcimônia. A ideia é
// deixar a informação musical protagonizar, sem brilhos ou gradientes de enfeite.
// ---------------------------------------------------------------------------

val Ink = Color(0xFF0B0C0F)          // fundo principal
val Surface1 = Color(0xFF14161B)     // cartões / superfícies base
val Surface2 = Color(0xFF1C1F26)     // superfícies elevadas
val Surface3 = Color(0xFF262A33)     // chips / estados pressionados
val Hairline = Color(0xFF2C313B)     // linhas divisórias sutis
val HairlineStrong = Color(0xFF3A414D)

val TextStrong = Color(0xFFECEEF1)   // títulos e destaques
val TextBody = Color(0xFFA6ADBA)     // texto corrente
val TextMuted = Color(0xFF8C93A0)    // rótulos e legendas (AA ≥ 4,6:1 em todas as superfícies)

// Acento da marca: latão. Usado SÓ na ação principal de cada tela, na aba ativa
// e na marca. Seleção de chips/abas internas é neutra (Selection), para o acento
// continuar significando "faça isto".
val Brass = Color(0xFFE0A94A)
val BrassDim = Color(0xFF7A5E28)

// Seleção neutra (chip/segmento escolhido): claro sobre escuro, texto Ink.
val Selection = Color(0xFFECEEF1)

// Estrela de favorito (mantida discreta, um pouco mais fria que o latão).
val Favorite = Color(0xFFE7B84B)

// ---------------------------------------------------------------------------
// Cores funcionais da harmonia. Em vez de sete tons neon arbitrários, usamos
// as três funções tonais clássicas — o que também ensina teoria ao músico:
//   Tônica (repouso) · Subdominante (preparação) · Dominante (tensão).
// Tons dessaturados, "joia", que convivem bem no escuro.
// ---------------------------------------------------------------------------

val FuncTonic = Color(0xFF57B892)        // verde: repouso / resolução
val FuncSubdominant = Color(0xFF6B97DF)  // azul: preparação / movimento
val FuncDominant = Color(0xFFE07468)     // vermelho argila: tensão / condução
val FuncNeutral = Color(0xFF8B93A1)      // ardósia: passagem / ambíguo

// ---------------------------------------------------------------------------
// Braço do violão (simulação): cores chapadas de madeira, trastes e cordas.
// Só servem ao desenho do instrumento — as notas continuam com as cores acima.
// ---------------------------------------------------------------------------

val NeckWood = Color(0xFF2B211C)      // escala (jacarandá escuro)
val NeckWoodEdge = Color(0xFF3A2D25)  // borda da escala
val NeckNut = Color(0xFFD8CFBE)       // rastilho (osso)
val FretWire = Color(0xFF8E9197)      // trastes
val StringPlain = Color(0xFFC9CCD1)   // cordas lisas (1ª e 2ª)
val StringWound = Color(0xFFB39466)   // cordas encapadas (3ª a 6ª)
val NeckInlay = Color(0xFF5E4E42)     // marcações de casa
