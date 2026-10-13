# Design: Digital Harmonic Field

O guia visual do app: leitura do projeto, organização das telas, cores,
tipografia, medidas, componentes e regras de uso. Serve para manter o app com a
mesma "cara" a cada tela nova, seja quem for que estiver mexendo no código.

> **Resumo em uma frase:** escuro e sóbrio, um único acento em latão usado só
> na ação principal, e cor só quando ela **significa** alguma coisa (a função
> do acorde).

Versão 2 (redesenho da organização e da interface). Feita seguindo duas
referências de design: *taste-skill* (Leonxlnx) e *anti-slop* (miqdadbadjuber).

---

## 1. Leitura do projeto (Design Read)

| Pergunta | Resposta |
|---|---|
| O que é | Ferramenta nativa Android para o violonista iniciante de igreja: tom, campo harmônico, acordes no braço e "graças" |
| Quem usa | Músico de igreja (Harpa Cristã), no ensaio e **no culto**: em pé, com o violão no colo, pouca luz, pouco tempo para procurar |
| Onde usa | Palco e salão escuros; celular apoiado na estante |
| Tom visual | Oficina calma: escuro neutro, latão discreto, nada de vitrine |

**Mostradores** (de 1 a 10):

| Mostrador | Valor | Por quê |
|---|---|---|
| Energia | 1 | Uso durante o culto: nada pode chamar mais atenção que a música |
| Ritmo | 2 | Blocos regulares e previsíveis; o olho acha as coisas sempre no mesmo lugar |
| Movimento | 1 | Quase sem animação; só o que dá retorno (toque, batida do metrônomo) |

---

## 2. Organização (as 4 abas)

A barra de baixo separa o app **pelo que o músico quer fazer**, não pelo tipo
de ferramenta:

| Aba | Para quê | O que tem |
|---|---|---|
| **Culto** | Na hora de tocar | Detectar o tom (ação principal), repertório do dia (cada hino abre o campo no tom certo), metrônomo, capotraste, apoiar o projeto |
| **Campos** | Consultar tons | Seletor **Tons \| Progressões**: os 24 campos e, no mesmo lugar, as progressões prontas |
| **Violão** | Tudo no braço | Braço do violão (destaque), Dicionário de acordes, Acordes do tom no braço, Harmonizar, Passagens e enfeites |
| **Aprender** | Estudar | **Treinar** no topo (Treino de graus, Decorar o braço) e **Lições** numeradas de 1 a 10, todas fechadas |

Regras da organização:
- Cada aba tem **um foco** visível de cara (Culto: "Detectar o tom"; Violão:
  "Abrir o braço do violão").
- Telas internas abrem **por cima** da aba (com botão Voltar e o voltar do
  Android). O Dicionário abre por cima de qualquer tela, para a de baixo não
  perder o estado.
- Um mesmo destino pode ter mais de uma porta (ex.: "Decorar o braço" no
  Aprender abre o braço já na aba Decorar), mas cada função mora num lugar só.

---

## 3. Cores

Todas ficam em `app/src/main/java/com/example/ui/theme/Color.kt`. **Nunca**
escreva uma cor solta (`Color(0xFF...)`) numa tela: crie ou use um token.

### 3.1 Base (fundo, superfícies e texto)

| Token | Hex | Uso |
|---|---|---|
| `Ink` | `#0B0C0F` | Fundo principal; texto sobre latão e sobre a seleção |
| `Surface1` | `#14161B` | Painéis e grupos de linhas |
| `Surface2` | `#1C1F26` | Chips, botões secundários, controles segmentados |
| `Surface3` | `#262A33` | Estados pressionados, item escolhido em grade de desenhos |
| `Hairline` | `#2C313B` | Bordas finas (1 dp) e divisórias |
| `HairlineStrong` | `#3A414D` | Linhas que precisam aparecer mais (campos de texto, grade dos diagramas) |
| `TextStrong` | `#ECEEF1` | Títulos e valores |
| `TextBody` | `#A6ADBA` | Texto corrido e subtítulos |
| `TextMuted` | `#8C93A0` | Rótulos e legendas |

**Contraste (WCAG AA, mínimo 4,5:1 para texto):** `TextMuted` foi clareado de
`#6B7280` (2,97 a 4,05:1, reprovava) para `#8C93A0` (4,65 a 6,33:1 em todas as
superfícies). `TextBody` e `TextStrong` passam com folga.

### 3.2 Acento e seleção

| Token | Hex | Uso |
|---|---|---|
| `Brass` | `#E0A94A` | **Só** a ação principal da tela (`PrimaryButton`), a aba ativa e o ponto de foco (ex.: nota escolhida no braço, arco de escuta do detector) |
| `Selection` | `#ECEEF1` | Item escolhido em chips, controles segmentados e chaves: fundo claro, texto `Ink` |
| `Favorite` | `#E7B84B` | Só a estrela de favorito |

Por que a seleção é neutra: se o latão marca tanto "escolhido" quanto "faça
isto", a tela fica cheia de dourado e o olho não sabe onde ir. Com a seleção em
branco, o latão aparece uma vez por tela e vira um sinal claro.

