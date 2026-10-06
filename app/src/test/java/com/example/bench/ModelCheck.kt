package com.example.bench

import com.example.music.EvidenceSnapshot
import com.example.music.KeyModel
import com.example.music.NoteSegment
import java.io.File

/**
 * Confere se o KeyModel (Kotlin) dá as mesmas chances que o treinador (Python):
 * lê o notas.jsonl da bancada e imprime arquivo, t e as 24 chances.
 * Uso: java -cp <classes> com.example.bench.ModelCheck <notas.jsonl> <saida.csv>
 */
object ModelCheck {
    private val num = Regex("-?[0-9]+(\\.[0-9]+)?([eE]-?[0-9]+)?")

    private fun segs(json: String, key: String): List<NoteSegment> {
        val start = json.indexOf("\"$key\":[") + key.length + 3
        var depth = 1
        var i = start + 1
        while (depth > 0) { if (json[i] == '[') depth++ else if (json[i] == ']') depth--; i++ }
        val body = json.substring(start, i)
        return Regex("\\[([^\\[\\]]+)]").findAll(body).map { m ->
            val v = m.groupValues[1].split(",").map { it.toDouble() }
            NoteSegment(v[0], v[1], v[2], v[3])
        }.toList()
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val out = File(args[1]).printWriter()
        File(args[0]).forEachLine { line ->
            val file = Regex("\"file\":\"([^\"]+)\"").find(line)!!.groupValues[1]
            val t = Regex("\"t\":([0-9.]+)").find(line)!!.groupValues[1]
            val h = line.substringAfter("\"harmony\":[").substringBefore("]").split(",").map { it.toDouble() }
            val bc = line.substringAfter("\"bchroma\":[").substringBefore("]").split(",").map { it.toDouble() }
            val present = line.contains("\"bpresent\":true")
            val snap = EvidenceSnapshot(
                segs(line, "voice"), segs(line, "bass"), h.toDoubleArray(), t.toDouble(), bc.toDoubleArray(), present,
            )
            val p = KeyModel.probabilities(snap)
            out.println("$file,$t," + (p?.joinToString(",") { "%.6f".format(java.util.Locale.US, it) } ?: "-"))
        }
        out.close()
    }
}
