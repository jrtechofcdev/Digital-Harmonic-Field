# Dominantes secundárias: o "5 de cada grau"

Guia do módulo **Dominantes secundárias** do detalhe do campo harmônico: o
que é, de onde veio, o que foi conferido e como usar no culto.

---

## 1. De onde veio (estudo do vídeo de referência)

O autor enviou um vídeo curto (1min26s) de uma aula de violão. Conteúdo,
transcrito e conferido quadro a quadro:

- Na tela aparece o campo de **Dó maior**: `C - Dm - Em - F - G - Am - Bm(b5)`.
- A ideia: todo mundo sabe que o **5 (G)** chama o **1 (C)**. Mas cada um dos
  outros acordes também pode ter **o seu próprio 5**, um acorde que chama ele,
  mesmo estando **fora do campo**.
- Exemplo usado: uma música pop em Dó, onde um **A7** aparece para chamar o
  **Dm**.
- No final, o professor escreve em cima de cada acorde o seu dominante:

| Dominante | Chama | Em graus (Brasil) |
|---|---|---|
| A7 | Dm | 6⁷ → 2m |
| B7 | Em | 7⁷ → 3m |
| C7 | F | 1⁷ → 4 |
| D7 | G | 2⁷ → 5 |
| E7 | Am | 3⁷ → 6m |

- Nome técnico: **dominante secundária**.

## 2. Auditoria (o que está certo e o que faltava)

**Está certo:**
- A regra: a dominante de qualquer acorde é o acorde **maior com sétima** uma
  **quinta acima** dele (A é a quinta de D; por isso A7 → Dm).
- Os cinco acordes da tabela e o nome técnico.
- O Bm(b5) ficou sem dominante, como deve ser.

**O que o vídeo não explica e o app explica:**

1. **Por que o diminuto não tem dominante:** o B° (ou Bm(b5)) é tensão pura e
   não funciona como "casa". Nenhum acorde resolve nele.
2. **A nota que faz a mágica:** a **3ª da dominante** fica meio tom abaixo do
   acorde-alvo e **sobe** até ele. No A7, o **Dó#** sobe para o **Ré** (a
   fundamental do Dm). É justamente a nota de fora do campo, e é o "puxão" que o
   ouvido sente.
3. **A segunda nota que anda:** a **7ª da dominante desce** até a 3ª do alvo.
   No A7, o **Sol** desce para o **Fá** (a 3ª do Dm).
4. **Onde entra:** logo antes do alvo, no lugar do acorde anterior ou na
   segunda metade do compasso. Dura 1 ou 2 tempos e resolve.
5. **Quando NÃO usar:** se a melodia estiver cantando a nota natural que a
   dominante altera (o **Dó** contra o Dó# do A7), as duas brigam. Fique no
   acorde do campo.
6. **No tom menor** (ex.: Lá menor: Am, B°, C, Dm, Em, F, G):

| Dominante | Chama | Em graus |
|---|---|---|
| E7 | Am | 5⁷ → 1m (a dominante do tom; o Sol# é a sensível) |
| G7 | C | 7⁷ → 3 (todas as notas já são do campo) |
| A7 | Dm | 1⁷ → 4m |
| B7 | Em | 2⁷ → 5m |
| C7 | F | 3⁷ → 6 |
| D7 | G | 4⁷ → 7 |

O B° (2°) não tem dominante. A lição "Graus a fundo" mostrava só 3 dominantes
no tom menor; agora mostra todas, calculadas pelo mesmo código do módulo.

## 3. Como o app mostra

No **detalhe do campo** (qualquer um dos 24 tons, com transposição):

- **Chave "Dominantes secundárias"** (fica salva no aparelho).
- Ligada, aparece **em cima de cada acorde** o dominante que chama ele, na cor
  da Dominante: `↓ A7  6⁷   chama o Dm`. O 1 mostra a dominante do próprio tom;
  o diminuto mostra que não tem.
- Tocar num acorde abre o cartão **"A7 chama o Dm"**:
  - o desenho do A7 no violão;
  - as **notas** (Lá 1, **Dó# 3 fora**, Mi 5, Sol 7), com a de fora marcada;
  - **Como entra:** para onde a 3ª e a 7ª andam;
  - **Onde entra** e o **cuidado** com a melodia;
  - **Exemplo no tom** (`1 - 6⁷ - 2m - 5 - 1`), na cor de cada função;
  - **Ouvir A7 → Dm** e **Exemplo** (o caminho inteiro).
- **Modo Grau:** tudo em números (`6⁷ chama o 2m`).
- **Paisagem:** a fileira de dominantes fica em cima dos 7 acordes, como no
  quadro do professor, e o cartão compacto cabe sem rolagem.

## 4. Como usar no culto (passo a passo)

1. Abra o campo no tom do hino e ligue **Dominantes secundárias**.
2. Comece por uma só: o **1⁷ → 4** (em Sol: G7 → C) é o mais comum em hino.
   Depois o **3⁷ → 6m** (B7 → Em) e o **2⁷ → 5** (A7 → D).
3. Toque a dominante só no último tempo antes do acorde-alvo e resolva.
4. Ouça a melodia: se a voz estiver na nota que briga, deixe para a próxima.
5. Uma ou duas por estrofe já mudam o hino. Não troque tudo de uma vez.

## 5. Código

- `music/SecondaryDominants.kt`: cálculo (dominante, rótulo em grau, notas de
  fora, 3ª que sobe, 7ª que desce, exemplo no tom). Vale para os 24 tons.
- `ui/screens/DominantsUi.kt`: `DominantSettings` (preferência),
  `DominantLine` (retrato), `DominantStrip` (paisagem) e `DominantCard`.
- `ui/components/Controls.kt`: `SwitchRow`.
- Testes: `SecondaryDominantsTest` (o exemplo do vídeo, Lá menor e as regras
  de resolução nos 24 tons) e `DominantsShot` (capturas em retrato, Grau e
  paisagem).
