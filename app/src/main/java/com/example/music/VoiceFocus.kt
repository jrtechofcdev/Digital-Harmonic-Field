package com.example.music

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * "Foco na voz": isola o que é canto do resto do som do ambiente, em tempo real.
 *
 * Não é separação de fontes por rede neural (como apps de estúdio); é processamento
 * de sinal leve, que roda instantaneamente no celular:
 *  1. [VoiceBandFilter] mantém só a faixa da voz cantada (120–1500 Hz) — corta
 *     graves de bateria/baixo, ronco de som e chiado agudo. O baixo não é jogado
 *     fora: vai para um canal próprio ([BassBandFilter]), analisado à parte.
 *  2. [VoiceFocus] estima o ruído de fundo de cada frequência (mínimo móvel) e o
 *     subtrai — tira ventilador, ar-condicionado, burburinho constante.
 *  3. Depois, o detector de altura só aceita trechos com nota clara e sustentada
 *     (ver KeyDetection), o que descarta conversa, tosse e palmas.
 */

/** Filtro biquad (fórmulas RBJ), processado amostra a amostra. */
class Biquad private constructor(
    private val b0: Double, private val b1: Double, private val b2: Double,
    private val a1: Double, private val a2: Double,
) {
    private var x1 = 0.0
    private var x2 = 0.0
    private var y1 = 0.0
    private var y2 = 0.0

    fun process(x: Double): Double {
        val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1; x1 = x
        y2 = y1; y1 = y
        return y
    }

    companion object {
        fun highPass(fc: Double, fs: Double, q: Double = 0.7071): Biquad {
            val w = 2.0 * PI * fc / fs
            val c = cos(w)
            val alpha = sin(w) / (2.0 * q)
            val a0 = 1.0 + alpha
            return Biquad(
                (1 + c) / 2 / a0, -(1 + c) / a0, (1 + c) / 2 / a0,
                -2 * c / a0, (1 - alpha) / a0,
            )
        }

        fun lowPass(fc: Double, fs: Double, q: Double = 0.7071): Biquad {
            val w = 2.0 * PI * fc / fs
            val c = cos(w)
            val alpha = sin(w) / (2.0 * q)
            val a0 = 1.0 + alpha
            return Biquad(
                (1 - c) / 2 / a0, (1 - c) / a0, (1 - c) / 2 / a0,
                -2 * c / a0, (1 - alpha) / a0,
            )
        }

        /** Rejeita-faixa estreito: apaga só [f0] (±f0/q), sem tocar no resto. */
        fun notch(f0: Double, fs: Double, q: Double = 15.0): Biquad {
            val w = 2.0 * PI * f0 / fs
            val c = cos(w)
            val alpha = sin(w) / (2.0 * q)
            val a0 = 1.0 + alpha
            return Biquad(
                1 / a0, -2 * c / a0, 1 / a0,
                -2 * c / a0, (1 - alpha) / a0,
            )
        }
    }
}

/**
 * Mantém só a faixa da voz (passa-altas 120 Hz + passa-baixas 1500 Hz, ambos de
 * 4ª ordem) e reduz a taxa pela metade (44 100 → 22 050 Hz). O corte em 120 Hz
 * tira o baixo do caminho da voz (senão o detector "canta" as notas do baixo),
 * mas ainda deixa passar a voz masculina grave pelos harmônicos.
 */
class VoiceBandFilter(inputRate: Int = 44100) {
    private val fs = inputRate.toDouble()
    private val hp1 = Biquad.highPass(120.0, fs)
    private val hp2 = Biquad.highPass(120.0, fs)
    private val lp1 = Biquad.lowPass(1500.0, fs)
    private val lp2 = Biquad.lowPass(1500.0, fs)
    private var keep = true

    /** Filtra [count] amostras e escreve as decimadas em [out]. Retorna quantas escreveu. */
    fun process(input: DoubleArray, count: Int, out: DoubleArray): Int {
        var n = 0
        for (i in 0 until count) {
            val y = lp2.process(lp1.process(hp2.process(hp1.process(input[i]))))
            if (keep) out[n++] = y
            keep = !keep
        }
        return n
    }
}

/**
 * Canal do baixo: guarda só 35–160 Hz (baixo elétrico, mão esquerda do teclado)
 * e reduz a taxa 8× (44 100 → 5 512 Hz). Depois apaga o zumbido da rede
 * elétrica (60, 120 e 180 Hz), que senão seria lido como uma "nota" grave fixa.
 */
