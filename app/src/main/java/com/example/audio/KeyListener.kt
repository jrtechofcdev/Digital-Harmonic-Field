package com.example.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.music.KeyCandidate
import com.example.music.KeyDetector
import com.example.music.KeyResult
import com.example.music.KeyStopRule
import kotlin.concurrent.thread
import kotlin.math.sqrt

/**
 * Microfone → detector de tom, com resposta quase instantânea.
 *
 * Enquanto a tela está aberta ([open]), o microfone fica ligado e os últimos
 * [PREROLL_SECONDS] segundos ficam guardados SÓ NA MEMÓRIA, num buffer circular
 * que é sobrescrito o tempo todo. Nada é gravado em arquivo nem enviado; ao
 * sair da tela ([close]) o buffer é apagado.
 *
 * Ao tocar em "Detectar" ([detect]) esses segundos já ouvidos são analisados na
 * hora; se ainda faltar evidência, o app continua ouvindo ao vivo e para assim
 * que tiver certeza ([KeyStopRule]) — até [KeyStopRule.maxSeconds] no máximo.
 * Requer permissão RECORD_AUDIO concedida.
 */
class KeyListener {

    companion object {
        const val PREROLL_SECONDS = 5
        private const val SAMPLE_RATE = 44100
        private const val CHUNK = 2048
        private const val EVAL_EVERY_CHUNKS = 6 // ~0,28 s
    }

    /**
     * OFF: microfone desligado · READY: guardando os últimos segundos ·
     * ANALYZING: analisando (pré-buffer + ao vivo) · DONE: resultado pronto
     * (o microfone continua pronto para uma nova leitura).
     */
    enum class Phase { OFF, READY, ANALYZING, DONE }

    var phase by mutableStateOf(Phase.OFF)
        private set
    /** Quanto do pré-buffer já está cheio (0..1). */
    var buffered by mutableFloatStateOf(0f)
        private set
    /** Segundos de áudio já analisados na leitura atual. */
    var analyzedSeconds by mutableFloatStateOf(0f)
        private set
    var level by mutableFloatStateOf(0f)
        private set
    var hearingVoice by mutableStateOf(false)
        private set
    var liveGuess by mutableStateOf<KeyCandidate?>(null) // parcial, durante a análise
        private set
    var result by mutableStateOf<KeyResult?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private var record: AudioRecord? = null
    private var worker: Thread? = null
    @Volatile private var running = false
    // Cada abertura do microfone tem um número; uma sessão antiga (fechada) nunca
    // publica nada nem mexe no microfone de uma sessão nova.
    @Volatile private var session = 0
    // Pedidos da tela para a thread de áudio (que é quem mexe no detector).
    @Volatile private var detectRequested = false
    @Volatile private var cancelRequested = false

    /** Liga o microfone e começa a guardar os últimos segundos (só na memória). */
    fun open() {
        if (running) return
        error = null

        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuf <= 0) { error = "Este aparelho não permite captura de áudio."; return }

