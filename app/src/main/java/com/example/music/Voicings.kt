package com.example.music

import kotlin.math.abs

/**
 * Laboratório de acordes no braço: tipos de acorde "com cor" (9, 7M, 6, sus…),
 * gerador automático de formas (voicings) espalhadas pelo braço e sugestões
 * para trocar o acorde "seco" por um mais harmonioso nos hinos.
 *
 * Tudo puro (sem Android) para poder testar: cada forma gerada só toca notas do
 * acorde, tem as notas obrigatórias e cabe na mão (até 4 dedos, abertura de 4 casas).
 */

/**
 * Tipo de acorde. [intervals] em semitons a partir da fundamental (14 = nona).
 * [optional] = notas que podem ficar de fora no violão (quase sempre a quinta).
 */
enum class ChordType(
    val suffix: String,
    val degreeSuffix: String,
    val intervals: List<Int>,
    val ptName: String,
    val optional: Set<Int> = emptySet(),
) {
    MAJOR("", "", listOf(0, 4, 7), "maior"),
    MINOR("m", "m", listOf(0, 3, 7), "menor"),
    DIM("°", "°", listOf(0, 3, 6), "diminuto"),
    ADD9("(9)", "(9)", listOf(0, 4, 7, 14), "maior com nona", setOf(7)),
    MADD9("m(9)", "m(9)", listOf(0, 3, 7, 14), "menor com nona", setOf(7)),
    SUS2("sus2", "sus2", listOf(0, 2, 7), "com 2ª no lugar da 3ª"),
    SUS4("sus4", "sus4", listOf(0, 5, 7), "com 4ª no lugar da 3ª"),
    SIX("6", "⁶", listOf(0, 4, 7, 9), "maior com sexta", setOf(7)),
    MIN6("m6", "m⁶", listOf(0, 3, 7, 9), "menor com sexta", setOf(7)),
    SIX9("6(9)", "⁶⁽⁹⁾", listOf(0, 4, 7, 9, 14), "com sexta e nona", setOf(7)),
    MAJ7("7M", "⁷M", listOf(0, 4, 7, 11), "com sétima maior", setOf(7)),
    MAJ9("7M(9)", "⁷M⁽⁹⁾", listOf(0, 4, 7, 11, 14), "com sétima maior e nona", setOf(7)),
    DOM7("7", "⁷", listOf(0, 4, 7, 10), "com sétima", setOf(7)),
    DOM9("7(9)", "⁷⁽⁹⁾", listOf(0, 4, 7, 10, 14), "com sétima e nona", setOf(7)),
    SEVSUS4("7sus4", "⁷sus4", listOf(0, 5, 7, 10), "com sétima e 4ª suspensa", setOf(7)),
    MIN7("m7", "m⁷", listOf(0, 3, 7, 10), "menor com sétima", setOf(7)),
    MIN9("m7(9)", "m⁷⁽⁹⁾", listOf(0, 3, 7, 10, 14), "menor com sétima e nona", setOf(7)),
    HALFDIM("m7(b5)", "ø", listOf(0, 3, 6, 10), "meio-diminuto"),
    DIM7("°7", "°⁷", listOf(0, 3, 6, 9), "diminuto com sétima");

    /** Classes de altura relativas (0..11) — a nona (14) vira 2. */
    val pcs: Set<Int> get() = intervals.map { it % 12 }.toSet()

    /** Notas que precisam soar (o resto pode ser omitido). */
    val required: Set<Int> get() = intervals.filter { it !in optional }.map { it % 12 }.toSet()

    /** Nome do papel de uma nota no acorde: "1", "3", "3m", "5", "7M", "9"… */
    fun role(semitones: Int): String = when (Math.floorMod(semitones, 12)) {
        0 -> "1"
        1 -> "b9"
        2 -> if (this == SUS2) "2" else "9"
        3 -> "3m"
        4 -> "3"
        5 -> "4"
        6 -> "5b"
        7 -> "5"
        8 -> "5#"
        9 -> if (this == DIM7) "7°" else "6"
        10 -> "7"
        else -> "7M"
    }

    companion object {
        /** Tipo equivalente às qualidades que o resto do app já usa. */
        fun of(q: ChordQuality): ChordType = when (q) {
            ChordQuality.MAJOR -> MAJOR
            ChordQuality.MINOR -> MINOR
            ChordQuality.DIM -> DIM
            ChordQuality.DOM7 -> DOM7
            ChordQuality.MAJ7 -> MAJ7
            ChordQuality.MIN7 -> MIN7
            ChordQuality.HALFDIM -> HALFDIM
        }
    }
}

