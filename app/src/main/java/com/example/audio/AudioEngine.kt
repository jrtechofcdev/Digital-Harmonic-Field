package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

private const val SAMPLE_RATE = 44100

/**
 * Metrônomo com clique sintetizado (sem arquivos de áudio) e temporização por
 * streaming — o próprio ritmo de consumo do AudioTrack mantém o compasso estável,
 * sem depender de timers imprecisos. O primeiro tempo do compasso é acentuado.
 */
class Metronome {

    @Volatile var bpm: Int = 90
    @Volatile var beatsPerMeasure: Int = 4

    var isPlaying by mutableStateOf(false)
        private set
    var currentBeat by mutableIntStateOf(-1)
        private set

    private var track: AudioTrack? = null
    private var worker: Thread? = null
    @Volatile private var running = false

    fun toggle() = if (isPlaying) stop() else start()

    fun start() {
        if (running) return
        running = true
        isPlaying = true

        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        val t = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track = t
        t.play()

        worker = thread(name = "metronome") {
            var beat = 0
            while (running) {
                val currentBpm = bpm.coerceIn(30, 300)
                val beatsPerBar = beatsPerMeasure.coerceIn(1, 12)
                val periodSamples = (SAMPLE_RATE * 60.0 / currentBpm).toInt()
                val accent = beat % beatsPerBar == 0
                currentBeat = beat % beatsPerBar
                val buffer = renderBeat(periodSamples, accent)
                t.write(buffer, 0, buffer.size) // bloqueia até consumir → mantém o tempo
                beat++
            }
        }
    }

    fun stop() {
        running = false
        isPlaying = false
        currentBeat = -1
        worker?.join(200)
        worker = null
        track?.run { runCatching { pause(); flush(); stop(); release() } }
        track = null
    }

    /** Um tempo = clique curto (senoide com decaimento) + silêncio até completar o período. */
    private fun renderBeat(periodSamples: Int, accent: Boolean): ShortArray {
        val buf = ShortArray(periodSamples)
        val clickMs = 0.030
        val clickSamples = (SAMPLE_RATE * clickMs).toInt().coerceAtMost(periodSamples)
        val freq = if (accent) 1760.0 else 1244.0
        val amp = if (accent) 0.9 else 0.55
        for (i in 0 until clickSamples) {
            val env = exp(-6.0 * i / clickSamples) // decaimento exponencial
            val s = sin(2.0 * PI * freq * i / SAMPLE_RATE) * env * amp
            buf[i] = (s * Short.MAX_VALUE).toInt().toShort()
        }
        return buf
    }
}

/**
 * Tocador de tom contínuo para afinação de ouvido (diapasão). Gera senoide por
 * streaming com fase contínua — sem estalos de loop. Um mesmo tom tocando é
 * interrompido ao ser tocado de novo (comportamento de "toggle").
 */
class TonePlayer {

    var playingKey by mutableStateOf<String?>(null)
        private set

    private var track: AudioTrack? = null
    private var worker: Thread? = null
    @Volatile private var running = false

    fun toggle(key: String, frequency: Double) {
        if (playingKey == key) {
            stop()
        } else {
            play(key, frequency)
        }
    }

    /** Toca a nota até alguém chamar stop() (usado para um toque curto de referência). */
    fun playOnce(key: String, frequency: Double) = play(key, frequency)

    private fun play(key: String, frequency: Double) {
        stop()
        running = true
        playingKey = key

        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        val t = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track = t
        t.play()

        worker = thread(name = "tone") {
            val chunk = ShortArray(2048)
            var phase = 0.0
            val inc = 2.0 * PI * frequency / SAMPLE_RATE
            var rendered = 0
            val fadeSamples = SAMPLE_RATE / 40 // ~25ms de fade-in evita estalo
            while (running) {
                for (i in chunk.indices) {
                    val gain = if (rendered < fadeSamples) rendered.toDouble() / fadeSamples else 1.0
                    val s = sin(phase) * 0.26 * gain
                    chunk[i] = (s * Short.MAX_VALUE).toInt().toShort()
                    phase += inc
                    if (phase > 2.0 * PI) phase -= 2.0 * PI
                    rendered++
                }
                t.write(chunk, 0, chunk.size)
            }
        }
    }

    fun stop() {
        running = false
        playingKey = null
        worker?.join(200)
        worker = null
        track?.run { runCatching { pause(); flush(); stop(); release() } }
        track = null
    }
}

/**
 * Frequências de referência (temperamento igual, A4 = 440 Hz) para a 4ª oitava,
 * usadas pelo diapasão. Chave pela cifra natural/sustenido.
 */
object ReferencePitches {
    val a4 = 440.0
    // Semitons a partir de C4.
    private val semitoneFromC = mapOf(
        "C" to -9, "C#" to -8, "D" to -7, "D#" to -6, "E" to -5, "F" to -4,
        "F#" to -3, "G" to -2, "G#" to -1, "A" to 0, "A#" to 1, "B" to 2
    )

    fun frequency(note: String): Double {
        val n = semitoneFromC[note] ?: 0
        return a4 * Math.pow(2.0, n / 12.0)
    }
}
