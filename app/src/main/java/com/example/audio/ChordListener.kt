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
import com.example.music.detectChord
import com.example.music.detectKey
import com.example.music.computeChroma
import kotlin.concurrent.thread
import kotlin.math.sqrt

/**
 * Escuta o microfone e estima, em tempo real, o acorde tocado e o tom provável
 * da música. Toda a análise é local; ver ChordAnalysis para a matemática.
 *
 * A permissão RECORD_AUDIO deve ser concedida ANTES de chamar start().
 */
class ChordListener {

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val FFT_SIZE = 8192 // ~186 ms por análise
        private const val SILENCE_RMS = 0.008 // abaixo disso, tratamos como silêncio
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

        worker = thread(name = "chord-listener") {
            val shorts = ShortArray(FFT_SIZE)
            val samples = DoubleArray(FFT_SIZE)
            var chordChroma = FloatArray(12) // suavização rápida (acorde)
            var keyChroma = FloatArray(12)   // acúmulo lento (tom)

            while (running) {
                // Preenche um bloco completo lendo em pedaços.
                var read = 0
                while (read < FFT_SIZE && running) {
                    val r = rec.read(shorts, read, FFT_SIZE - read)
                    if (r <= 0) break
                    read += r
                }
                if (!running || read < FFT_SIZE) continue

                if (keyResetRequested) {
                    keyChroma = FloatArray(12)
                    keyResetRequested = false
                }

                var sumSq = 0.0
                for (i in 0 until FFT_SIZE) {
                    val s = shorts[i] / 32768.0
                    samples[i] = s
                    sumSq += s * s
                }
                val rms = sqrt(sumSq / FFT_SIZE)
                level = (rms * 6f).coerceIn(0.0, 1.0).toFloat()

                if (rms < SILENCE_RMS) {
                    // Silêncio: não polui o acúmulo; apenas indica que está quieto.
                    currentChord = null
                    currentChordPt = null
                    continue
                }

                val instant = computeChroma(samples, SAMPLE_RATE)
                chordChroma = blendChroma(chordChroma, instant, 0.5f)
                keyChroma = blendChroma(keyChroma, instant, 0.04f)
                chroma = chordChroma

                val chord = detectChord(chordChroma)
                if (chord != null) {
                    currentChord = chord.label
                    currentChordPt = cipherToPtName(chord.label)
                }

                val key = detectKey(keyChroma)
                if (key != null) {
                    keyCipher = key.keyCipher
                    keyPt = HarmonicDatabase.ptNameByCipher[key.keyCipher]
                }
            }
        }
    }

    fun stop() {
        running = false
        isListening = false
        level = 0f
        currentChord = null
        currentChordPt = null
        worker?.join(200)
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