/** Acorde do laboratório: fundamental, tipo e baixo (para "C/E", "C/D"…). */
data class LabChord(val root: Int, val type: ChordType, val bass: Int? = null) {
    val cipher: String
        get() = pitchClassCiphers[root] + type.suffix + (bass?.let { "/" + pitchClassCiphers[it] } ?: "")

    val pitchClasses: Set<Int> get() = type.pcs.map { (root + it) % 12 }.toSet()

    /** Nota do baixo (a mais grave). */
    val bassPc: Int get() = bass ?: root

    /** Papel de uma classe de altura neste acorde ("1", "3", "5"…), ou null. */
    fun role(pc: Int): String? {
        val rel = Math.floorMod(pc - root, 12)
        return if (rel in type.pcs) type.role(rel) else null
    }
}

/**
 * Uma forma de tocar o acorde: casa de cada corda (6ª → 1ª), −1 = não tocar.
 * [barre] = casa da pestana, quando o dedo 1 deita; ela cobre da corda
 * [barreFrom] (0 = 6ª) até a corda mais aguda que soa.
 */
class Voicing(val frets: IntArray, val barre: Int?, val score: Double, val barreFrom: Int = -1) {
    val strings: List<Int> get() = frets.indices.filter { frets[it] >= 0 }
    val midiNotes: List<Int> get() = strings.map { Guitar.TUNING[it] + frets[it] }
    val pressed: List<Int> get() = frets.filter { it > 0 }
    val minFret: Int get() = pressed.minOrNull() ?: 0
    val maxFret: Int get() = pressed.maxOrNull() ?: 0
    val usesOpen: Boolean get() = frets.any { it == 0 }

    /** Onde fica no braço — para o rótulo da variação. */
    val position: String
        get() = when {
            pressed.isEmpty() || (usesOpen && maxFret <= 4) -> "Aberto"
            else -> "Casa $minFret"
        }

    /** A corda [s] está debaixo da pestana? */
    fun underBarre(s: Int): Boolean = barre != null && s >= barreFrom && frets[s] == barre

    /** Quantos dedos a mão esquerda usa (pestana conta como 1). */
    val fingers: Int
        get() = if (barre != null) 1 + frets.indices.count { frets[it] > 0 && !underBarre(it) } else pressed.size

    /**
     * Qual dedo aperta cada corda (1 = indicador … 4 = mindinho; 0 = solta ou
     * abafada). Regra das revistinhas: um dedo por casa a partir da casa mais
     * baixa; na mesma casa, a corda mais grave usa o dedo menor.
     */
    val fingering: IntArray
        get() {
            val out = IntArray(frets.size)
            var next = 1
            if (barre != null) {
                frets.indices.filter { underBarre(it) }.forEach { out[it] = 1 }
                next = 2
            }
            val notes = frets.indices.filter { frets[it] > 0 && out[it] == 0 }
                .sortedWith(compareBy({ frets[it] }, { it }))
            val base = minFret
            // 1ª tentativa: um dedo por casa; se passar do 4, vai em sequência.
            val byFret = IntArray(frets.size)
            var prev = next - 1
            var ok = true
            for (s in notes) {
                val f = maxOf(prev + 1, frets[s] - base + 1)
                if (f > 4) { ok = false; break }
                byFret[s] = f; prev = f
            }
            if (ok) notes.forEach { out[it] = byFret[it] } else notes.forEach { out[it] = next++ }
            return out
        }

    val shape: Guitar.Shape get() = Guitar.Shape(frets, barre, barreFrom.takeIf { barre != null })

    /** Mesma forma? (para não repetir na lista) */
    val key: String get() = frets.joinToString(",")
}

object Voicings {
    private const val MAX_FRET = 13
    private const val SPAN = 3          // da casa mais baixa à mais alta: até 4 casas (dedos 1 a 4)

