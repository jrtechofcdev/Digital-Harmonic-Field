package com.example.music

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Detector de tom pela voz. Em ~5 s de canto, devolve até 3 tons candidatos —
 * sempre tons reais (dos 24) e sempre com base nas notas efetivamente cantadas.
 * Quando não há evidência suficiente, NÃO responde (status SEM_VOZ/INSUFICIENTE).
 *
 * Pipeline (todo em tempo real, quadro a quadro):
 *   microfone → [VoiceBandFilter] → [VoiceFocus] → detectPitch (NSDF)
 *   → notas sustentadas → afinação do grupo → perfis de tonalidade → ranking.
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
private val MAJOR_SCALE = intArrayOf(0, 2, 4, 5, 7, 9, 11)
private val MINOR_SCALE = intArrayOf(0, 2, 3, 5, 7, 8, 10, 11) // natural + sensível

/** Acumula as notas sustentadas da voz e calcula os tons candidatos. */
class KeyEvidence(private val hopSeconds: Double) {

    private companion object {
        const val MELODY_WEIGHT = 0.7      // notas cantadas (principal)
        const val HARMONY_WEIGHT = 0.3     // perfil harmônico limpo (apoio)
        const val TEMPERATURE = 0.05       // nitidez da distribuição de chances
        const val FINAL_NOTE_BONUS = 0.06  // frases de hino costumam terminar na tônica
        const val LONGEST_NOTE_BONUS = 0.03
        const val BETWEEN_KEYS_SEMITONES = 0.40
    }

    private val frames = ArrayList<Int>()
    private val midis = ArrayList<Double>()
    private val weights = ArrayList<Double>()
    private val harmony = DoubleArray(12)

    fun addVoicedFrame(frame: Int, midi: Double, clarity: Double, chroma: FloatArray) {
        frames.add(frame); midis.add(midi); weights.add(clarity)
        for (i in 0 until 12) harmony[i] += chroma[i]
    }

    private class Segment(val start: Int, val end: Int, val midi: Double, val weight: Double) {
        val frames: Int get() = end - start + 1
    }

    /**
     * Agrupa quadros contínuos na mesma altura (±0,5 semitom) em NOTAS SUSTENTADAS.
     * Fala e burburinho deslizam o tom e não formam notas de ≥3 quadros (~140 ms);
     * o canto segura a nota. É aqui que o ruído "falado" é descartado.
     */
    private fun segments(): List<Segment> {
        val out = ArrayList<Segment>()
        var curFrames = ArrayList<Int>()
        var curMidis = ArrayList<Double>()
        var curW = 0.0
        fun flush() {
            if (curMidis.size >= 3 && (curMidis.max() - curMidis.min()) <= 0.8) {
                out.add(Segment(curFrames.first(), curFrames.last(), median(curMidis), curW))
            }
            curFrames = ArrayList(); curMidis = ArrayList(); curW = 0.0
        }
        for (i in frames.indices) {
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

    private fun scoreKeys(segs: List<Segment>, offset: Double): List<Scored> {
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
            val scale = if (minor) MINOR_SCALE else MAJOR_SCALE
            val fit = scale.sumOf { hist[(root + it) % 12] }
            out.add(Scored(root, minor, s, fit, hist, lastNote))
        }
        return out
    }

    fun result(): KeyResult {
        val segs = segments()
        if (segs.isEmpty()) {
            return KeyResult(KeyStatus.SEM_VOZ, emptyList(), 0.0, 0.0, false, FloatArray(12), null)
        }

        // Afinação do grupo: média circular da parte fracionária das notas.
        // Uma congregação 30 cents "acima" continua sendo lida no tom certo.
        var sx = 0.0
        var sy = 0.0
        for (s in segs) {
            val ang = 2 * PI * (s.midi - s.midi.roundToInt())
            sx += s.weight * cos(ang); sy += s.weight * sin(ang)
        }
        val offset = atan2(sy, sx) / (2 * PI) // em semitons, −0,5..0,5
        val between = abs(offset) > BETWEEN_KEYS_SEMITONES

        var scored = scoreKeys(segs, offset)
        if (between) scored = scored + scoreKeys(segs, offset - sign(offset))
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
            probs[0] >= 0.70 && top.fit >= 0.88 && voiced >= 1.5 && !between -> KeyStatus.ALTA
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
 * blocos e analisa cada novo trecho de ~46 ms assim que ele chega. Puro (sem
 * Android) — a mesma classe roda no app e nos testes.
 */
class KeyDetector(inputRate: Int = 44100) {

    companion object {
        const val RATE = 22050
        const val FRAME = 2048  // ~93 ms
        const val HOP = 1024    // ~46 ms entre análises
        private const val MIN_RMS = 0.002
        private const val MIN_CLARITY = 0.70
    }

    private val band = VoiceBandFilter(inputRate)
    private val focus = VoiceFocus(FRAME, RATE)
    private val evidence = KeyEvidence(HOP.toDouble() / RATE)
    private val window = DoubleArray(FRAME)
    private val pending = DoubleArray(HOP)
    private var pendingCount = 0
    private var decimated = DoubleArray(0)
    private var frame = 0

    /** Volume na faixa da voz (0..1), para o medidor da tela. */
    var level = 0f
        private set

    /** O último trecho analisado tinha uma nota cantada clara. */
    var hearingVoice = false
        private set

    fun feed(samples: DoubleArray, count: Int) {
        if (decimated.size < count / 2 + 1) decimated = DoubleArray(count / 2 + 1)
        val n = band.process(samples, count, decimated)
        for (i in 0 until n) {
            pending[pendingCount++] = decimated[i]
            if (pendingCount == HOP) {
                analyzeHop()
                pendingCount = 0
            }
        }
    }

    private fun analyzeHop() {
        System.arraycopy(window, HOP, window, 0, FRAME - HOP)
        System.arraycopy(pending, 0, window, FRAME - HOP, HOP)
        frame++

        var sumSq = 0.0
        for (v in window) sumSq += v * v
        val rms = sqrt(sumSq / FRAME)
        level = (rms * 8).coerceIn(0.0, 1.0).toFloat()

        val clean = focus.process(window)
        if (rms < MIN_RMS || frame < 2) { hearingVoice = false; return }

        val pitch = detectPitch(clean, RATE, minFreq = 80.0, maxFreq = 1000.0, minClarity = MIN_CLARITY)
        if (pitch == null) { hearingVoice = false; return }

        val midi = 69.0 + 12.0 * ln(pitch.frequency / 440.0) / ln(2.0)
        evidence.addVoicedFrame(frame, midi, pitch.clarity, focus.chroma)
        hearingVoice = true
    }

    fun result(): KeyResult = evidence.result()
}
