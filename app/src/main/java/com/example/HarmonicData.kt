package com.example

import com.example.music.HarmonicFunction
import com.example.music.functionForDegree

data class ChordInfo(
    val degree: String,          // e.g. "I", "ii", "iii", "IV", "V", "vi", "vii°"
    val cipher: String,          // e.g. "C", "Dm", "Em", "F", "G", "Am", "B°"
    val functionName: String,    // rótulo bruto (não usado na UI; ver `function`)
    val functionFeel: String,    // rótulo bruto (não usado na UI; ver `function`)
    val portugueseName: String,  // e.g. "Dó Maior", "Ré menor", "Si diminuto"
    val function: HarmonicFunction = HarmonicFunction.TONIC
)

data class HarmonicField(
    val keyCipher: String,       // e.g. "C", "Am"
    val keyNamePt: String,       // e.g. "Dó Maior", "Lá menor"
    val isMinor: Boolean,
    val scaleNotes: String,      // e.g. "C — D — E — F — G — A — B"
    val chords: List<ChordInfo>
)

object HarmonicDatabase {
    val majorFields = listOf(
        // C Major (Dó Maior)
        HarmonicField(
            keyCipher = "C",
            keyNamePt = "Dó Maior",
            isMinor = false,
            scaleNotes = "Dó — Ré — Mi — Fá — Sol — Lá — Si",
            chords = listOf(
                ChordInfo("I", "C", "Tônica", "Repouso", "Dó Maior"),
                ChordInfo("ii", "Dm", "Subdominante", "Passagem", "Ré menor"),
                ChordInfo("iii", "Em", "Mediante", "Repouso suave", "Mi menor"),
                ChordInfo("IV", "F", "Subdominante", "Preparação", "Fá Maior"),
                ChordInfo("V", "G", "Dominante", "Tensão", "Sol Maior"),
                ChordInfo("vi", "Am", "Rel. menor", "Repouso suave", "Lá menor"),
                ChordInfo("vii°", "B°", "Sensível", "Tensão forte", "Si diminuto")
            )
        ),
        // D Major (Ré Maior)
        HarmonicField(
            keyCipher = "D",
            keyNamePt = "Ré Maior",
            isMinor = false,
            scaleNotes = "Ré — Mi — Fá# — Sol — Lá — Si — Dó#",
            chords = listOf(
                ChordInfo("I", "D", "Tônica", "Repouso", "Ré Maior"),
                ChordInfo("ii", "Em", "Subdominante", "Passagem", "Mi menor"),
                ChordInfo("iii", "F#m", "Mediante", "Repouso suave", "Fá# menor"),
                ChordInfo("IV", "G", "Subdominante", "Preparação", "Sol Maior"),
                ChordInfo("V", "A", "Dominante", "Tensão", "Lá Maior"),
                ChordInfo("vi", "Bm", "Rel. menor", "Repouso suave", "Si menor"),
                ChordInfo("vii°", "C#°", "Sensível", "Tensão forte", "Dó# diminuto")
            )
        ),
        // E Major (Mi Maior)
        HarmonicField(
            keyCipher = "E",
            keyNamePt = "Mi Maior",
            isMinor = false,
            scaleNotes = "Mi — Fá# — Sol# — Lá — Si — Dó# — Ré#",
            chords = listOf(
                ChordInfo("I", "E", "Tônica", "Repouso", "Mi Maior"),
                ChordInfo("ii", "F#m", "Subdominante", "Passagem", "Fá# menor"),
                ChordInfo("iii", "G#m", "Mediante", "Repouso suave", "Sol# menor"),
                ChordInfo("IV", "A", "Subdominante", "Preparação", "Lá Maior"),
                ChordInfo("V", "B", "Dominante", "Tensão", "Si Maior"),
                ChordInfo("vi", "C#m", "Rel. menor", "Repouso suave", "Dó# menor"),
                ChordInfo("vii°", "D#°", "Sensível", "Tensão forte", "Ré# diminuto")
            )
        ),
        // F Major (Fá Maior)
        HarmonicField(
            keyCipher = "F",
            keyNamePt = "Fá Maior",
            isMinor = false,
            scaleNotes = "Fá — Sol — Lá — Sib — Dó — Ré — Mi",
            chords = listOf(
                ChordInfo("I", "F", "Tônica", "Repouso", "Fá Maior"),
                ChordInfo("ii", "Gm", "Subdominante", "Passagem", "Ré menor"),
                ChordInfo("iii", "Am", "Mediante", "Repouso suave", "Lá menor"),
                ChordInfo("IV", "Bb", "Subdominante", "Preparação", "Sib Maior"),
                ChordInfo("V", "C", "Dominante", "Tensão", "Dó Maior"),
                ChordInfo("vi", "Dm", "Rel. menor", "Repouso suave", "Ré menor"),
                ChordInfo("vii°", "E°", "Sensível", "Tensão forte", "Mi diminuto")
            )
        ),
        // G Major (Sol Maior)
        HarmonicField(
            keyCipher = "G",
            keyNamePt = "Sol Maior",
            isMinor = false,
            scaleNotes = "Sol — Lá — Si — Dó — Ré — Mi — Fá#",
            chords = listOf(
                ChordInfo("I", "G", "Tônica", "Repouso", "Sol Maior"),
                ChordInfo("ii", "Am", "Subdominante", "Passagem", "Lá menor"),
                ChordInfo("iii", "Bm", "Mediante", "Repouso suave", "Si menor"),
                ChordInfo("IV", "C", "Subdominante", "Preparação", "Dó Maior"),
                ChordInfo("V", "D", "Dominante", "Tensão", "Ré Maior"),
                ChordInfo("vi", "Em", "Rel. menor", "Repouso suave", "Mi menor"),
                ChordInfo("vii°", "F#°", "Sensível", "Tensão forte", "Fá# diminuto")
            )
        ),
        // A Major (Lá Maior)
        HarmonicField(
            keyCipher = "A",
            keyNamePt = "Lá Maior",
            isMinor = false,
            scaleNotes = "Lá — Si — Dó# — Ré — Mi — Fá# — Sol#",
            chords = listOf(
                ChordInfo("I", "A", "Tônica", "Repouso", "Lá Maior"),
                ChordInfo("ii", "Bm", "Subdominante", "Passagem", "Si menor"),
                ChordInfo("iii", "C#m", "Mediante", "Repouso suave", "Dó# menor"),
                ChordInfo("IV", "D", "Subdominante", "Preparação", "Ré Maior"),
                ChordInfo("V", "E", "Dominante", "Tensão", "Mi Maior"),
                ChordInfo("vi", "F#m", "Rel. menor", "Repouso suave", "Fá# menor"),
                ChordInfo("vii°", "G#°", "Sensível", "Tensão forte", "Sol# diminuto")
            )
        ),
        // B Major (Si Maior)
        HarmonicField(
            keyCipher = "B",
            keyNamePt = "Si Maior",
            isMinor = false,
            scaleNotes = "Si — Dó# — Ré# — Fá# — Sol# — Lá#",
            chords = listOf(
                ChordInfo("I", "B", "Tônica", "Repouso", "Si Maior"),
                ChordInfo("ii", "C#m", "Subdominante", "Passagem", "Dó# menor"),
                ChordInfo("iii", "D#m", "Mediante", "Repouso suave", "Ré# menor"),
                ChordInfo("IV", "E", "Subdominante", "Preparação", "Mi Maior"),
                ChordInfo("V", "F#", "Dominante", "Tensão", "Fá# Maior"),
                ChordInfo("vi", "G#m", "Rel. menor", "Repouso suave", "Sol# menor"),
                ChordInfo("vii°", "A#°", "Sensível", "Tensão forte", "Lá# diminuto")
            )
        ),
        // C# Major (Dó# Maior)
        HarmonicField(
            keyCipher = "C#",
            keyNamePt = "Dó# Maior",
            isMinor = false,
            scaleNotes = "Dó# — Ré# — Mí — Fá# — Sol# — Lá# — Si#",
            chords = listOf(
                ChordInfo("I", "C#", "Tônica", "Repouso", "Dó# Maior"),
                ChordInfo("ii", "D#m", "Subdominante", "Passagem", "Ré# menor"),
                ChordInfo("iii", "Fm", "Mediante", "Repouso suave", "Fá menor"),
                ChordInfo("IV", "F#", "Subdominante", "Preparação", "Fá# Maior"),
                ChordInfo("V", "G#", "Dominante", "Tensão", "Sol# Maior"),
                ChordInfo("vi", "A#m", "Rel. menor", "Repouso suave", "Lá# menor"),
                ChordInfo("vii°", "C°", "Sensível", "Tensão forte", "Dó diminuto")
            )
        ),
        // D# Major (Ré# Maior)
        HarmonicField(
            keyCipher = "D#",
            keyNamePt = "Ré# Maior",
            isMinor = false,
            scaleNotes = "Ré# — Fá — Sol — Sol# — Lá# — Dó — Ré",
            chords = listOf(
                ChordInfo("I", "D#", "Tônica", "Repouso", "Ré# Maior"),
                ChordInfo("ii", "Fm", "Subdominante", "Passagem", "Fá menor"),
                ChordInfo("iii", "Gm", "Mediante", "Repouso suave", "Sol menor"),
                ChordInfo("IV", "G#", "Subdominante", "Preparação", "Sol# Maior"),
                ChordInfo("V", "A#", "Dominante", "Tensão", "Lá# Maior"),
                ChordInfo("vi", "Cm", "Rel. menor", "Repouso suave", "Dó menor"),
                ChordInfo("vii°", "D°", "Sensível", "Tensão forte", "Ré diminuto")
            )
        ),
        // F# Major (Fá# Maior)
        HarmonicField(
            keyCipher = "F#",
            keyNamePt = "Fá# Maior",
            isMinor = false,
            scaleNotes = "Fá# — Sol# — Lá# — Si — Dó# — Ré# — Mí#",
            chords = listOf(
                ChordInfo("I", "F#", "Tônica", "Repouso", "Fá# Maior"),
                ChordInfo("ii", "G#m", "Subdominante", "Passagem", "Sol# menor"),
                ChordInfo("iii", "A#m", "Mediante", "Repouso suave", "Lá# menor"),
                ChordInfo("IV", "B", "Subdominante", "Preparação", "Si Maior"),
                ChordInfo("V", "C#", "Dominante", "Tensão", "Dó# Maior"),
                ChordInfo("vi", "D#m", "Rel. menor", "Repouso suave", "Ré# menor"),
                ChordInfo("vii°", "F°", "Sensível", "Tensão forte", "Fá diminuto")
            )
        ),
        // G# Major (Sol# Maior)
        HarmonicField(
            keyCipher = "G#",
            keyNamePt = "Sol# Maior",
            isMinor = false,
            scaleNotes = "Sol# — Lá# — Dó — Dó# — Ré# — Fá — Sol",
            chords = listOf(
                ChordInfo("I", "G#", "Tônica", "Repouso", "Sol# Maior"),
                ChordInfo("ii", "A#m", "Subdominante", "Passagem", "Lá# menor"),
                ChordInfo("iii", "Cm", "Mediante", "Repouso suave", "Dó menor"),
                ChordInfo("IV", "C#", "Subdominante", "Preparação", "Dó# Maior"),
                ChordInfo("V", "D#", "Dominante", "Tensão", "Ré# Maior"),
                ChordInfo("vi", "Fm", "Rel. menor", "Repouso suave", "Fá menor"),
                ChordInfo("vii°", "G°", "Sensível", "Tensão forte", "Sol diminuto")
            )
        ),
        // A# Major (Lá# Maior)
        HarmonicField(
            keyCipher = "A#",
            keyNamePt = "Lá# Maior",
            isMinor = false,
            scaleNotes = "Lá# — Dó — Ré — Ré# — Fá — Sol — Lá",
            chords = listOf(
                ChordInfo("I", "A#", "Tônica", "Repouso", "Lá# Maior"),
                ChordInfo("ii", "Cm", "Subdominante", "Passagem", "Dó menor"),
                ChordInfo("iii", "Dm", "Mediante", "Repouso suave", "Ré menor"),
                ChordInfo("IV", "D#", "Subdominante", "Preparação", "Ré# Maior"),
                ChordInfo("V", "F", "Dominante", "Tensão", "Fá Maior"),
                ChordInfo("vi", "Gm", "Rel. menor", "Repouso suave", "Sol menor"),
                ChordInfo("vii°", "A°", "Sensível", "Tensão forte", "Lá diminuto")
            )
        )
    )

