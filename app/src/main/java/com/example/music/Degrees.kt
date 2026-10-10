package com.example.music

import com.example.HarmonicField

/**
 * Graus "por número" — como muito músico de igreja toca: "vai no 4", "faz o 6m".
 *
 * Campo maior: 1  2m  3m  4  5  6m  7°
 * Campo menor: 1m 2°  3   4m 5m 6   7   (o 5 costuma virar MAIOR nos hinos)
 *
 * Tudo aqui é calculado a partir do campo, então vale para qualquer tom.
 */

/** Qualidade do acorde, com o sufixo de cifra e os intervalos (em semitons). */
enum class ChordQuality(val suffix: String, val degreeSuffix: String, val intervals: List<Int>, val ptName: String) {
    MAJOR("", "", listOf(0, 4, 7), "maior"),
    MINOR("m", "m", listOf(0, 3, 7), "menor"),
    DIM("°", "°", listOf(0, 3, 6), "diminuto"),
    DOM7("7", "⁷", listOf(0, 4, 7, 10), "com sétima"),
    MAJ7("7M", "⁷M", listOf(0, 4, 7, 11), "com sétima maior"),
    MIN7("m7", "m⁷", listOf(0, 3, 7, 10), "menor com sétima"),
    HALFDIM("m7(b5)", "ø", listOf(0, 3, 6, 10), "meio-diminuto"),
}

/** Acorde já entendido: fundamental (0 = Dó), qualidade e baixo (se invertido). */
data class Chord(val root: Int, val quality: ChordQuality, val bass: Int? = null) {
    val cipher: String
        get() = pitchClassCiphers[root] + quality.suffix + (bass?.let { "/" + pitchClassCiphers[it] } ?: "")

    /** Classes de altura do acorde (0..11). */
    val pitchClasses: List<Int> get() = quality.intervals.map { (root + it) % 12 }
}

private val flatToSharp = mapOf("Db" to "C#", "Eb" to "D#", "Gb" to "F#", "Ab" to "G#", "Bb" to "A#")

/** Fundamental de uma cifra ("F#m" → 6, "Bb" → 10). */
fun rootPc(cipher: String): Int? {
    if (cipher.isEmpty()) return null
    val name = if (cipher.length >= 2 && (cipher[1] == '#' || cipher[1] == 'b')) cipher.substring(0, 2) else cipher.substring(0, 1)
    val idx = pitchClassCiphers.indexOf(flatToSharp[name] ?: name)
    return idx.takeIf { it >= 0 }
}

/** Lê cifras do app: "C", "C#m", "B°", "G7", "C7M", "Am7", "Bm7(b5)", "D/F#". */
fun parseCipher(cipher: String): Chord? {
    val main = cipher.substringBefore("/")
    val bass = cipher.substringAfter("/", "").takeIf { it.isNotEmpty() }?.let { rootPc(it) }
    val root = rootPc(main) ?: return null
    val rootLen = if (main.length >= 2 && (main[1] == '#' || main[1] == 'b')) 2 else 1
    val rest = main.substring(rootLen)
    val quality = when (rest) {
        "" -> ChordQuality.MAJOR
        "m" -> ChordQuality.MINOR
        "°", "dim" -> ChordQuality.DIM
        "7" -> ChordQuality.DOM7
        "7M", "maj7" -> ChordQuality.MAJ7
        "m7" -> ChordQuality.MIN7
        "m7(b5)", "ø" -> ChordQuality.HALFDIM
        else -> return null
    }
    return Chord(root, quality, bass)
}

/** Rótulo por número: grau 0..6 do campo + cifra → "1", "2m", "7°", "1m"… */
fun degreeLabel(index: Int, cipher: String): String {
    val q = parseCipher(cipher)?.quality ?: ChordQuality.MAJOR
    return "${index + 1}${q.degreeSuffix}"
}

/** Sequência em números de uma progressão (ex.: "1 – 6m – 2m – 5 – 1"). */
fun degreeSequence(degrees: List<Int>, field: HarmonicField): String =
    degrees.joinToString(" – ") { i -> field.chords.getOrNull(i)?.let { degreeLabel(i, it.cipher) } ?: "?" }

/** Um exemplo de "grau a fundo": rótulo em números, acorde real no tom e explicação curta. */
data class DegreeExample(val label: String, val chord: Chord, val note: String = "")

/**
 * Exemplos calculados para um campo — usados no módulo "Graus a fundo".
 * Os nomes seguem o jeito brasileiro: 5⁷, 2m⁷, 7ø, "5 do 4" etc.
 */
class DegreeTheory(private val field: HarmonicField) {
    private val roots = field.chords.map { rootPc(it.cipher) ?: 0 }
    private val tonic = roots[0]
    private fun pc(semitones: Int) = (tonic + semitones + 120) % 12

    /** Os 7 graus do campo como acordes. */
    val triads: List<DegreeExample> = field.chords.mapIndexed { i, c ->
        DegreeExample(degreeLabel(i, c.cipher), parseCipher(c.cipher) ?: Chord(roots[i], ChordQuality.MAJOR))
    }

