package com.example

import com.example.dev.DevEval
import com.example.music.KeyModel
import com.example.music.KeyTrainer
import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyTrainerTest {

    /**
     * Mundo de mentira em que o tom "certo" segue uma cabeça diferente da original
     * (ex.: os hinos da igreja puxam mais para menor). O treinador precisa
     * aprender isso — e a validação cruzada precisa mostrar o ganho.
     */
    private fun world(n: Int, shift: Double = 10.0, seed: Long = 7): List<KeyTrainer.Sample> {
        val base = KeyModel.baseHead
        val truthHead = KeyModel.Head(base.w2, doubleArrayOf(base.b2[0], base.b2[1] + shift), base.temperature)
        val rng = Random(seed)
        val hidden = base.w2.size / 2
        return (0 until n).map { i ->
            val h = Array(12) { DoubleArray(hidden) { kotlin.math.abs(rng.nextGaussian()) } }
            val p = KeyModel.probabilitiesFromHidden(h, truthHead)
            val label = p.indices.maxBy { p[it] }
            KeyTrainer.Sample(h, label, "hino${i / 3}")
        }
    }

    @Test
    fun ajusteAprendeOViesDosHinosReais() {
        val samples = world(240)
        val r = KeyTrainer.crossValidate(samples, KeyModel.baseHead)
        assertTrue("ajustado ${r.adaptedAccuracy} deveria superar base ${r.baseAccuracy}", r.adaptedAccuracy > r.baseAccuracy + 0.1)
        assertEquals(80, r.hymns)
    }

    @Test
    fun treinoSeguro_naoPioraQuandoOriginalJaAcerta() {
        val samples = world(180, shift = 0.0)
        val o = KeyTrainer.train(samples, KeyModel.baseHead)
        // Ou não troca o modelo, ou troca por um que mediu igual/melhor.
        assertTrue(!o.adapted || o.report.adaptedAccuracy > o.report.baseAccuracy)
        if (!o.adapted) assertTrue(o.head.w2.contentEquals(KeyModel.baseHead.w2))
    }

    @Test
    fun treinoSeguro_trocaQuandoMelhora() {
        val o = KeyTrainer.train(world(180), KeyModel.baseHead)
        assertTrue(o.adapted)
    }

    @Test
    fun ajusteReduzAPerda() {
        val samples = world(120)
        val head = KeyTrainer.fit(samples, KeyModel.baseHead)
        assertTrue(KeyTrainer.loss(samples, head) < KeyTrainer.loss(samples, KeyModel.baseHead))
    }

    @Test
    fun semExemplos_mantemOriginal() {
        val head = KeyTrainer.fit(emptyList(), KeyModel.baseHead)
        assertTrue(head.w2.contentEquals(KeyModel.baseHead.w2))
    }

    @Test
    fun tiposDeErro() {
        assertNull(DevEval.errorKind("G", "G"))
        assertEquals("relativa", DevEval.errorKind("Em", "G"))
        assertEquals("relativa", DevEval.errorKind("G", "Em"))
        assertEquals("quinta_acima", DevEval.errorKind("D", "G"))
        assertEquals("quarta_acima", DevEval.errorKind("C", "G"))
        assertEquals("homonima", DevEval.errorKind("Gm", "G"))
        assertEquals("semitom", DevEval.errorKind("G#", "G"))
        assertEquals("sem_resposta", DevEval.errorKind(null, "G"))
    }
}
