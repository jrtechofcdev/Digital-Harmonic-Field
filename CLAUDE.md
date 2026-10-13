# CLAUDE.md

Orientações para o Claude Code trabalhar neste repositório.

## O que é

App Android nativo (**Kotlin + Jetpack Compose + Material 3**) de consulta a
campos harmônicos, voltado a músicos iniciantes de igreja. Tudo offline, com
tema escuro (padrão) e claro. Mantido por José Renato (JR TECH OFC).

## Build

O projeto usa **AGP 9.1.1**, que exige **Gradle 9.3.1+** e o **Android SDK
com platform android-36.1 + build-tools 36.1.0**. Não há Gradle wrapper
versionado; use um `gradle` do sistema na versão correta.

```bash
# APK de depuração
gradle :app:assembleDebug          # saída em app/build/outputs/apk/debug/

# Testes de lógica musical (Robolectric)
gradle :app:testDebugUnitTest

# Versão DEV de treino (grava trechos rotulados; instala ao lado do app)
gradle :app:assembleDev            # saída em app/build/outputs/apk/dev/
```

`local.properties` (com `sdk.dir=...`), `.env` e `debug.keystore` são gerados
localmente e ficam fora do versionamento. O `debug.keystore` é reconstruído a
partir de `debug.keystore.base64`.

## Arquitetura

Navegação por estado (sem `navigation-compose`). `AppRoot` faz a abertura
(vídeo `res/raw/abertura.mp4` → `BrandScreen` da JR TECH OFC → app; cada etapa
desligável nos Ajustes). `HarmonicApp` tem 5 abas pelo que o músico quer fazer:
**Culto** (detectar tom, repertório, metrônomo, capotraste), **Campos**
(Tons | Progressões), **Violão** (braço, dicionário, harmonizar, passagens),
**Aprender** (Treinar + Lições 1 a 10) e **Ajustes** (tudo o que é
configurável, Sobre e legal). Telas internas são uma **pilha** (`Dest` +
`push/pop/replace`): entram deslizando, a de baixo continua composta (mantém
o estado) mas sem toques (`blockTouchesBelow`) nem semântica. Ver `docs/DESIGN.md`.

