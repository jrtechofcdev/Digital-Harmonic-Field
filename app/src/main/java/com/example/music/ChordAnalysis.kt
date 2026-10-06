package com.example.music

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Base espectral do app: FFT, mapeamento frequência → classe de altura e
 * chromagram. Funções puras (sem Android), usadas pelo detector de tom.
 */

/** Nomes das 12 classes de altura, em cifra, começando em Dó (índice 0). */
val pitchClassCiphers = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

private const val C0_HZ = 16.351597831287414 // Dó0
private val LN2 = ln(2.0)

/** Classe de altura (0 = Dó … 11 = Si) mais próxima de uma frequência. */
fun pitchClassOfFrequency(freq: Double): Int =
    Math.floorMod(Math.round(12.0 * ln(freq / C0_HZ) / LN2).toInt(), 12)

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

// Janelas de Hann memorizadas por tamanho (evita recomputar cos a cada quadro).
private val hannWindows = HashMap<Int, DoubleArray>()

private fun hannWindow(n: Int): DoubleArray = hannWindows.getOrPut(n) {
    DoubleArray(n) { 0.5 - 0.5 * cos(2.0 * PI * it / (n - 1)) }
}

/**
 * Chromagram (12 valores, máximo = 1) de um trecho de áudio. Um portão de ruído
 * suave descarta o "chão" de banda larga e mantém as parciais musicais.
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
    val hann = hannWindow(n)
    for (i in 0 until n) re[i] = samples[i] * hann[i]
    fft(re, im)

    val maxBin = n / 2
    val minK = (minFreq * n / sampleRate).toInt().coerceAtLeast(1)
    val maxK = (maxFreq * n / sampleRate).toInt().coerceAtMost(maxBin - 1)

    var maxPower = 0.0
    for (k in minK..maxK) {
        val power = re[k] * re[k] + im[k] * im[k]
        if (power > maxPower) maxPower = power
    }
    val gate = maxPower * 0.003 // ≈ 5,5% da amplitude do pico

    val chroma = FloatArray(12)
    for (k in minK..maxK) {
        val power = re[k] * re[k] + im[k] * im[k]
        if (power < gate) continue
        chroma[pitchClassOfFrequency(k.toDouble() * sampleRate / n)] += sqrt(power).toFloat()
    }
    val max = chroma.maxOrNull() ?: 0f
    if (max > 0f) for (i in chroma.indices) chroma[i] /= max
    return chroma
}
