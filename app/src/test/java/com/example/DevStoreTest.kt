package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.dev.DevStore
import com.example.music.KeyDetector
import com.example.music.KeyModel
import java.io.File
import java.util.zip.ZipFile
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Fluxo da versão DEV: captar → salvar pendente → rotular → estatísticas → treino → .zip. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DevStoreTest {

    private val sr = 44100

    /** Hino em Sol, cantado (voz com harmônicos), [seconds] de duração, em PCM 16 bits. */
    private fun hymn(seconds: Double): ShortArray {
        val notes = listOf(67, 71, 74, 71, 67, 69, 71, 72, 71, 69, 67, 67)
        val n = (seconds * sr).toInt()
        val out = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / sr
            val idx = ((t / 0.45).toInt()) % notes.size
            val inNote = t % 0.45
            val f = 440.0 * 2.0.pow((notes[idx] - 69) / 12.0)
            phase += 2 * PI * f / sr
            val env = min(1.0, inNote / 0.04) * min(1.0, (0.43 - inNote).coerceAtLeast(0.0) / 0.05)
            var s = 0.0
            var a = 1.0
            for (k in 1..6) { s += a * sin(k * phase); a *= 0.6 }
            out[i] = (0.25 * env * s * 32767 / 2.5).toInt().toShort()
        }
        return out
    }

    @Test
    fun fluxoCompletoDaPastaDeTreino() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        DevStore.init(ctx, force = true)
        val rec = DevStore.recorder
        val det = KeyDetector(sr)
        val pcm = hymn(12.0)

        rec.onStart(sr)
        val chunk = ShortArray(2048)
        val samples = DoubleArray(2048)
        var i = 0
        var chunks = 0
        while (i < pcm.size) {
            val c = min(2048, pcm.size - i)
            System.arraycopy(pcm, i, chunk, 0, c)
            for (k in 0 until c) samples[k] = chunk[k] / 32768.0
            rec.onAudio(chunk, c)
            det.feed(samples, c)
            i += c
            if (++chunks % 6 == 0) rec.onEvaluation(det, det.result())
        }
        rec.onDone(det, det.result(), "limite")
        DevStore.awaitIdle()

        val id = rec.currentId!!
        val dir = DevStore.dir
        // Áudio: exatamente o que entrou no detector, com cabeçalho WAV de 44 bytes.
        val wav = File(dir, "audio/$id.wav")
        assertEquals(44L + pcm.size * 2L, wav.length())

        // sessoes.json: pendente, com rodadas de 5 s e resultado final.
        val s = JSONObject(File(dir, "sessoes.json").readText()).getJSONArray("sessoes").getJSONObject(0)
        assertEquals("pendente", s.getString("status"))
        assertTrue(s.getJSONArray("rodadas").length() >= 2)
        assertTrue(s.getJSONObject("resultado_final").getJSONArray("top").length() > 0)

        // Rótulo do músico → avaliação automática.
        DevStore.saveLabel(
            id,
            JSONObject().put("tom", "G").put("certeza", "certeza").put("estrelas", 5)
                .put("tags", JSONArray(listOf("acertou_de_primeira"))),
        )
        DevStore.awaitIdle()
        val labeled = DevStore.session(id)!!
        assertEquals("rotulado", labeled.getString("status"))
        assertTrue(labeled.getJSONObject("avaliacao").has("acertou_final"))
        val st = DevStore.stats()
        assertEquals(1, st.labeled)
        assertEquals(5.0, st.avgStars!!, 0.0)

        // Exemplos para o treinador do app (features a cada 2,5 s).
        val samplesForTrainer = DevStore.trainingSamples()
        assertTrue(samplesForTrainer.size >= 4)
        assertEquals(7, samplesForTrainer.first().label)  // Sol maior
        assertEquals(12, samplesForTrainer.first().hidden.size)

        // Log e .zip com tudo numa pasta só.
        assertTrue(File(dir, "log.jsonl").readLines().any { it.contains("rotulo_salvo") })
        val zip = DevStore.exportZip()
        val names = ZipFile(zip).use { z -> z.entries().toList().map { it.name } }
        assertTrue(names.contains("treino-real/sessoes.json"))
        assertTrue(names.contains("treino-real/audio/$id.wav"))
        assertTrue(names.contains("treino-real/LEIA-ME.txt"))
        assertTrue(KeyModel.DIM > 0)
    }
}
