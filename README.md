# Digital Harmonic Field

Aplicativo Android para consulta rápida de **campos harmônicos**, pensado para
músicos que tocam ao vivo — em cultos, ensaios e eventos. Escolha um tom e veja,
na hora, os sete graus, a função de cada acorde e as notas que o formam. Tudo
offline e em modo escuro, para uso confortável em palco.

Desenvolvido em **Kotlin** com **Jetpack Compose** e **Material 3**.
Mantido por **JR TECH** — José Renato, guitarrista e developer.

---

## O que o app faz

O app é organizado em cinco áreas, acessíveis pela barra inferior:

### Campos
A função central. Uma grade com os 24 tons (12 maiores e 12 menores). Ao abrir
um tom você vê:

- Os **sete graus** do campo, cada um com sua **função tonal** — Tônica,
  Subdominante ou Dominante — sinalizada por cor, com a **tendência** de cada
  acorde (para onde ele "puxa").
- **Transposição** em tempo real (± semitons), útil para adequar a música ao
  vocal ou ao instrumento.
- A **formação** de cada acorde (fundamental, terça e quinta).
- **Caminhos comuns** já montados no tom (a Progressão da Harpa e o 1‑4‑5),
  para bater o olho e saber para onde ir.
- As **notas da escala** e **favoritos** salvos no aparelho.
- **Modo paisagem** compacto: todos os graus na tela, sem rolagem, para uso
  ao vivo.

### Afinador
Afinador por **microfone**, no estilo dos afinadores populares. Dois modos:
**Corda por corda** (você toca na corda e ela fica travada; o app diz se está
frouxa ou apertada) e **Livre/cromático** (detecta qualquer nota). Usa detecção
de altura por autocorrelação (precisão em cents), com **ponteiro de velocímetro**,
tom de referência curto e as afinações mais usadas — **Padrão, Drop D, ½ tom
abaixo, Drop C, Open G e Open D**. Requer permissão de microfone.

### Progressões
Sequências harmônicas consagradas já montadas no tom escolhido. Em destaque, a
**Progressão da Harpa** (I – vi – ii – V – I) — a "volta" presente em quase todo
hino — além do 1‑4‑5, eixo do louvor, cadências e progressões menores.

### Aprender
Um guia para o tocador iniciante da assembleia: o que é campo harmônico, as três
funções, a progressão da Harpa, **como tirar música de ouvido**, o sistema de
números (1‑4‑5) e o **círculo das quintas explicado com uso prático** — um mapa
interativo que mostra, para cada tom, sua subdominante, dominante e relativa
menor.

### Ferramentas
Utilitários para o dia a dia, todos sem depender de internet:

- **Ouvir (identificar acorde)** — o app escuta e estima o **acorde tocado** e o
  **tom provável da música** (com um gráfico do que está ouvindo). É uma
  estimativa: funciona melhor com acordes claros e pouco ruído.
- **Metrônomo** — som sintetizado, ajuste por slider ou passo, "marcar tempo"
  (tap tempo), escolha de compasso e indicador visual dos tempos.
- **Capotraste** — indica em que casa colocar o capo para tocar com acordes
  abertos (formatos CAGED) e soar no tom desejado.
- **Configurações** — filtro de ruído, precisão do afinador, frequência de
  referência (Lá) e sons.

---

## Design

A interface foi reconstruída em torno de três princípios:

- **Sobriedade**: fundo quase-preto, superfícies em camadas e um único acento
  (latão) usado apenas para ação e foco. Sem brilhos ou gradientes decorativos.
- **Cor com significado**: em vez de tons avulsos, cada acorde recebe a cor da
  sua função tonal (Tônica / Subdominante / Dominante) — o que também ensina
  teoria enquanto se usa.
- **Tipografia própria**: a família *Space Grotesk*, com dígitos marcantes,
  dá identidade e legibilidade às cifras.

---

## Estrutura do projeto

```text
app/src/main/java/com/example/
├── MainActivity.kt              # Casca de navegação (4 abas + detalhe)
├── HarmonicData.kt              # Base dos 24 campos harmônicos
├── music/
│   ├── MusicTheory.kt           # Funções tonais, transposição, formação de acordes
│   ├── Progressions.kt          # Biblioteca de progressões
│   ├── ChordAnalysis.kt         # FFT, chromagram, detecção de acorde e tom (puro/testável)
│   ├── PitchDetection.kt        # Detecção de altura (autocorrelação) + notas/cents
│   └── Tunings.kt               # Presets de afinação (Padrão, Drop D, Open G…)
├── data/
│   └── AppSettings.kt           # Configurações persistidas (DataStore)
├── audio/
│   ├── AudioEngine.kt           # Metrônomo e tons de referência (AudioTrack)
│   ├── ChordListener.kt         # Microfone → identificação de acorde/tom (AudioRecord)
│   └── TunerListener.kt         # Microfone → afinador (AudioRecord)
└── ui/
    ├── theme/                   # Cores, tipografia e tema
    ├── components/              # Componentes reutilizáveis (inclui a marca/palheta)
    └── screens/                 # Campos, Detalhe, Ouvir, Progressões, Aprender,
                                 #  Ferramentas, Afinador, Configurações, Doação
```

---

## Como compilar

### Pré-requisitos
- JDK 17 ou superior.
- Android SDK com a plataforma **android-36.1** e **build-tools 36.1.0**.
- **Gradle 9.3.1+** (exigido pelo Android Gradle Plugin 9.1.1).

### Comandos
```bash
# Gerar o APK de depuração
gradle :app:assembleDebug

# Rodar os testes de lógica musical (Robolectric)
gradle :app:testDebugUnitTest
```

O APK é gerado em `app/build/outputs/apk/debug/`. Uma cópia da última build é
mantida em `.build-outputs/app-debug.apk` para instalação direta.

---

## Apoiar

O app é gratuito. Quem quiser apoiar o projeto encontra, no cabeçalho da tela
inicial, o botão **Apoiar** — uma tela com contribuição via Mercado Pago
(Pix ou cartão), com valores sugeridos ou valor livre. Nada é cobrado dentro
do app.

## Licença

Projeto open-source sob licença MIT. Contribuições são bem-vindas via
Pull Request.

© JR TECH — 2026.
