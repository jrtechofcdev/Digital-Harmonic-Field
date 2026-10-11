# Design — Digital Harmonic Field

O guia visual do app: princípios, cores, tipografia, medidas, componentes e
regras de uso. Serve para manter o app com a mesma "cara" a cada tela nova,
seja quem for que estiver mexendo no código.

> **Resumo em uma frase:** escuro e sóbrio, um único acento em latão, e cor
> só quando ela **significa** alguma coisa (a função do acorde).

---

## 1. Princípios

| Princípio | O que quer dizer na prática |
|---|---|
| **Feito para o culto** | O app é usado em palco e igreja com pouca luz. Por isso é **sempre escuro**, com textos grandes nas telas de tocar e a tela acesa enquanto toca. |
| **Sóbrio (anti-"cara de IA")** | Nada de brilho, neon, gradiente decorativo, sombra exagerada ou frases de marketing. A informação musical é a protagonista. |
| **Cor com significado** | Cada acorde recebe a cor da sua **função tonal**: Tônica, Subdominante ou Dominante. A cor ensina teoria enquanto o músico usa. |
| **Um só acento** | O **latão** (`Brass`) marca ação e foco: botão principal, item escolhido, chave ligada. Não é usado como enfeite. |
| **Para iniciante** | Texto em português claro e direto, explicando como para quem toca há pouco tempo. Termos técnicos sempre acompanhados do "para que serve". |
| **Sem rolar para o lado** | Sequências de acordes dividem a largura da tela e quebram a linha. Nunca use rolagem lateral para acordes. |

---

## 2. Cores

Todas ficam em `app/src/main/java/com/example/ui/theme/Color.kt`. **Nunca**
escreva uma cor solta (`Color(0xFF...)`) numa tela: crie ou use um token.

### 2.1 Base (fundo, superfícies e texto)

| Token | Hex | Uso |
|---|---|---|
| `Ink` | `#0B0C0F` | Fundo principal; texto sobre botões em latão |
| `Surface1` | `#14161B` | Cartões e superfícies base |
| `Surface2` | `#1C1F26` | Superfícies elevadas, chips e botões secundários |
| `Surface3` | `#262A33` | Chips, estados pressionados, notas "apagadas" |
| `Hairline` | `#2C313B` | Bordas finas (1 dp) e divisórias |
| `HairlineStrong` | `#3A414D` | Linhas que precisam aparecer mais (grade dos diagramas) |
| `TextStrong` | `#ECEEF1` | Títulos e destaques |
| `TextBody` | `#A6ADBA` | Texto corrido |
| `TextMuted` | `#6B7280` | Rótulos, legendas e dicas |

### 2.2 Acento da marca

| Token | Hex | Uso |
|---|---|---|
| `Brass` | `#E0A94A` | Ação principal, item selecionado, chave ligada, destaque de foco |
| `BrassDim` | `#7A5E28` | Variação apagada do acento (reservada; hoje sem uso) |
| `Favorite` | `#E7B84B` | Só a estrela de favorito |

### 2.3 Cores das funções (o coração do sistema)

| Token | Hex | Função | Sensação |
|---|---|---|---|
| `FuncTonic` | `#57B892` | **Tônica** (1, 3m, 6m) | verde: repouso, "casa" |
| `FuncSubdominant` | `#5B8AD6` | **Subdominante** (2m, 4) | azul: preparação, movimento |
| `FuncDominant` | `#D9695E` | **Dominante** (5, 7°) | vermelho argila: tensão, pede para voltar |
| `FuncNeutral` | `#8B93A1` | Passagem / fora do campo | ardósia: neutro |

**Regras:**
- A cor de um acorde vem **sempre** de `HarmonicFunction.color()`. Não invente cor por acorde.
- Dominantes secundárias (2⁷, 3⁷, 6⁷…) usam a cor da **Dominante** (é assim no
  laboratório do violão).
- Fundo de item selecionado: a cor da função com **alpha 0,14–0,18** e borda
  de 2 dp na cor cheia.
- Acertou/errou (nos treinos) reaproveita Tônica (verde) e Dominante (vermelho).

### 2.4 Cores do instrumento (só no braço do violão)

Usadas **apenas** para desenhar o violão. As notas em cima dele seguem as cores
das funções.

