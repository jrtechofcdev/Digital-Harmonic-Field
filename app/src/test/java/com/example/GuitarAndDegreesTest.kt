package com.example

import com.example.music.ChordQuality
import com.example.music.DegreeTheory
import com.example.music.Guitar
import com.example.music.degreeLabel
import com.example.music.degreeSequence
import com.example.music.parseCipher
import com.example.music.pitchClassCiphers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuitarAndDegreesTest {

    @Test
    fun grausDoCampoMaiorEMenor() {
        val g = HarmonicDatabase.getField("G")!!
        assertEquals(listOf("1", "2m", "3m", "4", "5", "6m", "7°"), g.chords.mapIndexed { i, c -> degreeLabel(i, c.cipher) })
        val am = HarmonicDatabase.getField("Am")!!
        assertEquals(listOf("1m", "2°", "3", "4m", "5m", "6", "7"), am.chords.mapIndexed { i, c -> degreeLabel(i, c.cipher) })
        assertEquals("1 - 6m - 2m - 5 - 1", degreeSequence(listOf(0, 5, 1, 4, 0), g))
    }

    @Test
    fun cifrasEntendidas() {
        assertEquals(ChordQuality.HALFDIM, parseCipher("F#m7(b5)")!!.quality)
        assertEquals("D/F#", parseCipher("D/F#")!!.cipher)
        assertEquals(10, parseCipher("Bb")!!.root)
    }

    @Test
    fun grausAFundoEmSol() {
        val t = DegreeTheory(HarmonicDatabase.getField("G")!!)
        assertEquals(listOf("G7M", "Am7", "Bm7", "C7M", "D7", "Em7", "F#m7(b5)"), t.sevenths.map { it.chord.cipher })
        // 1⁷ → 4: G7 → C;  2⁷ → 5: A7 → D;  3⁷ → 6m: B7 → Em
        val sec = t.secondaryDominants.associate { (d, target) -> d.label to (d.chord.cipher to target.chord.cipher) }
        assertEquals("G7" to "C", sec["1⁷"])
        assertEquals("A7" to "D", sec["2⁷"])
        assertEquals("B7" to "Em", sec["3⁷"])
        assertEquals(listOf("G", "G#°", "Am"), t.passingDiminished[0].toList().map { it.chord.cipher })
        assertEquals(listOf("G", "D/F#", "Em"), t.walkingBass[0].map { it.chord.cipher })
        assertEquals("Cm", t.borrowedFourMinor[1].chord.cipher)
    }

    @Test
    fun violao_todasAsFormasSoamOAcordeCerto() {
        val qualities = ChordQuality.entries
        for (root in 0 until 12) for (q in qualities) {
            val chord = com.example.music.Chord(root, q)
            val shape = Guitar.shape(chord)
            val notes = shape.midiNotes
            val pcs = notes.map { it % 12 }.toSet()
            val name = chord.cipher
            assertTrue("$name: notas ${pcs.map { pitchClassCiphers[it] }} fora do acorde", pcs.all { it in chord.pitchClasses })
            // Precisa ter fundamental, terça e (se houver) a sétima/quinta característica.
            val required = q.intervals.filter { it != 7 }.map { (root + it) % 12 }
            assertTrue("$name: faltam notas ${required - pcs}", pcs.containsAll(required))
            assertEquals("$name: o baixo precisa ser a fundamental", root, notes.first() % 12)
            val pressed = shape.frets.filter { it > 0 }
            if (pressed.isNotEmpty()) assertTrue("$name: abertura grande demais", pressed.max() - pressed.min() <= 3)
        }
    }

    @Test
    fun violao_formasAbertasConhecidas() {
        assertEquals(listOf(-1, 3, 2, 0, 1, 0), Guitar.shape("C")!!.frets.toList())
        val f = Guitar.shape("F")!!
        assertEquals(listOf(1, 3, 3, 2, 1, 1), f.frets.toList())
        assertEquals(1, f.barre)
        val bm = Guitar.shape("Bm")!!
        assertEquals(listOf(-1, 2, 4, 4, 3, 2), bm.frets.toList())
        assertEquals(2, bm.barre)
    }

    @Test
    fun treinoDeGraus_perguntasCoerentes() {
        val rnd = kotlin.random.Random(3)
        for (kind in com.example.music.DegreeQuiz.Kind.entries) for (level in com.example.music.DegreeQuiz.Level.entries) {
            repeat(30) {
                val q = com.example.music.DegreeQuiz.next(kind, level, rnd)
                assertTrue("${q.answer} precisa estar nas opções", q.answer in q.options)
                assertTrue(q.degree in level.degrees)
                val field = HarmonicDatabase.getField(q.key)!!
                assertEquals(field.chords[q.degree].cipher, q.chordCipher)
                assertEquals(2, q.playback.size)
                // o 1º acorde tocado é a tônica do tom
                assertEquals(com.example.music.parseCipher(q.key)!!.root + 36, q.playback[0][0])
            }
        }
    }
}