```
com/example/
├── MainActivity.kt        # AppRoot (abertura) + HarmonicApp (5 abas + pilha de telas) + troca de ícone no onStop
├── AppInfo.kt             # Autor, marca, ano, contato (usado na abertura, Sobre e textos legais)
├── settings/AppSettings.kt # Todas as preferências (tema, ícone, abertura, texto, animação...) + IconSwitcher
├── legal/LegalTexts.kt    # Privacidade, termos, permissões, licenças (espelho em docs/legal/)
├── HarmonicData.kt        # Os 24 campos (dados brutos) + HarmonicDatabase
├── music/
│   ├── MusicTheory.kt     # HarmonicFunction (T/SD/D), transposição, formação de acordes
│   ├── Progressions.kt    # ProgressionLibrary + ProgressionGroup (Harpa em destaque)
│   ├── ChordAnalysis.kt   # Base espectral: FFT, pitchClassOfFrequency, chromagram
│   ├── VoiceFocus.kt      # Foco na voz (120–1500 Hz) + canal do baixo (35–160 Hz,
│   │                      #   sem zumbido 60/120/180 Hz) + subtração espectral
│   ├── KeyDetection.kt    # Detector de tom: KeyDetector (streaming) → KeyResult;
│   │                      #   KeyStopRule (rodadas de 5 s)
│   ├── KeyModel.kt        # Modelo treinado (pesos em resources/com/example/music/key_model.bin)
│   ├── KeyTrainer.kt      # Treinador leve (ajuste seguro da última camada + validação cruzada)
│   ├── Degrees.kt         # Graus por número (degreeLabel), Chord/parseCipher, DegreeTheory (estendidos)
│   ├── DegreeQuiz.kt      # Perguntas do treino de graus (ouvido/cifra)
│   ├── Guitar.kt          # Formas de acorde no violão (abertas + pestana E/A), testadas nota a nota
│   ├── Voicings.kt        # ChordType (9, 7M, sus…), gerador de variações, Harmonize, NeckQuiz
│   ├── ChordShapes.kt     # Dicionário: moldes CAGED (common), todas as formas (all), howTo (dedos)
│   ├── Passages.kt        # Passagens/enfeites por momento (PassageMove), escritas em graus
│   ├── SecondaryDominants.kt # "5 de cada grau": dominante, nota de fora, 3ª sobe, 7ª desce, exemplo
│   └── PitchDetection.kt  # Detecção de altura (autocorrelação/MPM) + ptPitchClass
├── audio/
│   ├── AudioEngine.kt     # Metronome e ChordPlayer via AudioTrack (sem libs externas)
│   └── KeyListener.kt     # Pré-buffer de 5 s (só memória) + rodadas (RECORD_AUDIO) + SessionTap
├── data/Repertoire.kt     # Repertório do culto (SharedPreferences + JSON, só no aparelho)
├── dev/                   # SÓ versão DEV: DevStore (pasta treino-real), DevRecorder, DevJson
│                          #   (ui/dev: DevLabelForm, DevPanelScreen)
└── ui/
    ├── theme/             # Color.kt (AppPalette escura/clara; Ink, Surface1... leem Palette.current),
    │                      #   Type.kt (Space Grotesk), Theme.kt (HarmonicTheme + AppBackground)
    ├── components/         # Controls.kt (ScreenHeader, PrimaryButton, SecondaryButton, OptionChip,
    │                       #   Segmented, NavRow/RowGroup, Panel, RoundIconButton), CommonUi.kt,
    │                       #   ChordStrip.kt, Notation.kt (Cifra|Grau), GuitarViews.kt, Motion.kt (pressClick)
    └── screens/            # Home (aba Culto), Fields (CamposScreen), GuitarHub (aba Violão), Learn,
                            # FieldDetail, Progressions, ProgressionStage (tela cheia), DegreeLessons,
                            # DegreeTrainer, Guitar (+ GuitarChordsTab, GuitarStudyTab), ChordDictionary,
                            # Passages, Repertoire, PlayTools (Metrônomo, Capotraste), KeyFinder, Donation,
                            # OpeningScreens (IntroVideo, BrandScreen), Settings, AboutScreens (Sobre, Legal)
```

## Convenções e princípios

- **Design sóbrio (anti-slop):** dark neutro, um único acento (latão `Brass`),
  sem brilhos/gradientes decorativos nem linguagem de marketing na UI.
- **Latão só no foco:** `Brass` é para a ação principal da tela
  (`PrimaryButton`, no máximo um por tela), a aba ativa e o ponto de foco
  (nota escolhida no braço). Item escolhido usa `Selection` (claro, texto
  `Ink`) via `OptionChip`/`Segmented`. Subtítulos em `TextBody`, nunca latão.
- **Peças prontas:** monte telas com `Controls.kt` (não crie botão/chip local).
  Toque mínimo de 44 dp; ícones só do Phosphor (`res/drawable/ic_*.xml`, com
  `tint`); sem "›"/"▶" como texto em botões.
- **Texto:** sem travessão (— ou –) na UI; use dois-pontos, vírgula ou
  parênteses. Sequências com hífen (`2m - 5 - 1`). Sem rótulos em MAIÚSCULAS.
  `TextMuted` é o texto mais fraco permitido (contraste AA).
- **Cor com significado:** todo acorde é pintado pela função tonal
  (`HarmonicFunction.color()` — Tônica/Subdominante/Dominante). Não introduza
  cores por acorde fora desse sistema.
- **Público iniciante:** textos claros e diretos, em português. A aba Aprender
  é o guia; explique conceitos como se fosse para quem toca há pouco tempo.
