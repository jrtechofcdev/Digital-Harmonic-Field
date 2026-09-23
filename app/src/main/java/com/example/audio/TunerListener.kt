package com.example.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.music.centsBetween
import com.example.music.detectPitch
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Escuta o microfone e estima a frequência de UMA nota (corda) por vez, para o
 * afinador. Usa detecção de altura por autocorrelação e suaviza a leitura para
 * o ponteiro não tremer. Requer permissão RECORD_AUDIO concedida antes.
 */
class TunerListener {

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val WINDOW = 4096   // ~93 ms
        private const val HOP = 2048      // ~46 ms entre leituras
        private const val HOLD_FRAMES = 12
    }

    var isListening by mutableStateOf(false)
        private set
    var frequency by mutableStateOf<Double?>(null)   // Hz suavizado, ou null
        private set
    var clarity by mutableFloatStateOf(0f)
        private set
    var level by mutableFloatStateOf(0f)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    // Ajustados pelas Configurações (filtro de ruído).
    @Volatile var minClarity: Double = 0.90
    @Volatile var silenceRms: Double = 0.006

    // Quando o tom de referência está tocando, silenciamos a detecção para não
    // captar o próprio alto-falante.
    @Volatile var muted: Boolean = false

    private var record: AudioRecord? = null
    private var worker: Thread? = null
    @Volatile private var running = false

    fun toggle() = if (isListening) stop() else start()

    fun start() {
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
                maxOf(minBuf, WINDOW * 2),
            )
        } catch (e: Exception) {
            error = "Não foi possível acessar o microfone."
            null
        } ?: return

        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            error = "Microfone indisponível. Confira a permissão."
            rec.release()
            return
        }

        record = rec
        running = true
        isListening = true
        try {
            rec.startRecording()
        } catch (e: Exception) {
            error = "Não foi possível iniciar a escuta."
            stop()
            return
        }

        worker = thread(name = "tuner-listener") { loop(rec) }
    }

    private fun loop(rec: AudioRecord) {
        val window = DoubleArray(WINDOW)
        val hopShorts = ShortArray(HOP)
        var framesSinceGood = HOLD_FRAMES

        while (running) {
            var read = 0
            while (read < HOP && running) {
                val r = rec.read(hopShorts, read, HOP - read)
                if (r <= 0) break
                read += r
            }
            if (!running || read < HOP) continue

            System.arraycopy(window, HOP, window, 0, WINDOW - HOP)
            for (i in 0 until HOP) window[WINDOW - HOP + i] = hopShorts[i] / 32768.0

            var sumSq = 0.0
            for (i in 0 until WINDOW) sumSq += window[i] * window[i]
            val rms = sqrt(sumSq / WINDOW)
            level = (rms * 6f).coerceIn(0.0, 1.0).toFloat()

            if (muted) {
                frequency = null
                continue
            }

            if (rms < silenceRms) {
                framesSinceGood++
                if (framesSinceGood > HOLD_FRAMES) frequency = null
                continue
            }

            val res = detectPitch(window, SAMPLE_RATE, minClarity = minClarity)
            if (res != null) {
                framesSinceGood = 0
                clarity = res.clarity.toFloat()
                val prev = frequency
                frequency = if (prev == null || abs(centsBetween(res.frequency, prev)) > 250.0) {
                    res.frequency // troca de corda: pula direto
                } else {
                    prev * 0.6 + res.frequency * 0.4 // mesma nota: suaviza
                }
            } else {
                framesSinceGood++
                if (framesSinceGood > HOLD_FRAMES) frequency = null
            }
        }
    }

    fun stop() {
        running = false
        isListening = false
        level = 0f
        frequency = null
        worker?.join(300)
        worker = null
        record?.run { runCatching { stop(); release() } }
        record = null
    }
}