    /**
     * Todas as formas boas de [chord] no braço, as melhores primeiro, uma por
     * região (no máximo [limit]). A primeira é a mais confortável e cheia.
     */
    fun generate(chord: LabChord, limit: Int = 6): List<Voicing> {
        val all = HashMap<String, Voicing>()
        for (w in 1..MAX_FRET - SPAN) search(chord, w, all)
        // A forma "de revistinha" (a que todo mundo aprende) vem sempre na frente.
        familiar(chord)?.let { all[it.frets.joinToString(",")] = it }
        val ranked = all.values.sortedByDescending { it.score }
        // Uma por região do braço (e corda do baixo): evita mostrar formas quase iguais.
        val picked = mutableListOf<Voicing>()
        for (v in ranked) {
            if (picked.size >= limit) break
            val center = v.center()
            val low = v.strings.first()
            if (picked.none { abs(it.center() - center) < 2.0 && it.strings.first() == low }) picked += v
        }
        return picked.sortedBy { it.center() }
    }

    /** TODAS as formas tocáveis de [chord] com casas até [maxFret] (sem filtrar por região). */
    fun allShapes(chord: LabChord, maxFret: Int = MAX_FRET): List<Voicing> {
        val all = HashMap<String, Voicing>()
        for (w in 1..maxFret - SPAN) search(chord, w, all, maxFret)
        familiar(chord)?.let { all[it.key] = it }
        return all.values.toList()
    }

    private fun Voicing.center(): Double =
        if (pressed.isEmpty()) 0.0 else if (usesOpen && maxFret <= 4) 1.0 else pressed.average()

    /** Procura formas com as casas presas entre [w] e w+3 (cordas soltas sempre valem). */
    private fun search(chord: LabChord, w: Int, out: HashMap<String, Voicing>, maxFret: Int = MAX_FRET) {
        val pcs = chord.pitchClasses + chord.bassPc
        val options = Array(Guitar.STRINGS) { s ->
            buildList {
                add(-1)
                if (Guitar.pitchClassAt(s, 0) in pcs) add(0)
                for (f in w..w + SPAN) if (f <= maxFret && Guitar.pitchClassAt(s, f) in pcs) add(f)
            }
        }
        val frets = IntArray(Guitar.STRINGS)
        fun rec(s: Int) {
            if (s == Guitar.STRINGS) {
                evaluate(chord, frets.copyOf())?.let { v ->
                    val k = v.frets.joinToString(",")
                    if ((out[k]?.score ?: Double.NEGATIVE_INFINITY) < v.score) out[k] = v
                }
                return
            }
            for (o in options[s]) { frets[s] = o; rec(s + 1) }
        }
        rec(0)
    }

