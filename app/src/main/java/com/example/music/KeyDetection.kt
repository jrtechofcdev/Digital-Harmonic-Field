package com.example.music

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Detector de tom pela voz. Em poucos segundos de canto, devolve até 3 tons
 * candidatos — sempre tons reais (dos 24) e sempre com base nas notas
 * efetivamente ouvidas. Sem evidência suficiente, NÃO responde
 * (status SEM_VOZ/INSUFICIENTE).
 *
 * Pipeline (todo em tempo real, quadro a quadro), em dois canais:
 *   voz:   microfone → [VoiceBandFilter] → [VoiceFocus] → detectPitch (NSDF)
 *   baixo: microfone → [BassBandFilter]  → [VoiceFocus] → detectPitch (NSDF)
 *   → notas sustentadas → afinação do grupo → perfis de tonalidade → ranking.
 * A melodia decide; o baixo (quando há banda) ajuda a separar tons parecidos,
 * como a relativa menor, porque costuma tocar a fundamental de cada acorde.
 */

enum class KeyStatus { SEM_VOZ, INSUFICIENTE, BAIXA, MEDIA, ALTA }

data class KeyCandidate(val root: Int, val isMinor: Boolean, val probability: Double) {
    /** Cifra no padrão do app (ex.: "G", "Em", "C#m"). */
    val keyCipher: String get() = pitchClassCiphers[root] + if (isMinor) "m" else ""

    /** Relativa = mesmas notas e mesmos acordes (ex.: Sol Maior ↔ Mi menor). */
    fun isRelativeOf(other: KeyCandidate): Boolean =
        isMinor != other.isMinor &&
            if (isMinor) (root + 3) % 12 == other.root else (other.root + 3) % 12 == root
}

class KeyResult(
    val status: KeyStatus,
    /** Até 3 tons, do mais para o menos provável. Vazio quando não há base. */
    val candidates: List<KeyCandidate>,
    /** Segundos de canto sustentado que de fato entraram na análise. */
    val voicedSeconds: Double,
    /** Quanto o grupo está acima (+) ou abaixo (−) do padrão Lá 440, em cents. */
    val tuningOffsetCents: Double,
    /** O grupo canta quase meio semitom fora: os dois tons vizinhos são possíveis. */
    val betweenKeys: Boolean,
    /** Peso de cada nota (0 = Dó … 11 = Si) no canto analisado, soma 1. */
    val noteWeights: FloatArray,
    /** Última nota sustentada (0..11), costuma apontar a tônica no fim da frase. */
    val lastNote: Int?,
    /** Segundos de linha de baixo (baixo/teclado) que entraram na análise. */
    val bassSeconds: Double = 0.0,
) {
    val hasAnswer: Boolean get() = candidates.isNotEmpty()
}

// Perfis de Aarden (2003), derivados de milhares de melodias folclóricas — mais
// adequados para MELODIA cantada do que os perfis de Krumhansl (feitos com acordes).
private val AARDEN_MAJOR = doubleArrayOf(
    17.7661, 0.145624, 14.9265, 0.160186, 19.8049, 11.3587,
    0.291248, 22.062, 0.145624, 8.15494, 0.232998, 4.95122,
)
private val AARDEN_MINOR = doubleArrayOf(
    18.2648, 0.737619, 14.0499, 16.8599, 0.702494, 14.4362,
    0.702494, 18.6161, 4.56621, 1.93186, 7.37619, 1.75623,
)
// Onde o baixo costuma estar em cada tom: na fundamental dos acordes principais
// (I, IV, V e, no menor, também VI). Usado só como apoio à melodia.
private val BASS_MAJOR = doubleArrayOf(1.0, .02, .25, .02, .15, .5, .02, .55, .02, .3, .02, .05)
private val BASS_MINOR = doubleArrayOf(1.0, .02, .1, .35, .02, .45, .02, .5, .4, .02, .35, .05)
private val MAJOR_SCALE = intArrayOf(0, 2, 4, 5, 7, 9, 11)
private val MINOR_SCALE = intArrayOf(0, 2, 3, 5, 7, 8, 10, 11) // natural + sensível