| Token | Hex | Parte |
|---|---|---|
| `NeckWood` | `#2B211C` | Escala (jacarandá escuro) |
| `NeckWoodEdge` | `#3A2D25` | Borda da escala |
| `NeckNut` | `#D8CFBE` | Rastilho (osso) |
| `FretWire` | `#8E9197` | Trastes |
| `StringPlain` | `#C9CCD1` | Cordas lisas (1ª e 2ª) |
| `StringWound` | `#B39466` | Cordas encapadas (3ª a 6ª) |
| `NeckInlay` | `#5E4E42` | Marcações das casas 3, 5, 7, 9, 12 |

Todas as cores são **chapadas**: nenhum gradiente.

---

## 3. Tipografia

Família única: **Space Grotesk** (arquivos em `res/font/sg_400…sg_700`). Os
números e as cifras têm desenho marcante, o que dá identidade ao app e deixa as
cifras fáceis de ler. Estilos em `ui/theme/Type.kt`:

| Estilo | Peso | Tamanho / linha | Uso |
|---|---|---|---|
| `displaySmall` | Bold | 30 / 34 sp, −0,5 | Cifra em destaque (ex.: acorde escolhido) |
| `headlineMedium` | SemiBold | 22 / 26 sp, −0,3 | Título de tela |
| `titleLarge` | SemiBold | 18 / 22 sp | Título de cartão, rótulo de acorde |
| `titleMedium` | Medium | 15 / 20 sp | Subtítulos, nome do tom |
| `bodyLarge` | Regular | 15 / 22 sp | Texto principal das lições |
| `bodyMedium` | Regular | 13 / 18 sp | Texto corrido e explicações |
| `labelLarge` | Medium | 13 / 16 sp | Botões, chips |
| `labelSmall` | SemiBold | 10 / 13 sp, +1,2 | Rótulos de seção (MAIÚSCULAS, espaçadas) |

Na tela cheia de progressão, o tamanho da cifra é **calculado pelo espaço
de cada bloco**, para ocupar o máximo possível.

---

## 4. Medidas

| Item | Valor |
|---|---|
| Margem lateral das telas | **16 dp** (12 dp na tela cheia de progressão) |
| Espaço entre blocos | **12 dp** (8 dp dentro de cartões) |
| Espaço entre chips | 5–8 dp |
| Borda | **1 dp** `Hairline`; 2–3 dp na cor da função quando selecionado |
| Altura de chip / botão pequeno | 38–40 dp |
| Altura de botão principal | 56 dp |
| Botão redondo (voltar, − / +) | 40 dp |

**Raios de canto** (do mais usado para o menos):

| Raio | Onde |
|---|---|
| 14 dp | Cartões (`AppCard`, `LabCard`) |
| 12 dp | Botões de resposta, seletores segmentados, miniaturas de acorde |
| 10 dp | Chips e chaves (`LabChip`, `NotationToggle`) |
| 16 dp | Blocos grandes da tela cheia |
| 6–8 dp | Etiquetas pequenas (`FunctionTag`) |

---

## 5. Componentes

Ficam em `ui/components/` (e alguns internos em `ui/screens/`).

| Componente | Arquivo | Para que serve |
|---|---|---|
| `SectionLabel` | `CommonUi.kt` | Rótulo de seção em maiúsculas, discreto |
| `AppCard` | `CommonUi.kt` | Cartão padrão: `Surface1` + borda `Hairline` + raio 14 |
| `FunctionTag` | `CommonUi.kt` | Etiqueta com ponto colorido: Tônica / Subdominante / Dominante |
| `AccentBar` | `CommonUi.kt` | Barrinha vertical na cor da função ao lado de um item |
| `BrandMark` | `CommonUi.kt` | A marca (violão) dentro do app |
| `ChordStrip` | `ChordStrip.kt` | Sequência de acordes que divide a largura e quebra a linha |
| `NotationToggle` | `Notation.kt` | Chave **Cifra \| Grau** (muda o app inteiro) |
| `ChordDiagram` | `GuitarViews.kt` | Desenho de acorde no estilo das revistinhas de cifra |
| `GuitarNeck` | `GuitarViews.kt` | Braço de violão realista, com zoom, toque e pestana |
| `LabChip`, `LabCard`, `KeyPicker` | `GuitarScreen.kt` | Chip, cartão e seletor de tom do laboratório do violão |

### Estados de um chip/botão