    /** Valida a forma e dá uma nota (quanto maior, melhor). null = não serve. */
    internal fun evaluate(chord: LabChord, frets: IntArray): Voicing? {
        val sounding = frets.indices.filter { frets[it] >= 0 }
        if (sounding.size < 4) return null
        val low = sounding.first()
        val high = sounding.last()
        // Só pode abafar: as graves antes do baixo, as agudas depois da última
        // e, no máximo, UMA corda logo acima do baixo (o dedo do baixo encosta nela).
        val inner = (low..high).filter { frets[it] < 0 }
        if (inner.size > 1 || (inner.size == 1 && inner[0] != low + 1)) return null

        val notes = sounding.map { Guitar.TUNING[it] + frets[it] }
        if (Math.floorMod(notes.first(), 12) != chord.bassPc) return null
        val rel = notes.map { Math.floorMod(it - chord.root, 12) }.toSet()
        if (!rel.containsAll(chord.type.required)) return null
        // Nas inversões ("C/E") a fundamental também precisa aparecer.
        if (chord.bass != null && 0 !in rel) return null

        val pressed = frets.filter { it > 0 }
        val minF = pressed.minOrNull() ?: 0
        val maxF = pressed.maxOrNull() ?: 0
        if (pressed.isNotEmpty() && maxF - minF > SPAN) return null
        // Corda solta junto de acorde lá no alto fica difícil de soar bem.
        if (frets.any { it == 0 } && minF > 9) return null

        // Dedos: com 5 ou mais casas presas, o dedo 1 deita (pestana) na casa mais
        // baixa, de uma corda até a mais aguda que soa. Com até 4, um dedo por nota
        // (é como as revistinhas ensinam o Ré, o Lá, o Dó…).
        var barre: Int? = null
        var barreFrom = -1
        var fingers = pressed.size
        if (pressed.size > 4) {
            for (from in frets.indices.filter { frets[it] == minF }) {
                if (!(from..high).all { frets[it] >= minF }) continue
                val n = 1 + frets.indices.count { frets[it] > 0 && !(it >= from && frets[it] == minF) }
                if (barre == null || n < fingers) { barre = minF; barreFrom = from; fingers = n }
            }
        }
        if (fingers > 4) return null

        // Nota: cheio, confortável, com as cores do acorde e sem embolar o grave.
        var score = sounding.size * 1.2
        score -= fingers * 0.5
        score -= (maxF - minF) * 0.6
        if (barre != null) score -= 1.0
        score -= inner.size * 0.8
        score += frets.count { it == 0 } * 0.5                    // cordas soltas soam mais
        score -= minF * 0.06                                      // perto da mão é mais fácil
        if (rel.size == chord.type.pcs.size) score += 1.0         // acorde completo
        // Notas coladas (segunda) lá no grave embolam o som.
        notes.zip(notes.drop(1)).forEach { (a, b) -> if (b - a <= 2 && a < 50) score -= 1.2 }
        // A nota de "cor" (9, 6, 7M, 4) em cima canta melhor.
        val top = Math.floorMod(notes.last() - chord.root, 12)
        if (chord.type.pcs.size > 3 && top !in setOf(0, 7)) score += 0.6
        return Voicing(frets, barre, score, barreFrom)
    }

    private fun familiar(chord: LabChord): Voicing? {
        if (chord.bass != null) return null
        val q = ChordQuality.entries.firstOrNull { ChordType.of(it) == chord.type } ?: return null
        val shape = Guitar.shape(Chord(chord.root, q))
        return evaluate(chord, shape.frets)?.let { Voicing(it.frets, it.barre, it.score + 4.0, it.barreFrom) }
    }

    /** A melhor forma (a primeira, mais confortável). */
    fun best(chord: LabChord): Voicing? = generate(chord, 6).maxByOrNull { it.score }
}

/** Uma ideia de harmonização para um grau. */
data class Harmonization(
    val label: String,           // "1(9)", "5sus4", "4⁷M", "1/3"…
    val chord: LabChord,
    val why: String,
)

/**
 * Trocas que deixam o hino mais "harmonioso" sem mudar a função do acorde.
 * O 1 continua sendo casa, o 5 continua pedindo para voltar — só ganham cor.
 */
