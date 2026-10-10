package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
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
 * Toca acordes com um timbre de piano suave, sintetizado no próprio app (sem
 * arquivos de áudio) — usado no treino de ouvido dos graus. Cada acorde é uma
 * lista de notas MIDI; vários acordes tocam em sequência.
 */
class ChordPlayer {

    var isPlaying by mutableStateOf(false)
        private set

    private var track: AudioTrack? = null
    @Volatile private var generation = 0

    /** Toca [chords] em sequência, cada um por [secondsEach] segundos. */
    fun play(chords: List<List<Int>>, secondsEach: Double = 1.1) {
        stop()
        val myGen = ++generation
        isPlaying = true
        thread(name = "chord-player") {
            val pcm = render(chords, secondsEach)
            if (myGen != generation) return@thread
            val t = runCatching {
                AudioTrack.Builder()
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
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
            }.getOrNull() ?: run { isPlaying = false; return@thread }
            t.write(pcm, 0, pcm.size)
            synchronized(this) {
                if (myGen != generation) { t.release(); return@thread }
                track = t
                t.play()
            }
            Thread.sleep((pcm.size * 1000L / SAMPLE_RATE) + 100)
            synchronized(this) {
                if (myGen == generation) {
                    runCatching { t.stop(); t.release() }
                    track = null
                    isPlaying = false
                }
            }
        }
    }

    fun stop() {
        synchronized(this) {
            generation++
            track?.run { runCatching { pause(); flush(); stop(); release() } }
            track = null
        }
        isPlaying = false
    }

    private fun render(chords: List<List<Int>>, secondsEach: Double): ShortArray {
        val each = (SAMPLE_RATE * secondsEach).toInt()
        val out = DoubleArray(each * chords.size + SAMPLE_RATE / 2)
        chords.forEachIndexed { c, notes ->
            val start = c * each
            val len = minOf(out.size - start, (each * 1.4).toInt())
            for (m in notes) {
                val f = 440.0 * Math.pow(2.0, (m - 69) / 12.0)
                for (k in 1..6) {
                    val fk = f * k
                    if (fk > 8000) break
                    val amp = 1.0 / Math.pow(k.toDouble(), 1.3)
                    val decay = 2.2 + 0.9 * k                 // harmônicos agudos somem antes
                    for (i in 0 until len) {
                        val t = i.toDouble() / SAMPLE_RATE
                        val attack = minOf(1.0, t / 0.006)
                        val release = if (i > each) maxOf(0.0, 1.0 - (i - each) / (0.4 * each)) else 1.0
                        out[start + i] += amp * attack * release * exp(-decay * t) * sin(2.0 * PI * fk * t)
                    }
                }
            }
        }
        val peak = out.maxOf { kotlin.math.abs(it) }.coerceAtLeast(1e-9)
        return ShortArray(out.size) { (out[it] / peak * 0.7 * Short.MAX_VALUE).toInt().toShort() }
    }
}