| Estado | Fundo | Borda | Texto |
|---|---|---|---|
| Normal | `Surface2` | `Hairline` | `TextBody` |
| Selecionado / ação principal | `Brass` | `Brass` | `Ink` (negrito) |
| Acorde selecionado | cor da função a 18% | cor da função, 2 dp | cor da função |

### Notas no braço do violão

| Desenho | Significa |
|---|---|
| Bolinha **cheia** | Nota de base do acorde (1, 3, 5) |
| **Anel** (só contorno) | Nota de cor (7, 7M, 9, 6, 4) |
| Contorno **branco** | Fundamental (o "1") |
| **×** acima da corda | Não tocar essa corda |
| Barra larga | Pestana |

---

## 6. Padrões de tela

- **Cabeçalho:** botão redondo de voltar (40 dp) + título `headlineMedium` +
  subtítulo em `Brass` (`labelLarge`).
- **Abas internas** (ex.: braço do violão): uma barra segmentada em `Surface2`;
  a aba ativa fica em `Brass`.
- **Detalhe do campo:** dois layouts. Em **retrato** a tela rola; em
  **paisagem** é compacta e **sem rolagem** (tudo precisa caber).
- **Tela cheia de progressão:** grade que ocupa a tela toda, acorde da vez
  destacado, botões grandes em baixo e a tela fica sempre acesa.
- **Modo Grau:** todo rótulo de acorde passa por `chordLabels(índice, cifra)`.
  Na Cifra fica "C" grande e "4" pequeno; no Grau, o contrário.

---

## 7. Ícone e marca

- **Desenho (arte do autor):** o braço do violão visto de frente, com rastilho,
  6 cordas em latão e trastes, e as notas de dois acordes nas cores **Tônica
  (verde), Subdominante (azul) e Dominante (vermelho)**. Em volta, um **anel em
  latão** aberto, com traços nas mesmas cores. Fundo escuro `#0C0D10`.
- **Leitura:** quem olha vê "violão + acordes"; as cores ligam o ícone ao
  campo harmônico e ao resto do app.
- **Formatos:** ícone adaptativo (Android 8+), temático monocromático
  (Android 13+, só as partes claras: cordas, notas e anel), `.webp` para
  Android 7, imagem de 512 px para loja e a marca usada dentro do app
  (`brand_mark.png`).
- **Versão DEV:** a mesma arte com uma faixa vermelho-argila embaixo.
- **Fonte única:** a arte original fica em `tools/icon/source/icone-original.png`
  e tudo sai de `tools/icon/make_icon.py`. Não edite os arquivos gerados à mão;
  para trocar o ícone, substitua a imagem e rode o script.

---

## 8. Texto e tom de voz

- Português simples, frases curtas, falando com o músico ("toque", "veja",
  "escolha").
- Explique **para que serve** antes do termo técnico: "o 5 pede para voltar
  ao 1" antes de "dominante".
- Notação brasileira de graus: `1 2m 3m 4 5 6m 7°` (menor: `1m 2° 3 4m 5m 6 7`);
  sétimas como `5⁷`, `4⁷M`, `2m⁷`, `7ø`.
- Nada de exclamação em excesso, emoji na interface ou "frases de propaganda".

---

## 9. Faça / Não faça

| ✅ Faça | ❌ Não faça |
|---|---|
| Use os tokens de `Color.kt` | Escrever `Color(0xFF…)` direto na tela |
| Pinte acordes com `HarmonicFunction.color()` | Dar uma cor diferente para cada acorde |
| Use `Brass` para ação e seleção | Usar latão como decoração |
| Use `ChordStrip` para sequências | Usar `horizontalScroll` / `LazyRow` para acordes |
| Teste a tela em retrato **e** paisagem | Deixar o detalhe do campo rolar em paisagem |
| Cores chapadas, borda de 1 dp | Gradientes, brilhos, sombras fortes |
| Formas de acorde de `Guitar.shape` / `Voicings.generate` | Desenhar forma de acorde à mão |
| Textos em português claro | Jargão sem explicação |

---

## 10. Como conferir uma tela nova

As capturas automáticas (Roborazzi) geram imagens das telas num celular
simulado de 393 dp:

```bash
gradle :app:testDebugUnitTest -Proborazzi.test.record=true
# imagens em app/build/shots/
```

Antes de enviar uma tela nova, confira se ela está escura e sóbria, se a cor
aparece só onde significa algo, se nada rola para o lado e se tudo cabe em
paisagem.