### 3.3 Cores das funções (o coração do sistema)

| Token | Hex | Função | Sensação |
|---|---|---|---|
| `FuncTonic` | `#57B892` | **Tônica** (1, 3m, 6m) | verde: repouso, "casa" |
| `FuncSubdominant` | `#6B97DF` | **Subdominante** (2m, 4) | azul: preparação, movimento |
| `FuncDominant` | `#E07468` | **Dominante** (5, 7°) | vermelho argila: tensão, pede para voltar |
| `FuncNeutral` | `#8B93A1` | Passagem / fora do campo | ardósia: neutro |

**Regras:**
- A cor de um acorde vem **sempre** de `HarmonicFunction.color()`. Não invente cor por acorde.
- Dominantes secundárias (2⁷, 3⁷, 6⁷...) usam a cor da **Dominante**.
- Acorde selecionado: a cor da função com alpha 0,14 a 0,18 no fundo e borda
  de 2 dp na cor cheia.
- Acertou/errou (nos treinos) reaproveita Tônica (verde) e Dominante (vermelho).
- As cores das funções **não** são usadas para outras coisas (botões, avisos,
  remover item).

### 3.4 Cores do instrumento (só no braço do violão)

| Token | Hex | Parte |
|---|---|---|
| `NeckWood` | `#2B211C` | Escala (jacarandá escuro) |
| `NeckWoodEdge` | `#3A2D25` | Borda da escala |
| `NeckNut` | `#D8CFBE` | Rastilho (osso) |
| `FretWire` | `#8E9197` | Trastes |
| `StringPlain` | `#C9CCD1` | Cordas lisas (1ª e 2ª) |
| `StringWound` | `#B39466` | Cordas encapadas (3ª a 6ª) |
| `NeckInlay` | `#5E4E42` | Marcações das casas 3, 5, 7, 9, 12 |

Todas as cores são **chapadas**: nenhum gradiente, brilho ou sombra decorativa.

---

## 4. Tipografia

Família única: **Space Grotesk** (`res/font/sg_400` a `sg_700`). Números e
cifras com desenho marcante e fáceis de ler. Estilos em `ui/theme/Type.kt`:

| Estilo | Peso | Tamanho / linha | Uso |
|---|---|---|---|
| `displayLarge` | Bold | 44 / 48 sp | Número do BPM no metrônomo |
| `displaySmall` | Bold | 30 / 34 sp | Cifra em destaque |
| `headlineLarge` | SemiBold | 26 / 30 sp | Tom de cada hino no repertório |
| `headlineMedium` | SemiBold | 22 / 28 sp | Título de tela (`ScreenHeader`) |
| `titleLarge` | SemiBold | 18 / 24 sp | Título de seção (`SectionTitle`) e de painel |
| `titleMedium` | Medium | 16 / 22 sp | Título de linha (`NavRow`), nome do tom |
| `titleSmall` | SemiBold | 14 / 20 sp | Texto do botão principal |
| `bodyLarge` | Regular | 16 / 24 sp | Texto principal das lições |
| `bodyMedium` | Regular | 14 / 20 sp | Texto corrido, subtítulos |
| `bodySmall` | Regular | 12 / 16 sp | Legendas |
| `labelLarge` | Medium | 14 / 18 sp | Botões secundários, chips, rótulos de bloco |
| `labelMedium` | Medium | 12 / 16 sp | Rótulos da barra de abas |

- **Sem rótulos em MAIÚSCULAS espaçadas.** Seções usam frase normal
  (`SectionTitle` ou `SectionLabel`).
- Nenhum texto de interface abaixo de 12 sp.

---

## 5. Medidas

| Item | Valor |
|---|---|
| Margem lateral | **16 dp** (12 dp na tela cheia de progressão) |
| Espaço entre blocos | 12 a 16 dp |
| Área de toque mínima | **44 dp** em tudo que é clicável |
| Botão principal | 52 dp de altura (56 dp na tela cheia) |
| Linha de navegação (`NavRow`) | 64 dp |
| Botão redondo (voltar, −, +) | 44 dp |
| Borda | 1 dp `Hairline` |

**Raios:** 16 dp painéis e grupos; 12 dp botões e controles segmentados; 10 dp
chips; 6 a 8 dp etiquetas.

---

## 6. Componentes

Em `ui/components/Controls.kt` (peças do sistema) e `CommonUi.kt`:

