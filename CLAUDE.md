# CLAUDE.md

Orientações para o Claude Code trabalhar neste repositório.

## O que é

App Android nativo (**Kotlin + Jetpack Compose + Material 3**) de consulta a
campos harmônicos, voltado a músicos iniciantes de igreja. Tudo offline e em
modo escuro. Mantido por JR TECH.

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

Navegação por abas via estado (sem `navigation-compose`): `MainActivity`
mantém `Tab` e `detailKey`; o detalhe do campo é uma sobreposição de tela cheia.

```
com/example/
├── MainActivity.kt        # Scaffold + NavigationBar (Campos/Progressões/Aprender/Ferramentas)
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
│   └── PitchDetection.kt  # Detecção de altura (autocorrelação/MPM) + ptPitchClass
├── audio/
│   ├── AudioEngine.kt     # Metronome via AudioTrack (sem libs externas)
│   └── KeyListener.kt     # Pré-buffer de 5 s (só memória) + rodadas (RECORD_AUDIO) + SessionTap
├── dev/                   # SÓ versão DEV: DevStore (pasta treino-real), DevRecorder, DevJson
│                          #   (ui/dev: DevLabelForm, DevPanelScreen)
└── ui/
    ├── theme/             # Color.kt (tokens), Type.kt (Space Grotesk), Theme.kt
    ├── components/         # CommonUi.kt (SectionLabel, AppCard, FunctionTag), ChordStrip.kt
    └── screens/            # Fields, FieldDetail, Progressions, ProgressionStage (tela cheia),
                            # Learn, Tools, KeyFinder, Donation
```

## Convenções e princípios

- **Design sóbrio (anti-slop):** dark neutro, um único acento (latão `Brass`),
  sem brilhos/gradientes decorativos nem linguagem de marketing na UI.
- **Cor com significado:** todo acorde é pintado pela função tonal
  (`HarmonicFunction.color()` — Tônica/Subdominante/Dominante). Não introduza
  cores por acorde fora desse sistema.
- **Público iniciante:** textos claros e diretos, em português. A aba Aprender
  é o guia; explique conceitos como se fosse para quem toca há pouco tempo.
- **Detalhe do campo** tem dois layouts: retrato (rolável) e paisagem
  (compacto, **sem rolagem**). Ao mexer no landscape, garanta que tudo caiba.
- **Sem rolagem lateral:** sequências de acordes usam `ChordStrip` (divide a
  largura e quebra linha). Não use `horizontalScroll`/`LazyRow` para acordes.
- O afinador foi removido a pedido do autor; não reintroduza.
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
- **Entrada:** `InputLeveler` (ganho automático até +30 dB + limitador suave)
  antes dos filtros — no culto o celular capta baixo. As notas sustentadas
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