object Harmonize {
    fun forDegree(isMinor: Boolean, tonic: Int, degree: Int): List<Harmonization> {
        val scale = if (!isMinor) listOf(0, 2, 4, 5, 7, 9, 11) else listOf(0, 2, 3, 5, 7, 8, 10)
        val r = (tonic + scale[degree]) % 12
        val n = degree + 1
        fun h(type: ChordType, why: String, root: Int = r, bass: Int? = null, label: String? = null) =
            Harmonization(label ?: "$n${type.degreeSuffix}", LabChord(root, type, bass), why)
        fun deg(d: Int) = (tonic + scale[d]) % 12

        return if (!isMinor) when (degree) {
            0 -> listOf(
                h(ChordType.MAJOR, "O básico: firme, de casa."),
                h(ChordType.ADD9, "O mais versátil: abre o som sem mudar nada. Serve em quase todo hino."),
                h(ChordType.SIX, "Som de hino clássico, ótimo no fim da estrofe."),
                h(ChordType.MAJ7, "Suave e calmo. Se a voz estiver cantando a nota do 1, prefira o (9)."),
                h(ChordType.SIX9, "Final de hino \"aberto\", bem de igreja."),
                h(ChordType.MAJOR, "Baixo na 3ª: caminha suave até o 4 ou o 2m.", bass = deg(2), label = "1/3"),
            )
            1 -> listOf(
                h(ChordType.MINOR, "O básico."),
                h(ChordType.MIN7, "O jeito mais comum de preparar o 5 (2m⁷ → 5 → 1)."),
                h(ChordType.MIN9, "Mais aveludado, para hino lento."),
                h(ChordType.DOM7, "\"5 do 5\": vira maior com sétima e puxa forte para o 5.", label = "2⁷"),
            )
            2 -> listOf(
                h(ChordType.MINOR, "O básico."),
                h(ChordType.MIN7, "Liga bem o 1 ao 4 ou ao 6m."),
                h(ChordType.DOM7, "\"5 do 6m\": maior com sétima, leva ao 6m com emoção.", label = "3⁷"),
            )
            3 -> listOf(
                h(ChordType.MAJOR, "O básico."),
                h(ChordType.ADD9, "Brilha sem perder a cara de 4."),
                h(ChordType.MAJ7, "Som aberto de louvor; ótimo em hino calmo."),
                h(ChordType.SIX, "Clássico de hinário, antes de voltar ao 1."),
                h(ChordType.MINOR, "Emprestado do menor: o \"4 com saudade\" antes do Amém.", label = "4m"),
            )
            4 -> listOf(
                h(ChordType.MAJOR, "O básico."),
                h(ChordType.DOM7, "A sétima aumenta a vontade de voltar ao 1."),
                h(ChordType.SUS4, "Segura a tensão: toque sus4 e depois o 5 normal (5sus4 → 5)."),
                h(ChordType.SEVSUS4, "Tensão macia, ótima no fim da frase antes do 1."),
                h(ChordType.DOM9, "Dominante \"cheia\", de quarteto e coral."),
                h(ChordType.MAJOR, "O 4 com baixo no 5: som moderno e suave para o 5.", root = deg(3), bass = r, label = "4/5"),
            )
            5 -> listOf(
                h(ChordType.MINOR, "O básico."),
                h(ChordType.MIN7, "Mais macio; liga bem com o 2m ou o 4."),
                h(ChordType.MADD9, "Menor com nona: emoção sem pesar."),
                h(ChordType.MIN9, "Bem aveludado, para hino de consagração."),
            )
            else -> listOf(
                h(ChordType.DIM, "O básico (pouco usado nos hinos)."),
                h(ChordType.HALFDIM, "Mais suave que o diminuto."),
                h(ChordType.MAJOR, "Troque pelo 5 com baixo no 7: soa mais \"igreja\".", root = deg(4), bass = r, label = "5/7"),
            )
        } else when (degree) {
            0 -> listOf(
                h(ChordType.MINOR, "O básico."),
                h(ChordType.MADD9, "Menor com nona: o mais bonito para hinos de saudade."),
                h(ChordType.MIN7, "Mais macio, bom no meio da estrofe."),
                h(ChordType.MIN6, "Clima antigo, de hino solene."),
            )
            1 -> listOf(
                h(ChordType.DIM, "O básico."),
                h(ChordType.HALFDIM, "Prepara o 5 com suavidade (2ø → 5⁷ → 1m)."),
            )
            2 -> listOf(
                h(ChordType.MAJOR, "O básico: a \"luz\" do tom menor."),
                h(ChordType.MAJ7, "Abre o som; ótimo em hino calmo."),
                h(ChordType.ADD9, "Brilho sem perder o clima."),
            )
            3 -> listOf(
                h(ChordType.MINOR, "O básico."),
                h(ChordType.MIN7, "Mais macio antes do 5."),
                h(ChordType.MADD9, "Com nona: mais emoção."),
            )
            4 -> listOf(
                h(ChordType.MINOR, "O do campo (pouco usado nos hinos)."),
                h(ChordType.MAJOR, "5 maior: a volta para o 1m fica forte (é o mais usado).", label = "5"),
                h(ChordType.DOM7, "5 com sétima: o caminho clássico para o 1m.", label = "5⁷"),
                h(ChordType.SUS4, "Segure no sus4 e resolva no 5 maior.", label = "5sus4"),
            )
            5 -> listOf(
                h(ChordType.MAJOR, "O básico."),
                h(ChordType.MAJ7, "Som aberto, bonito em tom menor."),
                h(ChordType.ADD9, "Brilho com saudade."),
            )
            else -> listOf(
                h(ChordType.MAJOR, "O básico."),
                h(ChordType.DOM7, "Puxa para o 3 (o relativo maior)."),
            )
        }
    }

