package com.example.music

/**
 * Violão/guitarra em afinação padrão (E A D G B E). Calcula como montar cada
 * acorde no braço: usa a forma "aberta" conhecida quando existe (C, G, D, Am…)
 * e, senão, a pestana mais baixa entre as formas de Mi (raiz na 6ª corda) e de
 * Lá (raiz na 5ª corda).
 */
object Guitar {

    /** Notas MIDI das cordas soltas, da 6ª (Mi grave) para a 1ª (Mi agudo). */
    val TUNING = intArrayOf(40, 45, 50, 55, 59, 64)
    const val STRINGS = 6
    private const val X = -1

    /**
     * Forma do acorde: casa de cada corda (6ª → 1ª), −1 = não tocar, 0 = solta.
     * [barre] = casa da pestana (se houver).
     */
    class Shape(val frets: IntArray, val barre: Int? = null) {
        /** Primeira casa mostrada no desenho (1 = desde a pestana do braço). */
        val baseFret: Int
            get() {
                val pressed = frets.filter { it > 0 }
                return if (pressed.isEmpty() || pressed.max() <= 4) 1 else pressed.min()
            }

        /** Notas MIDI que soam, da mais grave para a mais aguda. */
        val midiNotes: List<Int> get() = frets.indices.filter { frets[it] >= 0 }.map { TUNING[it] + frets[it] }
    }

    // Formas abertas mais usadas (as que o iniciante aprende primeiro).
    private val open = mapOf(
        "C" to intArrayOf(X, 3, 2, 0, 1, 0),
        "D" to intArrayOf(X, X, 0, 2, 3, 2),
        "E" to intArrayOf(0, 2, 2, 1, 0, 0),
        "G" to intArrayOf(3, 2, 0, 0, 0, 3),
        "A" to intArrayOf(X, 0, 2, 2, 2, 0),
        "Am" to intArrayOf(X, 0, 2, 2, 1, 0),
        "Dm" to intArrayOf(X, X, 0, 2, 3, 1),
        "Em" to intArrayOf(0, 2, 2, 0, 0, 0),
        "C7" to intArrayOf(X, 3, 2, 3, 1, 0),
        "D7" to intArrayOf(X, X, 0, 2, 1, 2),
        "E7" to intArrayOf(0, 2, 0, 1, 0, 0),
        "G7" to intArrayOf(3, 2, 0, 0, 0, 1),
        "A7" to intArrayOf(X, 0, 2, 0, 2, 0),
        "B7" to intArrayOf(X, 2, 1, 2, 0, 2),
        "Am7" to intArrayOf(X, 0, 2, 0, 1, 0),
        "Em7" to intArrayOf(0, 2, 0, 0, 0, 0),
        "Dm7" to intArrayOf(X, X, 0, 2, 1, 1),
        "C7M" to intArrayOf(X, 3, 2, 0, 0, 0),
        "D7M" to intArrayOf(X, X, 0, 2, 2, 2),
        "E7M" to intArrayOf(0, 2, 1, 1, 0, 0),
        "G7M" to intArrayOf(3, 2, 0, 0, 0, 2),
        "A7M" to intArrayOf(X, 0, 2, 1, 2, 0),
        "F7M" to intArrayOf(X, X, 3, 2, 1, 0),
    )

    /** Forma "de Mi" (raiz na 6ª corda) na casa [f]. */
    private fun eShape(q: ChordQuality, f: Int): IntArray? = when (q) {
        ChordQuality.MAJOR -> intArrayOf(f, f + 2, f + 2, f + 1, f, f)
        ChordQuality.MINOR -> intArrayOf(f, f + 2, f + 2, f, f, f)
        ChordQuality.DOM7 -> intArrayOf(f, f + 2, f, f + 1, f, f)
        ChordQuality.MAJ7 -> intArrayOf(f, X, f + 1, f + 1, f, X)
        ChordQuality.MIN7 -> intArrayOf(f, f + 2, f, f, f, f)
        ChordQuality.HALFDIM -> if (f >= 1) intArrayOf(f, X, f, f, f - 1, X) else null
        ChordQuality.DIM -> null
    }

    /** Forma "de Lá" (raiz na 5ª corda) na casa [f]. */
    private fun aShape(q: ChordQuality, f: Int): IntArray = when (q) {
        ChordQuality.MAJOR -> intArrayOf(X, f, f + 2, f + 2, f + 2, f)
        ChordQuality.MINOR -> intArrayOf(X, f, f + 2, f + 2, f + 1, f)
        ChordQuality.DOM7 -> intArrayOf(X, f, f + 2, f, f + 2, f)
        ChordQuality.MAJ7 -> intArrayOf(X, f, f + 2, f + 1, f + 2, f)
        ChordQuality.MIN7 -> intArrayOf(X, f, f + 2, f, f + 1, f)
        ChordQuality.DIM -> intArrayOf(X, f, f + 1, f + 2, f + 1, X)
        ChordQuality.HALFDIM -> intArrayOf(X, f, f + 1, f, f + 1, X)
    }

    /** Como montar [chord] no violão (ignora o baixo de inversões). */
    fun shape(chord: Chord): Shape {
        val plain = Chord(chord.root, chord.quality)
        open[plain.cipher]?.let { return Shape(it) }
        val fe = Math.floorMod(chord.root - 4, 12)   // casa da raiz na 6ª corda
        val fa = Math.floorMod(chord.root - 9, 12)   // casa da raiz na 5ª corda
        val candidates = listOfNotNull(
            eShape(chord.quality, fe)?.let { fe to it },
            eShape(chord.quality, fe + 12)?.takeIf { fe == 0 }?.let { fe + 12 to it },
            aShape(chord.quality, fa).let { fa to it },
        )
        val (f, frets) = candidates.minBy { it.first }
        // Pestana: a primeira e a última corda tocadas ficam na mesma casa (o dedo 1 deita).
        val played = frets.indices.filter { frets[it] >= 0 }
        val barre = f.takeIf { it > 0 && frets[played.first()] == f && frets[played.last()] == f }
        return Shape(frets, barre)
    }

    fun shape(cipher: String): Shape? = parseCipher(cipher)?.let { shape(it) }

    /** Classe de altura (0..11) na corda [string] (0 = 6ª) e casa [fret]. */
    fun pitchClassAt(string: Int, fret: Int): Int = (TUNING[string] + fret) % 12
}
