# Braço do violão — como o app ensina

Este documento registra **o método** usado na aba *Decorar* e **as regras** do
gerador de acordes (abas *Acordes* e *Harmonizar*), com as fontes pesquisadas.

## 1. Decorar as notas do braço

O que mais aparece nas fontes, em comum, para iniciantes:

1. **Cordas soltas primeiro** (Mi Lá Ré Sol Si Mi / E A D G B E). Frase do app:
   *"Eu Amo Deus, Glória Bendita Eternamente"*.
2. **Regra do meio tom:** cada casa = meio tom; não há casa no meio entre
   Mi–Fá e Si–Dó. Na **casa 12 tudo se repete** — basta decorar de 0 a 11.
3. **Uma corda de cada vez**, começando pelas **naturais da 6ª e da 5ª**
   (são o baixo dos acordes com pestana). Tocar e **falar o nome em voz alta**.
4. **Marcos** (casas 3, 5, 7, 9, 12) e **atalho da oitava** (6ª→4ª e 5ª→3ª: +2
   casas; 4ª→2ª e 3ª→1ª: +3 casas; 1ª = 6ª).
5. **Nota do dia:** achar todas as posições de uma nota, depois com metrônomo.
6. **Pouco e sempre + testar a memória:** 5–10 min/dia, revisões espaçadas
   (1 dia, 3 dias, 1 semana) e exercícios de lembrar (os jogos *Que nota é?* e
   *Ache a nota*) em vez de só olhar o mapa.

## 2. Decorar os acordes

- **Pensar em notas (1 · 3 · 5)**, não só no desenho — por isso o modo
  "Onde estão 1 · 3 · 5" na aba Acordes.
- **CAGED:** as formas abertas de Dó, Lá, Sol, Mi e Ré se repetem pelo braço,
  emendadas nessa ordem — é de onde vêm as *variações* do mesmo acorde.
- **Tríades nas cordas de cima** (1ª–3ª): leves, não embolam com teclado e baixo.
- **Dedo guia** na troca de acordes e **uma cor nova por semana** ((9), 7M, sus4).

## 3. Gerador de variações (`Voicings.generate`)

Procura, em janelas de 4 casas pelo braço (até a casa 13), todas as combinações
de cordas que tocam **só notas do acorde** e mantém as que:

- têm as notas **obrigatórias** (a 5ª justa pode ficar de fora em acordes de 4+
  notas) e o **baixo certo** (fundamental, ou a nota pedida em "C/E", "C/D");
- soam **4 cordas ou mais**; abafam só cordas graves antes do baixo, agudas
  depois da última, ou uma logo acima do baixo;
- cabem na mão: **até 4 dedos** (pestana conta 1) e **abertura de até 4 casas**.

A nota de cada forma favorece: mais cordas, menos dedos, cordas soltas, acorde
completo, nota de cor (9, 6, 7M) em cima; penaliza pestana, abertura e notas
coladas no grave. Escolhe-se **uma forma por região do braço/corda do baixo**
(até 6); a forma "de revistinha" (`Guitar.shape`) sempre entra.

## 4. Harmonizar hinos (`Harmonize`)

Trocas que mantêm a **função** do grau (o 1 continua casa, o 5 continua pedindo
volta): 1(9), 1⁶, 1⁷M, 1⁶⁽⁹⁾, 1/3 · 2m⁷, 2m⁷⁽⁹⁾, 2⁷ · 3m⁷, 3⁷ · 4(9), 4⁷M, 4⁶,
4m · 5⁷, 5sus4 → 5, 5⁷sus4, 5⁷⁽⁹⁾, 4/5 · 6m⁷, 6m(9) · 7ø, 5/7 — e no menor:
1m(9), 1m⁷, 1m⁶, 2ø, 5 maior/5⁷, 3⁷M, 6⁷M. Cuidados que o app mostra: usar
poucas trocas por estrofe; evitar 7M quando a melodia canta a tônica; sus4
resolve; nona com moderação.

## Fontes

- TrueFire — *Learn guitar fretboard notes*: https://blog.truefire.com/guitar-lessons/learn-guitar-fretboard-notes-master-the-neck-fast-with-proven-strategies/
- Zager Guitar — memorizar o braço: https://blog.zagerguitar.com/?p=2647
- Riffhard — notas no braço: https://riffhard.com/?p=37288
- Good Guitarist — notas no braço: https://goodguitarist.com/?p=4550
- Guitar Gear Finder — notas no braço: https://guitargearfinder.com/?p=3462
- Superprof — memorizar notas no violão: https://www.superprof.com.br/blog/memorizar-notas-violao/
- PickupMusic — tríades e CAGED: https://www.pickupmusic.com/blog/mastering-triad-shapes-on-the-guitar-applying-the-caged-system
- Guitar World — tríades: https://www.guitarworld.com/lessons/guitar-triads
- jazzguitar.be — visualizar tríades: https://www.jazzguitar.be/forum/getting-started/81141-visualising-triads.html
- TrueFire — CAGED & Triad Trainer: https://learn.truefire.com/caged-triad-trainer-guitar
- ECA/USP — acorde sus4 (V7sus4): https://www.eca.usp.br/acervo/producao-academica/002867143.pdf
- Spossobin (trad. Merhy) — manual de harmonia (nonas): https://edisciplinas.usp.br/pluginfile.php/4214241/mod_resource/content/2/MANUAL%20DE%20HARMONIA%20%28SPOSSOBIN%201955%2C%20trad%20MERHY%202007%29.pdf
- UFU — rearmonização criativa: https://repositorio.ufu.br/bitstream/123456789/31318/1/Rearmoniza%C3%A7%C3%A3oCriativaPartir.pdf
