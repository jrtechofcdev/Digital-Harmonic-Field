package com.example.music

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Análise de áudio para identificar acorde e tom, sem bibliotecas externas.
 *
 * Estratégia (padrão da área de MIR — recuperação de informação musical):
 *  1. FFT do trecho de áudio → espectro de frequências.
 *  2. Dobra o espectro em 12 classes de altura (chromagram): Dó, Dó#, ... Si.
 *  3. Acorde: compara o chromagram com moldes de tríades maiores e menores.
 *  4. Tom: compara o chromagram acumulado com os perfis de Krumhansl-Schmuckler.
 *
 * As funções aqui são puras (sem Android), para poderem ser testadas.
 */

/** Nomes das 12 classes de altura, em cifra, começando em Dó (índice 0). */
val pitchClassCiphers = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

data class ChordGuess(val cipher: String, val isMinor: Boolean, val score: Double) {
    /** Cifra final (ex.: "C", "Am"). */
    val label: String get() = if (isMinor) "${cipher}m" else cipher
}

data class KeyGuess(val root: Int, val isMinor: Boolean, val score: Double) {
    /** Cifra do tom no padrão do app (ex.: "G", "Em", "A#m"). */
    val keyCipher: String get() = pitchClassCiphers[root] + if (isMinor) "m" else ""
}

/** FFT radix-2 in-place (Cooley-Tukey). Requer tamanho potência de 2. */
fun fft(re: DoubleArray, im: DoubleArray) {
    val n = re.size
    require(n and (n - 1) == 0) { "Tamanho da FFT deve ser potência de 2" }

    // Reordenação por inversão de bits.
    var j = 0
    for (i in 1 until n) {
        var bit = n shr 1
        while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
        j = j or bit
        if (i < j) {
            val tr = re[i]; re[i] = re[j]; re[j] = tr
            val ti = im[i]; im[i] = im[j]; im[j] = ti
        }
    }

    var len = 2
    while (len <= n) {
        val ang = -2.0 * PI / len
        val wLenRe = cos(ang)
        val wLenIm = sin(ang)
        var i = 0
        while (i < n) {
            var wRe = 1.0
            var wIm = 0.0
            val half = len / 2
            for (k in 0 until half) {
                val a = i + k
                val b = i + k + half
                val vRe = re[b] * wRe - im[b] * wIm
                val vIm = re[b] * wIm + im[b] * wRe
                re[b] = re[a] - vRe
                im[b] = im[a] - vIm
                re[a] += vRe
                im[a] += vIm
                val nwRe = wRe * wLenRe - wIm * wLenIm
                wIm = wRe * wLenIm + wIm * wLenRe
                wRe = nwRe
            }
            i += len
        }
        len = len shl 1
    }
}

/**
 * Calcula o chromagram (12 valores) de um trecho de áudio já janelado ou não.
 * Aplica janela de Hann internamente. Considera apenas frequências musicais úteis.
 */
fun computeChroma(
    samples: DoubleArray,
    sampleRate: Int,
    minFreq: Double = 60.0,
    maxFreq: Double = 2000.0,
): FloatArray {
    val n = samples.size
    val re = DoubleArray(n)
    val im = DoubleArray(n)
    for (i in 0 until n) {
        val hann = 0.5 - 0.5 * cos(2.0 * PI * i / (n - 1))
        re[i] = samples[i] * hann
    }
    fft(re, im)

    val chroma = FloatArray(12)
    val ln2 = ln(2.0)
    val c0 = 16.351597831287414 // Dó0 em Hz
    val maxBin = n / 2
    for (k in 1 until maxBin) {
        val freq = k.toDouble() * sampleRate / n
        if (freq < minFreq || freq > maxFreq) continue
        val mag = sqrt(re[k] * re[k] + im[k] * im[k])
        val pc = (Math.floorMod(Math.round(12.0 * ln(freq / c0) / ln2).toInt(), 12))
        chroma[pc] += mag.toFloat()
    }
    return normalize(chroma)
}

