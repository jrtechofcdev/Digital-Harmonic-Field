package com.example

import com.example.music.computeChroma
import com.example.music.pitchClassOfFrequency
import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Test

/** Testa a base espectral (FFT + chromagram) com sinais sintéticos. */
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
    fun pitchClassOfFrequency_confere() {
        assertEquals(9, pitchClassOfFrequency(440.0))   // Lá
        assertEquals(7, pitchClassOfFrequency(196.0))   // Sol
        assertEquals(0, pitchClassOfFrequency(261.63))  // Dó
    }
}
