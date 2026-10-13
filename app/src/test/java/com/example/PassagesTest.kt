package com.example

import com.example.music.ChordShapes
import com.example.music.PassageMove
import com.example.music.Passages
import com.example.music.Voicings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Passagens e enfeites: acordes certos no tom, marcação "fora do campo" e formas tocáveis. */
class PassagesTest {

    private fun ciphers(move: PassageMove, tonic: Int, minor: Boolean, name: String) =
        Passages.forMove(move, tonic, minor).first { it.name == name }.steps.map { it.chord.cipher }

    @Test fun everyPassageIsPlayableInEveryKey() {
        for (minor in listOf(false, true)) for (tonic in 0..11) {
            val moves = Passages.moves(minor)
            assertTrue(moves.isNotEmpty())
            for (m in moves) {
                assertTrue("${m.name}: sem caminho direto", Passages.plain(m, tonic, minor).size >= 2)
                val list = Passages.forMove(m, tonic, minor)
                assertTrue("${m.name}: sem passagens", list.size >= 2)
                assertEquals("ordem por nível", list.map { it.level.ordinal }.sorted(), list.map { it.level.ordinal })
                list.flatMap { it.steps }.forEach { st ->
                    assertTrue("${st.chord.cipher} sem forma no violão", Voicings.best(st.chord) != null)
                }
            }
        }
    }

    @Test fun chordsInG() {
        assertEquals(listOf("Am7", "D7", "G"), ciphers(PassageMove.VOLTAR, 7, false, "2m - 5 - 1 (a caída mais usada)"))
        assertEquals(listOf("G", "D/F#", "Em"), ciphers(PassageMove.DESCER, 7, false, "1 - 5/7 - 6m"))
        assertEquals(listOf("G", "B7", "Em"), ciphers(PassageMove.UM_SEIS, 7, false, "5 do 6m"))
        assertEquals(listOf("C", "C#°7", "D"), ciphers(PassageMove.QUATRO_CINCO, 7, false, "Diminuto subindo"))
        assertEquals(listOf("D#", "F", "G"), ciphers(PassageMove.FINAL, 7, false, "Final de vitória"))
        assertEquals(listOf("C", "Cm", "G"), ciphers(PassageMove.FINAL, 7, false, "4 com saudade"))
    }

    @Test fun minorKeys() {
        assertEquals(listOf("Am", "G", "F", "E"), ciphers(PassageMove.DESCER, 9, true, "Descida espanhola"))
        assertEquals(listOf("Dm", "E7", "A"), ciphers(PassageMove.FINAL, 9, true, "Terça de Picardia"))
        assertEquals(listOf("Am", "G", "C"), ciphers(PassageMove.UM_SEIS, 9, true, "Para o relativo maior"))
    }

    @Test fun outsideTheFieldIsMarked() {
        val g = Passages.forMove(PassageMove.VOLTAR, 7, false)
        assertEquals(0, g.first { it.name == "2m - 5 - 1 (a caída mais usada)" }.outsideCount)
        val dd = g.first { it.name.startsWith("5 do 5") }
        assertTrue(dd.steps.first().outside)                  // A7 sai do campo de Sol (Dó#)
        assertFalse(dd.steps.last().outside)
        val fin = Passages.forMove(PassageMove.FINAL, 7, false).first { it.name == "Final de vitória" }
        assertEquals(2, fin.outsideCount)
        // No tom menor, o 5 maior (com a sensível) é normal, não "fora".
        val e7 = Passages.forMove(PassageMove.VOLTAR, 9, true).first().steps.first()
        assertEquals("E7", e7.chord.cipher)
        assertFalse(e7.outside)
    }

    @Test fun howToTextForFamousShapes() {
        val f = ChordShapes.common(com.example.music.LabChord(5, com.example.music.ChordType.MAJOR))
            .first { it.voicing.frets.toList() == listOf(1, 3, 3, 2, 1, 1) }
        val steps = ChordShapes.howTo(f.voicing)
        assertEquals("Dedo 1 (indicador) deitado na casa 1, da 6ª até a 1ª corda (pestana).", steps.first())
        assertTrue(steps.contains("Dedo 2 (médio): 3ª corda, casa 2."))
        assertTrue(steps.contains("Dedo 3 (anelar): 5ª corda, casa 3."))
        assertTrue(steps.contains("Dedo 4 (mindinho): 4ª corda, casa 3."))
        val c = ChordShapes.common(com.example.music.LabChord(0, com.example.music.ChordType.MAJOR)).first()
        val cs = ChordShapes.howTo(c.voicing)
        assertTrue(cs.contains("Soltas: 3ª e 1ª cordas."))
        assertTrue(cs.contains("Não toque: 6ª corda."))
    }
}
