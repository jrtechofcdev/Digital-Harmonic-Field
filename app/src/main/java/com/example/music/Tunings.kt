package com.example.music

/**
 * Afinações de violão/guitarra de 6 cordas. As cordas são listadas da 6ª (mais
 * grave) para a 1ª (mais aguda), como o músico costuma ver o afinador.
 */
data class TuningString(val order: Int, val midi: Int) {
    val noteName: String get() = midiToName(midi)                 // ex.: "E2"
    val letter: String get() = noteName.dropLast(1)               // ex.: "E"
    val ptName: String get() = ptPitchClass(midi)                 // ex.: "Mi"
    fun targetFreq(refA: Double): Double = midiToFrequency(midi, refA)
}

data class Tuning(
    val id: String,
    val name: String,
    val description: String,
    val strings: List<TuningString>, // 6ª → 1ª
) {
    /** Sequência em letras, da 6ª à 1ª (ex.: "E A D G B E"). */
    val letters: String get() = strings.joinToString(" ") { it.letter }
}

private val ptPitchNames = listOf("Dó", "Dó#", "Ré", "Ré#", "Mi", "Fá", "Fá#", "Sol", "Sol#", "Lá", "Lá#", "Si")

fun ptPitchClass(midi: Int): String = ptPitchNames[Math.floorMod(midi, 12)]

private fun strings(vararg midi: Int): List<TuningString> {
    // midi vem da 6ª para a 1ª corda.
    return midi.mapIndexed { i, m -> TuningString(order = 6 - i, midi = m) }
}

object TuningLibrary {

    val padrao = Tuning(
        id = "padrao",
        name = "Padrão",
        description = "A afinação universal (E A D G B E). Base da maioria das músicas em " +
            "qualquer estilo. Se está começando, use esta.",
        strings = strings(40, 45, 50, 55, 59, 64), // E2 A2 D3 G3 B3 E4
    )

    val dropD = Tuning(
        id = "drop_d",
        name = "Drop D",
        description = "Só a 6ª corda desce para Ré (D A D G B E). Deixa o som mais grave e " +
            "encorpado e facilita power chords. Muito usada no rock e em dedilhados.",
        strings = strings(38, 45, 50, 55, 59, 64), // D2 A2 D3 G3 B3 E4
    )

    val meioTom = Tuning(
        id = "meio_tom",
        name = "½ tom abaixo",
        description = "Tudo meio tom abaixo (Eb Ab Db Gb Bb Eb). Cordas mais macias e voz mais " +
            "confortável. Clássica no rock e no blues.",
        strings = strings(39, 44, 49, 54, 58, 63), // Eb2 Ab2 Db3 Gb3 Bb3 Eb4
    )

    val dropC = Tuning(
        id = "drop_c",
        name = "Drop C",
        description = "Som pesado e grave (C G C F A D). Preferida no metal moderno e no hard rock.",
        strings = strings(36, 43, 48, 53, 57, 62), // C2 G2 C3 F3 A3 D4
    )

    val openG = Tuning(
        id = "open_g",
        name = "Open G",
        description = "Cordas soltas formam um acorde de Sol (D G D G B D). Marca do Keith Richards; " +
            "ótima para slide e blues.",
        strings = strings(38, 43, 50, 55, 59, 62), // D2 G2 D3 G3 B3 D4
    )

    val openD = Tuning(
        id = "open_d",
        name = "Open D",
        description = "Cordas soltas formam um acorde de Ré (D A D F# A D). Muito usada com slide e " +
            "no fingerstyle acústico.",
        strings = strings(38, 45, 50, 54, 57, 62), // D2 A2 D3 F#3 A3 D4
    )

    val all = listOf(padrao, dropD, meioTom, dropC, openG, openD)

    fun byId(id: String): Tuning = all.firstOrNull { it.id == id } ?: padrao
}