- **Dominantes secundárias:** chave `DominantSettings` no detalhe do campo
  (`DominantsUi.kt`); cálculo só em `SecondaryDominants` (a lição Graus a fundo
  usa o mesmo). Cor = Dominante. Guia em `docs/dominantes-secundarias.md`.
- **Detalhe do campo** tem dois layouts: retrato (rolável) e paisagem
  (compacto, **sem rolagem**). Ao mexer no landscape, garanta que tudo caiba.
- **Sem rolagem lateral:** sequências de acordes usam `ChordStrip` (divide a
  largura e quebra linha). Não use `horizontalScroll`/`LazyRow` para acordes.
- O afinador foi removido a pedido do autor; não reintroduza.
- **Modo Grau:** todo rótulo de acorde do campo passa por `chordLabels(índice,
  cifra)` (Notation.kt) — Cifra: ("C","4"); Grau: ("4","C"). Sequências usam
  `degreeSequence`. Notação brasileira: 1 2m 3m 4 5 6m 7° (menor: 1m 2° 3 4m 5m 6 7).
- **Violão:** formas vêm de `Guitar.shape` e `Voicings.generate` (nunca desenhe
  forma à mão). Os testes conferem nota a nota: só notas do acorde, notas
  obrigatórias presentes, baixo certo, ≤4 dedos (pestana conta 1), abertura ≤4
  casas. Diagramas e o braço realista (`GuitarNeck`, com zoom e toque) ficam em
  `GuitarViews.kt`; as cores da madeira/cordas em `Color.kt` (só para o
  instrumento — as notas seguem a cor da função). Bolinha cheia = 1/3/5; anel =
  nota de cor. Método de estudo em `docs/braco-do-violao.md`.
- **Dicionário:** "mais usadas" = moldes CAGED em `ChordShapes.templates`
  (offsets relativos à casa da fundamental), sempre validados por
  `Voicings.evaluate`; molde que não cabe na mão num tom simplesmente não
  aparece. Pestana só com 5+ casas presas (o Ré/Lá abertos não têm pestana). Os
  dedos vêm de `Voicing.fingering` (um dedo por casa) e o texto de `howTo`.
- **Passagens:** escreva sempre em graus (`St(rótulo, semitons da tônica, tipo,
  função, baixo)`), nunca em cifra. "Fora do campo" é calculado (no menor, a
  sensível do 5 maior conta como dentro). Toda passagem precisa ter forma no
  violão em todos os tons (`PassagesTest`).
- **Navegação:** toda tela interna entra na pilha de `HarmonicApp`
  (`push(Dest.X)`); voltar = `pop()`, e a tela de baixo não perde o estado.
- **Ícone/marca:** logo do autor (palheta iridescente com "DHF") em
  `tools/icon/source/logo-dhf.png`. `tools/icon/make_icon.py` só recorta,
  centraliza na zona segura (33 dp) e converte o preto em transparência; gera
  foreground/monochrome (mipmap PNG), webp legados, `ic_launcher-playstore.png`,
  `drawable-nodpi/brand_mark.png` e os fundos (normal e DEV com faixa). Não
  edite esses arquivos à mão nem redesenhe o logo: troque a arte e rode o
  script. `BrandMark` usa `R.drawable.brand_mark`.
- **Tema claro/escuro:** as cores são a paleta atual (`Palette.current`,
  trocada por `AppSettings.applyTheme`). Nunca use cor fixa numa tela: use os
  nomes de `Color.kt`, que valem nos dois temas. Telas de tela cheia ficam sobre
  `AppBackground` (claro = gradiente azul/bege + círculos da abertura). No claro,
  o acento é latão escurecido (azul já é Subdominante). Confira contraste AA.
- **Ícone trocável:** `activity-alias` `.IconDark` (padrão) e `.IconLight` no
  manifesto; `IconSwitcher.apply` no `onStop` (liga o novo antes de desligar o
  antigo; nunca os dois desligados). Ícone claro = quadro final da abertura
  (`tools/icon/source/logo-dhf-claro.png`), gerado pelo mesmo `make_icon.py`.
