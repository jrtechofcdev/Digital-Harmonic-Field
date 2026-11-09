package com.example

import com.example.music.ChordShapes
import com.example.music.PreachMoment
import com.example.music.Preaching
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fundos de pregação: todo acorde tem forma no violão nos 24 tons, nada de
 * acorde complicado, todo momento tem sequências (simples e com cor) e os
 * acordes saem certos no tom.
 */
class PreachingTest {

    private fun ciphers(tonic: Int, minor: Boolean, name: String) =
        Preaching.all(tonic, minor).first { it.name == name }.steps.map { it.chord.cipher }

    @Test fun tudoTocavelEmTodosOsTons() {
        for (minor in listOf(false, true)) for (tonic in 0..11) {
            val all = Preaching.all(tonic, minor)
            all.flatMap { it.steps }.forEach { st ->
                assertTrue("${st.chord.cipher} sem forma no violão", ChordShapes.common(st.chord).isNotEmpty())
            }
            Preaching.colors(tonic, minor).forEach { c ->
                assertTrue("${c.colored.chord.cipher} sem forma", ChordShapes.common(c.colored.chord).isNotEmpty())
                assertTrue("${c.plain.chord.cipher} sem forma", ChordShapes.common(c.plain.chord).isNotEmpty())
            }
        }
    }

    @Test fun soAcordesSimplesOuCoresLeves() {
        for (minor in listOf(false, true)) {
            Preaching.all(0, minor).flatMap { it.steps }.forEach { st ->
                assertTrue("${st.chord.cipher} é complicado demais", st.chord.type in Preaching.ALLOWED)
            }
        }
    }

    @Test fun todoMomentoTemSequencias() {
        for (minor in listOf(false, true)) for (m in PreachMoment.entries) {
            val list = Preaching.forMoment(m, 0, minor)
            assertTrue("${m.title} sem sequências (menor=$minor)", list.size >= 3)
            assertTrue("${m.title}: precisa de pelo menos uma simples", list.any { !it.colored })
        }
        // Muitas sequências, como pedido: no maior e no menor.
        assertTrue(Preaching.all(0, false).size >= 30)
        assertTrue(Preaching.all(0, true).size >= 15)
        // Os ids são únicos e acham a sequência de volta.
        val all = Preaching.all(7, false)
        assertEquals(all.size, all.map { it.id }.toSet().size)
        assertEquals(all[3].name, Preaching.byId(all[3].id, 7, false)!!.name)
    }

    @Test fun acordesCertosNoTom() {
        // Em Sol maior.
        assertEquals(listOf("G", "C"), ciphers(7, false, "1 - 4: o vai e vem"))
        assertEquals(listOf("C", "D", "Bm", "Em"), ciphers(7, false, "4 - 5 - 3m - 6m: a entrega"))
        assertEquals(listOf("C", "Cm", "G"), ciphers(7, false, "4 - 4m - 1: o aperto e o alívio"))
        assertEquals(listOf("G", "C/G"), ciphers(7, false, "Baixo parado: 1 - 4/1"))
        assertEquals(listOf("Am7", "C/D", "G"), ciphers(7, false, "2m⁷ - 4/5 - 1: a caída macia"))
        assertEquals(listOf("G", "F", "C", "G"), ciphers(7, false, "1 - b7 - 4 - 1: esperança"))
        // Em Lá menor: Am, F, C, G.
        assertEquals(listOf("Am", "F", "C", "G"), ciphers(9, true, "1m - 6 - 3 - 7: a do louvor no menor"))
        assertEquals(listOf("Am", "G", "F", "E"), ciphers(9, true, "A descida espanhola: 1m - 7 - 6 - 5"))
    }

    @Test fun foraDoCampoMarcado() {
        val g = Preaching.all(7, false)
        assertEquals(1, g.first { it.name == "4 - 4m - 1: o aperto e o alívio" }.outsideCount)   // Cm
        assertEquals(1, g.first { it.name == "1 - b7 - 4 - 1: esperança" }.outsideCount)         // F
        assertEquals(0, g.first { it.name == "1 - 5 - 6m - 4: a do louvor" }.outsideCount)
        // No menor, o 5 maior (com a sensível) não conta como fora.
        val e = Preaching.all(9, true).first { it.name == "4m - 5 - 1m: a cadência" }
        assertEquals(0, e.outsideCount)
    }

    @Test fun simplesECores() {
        val g = Preaching.all(7, false)
        assertFalse(g.first { it.name == "1 - 4: o vai e vem" }.colored)
        assertTrue(g.first { it.name == "1(9) - 4(9): o vai e vem com brilho" }.colored)
        assertTrue(g.first { it.name == "Baixo parado: 1 - 4/1" }.colored)      // baixo trocado conta como cor

        // Os acordes bonitos explicados no tom: em Dó, a nona do Dó é Ré.
        val c = Preaching.colors(0, false).first { it.title == "Com nona (9)" }
        assertEquals("C", c.plain.chord.cipher)
        assertEquals("C(9)", c.colored.chord.cipher)
        assertTrue(c.how.contains("Dó, Mi, Sol + Ré"))
        val pedal = Preaching.colors(0, false).first { it.title == "Baixo parado (pedal)" }
        assertEquals("F/C", pedal.colored.chord.cipher)
        assertNotNull(Preaching.colors(9, true).firstOrNull { it.title == "O 5 maior" })
    }
}