/** Acumula as notas sustentadas da voz (e do baixo) e calcula os tons candidatos. */
class KeyEvidence(
    private val hopSeconds: Double,
    private val bassHopSeconds: Double = hopSeconds,
) {

    private companion object {
        const val MELODY_WEIGHT = 0.7      // notas cantadas (principal)
        const val HARMONY_WEIGHT = 0.3     // perfil harmônico limpo (apoio)
        const val TEMPERATURE = 0.05       // nitidez da distribuição de chances
        const val FINAL_NOTE_BONUS = 0.06  // frases de hino costumam terminar na tônica
        const val LONGEST_NOTE_BONUS = 0.03
        const val BETWEEN_KEYS_SEMITONES = 0.40

        const val MAJOR_PRIOR = 0.02       // desempate: quase todo hino da Harpa é maior
        const val BASS_WEIGHT = 0.3        // peso máximo do baixo (a melodia manda)
        const val BASS_FULL_SECONDS = 3.0  // com 3 s de baixo, ele atinge o peso máximo
        const val BASS_MIN_SECONDS = 0.5   // menos que isso: baixo ignorado
        const val BASS_MIN_FRAMES = 4      // nota de baixo: ≥4 quadros (~370 ms)
        // O "baixo" só conta se tiver energia de instrumento: ≥20% da voz.
        // Vozes graves vazando para o canal ficam bem abaixo disso (~5%).
        const val BASS_ENERGY_RATIO = 0.2

        const val ALTA_MIN_NOTES = 4
        const val ALTA_MIN_VOICED = 1.5
    }

    private val frames = ArrayList<Int>()
    private val midis = ArrayList<Double>()
    private val weights = ArrayList<Double>()
    private val harmony = DoubleArray(12)
    private var voiceEnergy = 0.0

    private val bassFrames = ArrayList<Int>()
    private val bassMidis = ArrayList<Double>()
    private val bassWeights = ArrayList<Double>()
    private val bassEnergies = ArrayList<Double>()

    /** [energy] = RMS do quadro limpo (para comparar com o baixo). */
    fun addVoicedFrame(frame: Int, midi: Double, clarity: Double, chroma: FloatArray, energy: Double = 0.0) {
        frames.add(frame); midis.add(midi); weights.add(clarity)
        voiceEnergy += energy
        for (i in 0 until 12) harmony[i] += chroma[i]
    }

    fun addBassFrame(frame: Int, midi: Double, clarity: Double, energy: Double) {
        bassFrames.add(frame); bassMidis.add(midi); bassWeights.add(clarity); bassEnergies.add(energy)
    }

    private class Segment(val start: Int, val end: Int, val midi: Double, val weight: Double) {
        val frames: Int get() = end - start + 1
    }

    /**
     * Agrupa quadros contínuos na mesma altura (±0,5 semitom) em NOTAS SUSTENTADAS.
     * Fala e burburinho deslizam o tom e não formam notas de ≥3 quadros (~140 ms);
     * o canto segura a nota. É aqui que o ruído "falado" é descartado.
     */
    private fun segments(
        frames: List<Int>,
        midis: List<Double>,
        weights: List<Double>,
        minFrames: Int = 3,
    ): List<Segment> {
        val out = ArrayList<Segment>()
        var curFrames = ArrayList<Int>()
        var curMidis = ArrayList<Double>()
        var curW = 0.0
        fun flush() {
            if (curMidis.size >= minFrames && (curMidis.max() - curMidis.min()) <= 0.8) {
                out.add(Segment(curFrames.first(), curFrames.last(), median(curMidis), curW))
            }
            curFrames = ArrayList(); curMidis = ArrayList(); curW = 0.0
        }
        for (i in frames.indices) {
            if (weights[i] <= 0.0) { flush(); continue }
            val contiguous = curFrames.isNotEmpty() && frames[i] - curFrames.last() <= 1
            if (contiguous && abs(midis[i] - median(curMidis)) < 0.5) {
                curFrames.add(frames[i]); curMidis.add(midis[i]); curW += weights[i]
            } else {
                flush()
                curFrames.add(frames[i]); curMidis.add(midis[i]); curW = weights[i]
            }
        }
        flush()
        return out
    }

    private class Scored(
        val root: Int, val minor: Boolean, val score: Double,
        val fit: Double, val hist: DoubleArray, val lastNote: Int,
    )

    /** Perfil das notas do baixo (soma 1), já compensado pela afinação; null se pouco baixo. */
    private class BassLine(val hist: DoubleArray, val seconds: Double)

    private fun bassLine(): BassLine? {
        if (frames.isEmpty() || bassFrames.isEmpty()) return null
        val minEnergy = BASS_ENERGY_RATIO * voiceEnergy / frames.size
        val w = List(bassWeights.size) { if (bassEnergies[it] >= minEnergy) bassWeights[it] else 0.0 }
        val segs = segments(bassFrames, bassMidis, w, BASS_MIN_FRAMES)
        val seconds = segs.sumOf { it.frames } * bassHopSeconds
        if (segs.isEmpty() || seconds < BASS_MIN_SECONDS) return null
        val offset = tuningOffset(segs)
        val hist = DoubleArray(12)
        for (seg in segs) hist[Math.floorMod((seg.midi - offset).roundToInt(), 12)] += seg.weight
        val total = hist.sum()
        for (i in 0 until 12) hist[i] /= total
        return BassLine(hist, seconds)
    }

    private fun scoreKeys(segs: List<Segment>, offset: Double, bass: BassLine?): List<Scored> {
        val pcs = IntArray(segs.size) { Math.floorMod((segs[it].midi - offset).roundToInt(), 12) }
        val hist = DoubleArray(12)
        for (i in segs.indices) hist[pcs[i]] += segs[i].weight
        val total = hist.sum()
        for (i in 0 until 12) hist[i] /= total

        val harmonyTotal = harmony.sum()
        val profile = DoubleArray(12) {
            if (harmonyTotal > 0) MELODY_WEIGHT * hist[it] + HARMONY_WEIGHT * harmony[it] / harmonyTotal
            else hist[it]
        }
        val lastNote = pcs.last()
        val longestNote = pcs[segs.indices.maxBy { segs[it].frames }]

        val out = ArrayList<Scored>(24)
        for (root in 0 until 12) for (minor in booleanArrayOf(false, true)) {
            val ref = if (minor) AARDEN_MINOR else AARDEN_MAJOR
            val rotated = DoubleArray(12) { ref[Math.floorMod(it - root, 12)] }
            var s = pearson(profile, rotated)
            if (lastNote == root) s += FINAL_NOTE_BONUS
            if (longestNote == root) s += LONGEST_NOTE_BONUS
            if (!minor) s += MAJOR_PRIOR
            if (bass != null) {
                val bassRef = if (minor) BASS_MINOR else BASS_MAJOR
                val bassRotated = DoubleArray(12) { bassRef[Math.floorMod(it - root, 12)] }
                s += BASS_WEIGHT * min(1.0, bass.seconds / BASS_FULL_SECONDS) * pearson(bass.hist, bassRotated)
            }
            val scale = if (minor) MINOR_SCALE else MAJOR_SCALE
            val fit = scale.sumOf { hist[(root + it) % 12] }
            out.add(Scored(root, minor, s, fit, hist, lastNote))
        }
        return out
    }

    /**
     * Afinação do grupo: média circular da parte fracionária das notas, em
     * semitons (−0,5..0,5). Uma congregação 30 cents "acima" continua sendo lida
     * no tom certo.
     */
    private fun tuningOffset(segs: List<Segment>): Double {
        var sx = 0.0
        var sy = 0.0
        for (s in segs) {
            val ang = 2 * PI * (s.midi - s.midi.roundToInt())
            sx += s.weight * cos(ang); sy += s.weight * sin(ang)
        }
        return atan2(sy, sx) / (2 * PI)
    }

    fun result(): KeyResult {
        val segs = segments(frames, midis, weights)
        if (segs.isEmpty()) {
            return KeyResult(KeyStatus.SEM_VOZ, emptyList(), 0.0, 0.0, false, FloatArray(12), null)
        }

        val offset = tuningOffset(segs)
        val between = abs(offset) > BETWEEN_KEYS_SEMITONES
        val bass = bassLine()

        var scored = scoreKeys(segs, offset, bass)
        if (between) scored = scored + scoreKeys(segs, offset - sign(offset), bass)
        // Mesmo tom vindo das duas leituras: fica a melhor.
        val best = scored.groupBy { it.root * 2 + if (it.minor) 1 else 0 }
            .map { (_, v) -> v.maxBy { it.score } }
            .sortedByDescending { it.score }

        val maxScore = best.first().score
        val expScores = best.map { exp((it.score - maxScore) / TEMPERATURE) }
        val sum = expScores.sum()
        val probs = expScores.map { it / sum }

        val top = best.first()
        val voiced = segs.sumOf { it.frames } * hopSeconds
        val distinct = top.hist.count { it > 0.05 }
        val status = when {
            voiced < 1.0 || distinct < 3 || top.fit < 0.80 -> KeyStatus.INSUFICIENTE
            probs[0] >= 0.70 && top.fit >= 0.88 && voiced >= ALTA_MIN_VOICED &&
                distinct >= ALTA_MIN_NOTES && !between -> KeyStatus.ALTA
            probs[0] >= 0.45 -> KeyStatus.MEDIA
            else -> KeyStatus.BAIXA
        }
        val candidates = if (status == KeyStatus.INSUFICIENTE) emptyList() else
            best.take(3).mapIndexed { i, s -> KeyCandidate(s.root, s.minor, probs[i]) }

        return KeyResult(
            status = status,
            candidates = candidates,
            voicedSeconds = voiced,
            tuningOffsetCents = offset * 100,
            betweenKeys = between,
            noteWeights = FloatArray(12) { top.hist[it].toFloat() },
            lastNote = top.lastNote,
            bassSeconds = bass?.seconds ?: 0.0,
        )
    }

    private fun median(v: List<Double>): Double {
        val s = v.sorted()
        return if (s.size % 2 == 1) s[s.size / 2] else (s[s.size / 2 - 1] + s[s.size / 2]) / 2
    }

    private fun pearson(a: DoubleArray, b: DoubleArray): Double {
        val ma = a.average()
        val mb = b.average()
        var num = 0.0
        var da = 0.0
        var db = 0.0
        for (i in a.indices) {
            val xa = a[i] - ma
            val xb = b[i] - mb
            num += xa * xb; da += xa * xa; db += xb * xb
        }
        val den = sqrt(da * db)
        return if (den == 0.0) 0.0 else num / den
    }
}