| Componente | Para que serve |
|---|---|
| `ScreenHeader` | Voltar (opcional) + título + uma linha de apoio em `TextBody` + ações à direita |
| `SectionTitle` | Título de seção em frase normal |
| `PrimaryButton` | **A** ação da tela, em latão. No máximo um por tela |
| `SecondaryButton` | Ações secundárias: superfície com contorno, texto claro, ícone opcional |
| `RoundIconButton` | Botão redondo só com ícone (voltar, −, +, ouvir, subir, descer), sempre com descrição para leitor de tela |
| `OptionChip` | Escolha entre opções (nota, tipo de acorde, nível). Escolhido = `Selection` |
| `Segmented` | Alterna visões da mesma tela (Tons \| Progressões, Maior \| Menor, Forma pronta \| Onde estão 1, 3, 5) |
| `NavRow` + `RowGroup` | Lista de destinos: título, explicação curta e seta; agrupados com divisórias |
| `Panel` | Bloco com fundo próprio, só quando o conteúdo precisa ser separado |
| `NotationToggle` | Chave **Cifra \| Grau** (muda o app inteiro) |
| `ChordStrip` | Sequência de acordes que divide a largura e quebra a linha |
| `ChordDiagram`, `GuitarNeck` | Desenho de acorde e braço realista (`GuitarViews.kt`) |
| `FunctionTag` | Etiqueta Tônica / Subdominante / Dominante na cor da função |

### Estados

| Estado | Fundo | Borda | Texto |
|---|---|---|---|
| Normal | `Surface2` | `Hairline` | `TextBody` |
| Escolhido | `Selection` | `Selection` | `Ink` semibold |
| Ação principal | `Brass` | nenhuma | `Ink` |
| Acorde escolhido | cor da função a 14-18% | cor da função, 2 dp | cor da função |

Estados de tela: **vazio** (ex.: repertório sem hinos mostra o que fazer e o
botão "Montar a lista"), **carregando/escutando** (detector com arco e rodadas)
e **erro/sem evidência** (detector: `SEM_VOZ` e `INSUFICIENTE`, sem sugerir tom).

### Ícones

Uma família só: **Phosphor Icons** (licença MIT, ver
`docs/licencas/phosphor-icons-MIT.txt`), convertidos para VectorDrawable em
`res/drawable/ic_*.xml`, todos com a cor vinda de quem usa (`tint`). Nada de
setas de texto ("›", "▶") como enfeite em botões: quando a seta indica
navegação, ela é o ícone `ic_chevron` da `NavRow`.

---

## 7. Texto e tom de voz

- Português simples, frases curtas, falando com o músico ("toque", "veja",
  "escolha").
- Explique **para que serve** antes do termo técnico.
- **Sem travessão** (— ou –) nos textos da interface: use dois-pontos,
  vírgula, ponto ou parênteses. Sequências de acordes usam hífen:
  `2m - 5 - 1`, `I - vi - ii - V - I`.
- Ponto médio "·" no máximo um por linha.
- Notação brasileira de graus: `1 2m 3m 4 5 6m 7°` (menor: `1m 2° 3 4m 5m 6 7`);
  sétimas como `5⁷`, `4⁷M`, `2m⁷`, `7ø`.
- Nada de exclamação em excesso, emoji na interface ou frase de propaganda.

---

## 8. Padrões de tela

- **Abas:** título da aba + um foco (painel com `PrimaryButton`) + grupos de
  `NavRow` para o resto.
- **Telas internas:** `ScreenHeader` com Voltar; a ação principal em latão; o
  resto em `SecondaryButton`/`OptionChip`.
- **Ações perigosas** (remover hino, descartar trecho) ficam longe do toque
  acidental e pedem confirmação ("Toque de novo para remover da lista").
- **Detalhe do campo:** retrato rola; **paisagem** é compacta e **sem rolagem**.
- **Tela cheia de progressão:** grade que ocupa a tela toda, acorde da vez
  destacado na cor da função, "Próximo" em latão e a tela fica acesa.

---

## 9. Ícone e marca

- Arte do autor em `tools/icon/source/icone-original.png`; tudo (ícone
  adaptativo, monocromático, `.webp`, imagem da loja e `brand_mark.png`) sai de
  `tools/icon/make_icon.py`. Não edite os arquivos gerados à mão.
- A versão DEV usa a mesma arte com uma faixa embaixo.

---

## 10. Faça / Não faça

| Faça | Não faça |
|---|---|
| Use os tokens de `Color.kt` | Escrever `Color(0xFF...)` direto na tela |
| Pinte acordes com `HarmonicFunction.color()` | Dar uma cor diferente para cada acorde |
| Latão só na ação principal, na aba ativa e no foco | Latão para seleção, subtítulo ou enfeite |
| Seleção com `Selection` (claro, texto `Ink`) | Vários itens dourados na mesma tela |
| Toque mínimo de 44 dp | Chips e botões baixinhos |
| `ChordStrip` para sequências | `horizontalScroll` / `LazyRow` para acordes |
| Ícones Phosphor com `tint` | Misturar famílias de ícones ou usar "›" e "▶" como texto |
| Frases com dois-pontos, vírgula ou parênteses | Travessão nos textos |
| Teste em retrato **e** paisagem | Deixar o detalhe do campo rolar em paisagem |

---

## 11. Como conferir

```bash
gradle :app:testDebugUnitTest -Proborazzi.test.record=true
# imagens em app/build/shots/ (abas em shots/tabs, navegação em shots/nav)
```

`NavigationTest` abre o app inteiro, toca em cada aba e nas linhas principais
e confere que a tela certa abriu e que o Voltar devolve para a aba.
