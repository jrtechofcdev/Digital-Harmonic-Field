package com.example.music


/**
 * Detecção de altura (pitch) de UMA nota por vez — usada pelo detector de tom
 * para seguir a melodia cantada e a linha do baixo.
 *
 * Usamos autocorrelação normalizada (NSDF), base do McLeod Pitch Method, com
 * interpolação parabólica para precisão de subamostra. Funções puras, sem
 * Android, para poder testar.
 */

data class PitchResult(val frequency: Double, val clarity: Double)

private val ptPitchNames = listOf("Dó", "Dó#", "Ré", "Ré#", "Mi", "Fá", "Fá#", "Sol", "Sol#", "Lá", "Lá#", "Si")

/** Nome da nota em português (ex.: 7 ou 67 → "Sol"). */
fun ptPitchClass(midi: Int): String = ptPitchNames[Math.floorMod(midi, 12)]

/**
 * Estima a frequência fundamental de um trecho de áudio, ou null se não houver
 * um tom claro (silêncio/ruído). `minClarity` é a exigência de periodicidade (0..1).
 */
fun detectPitch(
    samples: DoubleArray,
    sampleRate: Int,
    minFreq: Double = 60.0,
    maxFreq: Double = 1200.0,
    minClarity: Double = 0.90,
): PitchResult? {
    val n = samples.size
    if (n < 64) return null

    // Remove componente contínua (DC).
    var mean = 0.0
    for (v in samples) mean += v
    mean /= n
    val x = DoubleArray(n) { samples[it] - mean }

    val minLag = (sampleRate / maxFreq).toInt().coerceAtLeast(2)
    val maxLag = (sampleRate / minFreq).toInt().coerceAtMost(n - 1)
    if (maxLag <= minLag) return null

    // NSDF: n(τ) = 2·Σ x[i]x[i+τ] / Σ (x[i]² + x[i+τ]²)
    val nsdf = DoubleArray(maxLag + 1)
    for (tau in minLag..maxLag) {
        var acf = 0.0
        var norm = 0.0
        val limit = n - tau
        for (i in 0 until limit) {
            val a = x[i]
            val b = x[i + tau]
            acf += a * b
            norm += a * a + b * b
        }
        nsdf[tau] = if (norm > 0.0) 2.0 * acf / norm else 0.0
    }

    // Maior pico da NSDF; escolhemos o PRIMEIRO pico local que chega perto dele
    // (evita cair numa oitava acima ao pegar um harmônico).
    var globalMax = 0.0
    for (tau in minLag..maxLag) if (nsdf[tau] > globalMax) globalMax = nsdf[tau]
    if (globalMax <= 0.0) return null

    val threshold = 0.9 * globalMax
    var chosen = -1
    var tau = minLag + 1
    while (tau < maxLag) {
        if (nsdf[tau] > nsdf[tau - 1] && nsdf[tau] >= nsdf[tau + 1] && nsdf[tau] >= threshold) {
            chosen = tau
            break
        }
        tau++
    }
    if (chosen <= minLag || chosen >= maxLag) return null

    // Interpolação parabólica em torno do pico → lag fracionário.
    val a = nsdf[chosen - 1]
    val b = nsdf[chosen]
    val c = nsdf[chosen + 1]
    val denom = a - 2.0 * b + c
    val shift = if (denom != 0.0) 0.5 * (a - c) / denom else 0.0
    val betterTau = chosen + shift
    if (betterTau <= 0.0) return null

    val freq = sampleRate / betterTau
    val clarity = b
    if (clarity < minClarity) return null
    if (freq < minFreq || freq > maxFreq) return null
    return PitchResult(freq, clarity)
}