/**
 * Detector completo e em streaming: recebe o áudio cru do microfone (44,1 kHz) em
 * blocos e analisa cada novo trecho assim que ele chega (voz a cada ~46 ms, baixo
 * a cada ~93 ms). Puro (sem Android) — a mesma classe roda no app e nos testes.
 */
class KeyDetector(inputRate: Int = 44100) {

    companion object {
        const val RATE = 22050
        const val FRAME = 2048  // ~93 ms
        const val HOP = 1024    // ~46 ms entre análises
        private const val MIN_RMS = 0.002
        private const val MIN_CLARITY = 0.70

        private const val BASS_FRAME = 2048  // ~370 ms (notas graves pedem janela longa)
        private const val BASS_HOP = 512     // ~93 ms
        private const val BASS_MIN_CLARITY = 0.60
        // Depois de tirar o ruído constante, ainda precisa sobrar ≥30% do sinal:
        // é o que separa uma nota tocada de um ronco/zumbido parado.
        private const val BASS_MIN_CLEAN = 0.3
    }

    private val bassRate = inputRate / BassBandFilter.DECIMATION

    private val band = VoiceBandFilter(inputRate)
    private val focus = VoiceFocus(FRAME, RATE)
    private val evidence = KeyEvidence(HOP.toDouble() / RATE, BASS_HOP.toDouble() / bassRate)
    private val window = DoubleArray(FRAME)
    private val pending = DoubleArray(HOP)
    private var pendingCount = 0
    private var decimated = DoubleArray(0)
    private var frame = 0