        val rec = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                // ~1 s de folga: cobre o instante em que o pré-buffer é analisado.
                maxOf(minBuf, SAMPLE_RATE * 2),
            )
        } catch (e: Exception) {
            null
        }
        if (rec == null || rec.state != AudioRecord.STATE_INITIALIZED) {
            error = "Microfone indisponível. Confira a permissão."
            rec?.release()
            return
        }
        try {
            rec.startRecording()
        } catch (e: Exception) {
            error = "Não foi possível ligar o microfone."
            rec.release()
            return
        }

        record = rec
        running = true
        detectRequested = false
        cancelRequested = false
        buffered = 0f
        if (phase == Phase.OFF) phase = Phase.READY
        val mySession = ++session
        worker = thread(name = "key-listener") { loop(rec, mySession) }
    }

    /** Analisa os segundos já guardados e, se precisar, continua ouvindo. */
    fun detect() {
        if (phase == Phase.ANALYZING) return
        if (!running) open() // microfone caiu ou foi desligado: religa e ouve ao vivo
        if (!running) return
        error = null
        liveGuess = null
        analyzedSeconds = 0f
        detectRequested = true
        phase = Phase.ANALYZING
    }

    /** Interrompe a leitura atual (sem resultado); o microfone continua pronto. */
    fun cancelDetection() {
        if (phase != Phase.ANALYZING) return
        cancelRequested = true
        phase = if (result != null) Phase.DONE else Phase.READY
        liveGuess = null
        hearingVoice = false
    }

    /** Desliga o microfone e apaga o que estava na memória. */
    fun close() {
        if (!running) return
        running = false
        session++
        worker?.join(500)
        worker = null
        releaseRecorder(record)
        phase = if (result != null) Phase.DONE else Phase.OFF
        buffered = 0f
        level = 0f
        hearingVoice = false
        liveGuess = null
    }

    private fun loop(rec: AudioRecord, mySession: Int) {
        val ringSize = PREROLL_SECONDS * SAMPLE_RATE
        val ring = ShortArray(ringSize) // pré-buffer circular, só na memória
        var ringPos = 0
        var ringFilled = 0
        val shorts = ShortArray(CHUNK)
        val samples = DoubleArray(CHUNK)

        var detector: KeyDetector? = null
        var stopRule = KeyStopRule()
        var chunks = 0

        fun active() = running && session == mySession

        try {
            while (active()) {
                val r = rec.read(shorts, 0, CHUNK)
                if (r <= 0) {
                    if (active()) error = "A captura de áudio foi interrompida."
                    break
                }

                // 1) Guarda no pré-buffer (sobrescreve o mais antigo).
                var sumSq = 0.0
                for (i in 0 until r) {
                    ring[ringPos] = shorts[i]
                    ringPos = (ringPos + 1) % ringSize
                    val v = shorts[i] / 32768.0
                    sumSq += v * v
                }
                ringFilled = minOf(ringSize, ringFilled + r)
                buffered = ringFilled.toFloat() / ringSize

                if (cancelRequested) {
                    cancelRequested = false
                    detector = null
                }

                if (detectRequested) {
                    // 2) Novo pedido: analisa de uma vez tudo o que já foi ouvido
                    //    (inclui o trecho que acabou de chegar).
                    detectRequested = false
                    val d = KeyDetector(SAMPLE_RATE)
                    var idx = (ringPos - ringFilled + ringSize) % ringSize
                    var left = ringFilled
                    while (left > 0) {
                        val c = minOf(CHUNK, left)
                        for (i in 0 until c) samples[i] = ring[(idx + i) % ringSize] / 32768.0
                        d.feed(samples, c)
                        idx = (idx + c) % ringSize
                        left -= c
                    }
                    detector = d
                    stopRule = KeyStopRule()
                    chunks = 0
                    if (evaluate(d, stopRule, mySession)) detector = null
                } else if (detector != null) {
                    // 3) Já analisando: segue ao vivo.
                    for (i in 0 until r) samples[i] = shorts[i] / 32768.0
                    detector.feed(samples, r)
                    level = detector.level
                    hearingVoice = detector.hearingVoice
                    analyzedSeconds = detector.secondsFed.toFloat()
                    if (++chunks % EVAL_EVERY_CHUNKS == 0 && evaluate(detector, stopRule, mySession)) {
                        detector = null
                    }
                } else {
                    level = (sqrt(sumSq / r) * 8).coerceIn(0.0, 1.0).toFloat()
                }
            }
        } finally {
            ring.fill(0)
            releaseRecorder(rec)
            if (session == mySession) {
                running = false
                phase = if (result != null) Phase.DONE else Phase.OFF
                level = 0f
                hearingVoice = false
            }
        }
    }

    /** Lê o detector; publica o resultado e devolve true quando já pode parar. */
    private fun evaluate(detector: KeyDetector, rule: KeyStopRule, mySession: Int): Boolean {
        val r = detector.result()
        val seconds = detector.secondsFed
        analyzedSeconds = seconds.toFloat()
        liveGuess = r.candidates.firstOrNull()
        if (!rule.shouldStop(r, seconds)) return false
        if (session != mySession || !running || phase != Phase.ANALYZING) return true
        result = r
        liveGuess = null
        hearingVoice = false
        phase = Phase.DONE
        return true
    }

    @Synchronized
    private fun releaseRecorder(target: AudioRecord?) {
        target?.run { runCatching { stop(); release() } }
        if (record === target) record = null
    }
}
