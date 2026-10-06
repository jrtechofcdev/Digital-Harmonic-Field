package com.example.music

import java.io.DataInputStream
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Modelo de tom TREINADO — a "inteligência" do detector.
 *
 * Em vez de regras escritas à mão, uma rede neural pequena aprendeu, com milhares
 * de trechos de hinos (congregação, teclado, baixo, templo com eco, burburinho),
 * o que um músico experiente percebe de ouvido:
 *  - quais notas a melodia mais usa e onde as frases RESPIRAM (fim de frase
 *    costuma cair na tônica, na terça ou na quinta);
 *  - os PASSOS da melodia (ex.: a sensível subindo para a tônica, 7 → 1);
 *  - o caminho do BAIXO (ex.: 5 → 1, a cadência que fecha a frase);
 *  - o perfil harmônico do que soa junto.
 * Por isso, no meio de uma frase ele já "espera" para onde ela vai resolver.
 *
 * A rede é a MESMA para as 12 tônicas (o trecho é visto a partir de cada tônica
 * candidata), então funciona igual em qualquer tom. Roda no aparelho, sem
 * internet, em menos de 1 ms. Os pesos ficam em `key_model.bin` (gerado pelo
 * treinamento; veja docs/detector-de-tom.md).
 */
object KeyModel {

    private const val GAP_PHRASE = 0.30  // respiro ≥ 0,3 s = fim de frase
    private const val GAP_LINK = 0.45    // notas "ligadas" para formar pares melódicos
    private const val GAP_BASS = 0.60
    private const val MIN_BASS_SECONDS = 0.5

    private class Weights(
        val dim: Int, val hidden: Int, val temperature: Float,
        val w1: FloatArray, val b1: FloatArray, val w2: FloatArray, val b2: FloatArray,
    )

    private val weights: Weights by lazy {
        val stream = KeyModel::class.java.getResourceAsStream("key_model.bin")
            ?: error("key_model.bin não encontrado")
        DataInputStream(stream.buffered()).use { inp ->
            fun f() = java.lang.Float.intBitsToFloat(Integer.reverseBytes(inp.readInt()))
            fun i() = Integer.reverseBytes(inp.readInt())
            val dim = i()
            val hidden = i()
            val t = f()
            val w1 = FloatArray(dim * hidden) { f() }
            val b1 = FloatArray(hidden) { f() }
            val w2 = FloatArray(hidden * 2) { f() }
            val b2 = FloatArray(2) { f() }
            Weights(dim, hidden, t, w1, b1, w2, b2)
        }
    }

    private const val BLOCKS12 = 9
    /** Número de features por tônica: 9 perfis de 12 + 2 tabelas de pares 12×12. */
    const val DIM = 12 * BLOCKS12 + 144 * 2

    /**
     * Chances (soma 1) dos 24 tons: índice = tônica (0 = Dó) + 12 se menor.
     * Retorna null se não houver nota cantada nenhuma.
     */
    fun probabilities(snapshot: EvidenceSnapshot): DoubleArray? {
        if (snapshot.voice.isEmpty()) return null
        val x = features(snapshot)
        val w = weights
        require(w.dim == DIM) { "modelo incompatível: ${w.dim} ≠ $DIM" }
        val logits = DoubleArray(24)
        val rotated = DoubleArray(DIM)
        val h = DoubleArray(w.hidden)
        for (k in 0 until 12) {
            rotate(x, k, rotated)
            for (j in 0 until w.hidden) {
                var acc = w.b1[j].toDouble()
                for (d in 0 until DIM) acc += rotated[d] * w.w1[d * w.hidden + j]
                h[j] = if (acc > 0) acc else 0.0
            }
            for (m in 0 until 2) {
                var acc = w.b2[m].toDouble()
                for (j in 0 until w.hidden) acc += h[j] * w.w2[j * 2 + m]
                logits[k + 12 * m] = acc / w.temperature
            }
        }
        val max = logits.max()
        var sum = 0.0
        for (i in 0 until 24) { logits[i] = exp(logits[i] - max); sum += logits[i] }
        for (i in 0 until 24) logits[i] /= sum
        return logits
    }

