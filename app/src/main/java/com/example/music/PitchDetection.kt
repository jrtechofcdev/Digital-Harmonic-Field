package com.example.music

import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Detecção de altura (pitch) para o afinador. Diferente da identificação de
 * acorde (polifônica), afinar exige precisão fina em UMA nota por vez.
 *
 * Usamos autocorrelação normalizada (NSDF), base do McLeod Pitch Method — o
 * padrão para afinadores: robusto para corda solta, com interpolação parabólica
 * para precisão de subamostra. Funções puras, sem Android, para poder testar.
 */

data class PitchResult(val frequency: Double, val clarity: Double)

private val noteNames = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

/**
 * Estima a frequência fundamental de um trecho de áudio, ou null se não houver
 * um tom claro (silêncio/ruído). `minClarity` vem do filtro de ruído.
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

/** Frequência de uma nota MIDI, dado o Lá de referência (A4 = MIDI 69). */
fun midiToFrequency(midi: Int, refA: Double = 440.0): Double =
    refA * 2.0.pow((midi - 69) / 12.0)

/** Nome + oitava de uma nota MIDI (ex.: 40 → "E2"). */
fun midiToName(midi: Int): String {
    val pc = Math.floorMod(midi, 12)
    val octave = midi / 12 - 1
    return noteNames[pc] + octave
}

/** Desvio em cents entre uma frequência e um alvo (positivo = acima/agudo). */
fun centsBetween(freq: Double, target: Double): Double =
    1200.0 * ln(freq / target) / ln(2.0)

/**
 * Cents até um alvo, dobrando a frequência para a oitava mais próxima do alvo.
 * Assim, no modo "corda por corda", uma corda muito frouxa (que soa uma oitava
 * abaixo) ainda aponta o desvio correto, sem o ponteiro estourar a escala.
 */
fun centsToTargetFolded(freq: Double, target: Double): Double {
    var f = freq
    val sqrt2 = 1.4142135623730951
    while (f / target > sqrt2) f /= 2.0
    while (target / f > sqrt2) f *= 2.0
    return centsBetween(f, target)
}

data class NoteReading(
    val midi: Int,
    val name: String,
    val cents: Double,   // -50..+50 em relação à nota cromática mais próxima
    val frequency: Double,
)

/** Nota cromática mais próxima de uma frequência (modo cromático do afinador). */
fun readingForFrequency(freq: Double, refA: Double = 440.0): NoteReading {
    val midiExact = 69.0 + 12.0 * ln(freq / refA) / ln(2.0)
    val nearest = midiExact.roundToInt()
    val cents = (midiExact - nearest) * 100.0
    return NoteReading(nearest, midiToName(nearest), cents, freq)
}
