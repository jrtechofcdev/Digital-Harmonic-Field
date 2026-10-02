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
import kotlin.concurrent.thread

/**
 * Ouve exatamente [SECONDS] segundos do microfone e entrega o tom da música.
 * A análise acontece ENQUANTO grava (a cada ~46 ms), então o resultado sai no
 * instante em que os 5 s terminam. Requer permissão RECORD_AUDIO concedida.
 */
class KeyListener {

    companion object {
        const val SECONDS = 5
        private const val SAMPLE_RATE = 44100
        private const val CHUNK = 2048
        private const val LIVE_EVERY_CHUNKS = 6 // ~0,28 s
    }

    enum class Phase { IDLE, LISTENING, DONE }

    var phase by mutableStateOf(Phase.IDLE)
        private set
    var progress by mutableFloatStateOf(0f)        // 0..1 dos 5 segundos
        private set
    var level by mutableFloatStateOf(0f)
        private set
    var hearingVoice by mutableStateOf(false)
        private set
    var liveGuess by mutableStateOf<KeyCandidate?>(null) // parcial, durante a escuta
        private set
    var result by mutableStateOf<KeyResult?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private var record: AudioRecord? = null
    private var worker: Thread? = null
    @Volatile private var running = false
    // Cada escuta tem um número; uma escuta antiga (cancelada) nunca publica nada
    // nem mexe no microfone de uma escuta nova.
    @Volatile private var session = 0

    fun start() {
        if (running) return
        error = null
        result = null
        liveGuess = null
        progress = 0f

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
                maxOf(minBuf, CHUNK * 8),
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
            error = "Não foi possível iniciar a escuta."
            rec.release()
            return
        }

        record = rec
        running = true
        phase = Phase.LISTENING
        val mySession = ++session
        worker = thread(name = "key-listener") { listen(rec, mySession) }
    }

    private fun listen(rec: AudioRecord, mySession: Int) {
        val detector = KeyDetector(SAMPLE_RATE)
        val shorts = ShortArray(CHUNK)
        val samples = DoubleArray(CHUNK)
        val totalSamples = SECONDS * SAMPLE_RATE
        var captured = 0
        var chunks = 0

        fun active() = running && session == mySession

        while (active() && captured < totalSamples) {
            val r = rec.read(shorts, 0, minOf(CHUNK, totalSamples - captured))
            if (r <= 0) break
            for (i in 0 until r) samples[i] = shorts[i] / 32768.0
            detector.feed(samples, r)
            captured += r
            chunks++

            progress = captured.toFloat() / totalSamples
            level = detector.level
            hearingVoice = detector.hearingVoice
            if (chunks % LIVE_EVERY_CHUNKS == 0) {
                liveGuess = detector.result().candidates.firstOrNull()
            }
        }

        releaseRecorder(rec)
        if (!active()) return // cancelada: não publica nada
        if (captured >= totalSamples) {
            result = detector.result()
            phase = Phase.DONE
        } else {
            error = "A captura de áudio foi interrompida."
            phase = Phase.IDLE
        }
        running = false
        hearingVoice = false
        level = 0f
    }

    /** Cancela a escuta (sem resultado). */
    fun cancel() {
        if (!running) return
        running = false
        session++
        worker?.join(500)
        worker = null
        releaseRecorder(record)
        phase = Phase.IDLE
        progress = 0f
        level = 0f
        hearingVoice = false
        liveGuess = null
    }

    @Synchronized
    private fun releaseRecorder(target: AudioRecord?) {
        target?.run { runCatching { stop(); release() } }
        if (record === target) record = null
    }
}
