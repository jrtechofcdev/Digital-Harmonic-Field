package com.example

import com.example.music.KeyDetector
import com.example.music.KeyResult
import com.example.music.KeyStatus
import com.example.music.KeyStopRule
import kotlin.math.exp
import java.util.Random
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cenários sintéticos de igreja (voz com harmônicos e vibrato, congregação em
 * uníssono desafinada, teclado, burburinho) para validar o detector de tom.
 */
class KeyDetectionTest {

    private val sr = 44100
    private val total = 5.0
    private val n = (total * sr).toInt()

    private fun mtof(m: Double) = 440.0 * 2.0.pow((m - 69) / 12)

    /** Melodia cantada: notas (midi, duração relativa) repetidas até 5 s. */
    private fun voice(
        notes: List<Pair<Int, Int>>,
        detuneCents: Double = 0.0,
        voices: Int = 1,
        amp: Double = 0.25,
        rng: Random = Random(1),
    ): DoubleArray {
        val out = DoubleArray(n)
        val seq = ArrayList<Triple<Double, Int, Double>>()
        var pos = 0.0
        while (pos < total) for ((m, d) in notes) {
            seq.add(Triple(pos, m, d * 0.45)); pos += d * 0.45
            if (pos >= total) break
        }
        for (v in 0 until voices) {
            val vd = detuneCents + if (voices > 1) rng.nextGaussian() * 12 else 0.0
            val octave = if (voices > 2 && v % 3 == 2) -12 else 0
            val lag = if (voices > 1) rng.nextDouble() * 0.06 else 0.0
            for ((start, m, dur) in seq) {
                val i0 = ((start + lag) * sr).toInt()
                val i1 = min(n, ((start + lag + dur * 0.95) * sr).toInt())
                if (i0 >= n) break
                var phase = 0.0
                val len = (i1 - i0).toDouble() / sr
                for (i in i0 until i1) {
                    val tt = (i - i0).toDouble() / sr
                    val f0 = mtof((m + octave).toDouble()) * 2.0.pow(vd / 1200) *
                        (1 + 0.006 * sin(2 * PI * 5.5 * tt))
                    phase += 2 * PI * f0 / sr
                    val env = min(1.0, tt / 0.04) * min(1.0, (len - tt + 1e-9) / 0.05)
                    var s = 0.0
                    var a = 1.0
                    for (k in 1..6) { s += a * sin(k * phase); a *= 0.6 }
                    out[i] += amp / voices * env * s
                }
            }
        }
        return out
    }

    private fun pads(chords: List<List<Int>>, amp: Double = 0.08): DoubleArray {
        val out = DoubleArray(n)
        val seg = n / chords.size
        chords.forEachIndexed { c, chord ->
            for (i in c * seg until min(n, (c + 1) * seg)) for (m in chord) {
                out[i] += amp * sin(2 * PI * mtof(m.toDouble()) * i / sr)
            }
        }
        return out
    }

    /** Burburinho: ruído + rajadas "faladas" com tom deslizante + zumbido de 60 Hz. */
    private fun crowd(amp: Double = 0.05, rng: Random = Random(2)): DoubleArray {
        val out = DoubleArray(n) { rng.nextGaussian() * amp + 0.03 * sin(2 * PI * 60 * it / sr) }
        repeat(40) {
            val st = rng.nextInt(n - 4000)
            val len = 1500 + rng.nextInt(2500)
            val f = 120 + rng.nextDouble() * 180
            var phase = 0.0
            for (i in 0 until len) {
                val tt = i.toDouble() / sr
                phase += 2 * PI * f * (1 + 0.2 * sin(2 * PI * 3 * tt)) / sr
                out[st + i] += amp * 1.5 * sin(phase)
            }
        }
        return out
    }

    /** Conversa: voz falada (tom deslizando o tempo todo), sem canto. */
    private fun speech(rng: Random = Random(3)): DoubleArray {
        val out = DoubleArray(n) { rng.nextGaussian() * 0.02 }
        repeat(25) {
            val st = rng.nextInt(n - 9000)
            val len = 3000 + rng.nextInt(6000)
            val base = 110 + rng.nextDouble() * 140
            val rate = 2 + rng.nextDouble() * 3
            var phase = 0.0
            for (i in 0 until len) {
                val tt = i.toDouble() / sr
                val drift = 1.2 - 0.4 * i / len
                phase += 2 * PI * base * (1 + 0.35 * sin(2 * PI * rate * tt)) * drift / sr
                var s = 0.0
                var a = 1.0
                for (k in 1..5) { s += a * sin(k * phase); a *= 0.5 }
                out[st + i] += 0.12 * s
            }
        }
        return out
    }

