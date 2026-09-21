package com.example

import com.example.music.HarmonicFunction
import com.example.music.functionForDegree
import com.example.music.getChordFormulaNotes
import com.example.music.transposeCipher
import com.example.music.transposePtNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Testes da lógica musical — a parte que precisa estar sempre correta,
 * independentemente da interface.
 */
class MusicTheoryTest {

    @Test
    fun transposeCipher_subindoUmTomInteiro() {
        assertEquals("D", transposeCipher("C", 2))
        assertEquals("Em", transposeCipher("Dm", 2))
        assertEquals("A#", transposeCipher("G#", 2))
    }

    @Test
    fun transposeCipher_dobraDaOitava() {
        assertEquals("C", transposeCipher("C", 12))
        assertEquals("C", transposeCipher("C", -12))
    }

    @Test
    fun transposePtNote_respeitaCircularidade() {
        assertEquals("Dó", transposePtNote("Si", 1))
        assertEquals("Si", transposePtNote("Dó", -1))
    }

    @Test
    fun funcaoDosGrausMaiores() {
        assertEquals(HarmonicFunction.TONIC, functionForDegree(0, isMinor = false))       // I
        assertEquals(HarmonicFunction.SUBDOMINANT, functionForDegree(3, isMinor = false)) // IV
        assertEquals(HarmonicFunction.DOMINANT, functionForDegree(4, isMinor = false))    // V
    }

    @Test
    fun formacaoDeAcordeMaior_temTerçaMaior() {
        val notas = getChordFormulaNotes("C")
        assertEquals("Dó", notas[0].note)
        assertEquals("Mi", notas[1].note)   // terça maior
        assertEquals("Sol", notas[2].note)  // quinta justa
    }

    @Test
    fun todosOs24TonsResolvem() {
        val fields = HarmonicDatabase.allKeys()
        assertEquals(24, fields.size)
        fields.forEach { field ->
            val resolved = HarmonicDatabase.getField(field.keyCipher)
            assertNotNull("Tom ${field.keyCipher} deveria resolver", resolved)
            assertEquals(7, resolved!!.chords.size)
        }
    }
}
