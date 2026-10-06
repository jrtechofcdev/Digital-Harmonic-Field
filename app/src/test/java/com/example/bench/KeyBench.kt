package com.example.bench

import com.example.music.KeyDetector
import com.example.music.KeyStopRule
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Bancada de acerto do detector de tom (ferramenta de desenvolvimento, não roda no app).
 * Lê WAVs mono 16 bits 44,1 kHz + labels.csv (arquivo,tônica,menor,...) e escreve,
 * para cada trecho, o resultado aos 5, 10 e 15 s e onde a regra de parada encerraria.
 *
 * Uso: java -cp <classes> com.example.bench.KeyBench <pasta> <saida.csv> [notas.jsonl] [parte] [partes]
 * Com o 3º argumento, também grava as notas extraídas a cada 2,5 s (para treinar o modelo).
 */
object KeyBench {
    private fun readWav(f: File): DoubleArray {
        val bytes = f.readBytes()
        var pos = 12
        while (pos + 8 <= bytes.size) {
            val id = String(bytes, pos, 4)
            val size = ByteBuffer.wrap(bytes, pos + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
            if (id == "data") {
                val bb = ByteBuffer.wrap(bytes, pos + 8, size).order(ByteOrder.LITTLE_ENDIAN)
                return DoubleArray(size / 2) { bb.short / 32768.0 }
            }
            pos += 8 + size
        }
        error("sem dados: $f")
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val dir = File(args[0])
        val out = File(args[1]).printWriter()
        val notes = args.getOrNull(2)?.takeIf { it.isNotEmpty() }?.let { File(it).printWriter() }
        out.println("file,tonic,minor,src,scene,checkpoint,status,k1,p1,k2,k3,voiced,bass,stop_s,stop_k1,stop_status")
        val chunk = DoubleArray(2048)
        val shard = args.getOrNull(3)?.toInt() ?: 0
        val shards = args.getOrNull(4)?.toInt() ?: 1
        File(dir, "labels.csv").readLines().filter { it.isNotBlank() }
            .filterIndexed { idx, _ -> idx % shards == shard }
            .forEach { line ->
            val c = line.split(",")
            val x = readWav(File(dir, c[0]))
            val det = KeyDetector(44100)
            val rule = KeyStopRule()
            var stop: Triple<Double, String, String>? = null
            val endSeconds = x.size / 44100.0
            // Pontos de medida a cada 5 s até o fim (gravações reais passam de 15 s).
            val checkpoints = generateSequence(5.0) { it + 5.0 }.takeWhile { it <= maxOf(15.0, endSeconds + 0.05) }
                .toMutableList()
            var i = 0
            var chunks = 0
            var nextDump = 2.5
            while (i < x.size) {
                val n = minOf(2048, x.size - i)
                System.arraycopy(x, i, chunk, 0, n)
                det.feed(chunk, n)
                i += n
                chunks++
                val t = det.secondsFed
                val last = i >= x.size
                if (stop == null && (chunks % 6 == 0 || last)) {
                    val r = det.result()
                    // No fim do áudio, a rodada encerra com o que tiver (como no app).
                    if (rule.shouldStop(r, t) || last && t >= endSeconds - 0.05) {
                        stop = Triple(t, r.candidates.firstOrNull()?.keyCipher ?: "-", r.status.name)
                    }
                }
                if (notes != null && t >= nextDump - 1e-6) {
                    notes.println(dump(c[0], nextDump, det.snapshot()))
                    nextDump += 2.5
                }
                if (checkpoints.isNotEmpty() && (t >= checkpoints.first() - 1e-6 || last && checkpoints.first() >= endSeconds - 0.05)) {
                    val cp = checkpoints.removeAt(0)
                    val r = det.result()
                    val k = r.candidates.map { it.keyCipher }
                    out.println(listOf(c[0], c[1], c[2], c[3], c[4], cp, r.status.name,
                        k.getOrElse(0) { "-" }, "%.3f".format(java.util.Locale.US, r.candidates.firstOrNull()?.probability ?: 0.0),
                        k.getOrElse(1) { "-" }, k.getOrElse(2) { "-" },
                        "%.2f".format(java.util.Locale.US, r.voicedSeconds), "%.2f".format(java.util.Locale.US, r.bassSeconds),
                        "%.2f".format(java.util.Locale.US, stop?.first ?: -1.0), stop?.second ?: "-", stop?.third ?: "-").joinToString(","))
                }
            }
        }
        out.close()
        notes?.close()
    }

    private fun dump(file: String, t: Double, s: com.example.music.EvidenceSnapshot): String {
        fun f(x: Double) = "%.4f".format(java.util.Locale.US, x)
        fun segs(l: List<com.example.music.NoteSegment>) =
            l.joinToString(",", "[", "]") { "[${f(it.startSec)},${f(it.endSec)},${f(it.midi)},${f(it.weight)}]" }
        return "{\"file\":\"$file\",\"t\":$t,\"voice\":${segs(s.voice)},\"bass\":${segs(s.bass)}," +
            "\"harmony\":${s.harmony.joinToString(",", "[", "]") { f(it) }}," +
            "\"bchroma\":${s.bassChroma.joinToString(",", "[", "]") { f(it) }},\"bpresent\":${s.bassPresent}}"
    }
}