    /** Baixo tocando a fundamental de cada acorde (corda com harmônicos, decaindo). */
    private fun bass(roots: List<Int>, amp: Double = 0.2): DoubleArray {
        val out = DoubleArray(n)
        val seg = n / roots.size
        roots.forEachIndexed { c, m ->
            val f = mtof(m.toDouble())
            for (i in c * seg until min(n, (c + 1) * seg)) {
                val tt = (i - c * seg).toDouble() / sr
                var s = 0.0
                var a = 1.0
                for (k in 1..4) { s += a * sin(2 * PI * f * k * tt); a *= 0.5 }
                out[i] += amp * exp(-tt * 0.8) * s
            }
        }
        return out
    }

    /** Zumbido da rede elétrica (60 Hz + harmônicos), alto e constante. */
    private fun hum(amp: Double = 0.08) = DoubleArray(n) {
        val t = it.toDouble() / sr
        amp * (sin(2 * PI * 60 * t) + 0.6 * sin(2 * PI * 120 * t) + 0.3 * sin(2 * PI * 180 * t))
    }

    private fun mix(vararg parts: DoubleArray) = DoubleArray(n) { i -> parts.sumOf { it[i] } }

    private fun analyze(signal: DoubleArray): KeyResult {
        val detector = KeyDetector(sr)
        val chunk = DoubleArray(2048)
        var i = 0
        while (i < signal.size) {
            val c = min(2048, signal.size - i)
            System.arraycopy(signal, i, chunk, 0, c)
            detector.feed(chunk, c)
            i += c
        }
        return detector.result()
    }

    /**
     * Simula o uso no app: alimenta em blocos e consulta a regra de parada a cada
     * ~0,28 s. Devolve (segundos até parar, resultado) — ou null se não parou.
     */
    private fun stopTime(signal: DoubleArray, rule: KeyStopRule = KeyStopRule()): Pair<Double, KeyResult>? {
        val detector = KeyDetector(sr)
        val chunk = DoubleArray(2048)
        var i = 0
        var chunks = 0
        while (i < signal.size) {
            val c = min(2048, signal.size - i)
            System.arraycopy(signal, i, chunk, 0, c)
            detector.feed(chunk, c)
            i += c
            if (++chunks % 6 == 0) {
                val r = detector.result()
                if (rule.shouldStop(r, detector.secondsFed)) return detector.secondsFed to r
            }
        }
        return null
    }

    private val hymnG = listOf(67 to 1, 71 to 1, 74 to 1, 71 to 1, 67 to 1, 69 to 1, 71 to 1, 72 to 1, 71 to 1, 69 to 1, 67 to 2)
    private val melodyAm = listOf(69 to 1, 72 to 1, 76 to 1, 69 to 1, 71 to 1, 72 to 1, 74 to 1, 72 to 1, 71 to 1, 69 to 2)

    @Test
    fun vozLimpaEmSol_identificaSolComConfiancaAlta() {
        val r = analyze(voice(hymnG))
        assertEquals("G", r.candidates.first().keyCipher)
        assertEquals(KeyStatus.ALTA, r.status)
    }

    @Test
    fun grupoDesafinado35Cents_continuaSol() {
        val r = analyze(voice(hymnG, detuneCents = 35.0))
        assertEquals("G", r.candidates.first().keyCipher)
        assertTrue("afinação do grupo detectada: ${r.tuningOffsetCents}", r.tuningOffsetCents in 25.0..45.0)
    }

    @Test
    fun congregacaoComTecladoEBurburinho_identificaSol() {
        val signal = mix(
            voice(hymnG, voices = 8, amp = 0.3),
            pads(listOf(listOf(55, 59, 62), listOf(60, 64, 67), listOf(62, 66, 69), listOf(55, 59, 62))),
            crowd(),
        )
        val r = analyze(signal)
        assertTrue(r.hasAnswer)
        assertEquals("G", r.candidates.first().keyCipher)
    }

    @Test
    fun melodiaMenorComRuido_identificaLaMenor() {
        val r = analyze(mix(voice(melodyAm), crowd(amp = 0.04)))
        assertEquals("Am", r.candidates.first().keyCipher)
    }

    @Test
    fun vozMasculinaGrave_identificaDo() {
        val low = listOf(48 to 1, 52 to 1, 55 to 1, 53 to 1, 52 to 1, 50 to 1, 48 to 2)
        val r = analyze(mix(voice(low), crowd()))
        assertEquals("C", r.candidates.first().keyCipher)
    }

    @Test
    fun soRuido_naoInventaTom() {
        val r = analyze(crowd())
        assertFalse("não pode sugerir tom só com ruído", r.hasAnswer)
    }