class BassBandFilter(inputRate: Int = 44100) {
    companion object {
        const val DECIMATION = 8
        private val HUM = doubleArrayOf(60.0, 120.0, 180.0)
    }

    private val fs = inputRate.toDouble()
    private val hp = Biquad.highPass(35.0, fs)
    private val lp1 = Biquad.lowPass(160.0, fs)
    private val lp2 = Biquad.lowPass(160.0, fs)
    private val notches = HUM.map { Biquad.notch(it, fs / DECIMATION) }
    private var phase = 0

    /** Filtra [count] amostras e escreve as decimadas em [out]. Retorna quantas escreveu. */
    fun process(input: DoubleArray, count: Int, out: DoubleArray): Int {
        var n = 0
        for (i in 0 until count) {
            var y = lp2.process(lp1.process(hp.process(input[i])))
            if (phase == 0) {
                for (notch in notches) y = notch.process(y)
                out[n++] = y
            }
            phase = (phase + 1) % DECIMATION
        }
        return n
    }
}

/**
 * Subtração espectral com estimativa de ruído por mínimo móvel (~1,5 s).
 * O piso proporcional (BETA) garante que, no pior caso, o quadro só é atenuado
 * por inteiro — a periodicidade da voz nunca é destruída.
 */
class VoiceFocus(private val n: Int = 2048, sampleRate: Int = 22050, history: Int = 32) {

    private companion object {
        const val ALPHA = 1.3   // sobre-subtração do ruído estimado
        const val BETA = 0.08   // piso: nunca menos que 8% do original
    }

    private val bins = n / 2 + 1
    private val window = tukey(n, 0.25)
    private val hist = Array(history) { DoubleArray(bins) }
    private var filled = 0
    private var pos = 0
    private var started = false
    private val smooth = DoubleArray(bins)
    private val mags = DoubleArray(bins)
    private val re = DoubleArray(n)
    private val im = DoubleArray(n)
    private val cleaned = DoubleArray(n)
    private val pcOfBin = IntArray(bins) { k ->
        val f = k.toDouble() * sampleRate / n
        if (k > 0 && f in 90.0..1500.0) pitchClassOfFrequency(f) else -1
    }

    /** Perfil harmônico (12 classes, soma 1) do último quadro limpo. */
    val chroma = FloatArray(12)

    /** Limpa o quadro e devolve o sinal limpo (buffer reutilizado — copie se precisar guardar). */
    fun process(frame: DoubleArray): DoubleArray {
        for (i in 0 until n) {
            re[i] = frame[i] * window[i]
            im[i] = 0.0
        }
        fft(re, im)

        val slot = hist[pos]
        for (k in 0 until bins) {
            val mag = sqrt(re[k] * re[k] + im[k] * im[k])
            mags[k] = mag
            smooth[k] = if (started) 0.7 * smooth[k] + 0.3 * mag else mag
            slot[k] = smooth[k]
        }
        started = true
        pos = (pos + 1) % hist.size
        filled = min(filled + 1, hist.size)

        chroma.fill(0f)
        var chromaSum = 0f
        for (k in 0 until bins) {
            var noise = Double.MAX_VALUE
            for (h in 0 until filled) if (hist[h][k] < noise) noise = hist[h][k]
            val mag = mags[k]
            val clean = max(mag - ALPHA * noise, BETA * mag)
            val gain = if (mag > 0.0) clean / mag else 0.0
            re[k] *= gain; im[k] *= gain
            if (k in 1 until n / 2) { re[n - k] *= gain; im[n - k] *= gain }
            val pc = pcOfBin[k]
            if (pc >= 0) { chroma[pc] += clean.toFloat(); chromaSum += clean.toFloat() }
        }
        if (chromaSum > 0f) for (i in 0 until 12) chroma[i] /= chromaSum

        // FFT inversa: conjuga, transforma, conjuga e divide por n.
        for (i in 0 until n) im[i] = -im[i]
        fft(re, im)
        for (i in 0 until n) cleaned[i] = re[i] / n
        return cleaned
    }

    /** Janela de Tukey: plana no meio, borda suave — preserva a periodicidade da voz. */
    private fun tukey(size: Int, alpha: Double): DoubleArray {
        val w = DoubleArray(size) { 1.0 }
        val m = (alpha * (size - 1) / 2).toInt()
        for (i in 0..m) {
            val v = if (m > 0) 0.5 * (1 - cos(PI * i / m)) else 1.0
            w[i] = v
            w[size - 1 - i] = v
        }
        return w
    }
}
