package com.example.music

import com.example.HarmonicDatabase
import kotlin.random.Random

/**
 * Perguntas do treino de graus. Puro (sem Android), para testar.
 *
 * - [Kind.EAR]: o app toca o 1 (a "casa") e depois um acorde — qual é o grau?
 * - [Kind.CIPHER_TO_DEGREE]: "No tom de Sol, Dó é qual grau?"
 * - [Kind.DEGREE_TO_CIPHER]: "No tom de Ré, qual acorde é o 5?"
 */
object DegreeQuiz {

    enum class Kind { EAR, CIPHER_TO_DEGREE, DEGREE_TO_CIPHER }

    /** Níveis: quais graus entram (índices 0..6 do campo maior). */
    enum class Level(val title: String, val degrees: List<Int>) {
        BASICO("1 · 4 · 5", listOf(0, 3, 4)),
        MEDIO("+ 6m", listOf(0, 3, 4, 5)),
        COMPLETO("+ 2m · 3m", listOf(0, 1, 2, 3, 4, 5)),
    }

    class Question(
        val kind: Kind,
        val key: String,              // tom (sempre maior no treino)
        val degree: Int,              // grau certo (0..6)
        val chordCipher: String,      // acorde do grau no tom
        val options: List<String>,    // respostas possíveis (rótulos de grau ou cifras)
        val answer: String,
    ) {
        /** Notas para tocar: primeiro o 1 (referência), depois o acorde do grau. */
        val playback: List<List<Int>>
            get() {
                val tonic = parseCipher(key) ?: return emptyList()
                val target = parseCipher(chordCipher) ?: return emptyList()
                return listOf(chordMidiNotes(tonic), chordMidiNotes(target))
            }
    }

    private val majorKeys = listOf("C", "D", "E", "F", "G", "A", "A#", "D#")

    fun next(kind: Kind, level: Level, random: Random = Random.Default, avoid: Int? = null): Question {
        val key = majorKeys[random.nextInt(majorKeys.size)]
        val field = HarmonicDatabase.getField(key)!!
        var degree = level.degrees[random.nextInt(level.degrees.size)]
        // Evita repetir a mesma resposta duas vezes seguidas.
        if (degree == avoid && level.degrees.size > 1) degree = level.degrees.first { it != avoid }
        val chord = field.chords[degree].cipher
        return when (kind) {
            Kind.EAR, Kind.CIPHER_TO_DEGREE -> {
                val opts = level.degrees.map { degreeLabel(it, field.chords[it].cipher) }
                Question(kind, key, degree, chord, opts, degreeLabel(degree, chord))
            }
            Kind.DEGREE_TO_CIPHER -> {
                val opts = level.degrees.map { field.chords[it].cipher }
                Question(kind, key, degree, chord, opts, chord)
            }
        }
    }

    /** O que cada grau "faz" — dica mostrada depois da resposta. */
    fun feeling(degree: Int): String = when (degree) {
        0 -> "O 1 é a casa: soa resolvido, dá vontade de parar aí."
        1 -> "O 2m prepara: soa \"a caminho\", quase sempre antes do 5."
        2 -> "O 3m é passagem: parecido com o 1, mas mais suave e triste."
        3 -> "O 4 abre: levanta, \"sai de casa\" sem tensão."
        4 -> "O 5 é tensão: parece que pede para voltar ao 1."
        5 -> "O 6m é o lado triste do 1: repousa, mas com saudade."
        else -> "O 7° é tensão forte e rara: puxa para o 1."
    }
}