- **JR TECH OFC:** logos do autor em `tools/brand/source/`; `make_brand.py` só
  recorta para `drawable-nodpi/jrtech_*.png`. Não redesenhe.
- **Animação:** use `pressClick` (escala ao tocar) e `Motion.ms(...)` nas
  durações: o ajuste "Menos animação" encurta tudo.
- **Legal:** textos em `legal/LegalTexts.kt` e `docs/legal/*.md` (mantenha os
  dois iguais e a data `LEGAL_UPDATED`). Componente novo de terceiros entra em
  `thirdParties` com o texto da licença em `res/raw/`.
- **Cores** ficam em `ui/theme/Color.kt`; **tons/nomes** vêm de
  `HarmonicDatabase` (use `ptNameByCipher`, não recalcule o mapa).

## Detector de tom

`KeyDetector` é puro (sem Android) e roda igual no app, nos testes e na bancada
(`tools/key-model`, ver `docs/detector-de-tom.md`). Regra de ouro: **nunca
sugerir tom sem evidência** — sem notas sustentadas o status é `SEM_VOZ`; notas
incoerentes dão `INSUFICIENTE` e a lista de candidatos fica vazia.

- **Modelo:** as chances vêm do `KeyModel` (rede pequena treinada). As features
  de `KeyModel.features` espelham `tools/key-model/feats.py` — mudou um, mude o
  outro, retreine e confira com `ModelCheck` (Kotlin = Python).
- **Status:** ALTA só com ≥9,5 s e chance ≥0,70 (calibrado na bancada: 93–95%
  de acerto); a 1ª rodada nunca crava. Ao mexer em limiares, meça com
  `runbench.sh` + `score.py` e rode os testes de ruído/conversa/cromático.
- **Entrada:** `InputLeveler` antes dos filtros: ganho automático (até +30 dB)
  **ou fader manual** de sensibilidade (−24…+30 dB, `KeyListener.manualGainDb`,
  salvo em `detector_prefs`), sempre com limitador suave (teto 0,98). As notas sustentadas
  aceitam ±0,8 semitom (`NOTE_TOLERANCE`) e clareza ≥0,60; a trava de escala é
  75% (`MIN_SCALE_FIT`) — abaixo disso notas cromáticas passam a ganhar tom. Valores medidos no material real (docs/detector-de-tom.md).
- **Dois canais:** voz (120–1500 Hz) e baixo (35–160 Hz). O baixo só conta com
  energia de instrumento (≥20% da voz); vozes graves e zumbido não viram baixo.
- **Rodadas:** `KeyStopRule` (5 s cada, até 15 s; para sozinho com ALTA estável).
  `KeyListener` permite parar e usar ou pedir mais 5 s no mesmo hino.
- **Pré-buffer:** com a tela aberta, só os últimos 5 s ficam em memória (buffer
  circular). Nunca grave em arquivo nem envie áudio; ao sair (`close`) apaga.
  **Única exceção: a variante `dev`** (`gradle :app:assembleDev`, ID `.dev`),
  que liga `BuildConfig.DEV_TOOLS` e instala `DevStore.recorder` como
  `KeyListener.tap` para gravar trechos de treino (ver `docs/treino-real.md`).
  Todo código que grava deve checar `DevStore.enabled`; no app normal o `tap`
  fica null.
- Treino: sintético + "sujeira" do culto (`augment.py`) + hinos reais rotulados
  na versão DEV (`train_final.py --real`). Meça sempre o acerto real com um
  modelo que NÃO viu aqueles hinos.

## Ao adicionar tons/acordes

Os dados dos 24 campos estão em `HarmonicData.kt`. `getField()` anexa a função
tonal por posição via `functionForDegree(index, isMinor)`. Cubra o novo dado
com um teste em `MusicTheoryTest.kt`.