    val minorFields = listOf(
        // C Minor (Dó menor)
        HarmonicField(
            keyCipher = "Cm",
            keyNamePt = "Dó menor",
            isMinor = true,
            scaleNotes = "Dó — Ré — Ré# — Fá — Sol — Sol# — Lá#",
            chords = listOf(
                ChordInfo("i", "Cm", "Tônica", "Repouso", "Dó menor"),
                ChordInfo("ii°", "D°", "Subdominante", "Tensão forte", "Ré diminuto"),
                ChordInfo("III", "D#", "Rel. Maior", "Repouso suave", "Ré# Maior"),
                ChordInfo("iv", "Fm", "Subdominante", "Passagem", "Fá menor"),
                ChordInfo("v", "Gm", "Dominante m", "Tensão leve", "Sol menor"),
                ChordInfo("VI", "G#", "Subdominante", "Preparação", "Sol# Maior"),
                ChordInfo("VII", "A#", "Subtônica", "Passagem", "Lá# Maior")
            )
        ),
        // D Minor (Ré menor)
        HarmonicField(
            keyCipher = "Dm",
            keyNamePt = "Ré menor",
            isMinor = true,
            scaleNotes = "Ré — Mi — Fá — Sol — Lá — Lá# — Dó",
            chords = listOf(
                ChordInfo("i", "Dm", "Tônica", "Repouso", "Ré menor"),
                ChordInfo("ii°", "E°", "Subdominante", "Tensão forte", "Mi diminuto"),
                ChordInfo("III", "F", "Rel. Maior", "Repouso suave", "Fá Maior"),
                ChordInfo("iv", "Gm", "Subdominante", "Passagem", "Sol menor"),
                ChordInfo("v", "Am", "Dominante m", "Tensão leve", "Lá menor"),
                ChordInfo("VI", "A#", "Subdominante", "Preparação", "Lá# Maior"),
                ChordInfo("VII", "C", "Subtônica", "Passagem", "Dó Maior")
            )
        ),
        // E Minor (Mi menor)
        HarmonicField(
            keyCipher = "Em",
            keyNamePt = "Mi menor",
            isMinor = true,
            scaleNotes = "Mi — Fá# — Sol — Lá — Si — Dó — Ré",
            chords = listOf(
                ChordInfo("i", "Em", "Tônica", "Repouso", "Mi menor"),
                ChordInfo("ii°", "F#°", "Subdominante", "Tensão forte", "Fá# diminuto"),
                ChordInfo("III", "G", "Rel. Maior", "Repouso suave", "Sol Maior"),
                ChordInfo("iv", "Am", "Subdominante", "Passagem", "Lá menor"),
                ChordInfo("v", "Bm", "Dominante m", "Tensão leve", "Si menor"),
                ChordInfo("VI", "C", "Subdominante", "Preparação", "Dó Maior"),
                ChordInfo("VII", "D", "Subtônica", "Passagem", "Ré Maior")
            )
        ),
        // F Minor (Fá menor)
        HarmonicField(
            keyCipher = "Fm",
            keyNamePt = "Fá menor",
            isMinor = true,
            scaleNotes = "Fá — Sol — Sol# — Lá# — Dó — Dó# — Ré#",
            chords = listOf(
                ChordInfo("i", "Fm", "Tônica", "Repouso", "Fá menor"),
                ChordInfo("ii°", "G°", "Subdominante", "Tensão forte", "Sol diminuto"),
                ChordInfo("III", "G#", "Rel. Maior", "Repouso suave", "Sol# Maior"),
                ChordInfo("iv", "A#m", "Subdominante", "Passagem", "Lá# menor"),
                ChordInfo("v", "Cm", "Dominante m", "Tensão leve", "Dó menor"),
                ChordInfo("VI", "C#", "Subdominante", "Preparação", "Dó# Maior"),
                ChordInfo("VII", "D#", "Subtônica", "Passagem", "Ré# Maior")
            )
        ),
        // G Minor (Sol menor)
        HarmonicField(
            keyCipher = "Gm",
            keyNamePt = "Sol menor",
            isMinor = true,
            scaleNotes = "Sol — Lá — Lá# — Dó — Ré — Ré# — Fá",
            chords = listOf(
                ChordInfo("i", "Gm", "Tônica", "Repouso", "Sol menor"),
                ChordInfo("ii°", "A°", "Subdominante", "Tensão forte", "Lá diminuto"),
                ChordInfo("III", "A#", "Rel. Maior", "Repouso suave", "Lá# Maior"),
                ChordInfo("iv", "Cm", "Subdominante", "Passagem", "Dó menor"),
                ChordInfo("v", "Dm", "Dominante m", "Tensão leve", "Ré menor"),
                ChordInfo("VI", "D#", "Subdominante", "Preparação", "Ré# Maior"),
                ChordInfo("VII", "F", "Subtônica", "Passagem", "Fá Maior")
            )
        ),
        // A Minor (Lá menor)
        HarmonicField(
            keyCipher = "Am",
            keyNamePt = "Lá menor",
            isMinor = true,
            scaleNotes = "Lá — Si — Dó — Ré — Mi — Fá — Sol",
            chords = listOf(
                ChordInfo("i", "Am", "Tônica", "Repouso", "Lá menor"),
                ChordInfo("ii°", "B°", "Subdominante", "Tensão forte", "Si diminuto"),
                ChordInfo("III", "C", "Rel. Maior", "Repouso suave", "Dó Maior"),
                ChordInfo("iv", "Dm", "Subdominante", "Passagem", "Ré menor"),
                ChordInfo("v", "Em", "Dominante m", "Tensão leve", "Mi menor"),
                ChordInfo("VI", "F", "Subdominante", "Preparação", "Fá Maior"),
                ChordInfo("VII", "G", "Subtônica", "Passagem", "Sol Maior")
            )
        ),
        // B Minor (Si menor)
        HarmonicField(
            keyCipher = "Bm",
            keyNamePt = "Si menor",
            isMinor = true,
            scaleNotes = "Si — Dó# — Ré — Mi — Fá# — Sol — Lá",
            chords = listOf(
                ChordInfo("i", "Bm", "Tônica", "Repouso", "Si menor"),
                ChordInfo("ii°", "C#°", "Subdominante", "Tensão forte", "Dó# diminuto"),
                ChordInfo("III", "D", "Rel. Maior", "Repouso suave", "Ré Maior"),
                ChordInfo("iv", "Em", "Subdominante", "Passagem", "Mi menor"),
                ChordInfo("v", "F#m", "Dominante m", "Tensão leve", "Fá# menor"),
                ChordInfo("VI", "G", "Subdominante", "Preparação", "Sol Maior"),
                ChordInfo("VII", "A", "Subtônica", "Passagem", "Lá Maior")
            )
        ),

        // C# Minor (Dó# menor)
        HarmonicField(
            keyCipher = "C#m",
            keyNamePt = "Dó# menor",
            isMinor = true,
            scaleNotes = "Dó# — Ré# — Mi — Fá# — Sol# — Lá — Si",
            chords = listOf(
                ChordInfo("i", "C#m", "Tônica", "Repouso", "Dó# menor"),
                ChordInfo("ii°", "D#°", "Subdominante", "Tensão forte", "Ré# diminuto"),
                ChordInfo("III", "E", "Rel. Maior", "Repouso suave", "Mi Maior"),
                ChordInfo("iv", "F#m", "Subdominante", "Passagem", "Fá# menor"),
                ChordInfo("v", "G#m", "Dominante m", "Tensão leve", "Sol# menor"),
                ChordInfo("VI", "A", "Subdominante", "Preparação", "Lá Maior"),
                ChordInfo("VII", "B", "Subtônica", "Passagem", "Si Maior")
            )
        ),
        // D# Minor (Ré# menor)
        HarmonicField(
            keyCipher = "D#m",
            keyNamePt = "Ré# menor",
            isMinor = true,
            scaleNotes = "Ré# — Fá — Fá# — Sol# — Lá# — Si — Dó#",
            chords = listOf(
                ChordInfo("i", "D#m", "Tônica", "Repouso", "Ré# menor"),
                ChordInfo("ii°", "F°", "Subdominante", "Tensão forte", "Fá diminuto"),
                ChordInfo("III", "F#,", "Rel. Maior", "Repouso suave", "Fá# Maior"),
                ChordInfo("iv", "G#m", "Subdominante", "Passagem", "Sol# menor"),
                ChordInfo("v", "A#m", "Dominante m", "Tensão leve", "Lá# menor"),
                ChordInfo("VI", "B", "Subdominante", "Preparação", "Si Maior"),
                ChordInfo("VII", "C#", "Subtônica", "Passagem", "Dó# Maior")
            ).mapIndexed { idx, item -> if (idx == 2) ChordInfo("III", "F#", "Rel. Maior", "Repouso suave", "Fá# Maior") else item }
        ),
        // F# Minor (Fá# menor)
        HarmonicField(
            keyCipher = "F#m",
            keyNamePt = "Fá# menor",
            isMinor = true,
            scaleNotes = "Fá# — Sol# — Lá — Si — Dó# — Ré — Mi",
            chords = listOf(
                ChordInfo("i", "F#m", "Tônica", "Repouso", "Fá# menor"),
                ChordInfo("ii°", "G#°", "Subdominante", "Tensão forte", "Sol# diminuto"),
                ChordInfo("III", "A", "Rel. Maior", "Repouso suave", "Lá Maior"),
                ChordInfo("iv", "Bm", "Subdominante", "Passagem", "Si menor"),
                ChordInfo("v", "C#m", "Dominante m", "Tensão leve", "Dó# menor"),
                ChordInfo("VI", "D", "Subdominante", "Preparação", "Ré Maior"),
                ChordInfo("VII", "E", "Subtônica", "Passagem", "Mi Maior")
            )
        ),
        // G# Minor (Sol# menor)
        HarmonicField(
            keyCipher = "G#m",
            keyNamePt = "Sol# menor",
            isMinor = true,
            scaleNotes = "Sol# — Lá# — Si — Dó# — Ré# — Mi — Fá#",
            chords = listOf(
                ChordInfo("i", "G#m", "Tônica", "Repouso", "Sol# menor"),
                ChordInfo("ii°", "A#°", "Subdominante", "Tensão forte", "Lá# diminuto"),
                ChordInfo("III", "B", "Rel. Maior", "Repouso suave", "Si Maior"),
                ChordInfo("iv", "C#m", "Subdominante", "Passagem", "Dó# menor"),
                ChordInfo("v", "D#m", "Dominante m", "Tensão leve", "Ré# menor"),
                ChordInfo("VI", "E", "Subdominante", "Preparação", "Mi Maior"),
                ChordInfo("VII", "F#", "Subtônica", "Passagem", "Fá# Maior")
            )
        ),
        // A# Minor (Lá# menor)
        HarmonicField(
            keyCipher = "A#m",
            keyNamePt = "Lá# menor",
            isMinor = true,
            scaleNotes = "Lá# — Dó — Dó# — Ré# — Fá — Fá# — Sol#",
            chords = listOf(
                ChordInfo("i", "A#m", "Tônica", "Repouso", "Lá# menor"),
                ChordInfo("ii°", "C°", "Subdominante", "Tensão forte", "Dó diminuto"),
                ChordInfo("III", "C#", "Rel. Maior", "Repouso suave", "Dó# Maior"),
                ChordInfo("iv", "D#m", "Subdominante", "Passagem", "Ré# menor"),
                ChordInfo("v", "Fm", "Dominante m", "Tensão leve", "Fá menor"),
                ChordInfo("VI", "F#", "Subdominante", "Preparação", "Fá# Maior"),
                ChordInfo("VII", "G#", "Subtônica", "Passagem", "Sol# Maior")
            )
        )
    )

    fun getField(cipher: String): HarmonicField? {
        val field = (majorFields + minorFields).firstOrNull { it.keyCipher == cipher } ?: return null
        val mappedChords = field.chords.mapIndexed { index, chord ->
            chord.copy(function = functionForDegree(index, field.isMinor))
        }
        return field.copy(chords = mappedChords)
    }

    /** Todos os 24 tons na ordem: 12 maiores seguidos de 12 menores. */
    fun allKeys(): List<HarmonicField> = majorFields + minorFields

    /** Mapa cifra → nome em português (ex.: "G#m" → "Sol# menor"), calculado uma vez. */
    val ptNameByCipher: Map<String, String> by lazy {
        allKeys().associate { it.keyCipher to it.keyNamePt }
    }
}