    /** Os 7 graus com sétima (tétrades). */
    val sevenths: List<DegreeExample> = run {
        val q = if (!field.isMinor) listOf(
            ChordQuality.MAJ7, ChordQuality.MIN7, ChordQuality.MIN7, ChordQuality.MAJ7,
            ChordQuality.DOM7, ChordQuality.MIN7, ChordQuality.HALFDIM,
        ) else listOf(
            ChordQuality.MIN7, ChordQuality.HALFDIM, ChordQuality.MAJ7, ChordQuality.MIN7,
            ChordQuality.MIN7, ChordQuality.MAJ7, ChordQuality.DOM7,
        )
        roots.mapIndexed { i, r ->
            val label = when (q[i]) {
                ChordQuality.HALFDIM -> "${i + 1}ø"
                else -> "${i + 1}${q[i].degreeSuffix}"
            }
            DegreeExample(label, Chord(r, q[i]))
        }
    }

    /**
     * Dominantes secundárias: "o 5 de cada grau" — um acorde maior com sétima que
     * puxa para outro grau do campo como se fosse a tônica dele.
     */
    val secondaryDominants: List<Pair<DegreeExample, DegreeExample>> = run {
        val targets = if (!field.isMinor) listOf(3, 4, 5, 1, 2) else listOf(3, 5, 2)
        targets.map { t ->
            val target = triads[t]
            val dom = Chord((roots[t] + 7) % 12, ChordQuality.DOM7)
            val label = when {
                !field.isMinor && t == 3 -> "1⁷"            // o 1 com sétima leva ao 4
                !field.isMinor && t == 4 -> "2⁷"            // o 2 maior leva ao 5
                !field.isMinor && t == 5 -> "3⁷"            // o 3 maior leva ao 6m
                !field.isMinor && t == 1 -> "6⁷"            // o 6 maior leva ao 2m
                !field.isMinor && t == 2 -> "7⁷"            // o 7 maior leva ao 3m
                else -> "5/${target.label}"
            }
            DegreeExample(label, dom, "5 do ${target.label}") to target
        }
    }

    /** Diminutos de passagem: ligam dois graus subindo meio tom. */
    val passingDiminished: List<Triple<DegreeExample, DegreeExample, DegreeExample>> =
        if (field.isMinor) emptyList() else listOf(
            Triple(triads[0], DegreeExample("#1°", Chord(pc(1), ChordQuality.DIM)), triads[1]),
            Triple(triads[3], DegreeExample("#4°", Chord(pc(6), ChordQuality.DIM)), triads[4]),
            Triple(triads[4], DegreeExample("#5°", Chord(pc(8), ChordQuality.DIM)), triads[5]),
        )

    /** Baixo caminhando com inversões (acorde/baixo). */
    val walkingBass: List<List<DegreeExample>> =
        if (field.isMinor) emptyList() else listOf(
            // 1 – 5/7 – 6m: o baixo desce 1 → 7 → 6
            listOf(
                triads[0],
                DegreeExample("5/7", Chord(roots[4], ChordQuality.MAJOR, roots[6])),
                triads[5],
            ),
            // 1 – 1/3 – 4: o baixo sobe 1 → 3 → 4
            listOf(
                triads[0],
                DegreeExample("1/3", Chord(roots[0], ChordQuality.MAJOR, roots[2])),
                triads[3],
            ),
            // 4 – 1/3 – 2m – 1: a descida clássica de fim de hino
            listOf(
                triads[3],
                DegreeExample("1/3", Chord(roots[0], ChordQuality.MAJOR, roots[2])),
                triads[1],
                triads[0],
            ),
        )

    /** Empréstimo do tom menor: o 4m antes do 1 ("Amém" com saudade). */
    val borrowedFourMinor: List<DegreeExample> =
        if (field.isMinor) emptyList() else listOf(
            triads[3],
            DegreeExample("4m", Chord(roots[3], ChordQuality.MINOR), "emprestado"),
            triads[0],
        )

    /** No tom menor, o 5 vira MAIOR (com a sensível) para a volta ficar forte. */
    val minorWithMajorFive: List<DegreeExample> =
        if (!field.isMinor) emptyList() else listOf(
            triads[0],
            triads[3],
            DegreeExample("5", Chord(roots[4], ChordQuality.MAJOR), "5 maior"),
            DegreeExample("5⁷", Chord(roots[4], ChordQuality.DOM7)),
            triads[0],
        )
}

/**
 * Notas MIDI para tocar um acorde (sintetizador do treino de ouvido): baixo uma
 * oitava abaixo + acorde fechado na região média.
 */
fun chordMidiNotes(chord: Chord): List<Int> {
    val base = 48 + chord.root                       // fundamental na região de Dó3
    val voiced = chord.quality.intervals.map { base + it }.map { if (it > 64) it - 12 else it }
    val bass = 36 + (chord.bass ?: chord.root)       // baixo na oitava 2
    return listOf(bass) + voiced
}
