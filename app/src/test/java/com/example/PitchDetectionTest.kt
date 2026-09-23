package com.example

import com.example.music.TuningLibrary
import com.example.music.centsBetween
import com.example.music.detectPitch
import com.example.music.midiToFrequency
import com.example.music.readingForFrequency
import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PitchDetectionTest {

    @Test
    fun detectPitch_senoide110Hz() {
        val sr = 44100
        val n = 4096
        val samples = DoubleArray(n) { sin(2.0 * PI * 110.0 * it / sr) }
        val res = detectPitch(samples, sr)
        assertNotNull(res)
        assertTrue("esperado ~110 Hz, veio ${res!!.frequency}", kotlin.math.abs(res.frequency - 110.0) < 1.0)
    }

    @Test
    fun readingForFrequency_la440() {
        val r = readingForFrequency(440.0, 440.0)
        assertEquals("A4", r.name)
        assertTrue(kotlin.math.abs(r.cents) < 1.0)
    }

    @Test
    fun midiToFrequency_confere() {
        assertEquals(440.0, midiToFrequency(69, 440.0), 0.01)   // A4
        assertEquals(82.41, midiToFrequency(40, 440.0), 0.1)    // E2 (6ª corda)
    }

    @Test
    fun centsBetween_oitava() {
        assertTrue(kotlin.math.abs(centsBetween(440.0, 440.0)) < 1e-9)
        assertTrue(kotlin.math.abs(centsBetween(880.0, 440.0) - 1200.0) < 1e-6)
    }

    @Test
    fun afinacaoPadrao_temEmi2Na6aCorda() {
        val sexta = TuningLibrary.padrao.strings.first()
        assertEquals(6, sexta.order)
        assertEquals("E2", sexta.noteName)
        assertTrue(kotlin.math.abs(sexta.targetFreq(440.0) - 82.41) < 0.1)
    }

    @Test
    fun openD_notasCorretas() {
        val letras = TuningLibrary.openD.strings.map { it.letter }
        assertEquals(listOf("D", "A", "D", "F#", "A", "D"), letras)
    }
}
