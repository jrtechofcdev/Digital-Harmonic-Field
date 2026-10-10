# Digital Harmonic Field

Aplicativo Android para consulta rápida de **campos harmônicos**, pensado para
músicos que tocam ao vivo — em cultos, ensaios e eventos. Escolha um tom e veja,
na hora, os sete graus, a função de cada acorde e as notas que o formam. Tudo
offline e em modo escuro, para uso confortável em palco.

Desenvolvido em **Kotlin** com **Jetpack Compose** e **Material 3**.
Mantido por **JR TECH** — José Renato, guitarrista e developer.

---

## O que o app faz

O app é organizado em quatro áreas, acessíveis pela barra inferior:

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

### Modo Grau (para quem toca "por número")
Uma chave **Cifra | Grau** muda o app inteiro: em vez de "C, Am, F, G", os
acordes aparecem como **1, 6m, 4, 5** (com a cifra pequena embaixo) — no campo,
nas progressões, na tela cheia e no detector de tom. A escolha fica salva.

### Braço do violão (laboratório)
Um braço de violão "de verdade" na tela (casas na proporção real, 6ª corda à
esquerda, marcações 3‑5‑7‑9‑12), com quatro abas:

- **Notas:** todas as notas do braço; toque numa casa (ou escolha a nota) e
  aparecem **todas as posições** dela, corda por corda, com o atalho da oitava.
  Nomes em Dó‑Ré‑Mi ou C‑D‑E.
- **Acordes:** escolha o grau (ou qualquer nota) e o **tipo** — maior, m, (9),
  m(9), sus2, sus4, 6, m6, 6(9), 7M, 7M(9), 7, 7(9), 7sus4, m7, m7(9), m7(b5),
  °, °7. O acorde **se monta sozinho** no braço, com **variações** espalhadas
  (aberto, casa 3, casa 5…), cada nota marcada pelo papel (1, 3, 5, 7, 9…), e dá
  para **ouvir** cada forma. No modo "Onde estão 1 · 3 · 5" você liga/desliga
  cada papel e monta o acorde sozinho.
- **Harmonizar:** para cada grau, trocas que tiram o hino do acorde "seco" sem
  mudar a função (1(9), 1⁶⁽⁹⁾, 4⁷M, 5sus4 → 5, 2m⁷, 4m emprestado, 1/3, 4/5,
  5/7…), com desenho, explicação e áudio — e um **antes × depois** para ouvir a
  diferença.
- **Decorar:** o método passo a passo (ver `docs/braco-do-violao.md`) e dois
  jogos: **Que nota é?** e **Ache a nota**, por fases (6ª corda, 5ª, as duas,
  braço todo).

Abre pelo detalhe do campo, pela aba Aprender ou por Ferramentas.

### Progressões
Sequências harmônicas já montadas no tom escolhido, organizadas por uso:
**Essenciais para hinos** (com destaque para a **Progressão da Harpa**,
I – vi – ii – V – I, e o 1‑4‑5), **Finais de frase** (cadência perfeita, do
"Amém", ii – V – I, meia cadência) e **Louvor e outras**. Os acordes ocupam a
largura da tela — nada de arrastar a linha para o lado — e cada progressão abre
em **tela cheia para tocar**: acordes gigantes, passo atual destacado, troca de
tom na hora (− / +) e tela sempre acesa.

### Aprender
Um guia para o tocador iniciante da assembleia: o que é campo harmônico, as três
funções, a progressão da Harpa, **como tirar música de ouvido**, o sistema de
números (1‑4‑5) e o **círculo das quintas explicado com uso prático** — um mapa
interativo que mostra, para cada tom, sua subdominante, dominante e relativa
menor.

Módulos de **graus**:
- **Como pegar os graus** — passo a passo (achar o 1, a "escada" do tom, a
  sensação de cada grau, seguir o baixo, caminhos de hino) e **treino**: pelo
  ouvido (o app toca o 1 e depois um acorde — qual é o grau?), cifra → grau e
  grau → cifra, em três níveis (1·4·5 → +6m → +2m·3m).
- **Graus a fundo** — campo maior × menor (o 5 maior no tom menor), graus com
  sétima, dominantes secundárias (1⁷ → 4…), diminutos de passagem, baixo
  caminhando (5/7, 1/3), empréstimo (4m) e transposição — tudo calculado no tom
  escolhido.

### Ferramentas
Utilitários para o dia a dia, todos sem depender de internet:

