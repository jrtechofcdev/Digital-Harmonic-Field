package com.example.music

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.FuncDominant
import com.example.ui.theme.FuncNeutral
import com.example.ui.theme.FuncSubdominant
import com.example.ui.theme.FuncTonic

/**
 * As três funções tonais clássicas. Cada grau do campo harmônico pertence a uma
 * delas — é o que dá cor e sentido ao movimento harmônico. Manter apenas três
 * famílias (em vez de sete cores avulsas) é mais fiel à teoria e mais legível.
 */
enum class HarmonicFunction(
    val short: String,
    val label: String,
    val tendency: String,
) {
    TONIC("T", "Tônica", "É a casa. Pode descansar aqui ou partir para qualquer acorde."),
    SUBDOMINANT("SD", "Subdominante", "Prepara o caminho — costuma seguir para a dominante."),
    DOMINANT("D", "Dominante", "Tensão máxima — pede para voltar à tônica."),
    NEUTRAL("—", "Passagem", "Acorde de passagem, liga um trecho ao outro.");

    fun color(): Color = when (this) {
        TONIC -> FuncTonic
        SUBDOMINANT -> FuncSubdominant
        DOMINANT -> FuncDominant
        NEUTRAL -> FuncNeutral
    }
}

/**
 * Classifica o grau (pela posição no campo) em sua função tonal.
 * Índices 0..6 correspondem aos sete graus da escala.
 */
fun functionForDegree(index: Int, isMinor: Boolean): HarmonicFunction {
    return if (!isMinor) {
        // Maior: I ii iii IV V vi vii°
        when (index) {
            0 -> HarmonicFunction.TONIC          // I
            1 -> HarmonicFunction.SUBDOMINANT    // ii
            2 -> HarmonicFunction.TONIC          // iii (mediante, função de tônica)
            3 -> HarmonicFunction.SUBDOMINANT    // IV
            4 -> HarmonicFunction.DOMINANT       // V
            5 -> HarmonicFunction.TONIC          // vi (relativa, função de tônica)
            6 -> HarmonicFunction.DOMINANT       // vii° (sensível)
            else -> HarmonicFunction.NEUTRAL
        }
    } else {
        // Menor natural: i ii° III iv v VI VII
        when (index) {
            0 -> HarmonicFunction.TONIC          // i
            1 -> HarmonicFunction.SUBDOMINANT    // ii°
            2 -> HarmonicFunction.TONIC          // III (relativa maior)
            3 -> HarmonicFunction.SUBDOMINANT    // iv
            4 -> HarmonicFunction.DOMINANT       // v
            5 -> HarmonicFunction.SUBDOMINANT    // VI
            6 -> HarmonicFunction.DOMINANT       // VII (subtônica / condução)
            else -> HarmonicFunction.NEUTRAL
        }
    }
}

// ---------------------------------------------------------------------------
// Transposição (cifras e notas em português)
// ---------------------------------------------------------------------------

private val chromaticRoots = listOf(
    "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
)

private val flatToSharp = mapOf(
    "Db" to "C#", "Eb" to "D#", "Gb" to "F#", "Ab" to "G#", "Bb" to "A#"
)

/** Transpõe uma cifra (ex.: "Dm", "F#°", "Bb") por N semitons. */
fun transposeCipher(cipher: String, semitones: Int): String {
    if (semitones == 0 || cipher.isEmpty()) return cipher

    val root: String
    val suffix: String
    if (cipher.length >= 2 && (cipher[1] == '#' || cipher[1] == 'b')) {
        root = cipher.substring(0, 2)
        suffix = cipher.substring(2)
    } else {
        root = cipher.substring(0, 1)
        suffix = cipher.substring(1)
    }

    val normalized = flatToSharp[root] ?: root
    val index = chromaticRoots.indexOf(normalized)
    if (index == -1) return cipher

    val newIndex = ((index + semitones) % 12 + 12) % 12
    return chromaticRoots[newIndex] + suffix
}

