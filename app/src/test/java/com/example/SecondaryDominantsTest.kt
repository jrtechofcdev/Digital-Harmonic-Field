package com.example

import com.example.music.ChordQuality
import com.example.music.Guitar
import com.example.music.SecondaryDominants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Dominantes secundárias: o "5 de cada grau", conferido nota a nota. */
class SecondaryDominantsTest {

    private fun of(key: String) = SecondaryDominants.forField(HarmonicDatabase.getField(key)!!)

    @Test
    fun emDo_igualAoVideo() {
        val d = of("C")
        // A7 → Dm, B7 → Em, C7 → F, D7 → G, E7 → Am; o Bm(b5)/B° não tem dominante.
        assertEquals(listOf("G7", "A7", "B7", "C7", "D7", "E7", null), d.map { it?.dominant?.cipher })
        assertEquals(listOf("5⁷", "6⁷", "7⁷", "1⁷", "2⁷", "3⁷", null), d.map { it?.label })
        assertEquals(listOf("1", "2m", "3m", "4", "5", "6m", null), d.map { it?.targetLabel })
        assertTrue(d[0]!!.primary)
        assertTrue(d.drop(1).filterNotNull().none { it.primary })
        assertNull(d[6])
    }

    @Test
    fun emDo_notasDeForaEOndeElasVao() {
        val d = of("C")
        // Notas fora do campo de Dó: A7 tem Dó#; B7 tem Ré# e Fá#; C7 tem Sib (A#); D7 Fá#; E7 Sol#; G7 nenhuma.
        assertEquals(listOf(emptyList(), listOf(1), listOf(3, 6), listOf(10), listOf(6), listOf(8)),
            d.take(6).map { it!!.outsideNotes.sorted() })
        // A7 → Dm: Dó# (1) sobe para Ré (2); Sol (7) desce para Fá (5).
        val a7 = d[1]!!
        assertEquals(1, a7.leadingTone)
        assertEquals(7, a7.seventh)
        assertEquals(5, a7.seventhLandsOn)
        // Exemplo no tom: 1 - 6⁷ - 2m - 5 - 1
        assertEquals(listOf("1", "6⁷", "2m", "5", "1"), a7.example.map { it.first })
        assertEquals(listOf("C", "A7", "Dm", "G", "C"), a7.example.map { it.second.cipher })
    }

    @Test
    fun emLaMenor() {
        val d = of("Am")
        // 1m ← E7 (5⁷), 2° sem dominante, 3 ← G7, 4m ← A7, 5m ← B7, 6 ← C7, 7 ← D7
        assertEquals(listOf("E7", null, "G7", "A7", "B7", "C7", "D7"), d.map { it?.dominant?.cipher })
        assertEquals(listOf("5⁷", null, "7⁷", "1⁷", "2⁷", "3⁷", "4⁷"), d.map { it?.label })
        assertEquals(listOf(8), d[0]!!.outsideNotes)          // Sol#, a sensível do tom menor
        assertEquals(emptyList<Int>(), d[2]!!.outsideNotes)   // G7 é todo do campo de Lá menor
        assertEquals(listOf("1m", "4⁷", "7", "3"), d[6]!!.example.map { it.first })
    }

    @Test
    fun todosOs24Tons_regrasDaResolucao() {
        HarmonicDatabase.allKeys().forEach { f ->
            val list = SecondaryDominants.forField(HarmonicDatabase.getField(f.keyCipher)!!)
            assertEquals(7, list.size)
            assertEquals("um só grau sem dominante em ${f.keyCipher}", 1, list.count { it == null })
            list.filterNotNull().forEach { d ->
                val t = d.target.root
                val msg = "${d.dominant.cipher} → ${d.target.cipher} em ${f.keyCipher}"
                assertEquals(msg, ChordQuality.DOM7, d.dominant.quality)
                assertEquals(msg, (t + 7) % 12, d.dominant.root)                      // uma quinta acima
                assertEquals(msg, (t + 11) % 12, d.leadingTone)                       // meio tom abaixo do alvo
                assertTrue(msg, d.leadingTone in d.dominant.pitchClasses)             // é a 3ª da dominante
                assertTrue(msg, d.seventh in d.dominant.pitchClasses)
                assertTrue(msg, d.seventhLandsOn in d.target.pitchClasses)            // a 7ª cai na 3ª do alvo
                val step = (d.seventh - d.seventhLandsOn + 12) % 12
                assertTrue(msg, step == 1 || step == 2)                               // desce meio ou um tom
                assertTrue(msg, d.label.endsWith("⁷"))                                // a raiz é nota do tom
                assertTrue(msg, d.example.any { it.second == d.dominant })
                assertTrue(msg, d.example.any { it.second == d.target })
                assertTrue(msg, Guitar.shape(d.dominant).frets.isNotEmpty())          // tem forma no violão
            }
        }
    }
}
