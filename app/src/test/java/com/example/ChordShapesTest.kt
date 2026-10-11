package com.example

import com.example.music.ChordShapes
import com.example.music.ChordType
import com.example.music.LabChord
import com.example.music.Voicing
import com.example.music.Voicings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Dicionário de acordes: formas mais usadas (CAGED), todas as formas e os dedos. */
class ChordShapesTest {

    private fun frets(v: Voicing) = v.frets.toList()
    private fun f(vararg x: Int) = x.toList()

    /** Dedos coerentes: cada casa presa tem dedo; um dedo não aperta duas casas diferentes. */
    private fun checkFingering(chord: LabChord, v: Voicing) {
        val tag = "${chord.cipher} ${frets(v)}"
        val fg = v.fingering
        for (s in v.frets.indices) {
            if (v.frets[s] > 0) assertTrue("$tag: corda sem dedo", fg[s] in 1..4) else assertEquals(tag, 0, fg[s])
        }
        val byFinger = v.frets.indices.filter { v.frets[it] > 0 }.groupBy { fg[it] }
        byFinger.forEach { (finger, strings) ->
            assertEquals("$tag: dedo $finger em casas diferentes", 1, strings.map { v.frets[it] }.distinct().size)
            if (strings.size > 1) assertTrue("$tag: só o dedo 1 faz pestana", finger == 1 && v.barre != null || strings.size == 1)
        }
    }

    @Test fun commonShapesAreValidInEveryKeyAndType() {
        for (root in 0..11) for (type in ChordType.entries) {
            val c = LabChord(root, type)
            val list = ChordShapes.common(c)
            assertTrue("${c.cipher}: sem formas", list.isNotEmpty())
            list.forEach { nv ->
                assertNotNull("${c.cipher} ${frets(nv.voicing)} inválida", Voicings.evaluate(c, nv.voicing.frets))
                checkFingering(c, nv.voicing)
            }
        }
    }

    @Test fun mainChordsHaveTheFiveCagedShapes() {
        for (root in 0..11) for (type in listOf(ChordType.MAJOR, ChordType.DOM7, ChordType.MAJ7)) {
            val n = ChordShapes.common(LabChord(root, type)).count { it.name.startsWith("Forma de") }
            assertTrue("${LabChord(root, type).cipher}: só $n formas CAGED", n >= 4)
        }
        for (root in 0..11) for (type in listOf(ChordType.MINOR, ChordType.MIN7)) {
            assertTrue(ChordShapes.common(LabChord(root, type)).size >= 3)
        }
    }

    @Test fun gMajorShapesAreTheOnesInTheBooks() {
        val g = ChordShapes.common(LabChord(7, ChordType.MAJOR)).associate { frets(it.voicing) to it.name }
        assertEquals("Forma de Sol", g[f(3, 2, 0, 0, 0, 3)])          // Sol aberto
        assertEquals("Forma de Mi", g[f(3, 5, 5, 4, 3, 3)])           // pestana na 3ª
        assertEquals("Forma de Ré", g[f(-1, -1, 5, 7, 8, 7)])         // 5ª casa
        assertEquals("Forma de Dó", g[f(-1, 10, 9, 7, 8, 7)])         // 7ª casa
        assertEquals("Forma de Lá", g[f(-1, 10, 12, 12, 12, 10)])     // pestana na 10ª
        val all = ChordShapes.all(LabChord(7, ChordType.MAJOR))
        assertTrue("todas (${all.size}) deve ter mais que as comuns", all.size > g.size + 10)
        assertTrue(all.map { frets(it.voicing) }.containsAll(g.keys))
        all.forEach { assertNotNull(Voicings.evaluate(LabChord(7, ChordType.MAJOR), it.voicing.frets)) }
    }

    @Test fun fingersLikeTheChordBooks() {
        fun fingering(root: Int, type: ChordType, vararg fr: Int): List<Int> {
            val v = ChordShapes.all(LabChord(root, type)).first { frets(it.voicing) == fr.toList() }.voicing
            return v.fingering.toList()
        }
        assertEquals(f(0, 3, 2, 0, 1, 0), fingering(0, ChordType.MAJOR, -1, 3, 2, 0, 1, 0))   // Dó
        assertEquals(f(2, 1, 0, 0, 0, 3), fingering(7, ChordType.MAJOR, 3, 2, 0, 0, 0, 3))    // Sol
        assertEquals(f(1, 3, 4, 2, 1, 1), fingering(5, ChordType.MAJOR, 1, 3, 3, 2, 1, 1))    // Fá com pestana
        assertEquals(f(0, 0, 0, 1, 3, 2), fingering(2, ChordType.MAJOR, -1, -1, 0, 2, 3, 2))  // Ré
        assertEquals(f(0, 1, 3, 4, 2, 1), fingering(11, ChordType.MINOR, -1, 2, 4, 4, 3, 2))  // Si menor
    }

    @Test fun openChordsHaveNoFakeBarre() {
        val d = ChordShapes.common(LabChord(2, ChordType.MAJOR)).first { frets(it.voicing) == f(-1, -1, 0, 2, 3, 2) }
        assertEquals(null, d.voicing.barre)
        val a = ChordShapes.common(LabChord(9, ChordType.MAJOR)).first { frets(it.voicing) == f(-1, 0, 2, 2, 2, 0) }
        assertEquals(null, a.voicing.barre)
        val f = ChordShapes.common(LabChord(5, ChordType.MAJOR)).first { frets(it.voicing) == f(1, 3, 3, 2, 1, 1) }
        assertEquals(1, f.voicing.barre)
        assertEquals("Forma de Mi · pestana na casa 1", f.title)
    }
}
