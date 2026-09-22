package com.example

import com.example.music.computeChroma
import com.example.music.detectChord
import com.example.music.detectKey
import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testa a cadeia de identificação de acorde/tom com sinais sintéticos —
 * sem depender do microfone.
 */
class ChordAnalysisTest {

    @Test
    fun computeChroma_senoideDeLa440_temPicoEmLa() {
        val n = 8192
        val sr = 44100
        val samples = DoubleArray(n) { sin(2.0 * PI * 440.0 * it / sr) }
        val chroma = computeChroma(samples, sr)
        val argmax = chroma.indices.maxByOrNull { chroma[it] }!!
        assertEquals("Lá é a classe 9", 9, argmax)
    }

    @Test
    fun detectChord_triadeMaiorDeDo() {
        val chroma = FloatArray(12)
        chroma[0] = 1f; chroma[4] = 1f; chroma[7] = 1f // Dó, Mi, Sol
        val guess = detectChord(chroma)
        assertNotNull(guess)
        assertEquals("C", guess!!.label)
    }

    @Test
    fun detectChord_triadeMenorDeLa() {
        val chroma = FloatArray(12)
        chroma[9] = 1f; chroma[0] = 1f; chroma[4] = 1f // Lá, Dó, Mi
        val guess = detectChord(chroma)
        assertNotNull(guess)
        assertEquals("Am", guess!!.label)
    }

    @Test
    fun detectKey_perfilDeSolMaior() {
        // Perfil de Krumhansl maior rotacionado para Sol (raiz 7).
        val ksMajor = doubleArrayOf(6.35, 2.23, 3.48, 2.33, 4.38, 4.09, 2.52, 5.19, 2.39, 3.66, 2.29, 2.88)
        val chroma = FloatArray(12) { ksMajor[Math.floorMod(it - 7, 12)].toFloat() }
        val key = detectKey(chroma)
        assertNotNull(key)
        assertEquals("G", key!!.keyCipher)
        assertTrue(key.score > 0.9)
    }
}
