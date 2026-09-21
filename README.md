# Digital Harmonic Field

Aplicativo Android para consulta rápida de **campos harmônicos**, pensado para
músicos que tocam ao vivo — em cultos, ensaios e eventos. Escolha um tom e veja,
na hora, os sete graus, a função de cada acorde e as notas que o formam. Tudo
offline e em modo escuro, para uso confortável em palco.

Desenvolvido em **Kotlin** com **Jetpack Compose** e **Material 3**.
Mantido por **JR TECH** (José Renato).

---

## O que o app faz

O app é organizado em quatro áreas, acessíveis pela barra inferior:

### Campos
A função central. Uma grade com os 24 tons (12 maiores e 12 menores). Ao abrir
um tom você vê:

- Os **sete graus** do campo, cada um com sua **função tonal** — Tônica,
  Subdominante ou Dominante — sinalizada por cor.
- **Transposição** em tempo real (± semitons), útil para adequar a música ao
  vocal ou ao instrumento.
- A **formação** de cada acorde (fundamental, terça e quinta).
- As **notas da escala** do tom.
- **Favoritos**, salvos no aparelho para acesso imediato.

### Progressões
Sequências harmônicas consagradas (eixo pop/louvor, cadências, ciclo de
Pachelbel, progressões menores) já montadas no tom escolhido. Serve para compor,
ensaiar e conduzir a ministração sem precisar transpor de cabeça.

### Círculo das Quintas
O círculo das quintas interativo: tons vizinhos ficam lado a lado e os relativos
menores aparecem no anel interno. Toque em qualquer tom para abrir o campo dele.

### Ferramentas
Três utilitários para o dia a dia, todos sem depender de internet:

- **Metrônomo** — som sintetizado, ajuste por slider ou passo, "marcar tempo"
  (tap tempo), escolha de compasso e indicador visual dos tempos.
- **Diapasão** — tons de referência (Lá = 440 Hz) para afinar de ouvido.
- **Capotraste** — indica em que casa colocar o capo para tocar com acordes
  abertos (formatos CAGED) e soar no tom desejado.

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
│   └── Progressions.kt          # Biblioteca de progressões
├── audio/
│   └── AudioEngine.kt           # Metrônomo e tons de referência (AudioTrack)
└── ui/
    ├── theme/                   # Cores, tipografia e tema
    ├── components/              # Componentes reutilizáveis
    └── screens/                 # Campos, Detalhe, Progressões, Círculo, Ferramentas
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

## Licença

Projeto open-source sob licença MIT. Contribuições são bem-vindas via
Pull Request.

© JR TECH — 2026.
