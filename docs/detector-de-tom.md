# Detector de tom — como funciona e como retreinar

## Visão geral

```
microfone → ganho automático OU fader manual (−24…+30 dB) + limitador (InputLeveler)
          ─┬─ canal da voz  (120–1500 Hz) → foco na voz → altura (NSDF) → notas sustentadas ─┐
           └─ canal do baixo (35–160 Hz, sem zumbido 60/120/180 Hz) → notas + perfil grave ───┤
                                                                                              ▼
                                         KeyModel (rede pequena treinada) → chances dos 24 tons
                                                                                              ▼
                                travas de evidência (sem canto / notas incoerentes = sem resposta)
                                                                                              ▼
                                       KeyStopRule: rodadas de 5 s até ter certeza (máx. 15 s)
```

- `music/VoiceFocus.kt`, `music/PitchDetection.kt`: front-end de sinal (sem IA).
- `music/KeyDetection.kt`: `KeyDetector` (streaming), `KeyEvidence` (notas
  sustentadas, travas, status) e `KeyStopRule` (rodadas).
- `music/KeyModel.kt` + `resources/com/example/music/key_model.bin`: o modelo.
- `audio/KeyListener.kt`: microfone, pré-buffer de 5 s só na memória, rodadas.

## O modelo (`KeyModel`)

Rede neural pequena (396 entradas → 16 neurônios → 2 saídas, ~6,4 mil pesos,
~25 KB). As entradas são, vistas a partir de cada tônica candidata:

| Bloco | O que capta |
|---|---|
| histograma da melodia (duração e contagem) | quais notas o canto mais usa |
| última nota, notas de fim de frase (antes de respiro ≥ 0,3 s), notas longas | onde a frase descansa |
| pares de notas seguidas (12×12) | passos típicos: sensível → tônica, 4 → 3… |
| histograma, última nota e pares do baixo | caminho do baixo (5 → 1) |
| perfil harmônico (voz) e perfil espectral grave | o que soa junto |

Os mesmos pesos valem para as 12 tônicas (invariante à transposição). A saída é
calibrada (temperatura), então "72%" significa de fato ~72% de chance na bancada.

## Resultados na bancada (hinos que o modelo nunca viu)

Bancada: 603 trechos de 15 s, entrando em ponto aleatório do hino, com
congregação desafinada (homens uma oitava abaixo), teclado/órgão, baixo, eco de
templo, burburinho, zumbido e resposta de microfone de celular.

| | Antes (regras) | Modelo |
|---|---|---|
| Fluxo do app — hinos simples (estilo Harpa) | — | **84,7%** |
| Fluxo do app — todos (inclui corais de Bach) | 45% | **74%** |
| "Tom identificado" (ALTA) acerta | 67% | **90–95%** |

Teto com notas perfeitas (direto da partitura): 65% com 5 s, 80% com 10 s, 84%
com 15 s — por isso a 1ª rodada nunca crava o tom e o app ouve em rodadas.

**Limite honesto:** a bancada é sintética (hinos reais, áudio gerado). O passo
seguinte é medir com gravações reais de cultos e hinos da Harpa.

## Sensibilidade do microfone (fader)

Na tela "Detectar tom": **Automático** (o app ajusta o ganho sozinho) ou fader
manual de −24 a +30 dB, com atalhos **Culto forte** (−12 dB), **Normal** (0) e
**Capela baixa** (+18 dB). O medidor mostra o volume depois do fader, com a
faixa ideal marcada (≈ −35 a −17 dB). Abaixar a sensibilidade faz o detector
ignorar o que está fraco (conversa, barulho de fundo); subir faz ele ouvir vozes
fracas. O ajuste é digital: não muda o microfone em si (o Android não expõe o
ganho do hardware), mas decide o que o detector considera.

## O que o material real ensinou (out/2026, 25 sessões, 12 hinos rotulados)

Primeira prova no culto com a versão DEV (Samsung A16, microfone sem filtros):

- **Som baixo:** a faixa da voz chegava a −40/−50 dB; ~1 em cada 4 quadros
  caía abaixo do volume mínimo. → **ganho automático com limitador** na entrada.
- **Canto "sujo":** ~27% das notas captadas caem fora da escala (afinação do
  grupo andando até 70 cents durante o hino, eco, vozes desencontradas); na
  simulação eram 4%. → notas sustentadas mais tolerantes (±0,8 semitom,
  clareza ≥ 0,60), trava de escala em 75% e **treino com "sujeira" proposital**
  (`tools/key-model/augment.py`).
- **Harmonia quase plana e baixo ausente** no microfone do celular — mesmo um
  croma de alta resolução sobre a gravação inteira não acerta o tom. Quem decide
  é o canto; o modelo foi treinado com o baixo desligado em parte dos exemplos.

Resultado no app completo, nesses 12 hinos:

| | Antes | Depois (sem ver estes hinos) | Depois (modelo final) |
|---|---|---|---|
| Momentos certos (a cada 5 s) | 27% | 30% | 41% |
| Aos 10 s | 2/12 | 4/12 | 8/12 |
| Fim de cada hino | 7/12 | 8/12 | 8/12 |

A coluna "sem ver estes hinos" é a medida justa para um hino novo; o modelo
final inclui os 12 hinos no treino. Com 12 hinos a margem de erro é grande:
mais sessões rotuladas são o que mais vai melhorar o detector.

## Como retreinar

Requisitos: Python 3 com `numpy`, `scipy`, `music21`; Gradle 9.3.1 + SDK.

```bash
cd tools/key-model
python3 corpus_extract.py                       # → corpus.json (corais + Essen, com o tom)
python3 render.py train 3000 21 train           # trechos de treino (WAV + labels.csv)
python3 render.py val 600 31 val                # validação (hinos separados por hash)
gradle :app:compileDebugUnitTestKotlin :app:processDebugJavaRes
./runbench.sh train tr.csv train_notes.jsonl    # notas extraídas pelo detector real
./runbench.sh val   va.csv val_notes.jsonl
python3 train_final.py train_notes.jsonl train/labels.csv val_notes.jsonl val/labels.csv \
    --real real_notes.jsonl:treino-real/labels.csv --out modelo.npz   # sintético + sujeira + reais
python3 export.py modelo.npz ../../app/src/main/resources/com/example/music/key_model.bin
./runbench.sh val final.csv && python3 score.py final.csv   # acerto do fluxo do app
```

Com gravações reais: coloque WAVs mono 44,1 kHz numa pasta com `labels.csv`
(`arquivo,tônica 0–11,menor 0/1,origem,cenário`) e rode `runbench.sh` + `score.py`.
Para treinar com elas, gere `*_notes.jsonl` do mesmo jeito e junte ao treino.

`ModelCheck` (testes) confere que o Kotlin dá as mesmas chances que o Python.
