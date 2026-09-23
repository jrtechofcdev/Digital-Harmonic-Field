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
```

`local.properties` (com `sdk.dir=...`), `.env` e `debug.keystore` são gerados
localmente e ficam fora do versionamento. O `debug.keystore` é reconstruído a
partir de `debug.keystore.base64`.

## Arquitetura

Navegação por abas via estado (sem `navigation-compose`): `MainActivity`
mantém `Tab` e `detailKey`; o detalhe do campo é uma sobreposição de tela cheia.

```
com/example/
├── MainActivity.kt        # Scaffold + NavigationBar (Campos/Ouvir/Progressões/Aprender/Ferramentas)
├── HarmonicData.kt        # Os 24 campos (dados brutos) + HarmonicDatabase
├── music/
│   ├── MusicTheory.kt     # HarmonicFunction (T/SD/D), transposição, formação de acordes
│   ├── Progressions.kt    # ProgressionLibrary (inclui a Progressão da Harpa)
│   ├── ChordAnalysis.kt   # FFT/chroma/detecção de acorde e tom — puro e testável
│   ├── PitchDetection.kt  # Detecção de altura (autocorrelação/MPM) + notas/cents
│   └── Tunings.kt         # Presets de afinação do afinador
├── data/
│   └── AppSettings.kt     # Configurações do afinador via DataStore (ref A, precisão…)
├── audio/
│   ├── AudioEngine.kt     # Metronome e TonePlayer via AudioTrack (sem libs externas)
│   ├── ChordListener.kt   # AudioRecord do microfone → ChordAnalysis (requer RECORD_AUDIO)
│   └── TunerListener.kt   # AudioRecord do microfone → PitchDetection (afinador)
└── ui/
    ├── theme/             # Color.kt (tokens), Type.kt (Space Grotesk), Theme.kt
    ├── components/         # CommonUi.kt (SectionLabel, AppCard, FunctionTag)
    └── screens/            # Fields, FieldDetail, Progressions, Learn, Tools
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
- **Cores** ficam em `ui/theme/Color.kt`; **tons/nomes** vêm de
  `HarmonicDatabase` (use `ptNameByCipher`, não recalcule o mapa).

## Ao adicionar tons/acordes

Os dados dos 24 campos estão em `HarmonicData.kt`. `getField()` anexa a função
tonal por posição via `functionForDegree(index, isMinor)`. Cubra o novo dado
com um teste em `MusicTheoryTest.kt`.