private val ptNotesInput = mapOf(
    "Dó" to 0, "Dó#" to 1, "Do" to 0, "Do#" to 1,
    "Ré" to 2, "Ré#" to 3, "Re" to 2, "Re#" to 3,
    "Mi" to 4, "Mí" to 4, "Mi#" to 5, "Mí#" to 5,
    "Fá" to 5, "Fá#" to 6, "Fa" to 5, "Fa#" to 6,
    "Sol" to 7, "Sol#" to 8,
    "Lá" to 9, "Lá#" to 10, "La" to 9, "La#" to 10,
    "Si" to 11, "Si#" to 0, "Sib" to 10
)

private val ptNotesOutput = listOf(
    "Dó", "Dó#", "Ré", "Ré#", "Mi", "Fá", "Fá#", "Sol", "Sol#", "Lá", "Lá#", "Si"
)

fun transposePtNote(note: String, semitones: Int): String {
    val clean = note.trim()
    val index = ptNotesInput[clean] ?: return note
    val newIndex = ((index + semitones) % 12 + 12) % 12
    return ptNotesOutput[newIndex]
}

fun transposePtChord(name: String, semitones: Int): String {
    val parts = name.split(" ", limit = 2)
    if (parts.isEmpty()) return name
    val transposedNote = transposePtNote(parts[0], semitones)
    return if (parts.size > 1) "$transposedNote ${parts[1]}" else transposedNote
}

/** Nome em português de uma cifra latina (ex.: "G#m" -> "Sol# menor"). */
fun cipherToPtName(cipher: String): String {
    if (cipher.isEmpty()) return cipher
    val root: String
    val suffix: String
    if (cipher.length >= 2 && (cipher[1] == '#' || cipher[1] == 'b')) {
        root = cipher.substring(0, 2); suffix = cipher.substring(2)
    } else {
        root = cipher.substring(0, 1); suffix = cipher.substring(1)
    }
    val normalized = flatToSharp[root] ?: root
    val idx = chromaticRoots.indexOf(normalized)
    val ptRoot = if (idx >= 0) ptNotesOutput[idx] else root
    val quality = when {
        suffix.contains("°") || suffix.contains("dim") -> " diminuto"
        suffix.startsWith("m") -> " menor"
        else -> " Maior"
    }
    return ptRoot + quality
}

// ---------------------------------------------------------------------------
// Formação do acorde (fundamental, terça, quinta)
// ---------------------------------------------------------------------------

data class FormulaNote(val note: String, val interval: String, val detail: String)

fun getChordFormulaNotes(cipher: String): List<FormulaNote> {
    var rootCipher = ""
    if (cipher.length >= 2 && (cipher[1] == '#' || cipher[1] == 'b')) {
        rootCipher = cipher.substring(0, 2)
    } else if (cipher.isNotEmpty()) {
        rootCipher = cipher.substring(0, 1)
    }

    val rest = cipher.substring(rootCipher.length)
    val isMinor = rest.startsWith("m") && !rest.contains("maj")
    val isDiminished = rest.contains("°") || rest.contains("dim")

    val rootsMap = mapOf(
        "C" to 0, "C#" to 1, "Db" to 1, "D" to 2, "D#" to 3, "Eb" to 3,
        "E" to 4, "F" to 5, "F#" to 6, "Gb" to 6, "G" to 7, "G#" to 8,
        "Ab" to 8, "A" to 9, "A#" to 10, "Bb" to 10, "B" to 11
    )
    val rootIndex = rootsMap[rootCipher] ?: 0
    val scale = ptNotesOutput
    val rootNote = scale[rootIndex]

    val (thirdNote, thirdLabel) = when {
        isDiminished || isMinor ->
            scale[(rootIndex + 3) % 12] to "Terça menor · caráter grave do acorde"
        else ->
            scale[(rootIndex + 4) % 12] to "Terça maior · caráter brilhante do acorde"
    }
    val (fifthNote, fifthLabel) = if (isDiminished) {
        scale[(rootIndex + 6) % 12] to "Quinta diminuta · a tensão do diminuto"
    } else {
        scale[(rootIndex + 7) % 12] to "Quinta justa · corpo e estabilidade"
    }

    return listOf(
        FormulaNote(rootNote, "Fundamental", "Dá nome e base ao acorde"),
        FormulaNote(thirdNote, "Terça", thirdLabel.substringAfter("· ")),
        FormulaNote(fifthNote, "Quinta", fifthLabel.substringAfter("· ")),
    )
}
