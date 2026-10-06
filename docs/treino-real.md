# Treino real — versão DEV

Versão do app para **captar hinos reais no culto**, rotular o tom certo e
gerar material para treinar o detector. Instala **ao lado** do app normal
(ícone com faixa argila, nome "DHF DEV · Treino"). O app normal continua sem
gravar nada.

```bash
gradle :app:assembleDev        # → app/build/outputs/apk/dev/app-dev.apk
```

## Como usar no culto

1. **Ferramentas → Detectar tom** (ou o atalho da tela inicial). Use como
   sempre: rodadas de 5 s, "Ouvir mais 5 s", "Parar e usar".
2. Cada hino detectado é **salvo sozinho** como *pendente*: o áudio exato que o
   detector ouviu + o que ele pensou em cada rodada.
3. Abaixo do resultado aparece o cartão **TREINO**:
   - toque no tom certo entre os sugeridos (1º, 2º, 3º) **ou** escolha outro
     (Maior/Menor + nota), ou marque "Não sei o tom" / "Não era hino";
   - "Tenho certeza" ou "Acho que é";
   - **estrelas** (como o app se saiu) e **etiquetas** (acertou de 1ª, errou
     relativa, banda completa, muito ruído…);
   - nº da Harpa e comentário (opcionais) → **Salvar rótulo**.
4. Se não der para rotular na hora: **Ferramentas → Treino real (DEV)** lista
   os pendentes; dá para **ouvir** o trecho e rotular depois.
5. No fim da semana: **Treino real → Compartilhar .zip** (WhatsApp, Drive,
   e-mail) ou **Salvar em Downloads** → `Download/DHF-treino/`.

O painel também mostra o **acerto real nos seus hinos** (por rodada), a nota
média, os erros mais comuns e o **treinador do app** (abaixo).

## Pasta `treino-real/` (vai inteira no .zip)

| Arquivo | Conteúdo |
|---|---|
| `LEIA-ME.txt` | resumo desta tabela |
| `sessoes.json` | 1 entrada por hino; reescrito a cada hino e a cada rótulo |
| `log.jsonl` | diário de eventos (abrir app, detectar, ouvir mais, rótulo, treino, exportar) |
| `audio/<sessão>.wav` | áudio exato que o detector ouviu, 44,1 kHz mono 16 bits |
| `features/<sessão>.json` | entradas do modelo a cada 2,5 s (usadas pelo treinador do app) |
| `modelo_ajustado.json` | última camada ajustada no aparelho (só se melhorou) |
| `treino_relatorios.json` | cada execução do treinador (acerto original × ajustado) |

Fica em `Android/data/<app>.dev/files/treino-real/`. Cerca de **90 KB por
segundo** de áudio (~1,3 MB por hino de 15 s).

### Uma sessão em `sessoes.json`

```json
{
  "id": "s0012_20261012-193210",
  "arquivo_audio": "audio/s0012_20261012-193210.wav",
  "inicio": "2026-10-12T19:32:10-03:00",
  "duracao_s": 14.9,
  "modelo": "base",
  "ouvir_mais": 1,
  "ganho": {"modo": "manual", "fader_db": -12, "aplicado_db": -12.0},
  "paradas": [{"t": 10.2, "motivo": "sozinho"}],
  "rodadas": [{"t": 5.0, "status": "MEDIA", "top": [{"tom": "G", "nome": "Sol Maior", "chance": 0.61}, …],
               "voz_s": 3.4, "baixo_s": 1.2, "afinacao_cents": -14, "entre_tons": false}, …],
  "resultado_final": { …mesmo formato… },
  "linha_do_tempo": [[0.28, "D", 0.31, "BAIXA"], …],
  "status": "rotulado",
  "rotulo": {"tom": "G", "certeza": "certeza", "estrelas": 4,
             "tags": ["acertou_ouvindo_mais", "banda_completa"],
             "harpa_numero": 15, "comentario": "", "app_sugeriu": ["G", "D", "Em"]},
  "avaliacao": {"acertou_rodada": [false, true], "acertou_final": true,
                "certo_entre_3": true}
}
```

`tipo_erro` (quando errou): `relativa`, `quinta_acima` (disse a dominante),
`quarta_acima` (a subdominante), `homonima` (maior ↔ menor), `semitom`,
`sem_resposta`, `outro`.

## Treinador do app

Ajusta só a **última camada** do modelo com os seus rótulos e mede por
**validação cruzada por hino** (cada hino é testado sem ter entrado no ajuste).
É **seguro**: testa três intensidades e só troca o modelo se o ajuste for
**melhor que o original** nos seus hinos; senão mantém o original. A chave
"Usar modelo ajustado" liga/desliga, e cada sessão registra qual modelo usou
(`"modelo": "base" | "ajustado"`).

## Do .zip ao modelo final (lado do desenvolvedor)

```bash
cd tools/key-model
unzip treino-real_*.zip                         # → treino-real/
python3 ingest_real.py treino-real [--so-certeza]   # resumo + labels.csv
./runbench.sh treino-real real.csv real_notes.jsonl
python3 score.py real.csv                       # acerto do detector atual nos hinos reais
python3 train.py 16 30 --extra real_notes.jsonl:treino-real/labels.csv
python3 export.py model_h16.npz ../../app/src/main/resources/com/example/music/key_model.bin
```