    /** Features em classes de altura ABSOLUTAS (0 = Dó). Espelho de feats.py. */
    fun features(s: EvidenceSnapshot): DoubleArray {
        val out = DoubleArray(DIM)
        val voice = s.voice
        val off = tuningOffset(voice)
        val pv = IntArray(voice.size) { Math.floorMod((voice[it].midi - off).roundToInt(), 12) }

        val vHist = DoubleArray(12); val vCnt = DoubleArray(12); val vLast = DoubleArray(12)
        val vEnd = DoubleArray(12); val vLong = DoubleArray(12); val vBig = DoubleArray(144)
        for (i in voice.indices) {
            val d = voice[i].endSec - voice[i].startSec
            vHist[pv[i]] += d; vCnt[pv[i]] += 1.0
            if (i + 1 < voice.size) {
                val gap = voice[i + 1].startSec - voice[i].endSec
                if (gap >= GAP_PHRASE) vEnd[pv[i]] += d
                if (gap < GAP_LINK && pv[i] != pv[i + 1]) vBig[pv[i] * 12 + pv[i + 1]] += 1.0
            }
        }
        if (voice.isNotEmpty()) {
            vLast[pv.last()] = 1.0
            val md = median(voice.map { it.endSec - it.startSec })
            for (i in voice.indices) {
                val d = voice[i].endSec - voice[i].startSec
                if (d >= 1.6 * md) vLong[pv[i]] += d
            }
        }

        val bass = s.bass
        val boff = if (bass.isEmpty()) 0.0 else tuningOffset(bass)
        val pb = IntArray(bass.size) { Math.floorMod((bass[it].midi - boff).roundToInt(), 12) }
        val bHist = DoubleArray(12); val bLast = DoubleArray(12); val bBig = DoubleArray(144)
        var bassSec = 0.0
        for (i in bass.indices) {
            val d = bass[i].endSec - bass[i].startSec
            bHist[pb[i]] += d; bassSec += d
            if (i + 1 < bass.size && bass[i + 1].startSec - bass[i].endSec < GAP_BASS && pb[i] != pb[i + 1]) {
                bBig[pb[i] * 12 + pb[i + 1]] += 1.0
            }
        }
        if (bass.isNotEmpty()) bLast[pb.last()] = 1.0
        val hasBass = if (bassSec >= MIN_BASS_SECONDS) 1.0 else 0.0

        val blocks12 = listOf(
            normalize(vHist), normalize(vCnt), vLast, normalize(vEnd), normalize(vLong),
            scale(normalize(bHist), hasBass), scale(bLast, hasBass), normalize(s.harmony.copyOf()),
            scale(normalize(s.bassChroma.copyOf()), if (s.bassPresent) 1.0 else 0.0),
        )
        var p = 0
        for (b in blocks12) { b.copyInto(out, p); p += 12 }
        normalize(vBig).copyInto(out, p); p += 144
        scale(normalize(bBig), hasBass).copyInto(out, p)
        return out
    }

    /** Vê as features a partir da tônica [k]: a classe k vira 0. */
    private fun rotate(x: DoubleArray, k: Int, out: DoubleArray) {
        for (b in 0 until BLOCKS12) for (i in 0 until 12) out[b * 12 + i] = x[b * 12 + (i + k) % 12]
        for (t in 0 until 2) {
            val base = 12 * BLOCKS12 + t * 144
            for (i in 0 until 12) for (j in 0 until 12) {
                out[base + i * 12 + j] = x[base + ((i + k) % 12) * 12 + (j + k) % 12]
            }
        }
    }

    private fun tuningOffset(segs: List<NoteSegment>): Double {
        var sx = 0.0
        var sy = 0.0
        for (s in segs) {
            val a = 2 * PI * (s.midi - s.midi.roundToInt())
            sx += s.weight * cos(a); sy += s.weight * sin(a)
        }
        return atan2(sy, sx) / (2 * PI)
    }

    private fun normalize(v: DoubleArray): DoubleArray {
        val s = v.sum()
        if (s > 0) for (i in v.indices) v[i] /= s
        return v
    }

    private fun scale(v: DoubleArray, f: Double): DoubleArray {
        for (i in v.indices) v[i] *= f
        return v
    }

    private fun median(v: List<Double>): Double {
        val s = v.sorted()
        return if (s.size % 2 == 1) s[s.size / 2] else (s[s.size / 2 - 1] + s[s.size / 2]) / 2
    }
}