    @Test
    fun conversaSemCanto_naoInventaTom() {
        val r = analyze(speech())
        assertFalse("não pode sugerir tom com conversa", r.hasAnswer)
    }

    @Test
    fun notasCromaticasSemTonalidade_recusa() {
        val chromatic = (60..71).map { it to 1 }
        val r = analyze(voice(chromatic))
        assertEquals(KeyStatus.INSUFICIENTE, r.status)
        assertFalse(r.hasAnswer)
    }

    @Test
    fun silencio_semVoz() {
        assertEquals(KeyStatus.SEM_VOZ, analyze(DoubleArray(n)).status)
    }

    @Test
    fun poucasNotas_naoMarcaConfiancaAlta() {
        val r = analyze(voice(listOf(67 to 1, 69 to 1, 71 to 1, 69 to 1)))
        assertNotEquals(KeyStatus.ALTA, r.status)
    }

    @Test
    fun candidatosSaoSempreTonsReais() {
        val r = analyze(voice(hymnG, voices = 6, detuneCents = -20.0))
        assertTrue(r.candidates.size in 1..3)
        r.candidates.forEach {
            assertTrue("${it.keyCipher} precisa existir no app", HarmonicDatabase.ptNameByCipher.containsKey(it.keyCipher))
        }
    }

    // ----- Canal do baixo -----

    @Test
    fun bandaComBaixo_identificaSolEUsaOBaixo() {
        val signal = mix(
            voice(hymnG, voices = 6, amp = 0.3),
            pads(listOf(listOf(55, 59, 62), listOf(60, 64, 67), listOf(62, 66, 69), listOf(55, 59, 62)), amp = 0.05),
            bass(listOf(43, 48, 50, 43)),
            crowd(amp = 0.05),
        )
        val r = analyze(signal)
        assertEquals("G", r.candidates.first().keyCipher)
        assertTrue("baixo deveria entrar na análise: ${r.bassSeconds}", r.bassSeconds >= 1.0)
    }

    @Test
    fun baixoDesempataMelodiaAmbigua() {
        // Melodia só com Lá, Dó, Mi e Sol: serve tanto para Dó maior quanto Lá menor.
        val ambiguous = listOf(69 to 1, 72 to 1, 76 to 1, 79 to 1, 76 to 1, 72 to 1, 69 to 1, 72 to 1)
        val withMinorBass = analyze(mix(voice(ambiguous), bass(listOf(45, 50, 52, 45))))
        val withMajorBass = analyze(mix(voice(ambiguous), bass(listOf(48, 53, 55, 48))))
        assertEquals("Am", withMinorBass.candidates.first().keyCipher)
        assertEquals("C", withMajorBass.candidates.first().keyCipher)
    }

    @Test
    fun zumbidoDaRede_naoViraBaixo() {
        val r = analyze(mix(voice(hymnG), hum()))
        assertEquals("G", r.candidates.first().keyCipher)
        assertEquals("zumbido não pode contar como baixo", 0.0, r.bassSeconds, 0.0)
    }

    @Test
    fun congregacaoACapela_naoInventaBaixo() {
        val r = analyze(mix(voice(hymnG, voices = 8, amp = 0.3), crowd()))
        assertEquals("G", r.candidates.first().keyCipher)
        assertEquals("vozes graves não são baixo", 0.0, r.bassSeconds, 0.0)
    }

    @Test
    fun soBaixoSemCanto_naoResponde() {
        val r = analyze(bass(listOf(43, 48, 50, 43)))
        assertFalse("sem voz, o baixo sozinho não decide o tom", r.hasAnswer)
    }

    // ----- Parada antecipada -----

    @Test
    fun cantoClaro_respondeAntesDos5Segundos() {
        val stop = stopTime(mix(voice(hymnG, voices = 6, amp = 0.3), crowd(amp = 0.04)))
        assertTrue("deveria responder antes de 5 s", stop != null && stop.first < 4.5)
        assertEquals(KeyStatus.ALTA, stop!!.second.status)
        assertEquals("G", stop.second.candidates.first().keyCipher)
    }

    @Test
    fun soRuido_naoParaAntesDoLimite() {
        // Com 5 s de ruído e limite de 8 s, a regra nunca encerra "achando" um tom.
        assertEquals(null, stopTime(crowd()))
    }

    @Test
    fun regraDeParada_noLimiteEncerraMesmoSemResposta() {
        val rule = KeyStopRule(maxSeconds = 4.0)
        val stop = stopTime(crowd(), rule)
        assertTrue(stop != null && stop.first >= 4.0)
        assertFalse(stop!!.second.hasAnswer)
    }
}