private fun normalize(v: FloatArray): FloatArray {
    val max = v.maxOrNull() ?: 0f
    if (max <= 0f) return v
    val out = FloatArray(v.size)
    for (i in v.indices) out[i] = v[i] / max
    return out
}

// Moldes de tríade (maior: fundamental, 3ª maior, 5ª justa; menor: 3ª menor).
private val majorIntervals = intArrayOf(0, 4, 7)
private val minorIntervals = intArrayOf(0, 3, 7)

/**
 * Estima o acorde mais provável a partir do chromagram, por similaridade de cosseno
 * com os 24 moldes de tríade. Retorna null se o sinal for fraco ou ambíguo.
 */
fun detectChord(chroma: FloatArray, minScore: Double = 0.5): ChordGuess? {
    val energy = chroma.sumOf { it.toDouble() }
    if (energy <= 0.0) return null

    var best: ChordGuess? = null
    var second = 0.0
    for (root in 0 until 12) {
        for (isMinor in booleanArrayOf(false, true)) {
            val intervals = if (isMinor) minorIntervals else majorIntervals
            var dot = 0.0
            for (iv in intervals) dot += chroma[(root + iv) % 12]
            // Cosseno: molde tem norma sqrt(3); chroma norma abaixo.
            val chromaNorm = sqrt(chroma.sumOf { (it * it).toDouble() })
            val score = if (chromaNorm > 0) dot / (chromaNorm * sqrt(3.0)) else 0.0
            if (best == null || score > best!!.score) {
                second = best?.score ?: 0.0
                best = ChordGuess(pitchClassCiphers[root], isMinor, score)
            } else if (score > second) {
                second = score
            }
        }
    }
    val b = best ?: return null
    // Exige confiança mínima e alguma separação do segundo colocado.
    if (b.score < minScore || b.score - second < 0.02) return null
    return b
}

// Perfis de Krumhansl-Schmuckler (correlação de tonalidade).
private val ksMajor = doubleArrayOf(6.35, 2.23, 3.48, 2.33, 4.38, 4.09, 2.52, 5.19, 2.39, 3.66, 2.29, 2.88)
private val ksMinor = doubleArrayOf(6.33, 2.68, 3.52, 5.38, 2.60, 3.53, 2.54, 4.75, 3.98, 2.69, 3.34, 3.17)

/**
 * Estima o tom da música a partir do chromagram acumulado, correlacionando com
 * os perfis de Krumhansl-Schmuckler em todas as 24 tonalidades.
 */
fun detectKey(chroma: FloatArray): KeyGuess? {
    if (chroma.sumOf { it.toDouble() } <= 0.0) return null
    val v = DoubleArray(12) { chroma[it].toDouble() }

    var best: KeyGuess? = null
    for (root in 0 until 12) {
        for (isMinor in booleanArrayOf(false, true)) {
            val profile = if (isMinor) ksMinor else ksMajor
            val rotated = DoubleArray(12) { profile[Math.floorMod(it - root, 12)] }
            val score = pearson(v, rotated)
            if (best == null || score > best!!.score) {
                best = KeyGuess(root, isMinor, score)
            }
        }
    }
    return best
}

private fun pearson(a: DoubleArray, b: DoubleArray): Double {
    val n = a.size
    val ma = a.average()
    val mb = b.average()
    var num = 0.0
    var da = 0.0
    var db = 0.0
    for (i in 0 until n) {
        val xa = a[i] - ma
        val xb = b[i] - mb
        num += xa * xb
        da += xa * xa
        db += xb * xb
    }
    val den = sqrt(da * db)
    return if (den == 0.0) 0.0 else num / den
}

/** Mistura exponencial de dois chromagrams (para suavizar no tempo). */
fun blendChroma(previous: FloatArray, current: FloatArray, alpha: Float): FloatArray {
    val out = FloatArray(12)
    for (i in 0 until 12) out[i] = previous[i] * (1 - alpha) + current[i] * alpha
    return out
}

/** Índice cromático de uma cifra (para testes). */
fun pitchClassOf(cipher: String): Int = pitchClassCiphers.indexOf(cipher)
