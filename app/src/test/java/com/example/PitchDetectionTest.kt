package com.example

import com.example.music.detectPitch
import com.example.music.ptPitchClass
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PitchDetectionTest {

    @Test
    fun detectPitch_senoide110Hz() {
        val sr = 44100
        val samples = DoubleArray(4096) { sin(2.0 * PI * 110.0 * it / sr) }
        val res = detectPitch(samples, sr)
        assertNotNull(res)
        assertTrue("esperado ~110 Hz, veio ${res!!.frequency}", abs(res.frequency - 110.0) < 1.0)
    }

    @Test
    fun detectPitch_ruidoNaoTemAltura() {
        val rng = java.util.Random(4)
        val noise = DoubleArray(4096) { rng.nextGaussian() * 0.3 }
        assertNull(detectPitch(noise, 44100))
    }

    @Test
    fun ptPitchClass_nomesEmPortugues() {
        assertEquals("Dó", ptPitchClass(60))
        assertEquals("Sol", ptPitchClass(7))
        assertEquals("Si", ptPitchClass(-1))
    }
}