    private val bassBand = BassBandFilter(inputRate)
    private val bassFocus = VoiceFocus(BASS_FRAME, bassRate)
    private val bassWindow = DoubleArray(BASS_FRAME)
    private val bassPending = DoubleArray(BASS_HOP)
    private var bassPendingCount = 0
    private var bassDecimated = DoubleArray(0)
    private var bassFrame = 0

    /** Volume na faixa da voz (0..1), para o medidor da tela. */
    var level = 0f
        private set

    /** O último trecho analisado tinha uma nota cantada clara. */
    var hearingVoice = false
        private set

    /** Quantos segundos de áudio já entraram no detector. */
    var secondsFed = 0.0
        private set
    private val inputRateD = inputRate.toDouble()

    fun feed(samples: DoubleArray, count: Int) {
        secondsFed += count / inputRateD

        if (decimated.size < count / 2 + 1) decimated = DoubleArray(count / 2 + 1)
        val n = band.process(samples, count, decimated)
        for (i in 0 until n) {
            pending[pendingCount++] = decimated[i]
            if (pendingCount == HOP) {
                analyzeHop()
                pendingCount = 0
            }
        }

        val bassMax = count / BassBandFilter.DECIMATION + 1
        if (bassDecimated.size < bassMax) bassDecimated = DoubleArray(bassMax)
        val nb = bassBand.process(samples, count, bassDecimated)
        for (i in 0 until nb) {
            bassPending[bassPendingCount++] = bassDecimated[i]
            if (bassPendingCount == BASS_HOP) {
                analyzeBassHop()
                bassPendingCount = 0
            }
        }
    }

