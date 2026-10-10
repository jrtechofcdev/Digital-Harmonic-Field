package com.example

import com.example.music.ChordType
import com.example.music.Guitar
import com.example.music.Harmonize
import com.example.music.LabChord
import com.example.music.NeckQuiz
import com.example.music.Voicings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** O gerador de formas só pode sugerir o que soa certo e cabe na mão. */
class VoicingsTest {

    private fun check(chord: LabChord) {
        val vs = Voicings.generate(chord)
        assertTrue("${chord.cipher}: nenhuma forma", vs.isNotEmpty())
        for (v in vs) {
            val tag = "${chord.cipher} ${v.frets.toList()}"
            val pcs = v.midiNotes.map { it % 12 }
            assertTrue("$tag: nota fora do acorde", (chord.pitchClasses + chord.bassPc).containsAll(pcs))
            val rel = pcs.map { Math.floorMod(it - chord.root, 12) }.toSet()
            assertTrue("$tag: faltou nota obrigatória", rel.containsAll(chord.type.required))
            assertEquals("$tag: baixo errado", chord.bassPc, v.midiNotes.first() % 12)
            assertTrue("$tag: dedos demais", v.fingers <= 4)
            assertTrue("$tag: abertura grande", v.maxFret - v.minFret <= 3)
            assertTrue("$tag: poucas cordas", v.strings.size >= 4)
        }
    }

    @Test fun everyChordTypeInEveryKeyHasPlayableShapes() {
        for (root in 0..11) for (type in ChordType.entries) check(LabChord(root, type))
    }

    @Test fun mostChordsHaveSeveralVariations() {
        val few = mutableListOf<String>()
        for (root in 0..11) for (type in ChordType.entries) {
            val c = LabChord(root, type)
            if (Voicings.generate(c).size < 3) few += c.cipher
        }
        assertTrue("Poucas variações: $few", few.size <= 12)
    }

    @Test fun inversionsAndSlashChords() {
        check(LabChord(0, ChordType.MAJOR, bass = 4))      // C/E
        check(LabChord(0, ChordType.MAJOR, bass = 2))      // C/D (4/5 em Sol)
        check(LabChord(7, ChordType.MAJOR, bass = 11))     // G/B
    }

    @Test fun knownShapesAppear() {
        // G(9) aberto clássico: 3 X 0 2 0 3? ou 3 2 0 2 0 3 — basta conter um G aberto completo.
        val g9 = Voicings.generate(LabChord(7, ChordType.ADD9))
        assertTrue(g9.any { it.position == "Aberto" })
        // C maior: a forma aberta de sempre aparece.
        val c = Voicings.generate(LabChord(0, ChordType.MAJOR))
        assertTrue(c.any { it.frets.toList() == listOf(-1, 3, 2, 0, 1, 0) })
        // F com pestana de Mi.
        val f = Voicings.generate(LabChord(5, ChordType.MAJOR))
        assertTrue(f.any { it.frets.toList() == listOf(1, 3, 3, 2, 1, 1) && it.barre == 1 })
    }

    @Test fun harmonizationKeepsTheDegreeRoot() {
        for (minor in listOf(false, true)) for (d in 0..6) {
            val list = Harmonize.forDegree(minor, 7, d)
            assertTrue(list.isNotEmpty())
            list.forEach { assertTrue("${it.label} sem forma", Voicings.generate(it.chord).isNotEmpty()) }
        }
        val (dry, rich) = Harmonize.beforeAfter(false, 7)
        assertEquals(listOf("G", "C", "D", "G"), dry.map { it.chord.cipher })
        assertEquals("G(9)", rich.first().chord.cipher)
        assertEquals("C/D", Harmonize.forDegree(false, 7, 4).last().chord.cipher)
    }

    @Test fun neckPositions() {
        // Dó entre as casas 0 e 12: 6ª(8) 5ª(3) 4ª(10) 3ª(5) 2ª(1) 1ª(8) = 6 posições.
        val c = NeckQuiz.positions(0)
        assertEquals(listOf(0 to 8, 1 to 3, 2 to 10, 3 to 5, 4 to 1, 5 to 8), c)
        repeat(50) {
            val s = NeckQuiz.spot(NeckQuiz.Stage.SIXTH)
            assertEquals(0, s.string)
            assertTrue(s.pc in NeckQuiz.choices(NeckQuiz.Stage.SIXTH))
        }
        assertEquals(4, Guitar.pitchClassAt(0, 12))
    }
}