    /**
     * Um mesmo trecho tocado "seco" e "harmonioso", para ouvir a diferença.
     * Graus: 1 – 4 – 5 – 1 (maior) ou 1m – 4m – 5 – 1m (menor).
     */
    fun beforeAfter(isMinor: Boolean, tonic: Int): Pair<List<Harmonization>, List<Harmonization>> {
        val scale = if (!isMinor) listOf(0, 2, 4, 5, 7, 9, 11) else listOf(0, 2, 3, 5, 7, 8, 10)
        fun r(d: Int) = (tonic + scale[d]) % 12
        return if (!isMinor) {
            listOf(
                Harmonization("1", LabChord(r(0), ChordType.MAJOR), ""),
                Harmonization("4", LabChord(r(3), ChordType.MAJOR), ""),
                Harmonization("5", LabChord(r(4), ChordType.MAJOR), ""),
                Harmonization("1", LabChord(r(0), ChordType.MAJOR), ""),
            ) to listOf(
                Harmonization("1(9)", LabChord(r(0), ChordType.ADD9), ""),
                Harmonization("4⁷M", LabChord(r(3), ChordType.MAJ7), ""),
                Harmonization("5sus4", LabChord(r(4), ChordType.SUS4), ""),
                Harmonization("5", LabChord(r(4), ChordType.MAJOR), ""),
                Harmonization("1⁶⁽⁹⁾", LabChord(r(0), ChordType.SIX9), ""),
            )
        } else {
            listOf(
                Harmonization("1m", LabChord(r(0), ChordType.MINOR), ""),
                Harmonization("4m", LabChord(r(3), ChordType.MINOR), ""),
                Harmonization("5", LabChord(r(4), ChordType.MAJOR), ""),
                Harmonization("1m", LabChord(r(0), ChordType.MINOR), ""),
            ) to listOf(
                Harmonization("1m(9)", LabChord(r(0), ChordType.MADD9), ""),
                Harmonization("4m⁷", LabChord(r(3), ChordType.MIN7), ""),
                Harmonization("5sus4", LabChord(r(4), ChordType.SUS4), ""),
                Harmonization("5⁷", LabChord(r(4), ChordType.DOM7), ""),
                Harmonization("1m(9)", LabChord(r(0), ChordType.MADD9), ""),
            )
        }
    }
}

/** Jogos para decorar o braço: "ache a nota" e "que nota é esta?". */
object NeckQuiz {
    /** Casas 0..12 já cobrem o braço todo (da 12 em diante repete). */
    const val LAST_FRET = 12

    /** Todas as posições (corda, casa) de uma nota entre as casas [from]..[to]. */
    fun positions(pc: Int, from: Int = 0, to: Int = LAST_FRET): List<Pair<Int, Int>> =
        (0 until Guitar.STRINGS).flatMap { s -> (from..to).filter { Guitar.pitchClassAt(s, it) == pc }.map { s to it } }

    /** Fase de estudo: começa nas notas naturais das cordas graves. */
    enum class Stage(val title: String, val strings: List<Int>, val naturalsOnly: Boolean) {
        SIXTH("6ª corda", listOf(0), true),
        FIFTH("5ª corda", listOf(1), true),
        BASS("6ª + 5ª", listOf(0, 1), true),
        ALL("Braço todo", listOf(0, 1, 2, 3, 4, 5), false),
    }

    private val naturals = listOf(0, 2, 4, 5, 7, 9, 11)

    data class Spot(val string: Int, val fret: Int) {
        val pc: Int get() = Guitar.pitchClassAt(string, fret)
    }

    /** Uma posição sorteada da fase (diferente de [avoid]). */
    fun spot(stage: Stage, random: kotlin.random.Random = kotlin.random.Random, avoid: Spot? = null): Spot {
        val pool = stage.strings.flatMap { s ->
            (0..LAST_FRET).map { Spot(s, it) }
        }.filter { !stage.naturalsOnly || it.pc in naturals }.filter { it != avoid }
        return pool.random(random)
    }

    /** Opções de resposta para "que nota é esta?". */
    fun choices(stage: Stage): List<Int> = if (stage.naturalsOnly) naturals else (0..11).toList()
}