    private fun analyzeHop() {
        System.arraycopy(window, HOP, window, 0, FRAME - HOP)
        System.arraycopy(pending, 0, window, FRAME - HOP, HOP)
        frame++

        val rms = rms(window)
        level = (rms * 8).coerceIn(0.0, 1.0).toFloat()

        val clean = focus.process(window)
        if (rms < MIN_RMS || frame < 2) { hearingVoice = false; return }

        val pitch = detectPitch(clean, RATE, minFreq = 100.0, maxFreq = 1000.0, minClarity = MIN_CLARITY)
        if (pitch == null) { hearingVoice = false; return }

        val midi = 69.0 + 12.0 * ln(pitch.frequency / 440.0) / ln(2.0)
        evidence.addVoicedFrame(frame, midi, pitch.clarity, focus.chroma, rms(clean))
        hearingVoice = true
    }

    private fun analyzeBassHop() {
        System.arraycopy(bassWindow, BASS_HOP, bassWindow, 0, BASS_FRAME - BASS_HOP)
        System.arraycopy(bassPending, 0, bassWindow, BASS_FRAME - BASS_HOP, BASS_HOP)
        bassFrame++

        val rms = rms(bassWindow)
        val clean = bassFocus.process(bassWindow)
        val cleanRms = rms(clean)
        if (rms < MIN_RMS || bassFrame < BASS_FRAME / BASS_HOP || cleanRms < BASS_MIN_CLEAN * rms) return

        val pitch = detectPitch(clean, bassRate, minFreq = 38.0, maxFreq = 170.0, minClarity = BASS_MIN_CLARITY)
            ?: return
        val midi = 69.0 + 12.0 * ln(pitch.frequency / 440.0) / ln(2.0)
        evidence.addBassFrame(bassFrame, midi, pitch.clarity, cleanRms)
    }

    private fun rms(x: DoubleArray): Double {
        var sumSq = 0.0
        for (v in x) sumSq += v * v
        return sqrt(sumSq / x.size)
    }

    fun result(): KeyResult = evidence.result()
}

/**
 * Quando parar de ouvir. Responde assim que der — sem chutar:
 *  - confiança ALTA no mesmo tom em 2 leituras seguidas (~0,3 s) → para já;
 *    com 5 s ou mais de áudio, uma leitura ALTA basta;
 *  - a partir de 5 s, confiança MÉDIA também encerra;
 *  - caso difícil (pouco canto, tons empatados) → continua até [maxSeconds]
 *    em vez de adivinhar, e aí mostra o que tiver (ou nenhum tom).
 */
class KeyStopRule(
    private val answerSeconds: Double = 5.0,
    val maxSeconds: Double = 8.0,
) {
    private var lastKey = -1
    private var streak = 0

    /** Registra uma leitura; devolve true quando já dá para encerrar. */
    fun shouldStop(result: KeyResult, seconds: Double): Boolean {
        val top = result.candidates.firstOrNull()
        if (result.status == KeyStatus.ALTA && top != null) {
            val key = top.root * 2 + if (top.isMinor) 1 else 0
            streak = if (key == lastKey) streak + 1 else 1
            lastKey = key
        } else {
            streak = 0
            lastKey = -1
        }
        return streak >= 2 ||
            (streak >= 1 && seconds >= answerSeconds) ||
            (result.status == KeyStatus.MEDIA && seconds >= answerSeconds) ||
            seconds >= maxSeconds
    }
}
