# Detector de tom — como funciona e como retreinar

## Visão geral

```
microfone ─┬─ canal da voz  (120–1500 Hz) → foco na voz → altura (NSDF) → notas sustentadas ─┐
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
python3 train.py 16 30                          # treina, calibra → model_h16.npz
python3 export.py model_h16.npz ../../app/src/main/resources/com/example/music/key_model.bin
./runbench.sh val final.csv && python3 score.py final.csv   # acerto do fluxo do app
```

Com gravações reais: coloque WAVs mono 44,1 kHz numa pasta com `labels.csv`
(`arquivo,tônica 0–11,menor 0/1,origem,cenário`) e rode `runbench.sh` + `score.py`.
Para treinar com elas, gere `*_notes.jsonl` do mesmo jeito e junte ao treino.

`ModelCheck` (testes) confere que o Kotlin dá as mesmas chances que o Python.
