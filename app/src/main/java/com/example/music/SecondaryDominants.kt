package com.example.music

import com.example.HarmonicField

/**
 * Dominantes secundárias: "o 5 de cada grau".
 *
 * Todo acorde maior ou menor do campo pode ganhar, por um instante, o seu
 * próprio 5: um acorde MAIOR COM SÉTIMA uma quinta acima dele, que "chama" o
 * acorde como se ele fosse a casa. No tom de Dó:
 *
 *   A7 → Dm   (6⁷ chama o 2m)      B7 → Em   (7⁷ chama o 3m)
 *   C7 → F    (1⁷ chama o 4)       D7 → G    (2⁷ chama o 5)
 *   E7 → Am   (3⁷ chama o 6m)      G7 → C    (5⁷, a dominante do próprio tom)
 *
 * O acorde diminuto (7° no maior, 2° no menor) não tem dominante: ele não
 * funciona como "casa", então ninguém resolve nele.
 *
 * Por que funciona (o que o ouvido percebe):
 *  - a 3ª da dominante fica meio tom ABAIXO da fundamental do alvo e sobe até
 *    ela (no A7, o Dó# sobe para o Ré do Dm). É a nota "de fora do campo";
 *  - a 7ª da dominante desce até a 3ª do alvo (no A7, o Sol desce para o Fá).
 *
 * Tudo é calculado a partir do campo, então vale para os 24 tons.
 */
data class DominantFor(
    /** Grau alvo (0..6) no campo. */
    val targetIndex: Int,
    /** O acorde alvo, como está no campo (ex.: Dm). */
    val target: Chord,
    /** O alvo em número (ex.: "2m"). */
    val targetLabel: String,
    /** A dominante (sempre maior com sétima, ex.: A7). */
    val dominant: Chord,
    /** A dominante em número, pela nota onde ela está no tom (ex.: "6⁷"). */
    val label: String,
    /** true = é a dominante do próprio tom (5⁷ → 1), não uma secundária. */
    val primary: Boolean,
    /** Notas da dominante que não são da escala do tom (0..11). */
    val outsideNotes: List<Int>,
    /** 3ª da dominante: meio tom abaixo do alvo; sobe até a fundamental dele. */
    val leadingTone: Int,
    /** 7ª da dominante: desce até a 3ª do alvo. */
    val seventh: Int,
    /** A 3ª do alvo, onde a 7ª chega. */
    val seventhLandsOn: Int,
    /** Um caminho no tom para ouvir e tocar: (rótulo em número, acorde). */
    val example: List<Pair<String, Chord>>,
)

object SecondaryDominants {

    /**
     * Caminhos de exemplo, por grau alvo: índices do campo e -1 no lugar da
     * dominante. Curtos, do jeito que aparecem nos hinos.
     */
    private val MAJOR_EXAMPLES = mapOf(
        0 to listOf(3, -1, 0),            // 4 - 5⁷ - 1
        1 to listOf(0, -1, 1, 4, 0),      // 1 - 6⁷ - 2m - 5 - 1
        2 to listOf(0, -1, 2, 5),         // 1 - 7⁷ - 3m - 6m
        3 to listOf(0, -1, 3, 0),         // 1 - 1⁷ - 4 - 1
        4 to listOf(0, -1, 4, 0),         // 1 - 2⁷ - 5 - 1
        5 to listOf(0, -1, 5, 3),         // 1 - 3⁷ - 6m - 4
    )
    private val MINOR_EXAMPLES = mapOf(
        0 to listOf(3, -1, 0),            // 4m - 5⁷ - 1m
        2 to listOf(0, -1, 2),            // 1m - 7⁷ - 3
        3 to listOf(0, -1, 3, 0),         // 1m - 1⁷ - 4m - 1m
        4 to listOf(0, -1, 4, 0),         // 1m - 2⁷ - 5m - 1m
        5 to listOf(0, -1, 5, 3),         // 1m - 3⁷ - 6 - 4m
        6 to listOf(0, -1, 6, 2),         // 1m - 4⁷ - 7 - 3
    )

    /** As 7 posições do campo; null onde não há dominante (o acorde diminuto). */
    fun forField(field: HarmonicField): List<DominantFor?> {
        val chords = field.chords.map { parseCipher(it.cipher) }
        val scale = chords.map { it?.root ?: 0 }
        val labels = field.chords.mapIndexed { i, c -> degreeLabel(i, c.cipher) }
        val examples = if (field.isMinor) MINOR_EXAMPLES else MAJOR_EXAMPLES

        fun dominantOf(i: Int): Chord? {
            val target = chords[i] ?: return null
            if (target.quality == ChordQuality.DIM || target.quality == ChordQuality.HALFDIM) return null
            return Chord((target.root + 7) % 12, ChordQuality.DOM7)
        }

        fun labelOf(dom: Chord): String {
            val pos = scale.indexOf(dom.root)
            return if (pos >= 0) "${pos + 1}⁷" else dom.cipher
        }

        return chords.indices.map { i ->
            val target = chords[i] ?: return@map null
            val dom = dominantOf(i) ?: return@map null
            val third = if (target.quality == ChordQuality.MINOR) 3 else 4
            val path = examples[i].orEmpty().map { idx ->
                if (idx < 0) labelOf(dom) to dom
                else labels[idx] to (chords[idx] ?: Chord(scale[idx], ChordQuality.MAJOR))
            }
            DominantFor(
                targetIndex = i,
                target = target,
                targetLabel = labels[i],
                dominant = dom,
                label = labelOf(dom),
                primary = i == 0,
                outsideNotes = dom.pitchClasses.filter { it !in scale },
                leadingTone = (target.root + 11) % 12,
                seventh = (target.root + 5) % 12,
                seventhLandsOn = (target.root + third) % 12,
                example = path,
            )
        }
    }
}