- **Detectar tom** — também com atalho na tela inicial. Ouve em **rodadas de
  5 segundos** e mostra os **3 tons mais prováveis**, com a chance de cada um e
  os acordes para começar.
  - **1ª rodada na hora:** com a tela aberta, os últimos 5 s ficam só na memória
    (nada é gravado nem enviado; ao sair, tudo é apagado).
  - **2ª e 3ª rodadas:** somam mais canto e mostram o palpite ao vivo. Para
    sozinho quando tem certeza; dá para **parar e usar** a qualquer momento ou
    pedir **mais 5 s** no mesmo hino. Vibra ao terminar.
  - **Modelo treinado:** uma rede neural pequena, treinada com milhares de
    trechos de hinos, analisa o que o músico percebe de ouvido — notas mais
    cantadas, fim de frase, sensível subindo para a tônica, baixo fazendo 5 → 1.
    Roda no aparelho, sem internet.
  - **Sensibilidade do microfone:** automático ou fader manual, com atalhos
    *Culto forte*, *Normal* e *Capela baixa* e um medidor com a faixa ideal.
  - **Foco na voz + canal do baixo**, filtro de ruído constante e do zumbido da
    rede elétrica. Sem evidência suficiente, **não sugere tom**.
  - Na bancada de testes (hinos que o modelo nunca viu, áudio simulado de
    culto), acerta ~**8,5 em 10** hinos simples no estilo da Harpa. Detalhes em
    [`docs/detector-de-tom.md`](docs/detector-de-tom.md).
- **Metrônomo** — som sintetizado, ajuste por slider ou passo, "marcar tempo"
  (tap tempo), escolha de compasso e indicador visual dos tempos.
- **Capotraste** — indica em que casa colocar o capo para tocar com acordes
  abertos (formatos CAGED) e soar no tom desejado.

### Versão DEV — treino real
Uma variante para **testar no culto e gerar material de treino** (`gradle
:app:assembleDev`). Instala ao lado do app normal e, a cada hino detectado,
salva o trecho de áudio e o que o detector pensou em cada rodada. Abaixo do
resultado você marca o **tom certo** (um toque no sugerido ou num seletor),
dá **estrelas** e **etiquetas**; tudo vai para uma única pasta (`sessoes.json`,
`log.jsonl`, áudios) que o app compacta e **compartilha em .zip**. Inclui um
painel com o acerto real nos seus hinos e um **treinador** que só troca o
modelo se melhorar. Guia completo em [`docs/treino-real.md`](docs/treino-real.md).
O app normal **não grava nada**.

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
├── MainActivity.kt              # Casca de navegação (4 abas + telas sobrepostas)
├── HarmonicData.kt              # Base dos 24 campos harmônicos
├── music/
│   ├── MusicTheory.kt           # Funções tonais, transposição, formação de acordes
│   ├── Progressions.kt          # Biblioteca de progressões (por grupo de uso)
│   ├── ChordAnalysis.kt         # Base espectral: FFT, classes de altura, chromagram
│   ├── VoiceFocus.kt            # Foco na voz + canal do baixo + subtração de ruído
│   ├── PitchDetection.kt        # Detecção de altura (autocorrelação) + nomes das notas
│   ├── KeyDetection.kt          # Detector de tom: notas sustentadas, travas, rodadas
│   ├── KeyModel.kt              # Modelo treinado (pesos em resources/…/key_model.bin)
│   ├── KeyTrainer.kt            # Treinador leve (versão DEV)
│   ├── Degrees.kt               # Graus por número, sétimas, dominantes secundárias…
│   ├── DegreeQuiz.kt            # Perguntas do treino de graus
│   ├── Guitar.kt                # Formas de acorde no violão (abertas e pestana)
│   └── Voicings.kt              # Tipos de acorde, gerador de variações, harmonização, jogos do braço
├── audio/
│   ├── AudioEngine.kt           # Metrônomo e ChordPlayer (acordes do treino de ouvido)
│   └── KeyListener.kt           # Microfone → detector de tom (pré-buffer de 5 s em memória)
├── dev/                         # Só na versão DEV: pasta de treino real, gravador de sessões
└── ui/
    ├── theme/                   # Cores, tipografia e tema
    ├── components/              # Componentes reutilizáveis (marca, ChordStrip…)
    └── screens/                 # Campos, Detalhe, Progressões (+ tela cheia), Aprender (+ lições de
                                 #  graus), Treino de graus, Braço do violão, Ferramentas, Detectar tom, Doação (+ ui/dev na versão DEV)
tools/key-model/                 # Bancada de hinos, treino do modelo e ingestão do material real
docs/detector-de-tom.md          # Como o detector funciona e como retreinar
docs/treino-real.md              # Versão DEV: como captar, rotular e enviar
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
