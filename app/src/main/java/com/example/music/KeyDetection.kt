package com.example.music

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.roundToInt
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

private val MAJOR_SCALE = intArrayOf(0, 2, 4, 5, 7, 9, 11)
private val MINOR_SCALE = intArrayOf(0, 2, 3, 5, 7, 8, 10, 11) // natural + sensível

/** Uma nota sustentada (voz ou baixo), com início/fim em segundos. */
class NoteSegment(val startSec: Double, val endSec: Double, val midi: Double, val weight: Double)

/** Tudo o que foi ouvido até agora, em forma de notas — a entrada do modelo de tom. */
class EvidenceSnapshot(
    val voice: List<NoteSegment>,
    val bass: List<NoteSegment>,
    /** Perfil harmônico (12 classes) somado nos quadros com som. */
    val harmony: DoubleArray,
    val seconds: Double,
    /** Perfil espectral do grave (40–250 Hz): o que baixo/teclado sustentam. */
    val bassChroma: DoubleArray = DoubleArray(12),
    /** Grave com energia de instrumento (não só voz grave vazando)? */
    val bassPresent: Boolean = false,
)

/** Acumula as notas sustentadas da voz (e do baixo) e calcula os tons candidatos. */
class KeyEvidence(
    private val hopSeconds: Double,
    private val bassHopSeconds: Double = hopSeconds,
) {

    private companion object {
        const val BETWEEN_KEYS_SEMITONES = 0.40
        const val BASS_MIN_FRAMES = 4      // nota de baixo: ≥4 quadros (~370 ms)
        // O "baixo" só conta se tiver energia de instrumento: ≥20% da voz.
        // Vozes graves vazando para o canal ficam bem abaixo disso (~5%).
        const val BASS_ENERGY_RATIO = 0.2

        // Limiares sobre a chance calibrada do modelo (ver docs/detector-de-tom.md).
        // ALTA só a partir da 2ª rodada: com ≥10 s e chance ≥70%, acertou 93–95%
        // na bancada de hinos (95–98% nos hinos simples). Com 5 s a chance é menos
        // confiável, então a 1ª rodada nunca crava o tom.
        const val P_ALTA = 0.70
        const val ALTA_MIN_SECONDS = 9.5
        const val P_MEDIA = 0.50
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

    private val harmonyAll = DoubleArray(12)
    private val bassChroma = DoubleArray(12)
    private var bassChromaEnergy = 0.0
    private var bassChromaFrames = 0

    /** Perfil espectral de um quadro do canal grave (já sem ruído constante). */
    fun addBassChroma(chroma: FloatArray, energy: Double) {
        for (i in 0 until 12) bassChroma[i] += chroma[i] * energy
        bassChromaEnergy += energy
        bassChromaFrames++
    }

    /** Perfil harmônico de um quadro com som (com ou sem voz detectada). */
    fun addHarmonyFrame(chroma: FloatArray, energy: Double) {
        for (i in 0 until 12) harmonyAll[i] += chroma[i] * energy
    }

    /** Notas da voz e do baixo (já com o filtro de energia do baixo) até agora. */
    fun snapshot(seconds: Double): EvidenceSnapshot {
        val voice = segments(frames, midis, weights).map {
            NoteSegment((it.start - 1) * hopSeconds, it.end * hopSeconds, it.midi, it.weight)
        }
        val minEnergy = if (frames.isEmpty()) Double.MAX_VALUE else BASS_ENERGY_RATIO * voiceEnergy / frames.size
        val w = List(bassWeights.size) { if (bassEnergies[it] >= minEnergy) bassWeights[it] else 0.0 }
        val bass = segments(bassFrames, bassMidis, w, BASS_MIN_FRAMES).map {
            NoteSegment((it.start - 1) * bassHopSeconds, it.end * bassHopSeconds, it.midi, it.weight)
        }
        val bassLevel = if (bassChromaFrames > 0) bassChromaEnergy / bassChromaFrames else 0.0
        val voiceLevel = if (frames.isEmpty()) 0.0 else voiceEnergy / frames.size
        val present = voiceLevel > 0 && bassLevel >= BASS_ENERGY_RATIO * voiceLevel
        return EvidenceSnapshot(voice, bass, harmonyAll.copyOf(), seconds, bassChroma.copyOf(), present)
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

    /**
     * Resultado até agora. As chances vêm do [KeyModel] (treinado com hinos); as
     * travas abaixo garantem que, sem evidência, NÃO há resposta.
     */
    fun result(seconds: Double = 0.0): KeyResult {
        val segs = segments(frames, midis, weights)
        if (segs.isEmpty()) {
            return KeyResult(KeyStatus.SEM_VOZ, emptyList(), 0.0, 0.0, false, FloatArray(12), null)
        }

        val offset = tuningOffset(segs)
        val between = abs(offset) > BETWEEN_KEYS_SEMITONES
        val pcs = IntArray(segs.size) { Math.floorMod((segs[it].midi - offset).roundToInt(), 12) }
        val hist = DoubleArray(12)
        for (i in segs.indices) hist[pcs[i]] += segs[i].frames.toDouble()
        val total = hist.sum()
        for (i in 0 until 12) hist[i] /= total

        val snap = snapshot(seconds)
        val probs = KeyModel.probabilities(snap) ?: DoubleArray(24)
        val order = (0 until 24).sortedByDescending { probs[it] }
        val best = order.first()
        val bestRoot = best % 12
        val bestMinor = best >= 12

        val voiced = segs.sumOf { it.frames } * hopSeconds
        val distinct = hist.count { it > 0.05 }
        val scale = if (bestMinor) MINOR_SCALE else MAJOR_SCALE
        val fit = scale.sumOf { hist[(bestRoot + it) % 12] }
        val p0 = probs[best]
        val status = when {
            voiced < 1.0 || distinct < 3 || fit < 0.80 -> KeyStatus.INSUFICIENTE
            p0 >= P_ALTA && seconds >= ALTA_MIN_SECONDS && voiced >= ALTA_MIN_VOICED &&
                distinct >= ALTA_MIN_NOTES && !between -> KeyStatus.ALTA
            p0 >= P_MEDIA -> KeyStatus.MEDIA
            else -> KeyStatus.BAIXA
        }
        val candidates = if (status == KeyStatus.INSUFICIENTE) emptyList() else
            order.take(3).map { KeyCandidate(it % 12, it >= 12, probs[it]) }

        return KeyResult(
            status = status,
            candidates = candidates,
            voicedSeconds = voiced,
            tuningOffsetCents = offset * 100,
            betweenKeys = between,
            noteWeights = FloatArray(12) { hist[it].toFloat() },
            lastNote = pcs.last(),
            // Só conta como "baixo" a partir de meio segundo (mesmo critério do modelo).
            bassSeconds = snap.bass.sumOf { it.endSec - it.startSec }.takeIf { it >= 0.5 } ?: 0.0,
        )
    }
    private fun median(v: List<Double>): Double {
        val s = v.sorted()
        return if (s.size % 2 == 1) s[s.size / 2] else (s[s.size / 2 - 1] + s[s.size / 2]) / 2
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
    private val bassFocus = VoiceFocus(BASS_FRAME, bassRate, chromaMinHz = 40.0, chromaMaxHz = 250.0)
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
        evidence.addHarmonyFrame(focus.chroma, rms(clean))

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
        evidence.addBassChroma(bassFocus.chroma, cleanRms)

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

    fun result(): KeyResult = evidence.result(secondsFed)

    fun snapshot(): EvidenceSnapshot = evidence.snapshot(secondsFed)
}

/**
 * Escuta em RODADAS de [roundSeconds] (5 s): a cada rodada a evidência se soma e
 * o palpite fica mais seguro. Encerra sozinha só quando:
 *  - a confiança é ALTA no mesmo tom em 2 leituras seguidas (~0,3 s) e já há
 *    pelo menos meia rodada de áudio; ou
 *  - chegou ao limite ([limitSeconds], 3 rodadas = 15 s) — e aí mostra o que tiver.
 * O músico pode interromper antes e pedir mais rodadas depois ([extend]).
 */
class KeyStopRule(
    val roundSeconds: Double = 5.0,
    limitSeconds: Double = 15.0,
) {
    var limitSeconds = limitSeconds
        private set
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
        return (streak >= 2 && seconds >= roundSeconds / 2) || seconds >= limitSeconds
    }

    /** Mais uma rodada a partir de [fromSeconds]. */
    fun extend(fromSeconds: Double) {
        limitSeconds = fromSeconds + roundSeconds
        streak = 0
        lastKey = -1
    }
}
