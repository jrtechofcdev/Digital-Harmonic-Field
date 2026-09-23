package com.example.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.HarmonicDatabase
import com.example.music.blendChroma
import com.example.music.cipherToPtName
import com.example.music.computeChroma
import com.example.music.detectChord
import com.example.music.detectKey
import kotlin.concurrent.thread
import kotlin.math.sqrt

/**
 * Escuta o microfone e estima, em tempo real, o acorde tocado e o tom provável
 * da música. Toda a análise é local; ver ChordAnalysis para a matemática.
 *
 * Melhorias de captação:
 *  - Janela deslizante com sobreposição (hop menor que a janela) → resposta
 *    mais rápida e suave, com boa resolução de frequência.
 *  - Estabilização: o acorde só troca quando um novo candidato se confirma, e
 *    o último acorde é mantido por um instante nas pausas (não fica piscando).
 *
 * A permissão RECORD_AUDIO deve ser concedida ANTES de chamar start().
 */
class ChordListener {

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val FFT_SIZE = 8192       // ~186 ms de janela (bom custo/resolução)
        private const val HOP = 4096            // ~93 ms entre análises (50% overlap)
        private const val SILENCE_RMS = 0.006   // abaixo disso, tratamos como silêncio
        private const val STABLE_FRAMES = 2     // confirmações para trocar o acorde
        private const val HOLD_FRAMES = 14       // ~1,3 s segurando o último acorde
    }

    var isListening by mutableStateOf(false)
        private set
    var currentChord by mutableStateOf<String?>(null)   // ex.: "C", "Am"
        private set
    var currentChordPt by mutableStateOf<String?>(null)  // ex.: "Dó Maior"
        private set
    var keyCipher by mutableStateOf<String?>(null)       // ex.: "G", "Em"
        private set
    var keyPt by mutableStateOf<String?>(null)           // ex.: "Sol Maior"
        private set
    var level by mutableFloatStateOf(0f)                 // volume de entrada 0..1
        private set
    var chroma by mutableStateOf(FloatArray(12))         // para o gráfico
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private var record: AudioRecord? = null
    private var worker: Thread? = null
    @Volatile private var running = false
    @Volatile private var keyResetRequested = false

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
                maxOf(minBuf, FFT_SIZE * 2),
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

        worker = thread(name = "chord-listener") { analysisLoop(rec) }
    }

    private fun analysisLoop(rec: AudioRecord) {
        val window = DoubleArray(FFT_SIZE)      // janela deslizante
        val hopShorts = ShortArray(HOP)
        var chordChroma = FloatArray(12)        // suavização rápida (acorde)
        var keyChroma = FloatArray(12)          // acúmulo lento (tom)

        var stable: String? = null
        var pending: String? = null
        var pendingCount = 0
        var framesSinceGood = HOLD_FRAMES

        while (running) {
            // Lê um hop completo (bloco novo que entra na janela).
            var read = 0
            while (read < HOP && running) {
                val r = rec.read(hopShorts, read, HOP - read)
                if (r <= 0) break
                read += r
            }
            if (!running || read < HOP) continue

            if (keyResetRequested) {
                keyChroma = FloatArray(12)
                keyResetRequested = false
            }

            // Desliza a janela: descarta o hop mais antigo, anexa o novo.
            System.arraycopy(window, HOP, window, 0, FFT_SIZE - HOP)
            for (i in 0 until HOP) {
                window[FFT_SIZE - HOP + i] = hopShorts[i] / 32768.0
            }
            // RMS da janela inteira (mais estável que só o último hop).
            var sumSq = 0.0
            for (i in 0 until FFT_SIZE) sumSq += window[i] * window[i]
            val rms = sqrt(sumSq / FFT_SIZE)
            level = (rms * 6f).coerceIn(0.0, 1.0).toFloat()

            if (rms < SILENCE_RMS) {
                framesSinceGood++
                if (framesSinceGood > HOLD_FRAMES) {
                    stable = null; pending = null; pendingCount = 0
                }
                // Faz o gráfico decair no silêncio (não congela na última leitura).
                chordChroma = FloatArray(12) { chordChroma[it] * 0.8f }
                chroma = chordChroma
                publishChord(stable)
                continue
            }

            val instant = computeChroma(window, SAMPLE_RATE)
            chordChroma = blendChroma(chordChroma, instant, 0.45f)
            keyChroma = blendChroma(keyChroma, instant, 0.05f)
            chroma = chordChroma

            val cand = detectChord(chordChroma)
            if (cand != null) {
                framesSinceGood = 0
                if (cand.label == pending) {
                    pendingCount++
                } else {
                    pending = cand.label
                    pendingCount = 1
                }
                // Exige confirmação também no primeiro acorde após uma pausa,
                // para não piscar o candidato errado no ataque da nota.
                if (pendingCount >= STABLE_FRAMES) {
                    stable = cand.label
                }
            } else {
                framesSinceGood++
                if (framesSinceGood > HOLD_FRAMES) {
                    stable = null; pending = null; pendingCount = 0
                }
            }
            publishChord(stable)

            val key = detectKey(keyChroma)
            if (key != null) {
                keyCipher = key.keyCipher
                keyPt = HarmonicDatabase.ptNameByCipher[key.keyCipher]
            }
        }
    }

    private fun publishChord(label: String?) {
        currentChord = label
        currentChordPt = label?.let { cipherToPtName(it) }
    }

    fun stop() {
        running = false
        isListening = false
        level = 0f
        currentChord = null
        currentChordPt = null
        worker?.join(300)
        worker = null
        record?.run { runCatching { stop(); release() } }
        record = null
    }

    /** Zera a estimativa de tom acumulada (recomeça a "ouvir" a música). */
    fun resetKey() {
        keyCipher = null
        keyPt = null
        keyResetRequested = true
    }
}
