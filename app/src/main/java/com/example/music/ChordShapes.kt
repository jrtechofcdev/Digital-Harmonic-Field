package com.example.music

/**
 * Dicionário de acordes: as formas "de revistinha" (as mais usadas) e TODAS as
 * formas tocáveis de um acorde no braço.
 *
 * As mais usadas seguem o sistema CAGED: os desenhos abertos de Dó, Lá, Sol, Mi
 * e Ré viram formas móveis (com pestana ou não) e se repetem pelo braço. Ex.: o
 * Sol aparece como "forma de Sol" (aberto), "forma de Mi" (pestana na 3ª casa),
 * "forma de Ré" (5ª casa), "forma de Dó" (7ª casa) e "forma de Lá" (10ª casa).
 *
 * Cada molde é validado por [Voicings.evaluate]: só entra se soar exatamente o
 * acorde e couber na mão. Moldes que só funcionam com cordas soltas (ex.: Lá(9))
 * aparecem só no tom em que caem abertos.
 */

/** Uma forma do dicionário: a forma em si e o nome dela ("Forma de Mi"…). */
data class NamedVoicing(val voicing: Voicing, val name: String) {
    /** "Forma de Mi · pestana na casa 3", "Forma de Sol · aberto"… */
    val title: String
        get() = name + " · " + when {
            voicing.position == "Aberto" -> "aberto"
            voicing.barre != null -> "pestana na casa ${voicing.barre}"
            else -> "casa ${voicing.minFret}"
        }
}

object ChordShapes {
    private const val X = -99
    const val MAX_FRET = 15

    /** Molde móvel: corda da fundamental (0 = 6ª) e casa de cada corda RELATIVA à da fundamental. */
    private class Template(val name: String, val rootString: Int, vararg val offsets: Int)

    private val E = "Forma de Mi"
    private val A = "Forma de Lá"
    private val D = "Forma de Ré"
    private val C = "Forma de Dó"
    private val G = "Forma de Sol"

    private val templates: Map<ChordType, List<Template>> = mapOf(
        ChordType.MAJOR to listOf(
            Template(E, 0, 0, 2, 2, 1, 0, 0),
            Template(A, 1, X, 0, 2, 2, 2, 0),
            Template(D, 2, X, X, 0, 2, 3, 2),
            Template(C, 1, X, 0, -1, -3, -2, -3),
            Template(G, 0, 0, -1, -3, -3, -3, 0),
        ),
        ChordType.MINOR to listOf(
            Template(E, 0, 0, 2, 2, 0, 0, 0),
            Template(A, 1, X, 0, 2, 2, 1, 0),
            Template(D, 2, X, X, 0, 2, 3, 1),
            Template(C, 1, X, 0, -2, -3, -2, X),
        ),
        ChordType.DOM7 to listOf(
            Template(E, 0, 0, 2, 0, 1, 0, 0),
            Template(A, 1, X, 0, 2, 0, 2, 0),
            Template(D, 2, X, X, 0, 2, 1, 2),
            Template(C, 1, X, 0, -1, 0, -2, X),
            Template(G, 0, 0, -1, -3, -3, -3, -2),
        ),
        ChordType.MIN7 to listOf(
            Template(E, 0, 0, 2, 0, 0, 0, 0),
            Template("$E (sem a 5ª corda)", 0, 0, X, 0, 0, 0, X),
            Template(A, 1, X, 0, 2, 0, 1, 0),
            Template(D, 2, X, X, 0, 2, 1, 1),
        ),
        ChordType.MAJ7 to listOf(
            Template(E, 0, 0, X, 1, 1, 0, X),
            Template(A, 1, X, 0, 2, 1, 2, 0),
            Template(D, 2, X, X, 0, 2, 2, 2),
            Template(C, 1, X, 0, -1, -3, -3, -3),
            Template(G, 0, 0, -1, -3, -3, -3, -1),
        ),
        ChordType.ADD9 to listOf(
            Template(C, 1, X, 0, -1, -3, 0, -3),
            Template(G, 0, 0, -3, -3, -1, -3, 0),
            Template(A, 1, X, 0, 2, 4, 2, 0),
            Template(E, 0, 0, 2, 2, 1, 0, 2),
        ),
        ChordType.MADD9 to listOf(
            Template(C, 1, X, 0, -2, -3, 0, X),
            Template(A, 1, X, 0, 2, 4, 1, 0),
            Template(E, 0, 0, 2, 4, 0, 0, 0),
        ),
        ChordType.SUS2 to listOf(
            Template(A, 1, X, 0, 2, 2, 0, 0),
            Template(D, 2, X, X, 0, 2, 3, 0),
            Template(E, 0, 0, 2, 4, 4, 0, 0),
        ),
        ChordType.SUS4 to listOf(
            Template(E, 0, 0, 2, 2, 2, 0, 0),
            Template(A, 1, X, 0, 2, 2, 3, 0),
            Template(D, 2, X, X, 0, 2, 3, 3),
        ),
        ChordType.SIX to listOf(
            Template(E, 0, 0, 2, 2, 1, 2, 0),
            Template(A, 1, X, 0, 2, 2, 2, 2),
            Template(D, 2, X, X, 0, 2, 0, 2),
            Template(C, 1, X, 0, -1, -1, -2, -3),
            Template(G, 0, 0, -1, -3, -3, -3, -3),
        ),
        ChordType.MIN6 to listOf(
            Template(E, 0, 0, 2, 2, 0, 2, 0),
            Template(A, 1, X, 0, 2, 2, 1, 2),
            Template(D, 2, X, X, 0, 2, 0, 1),
        ),
        ChordType.SIX9 to listOf(
            Template(C, 1, X, 0, -1, -1, 0, 0),
            Template(G, 0, 0, -3, -3, -3, -3, -3),
        ),
        ChordType.MAJ9 to listOf(
            Template(C, 1, X, 0, -1, 1, 0, X),
        ),
        ChordType.DOM9 to listOf(
            Template(C, 1, X, 0, -1, 0, 0, X),
        ),
        ChordType.MIN9 to listOf(
            Template(C, 1, X, 0, -2, 0, 0, X),
            Template(E, 0, 0, X, 0, 0, 0, 2),
        ),
        ChordType.SEVSUS4 to listOf(
            Template(E, 0, 0, 2, 0, 2, 0, 0),
            Template(A, 1, X, 0, 2, 0, 3, 0),
            Template(D, 2, X, X, 0, 2, 1, 3),
        ),
        ChordType.HALFDIM to listOf(
            Template(E, 0, 0, X, 0, 0, -1, X),
            Template(A, 1, X, 0, 1, 0, 1, X),
            Template(D, 2, X, X, 0, 1, 1, 1),
        ),
        ChordType.DIM7 to listOf(
            Template(E, 0, 0, X, -1, 0, -1, X),
            Template(A, 1, X, 0, 1, -1, 1, X),
            Template(D, 2, X, X, 0, 1, 0, 1),
        ),
        ChordType.DIM to listOf(
            Template(A, 1, X, 0, 1, 2, 1, X),
            Template(D, 2, X, X, 0, 1, 3, 1),
        ),
    )

    /** Casa (0..11) da fundamental na corda [s]. */
    private fun rootFret(root: Int, s: Int): Int = Math.floorMod(root - Guitar.pitchClassAt(s, 0), 12)

    /** Coloca o molde no braço (na 1ª e na 2ª oitava), só onde todas as casas existem. */
    private fun place(t: Template, root: Int): List<IntArray> {
        val r0 = rootFret(root, t.rootString)
        return listOf(r0, r0 + 12).mapNotNull { r ->
            val frets = IntArray(Guitar.STRINGS) { s -> if (t.offsets[s] == X) -1 else r + t.offsets[s] }
            frets.takeIf { t.offsets.indices.all { s -> t.offsets[s] == X || frets[s] in 0..MAX_FRET } }
        }
    }

    /**
     * As formas mais usadas (CAGED), da mais perto da cabeça do violão para a
     * mais aguda. A forma que todo mundo aprende primeiro sempre entra.
     */
    fun common(chord: LabChord): List<NamedVoicing> {
        val found = LinkedHashMap<String, NamedVoicing>()
        if (chord.bass == null) {
            for (t in templates[chord.type].orEmpty()) {
                // Só a primeira posição válida de cada molde (a mesma forma 12 casas acima é repetição).
                val v = place(t, chord.root).firstNotNullOfOrNull { Voicings.evaluate(chord, it) } ?: continue
                found.putIfAbsent(v.key, NamedVoicing(v, t.name))
            }
        }
        // Completa com as melhores formas do gerador (para acordes com poucos moldes e inversões).
        if (found.size < 4) {
            for (v in Voicings.generate(chord, 6)) {
                if (found.size >= 5) break
                found.putIfAbsent(v.key, NamedVoicing(v, nameOf(v)))
            }
        }
        return found.values.sortedWith(compareBy({ order(it.voicing) }, { it.voicing.minFret }))
    }

    /**
     * Todas as formas tocáveis (até a casa [MAX_FRET]), da cabeça para o corpo
     * do violão. Formas que coincidem com um molde CAGED levam o nome dele.
     */
    fun all(chord: LabChord): List<NamedVoicing> {
        val named = common(chord).associateBy { it.voicing.key }
        val out = LinkedHashMap<String, NamedVoicing>()
        named.forEach { (k, v) -> out[k] = v }
        Voicings.allShapes(chord, MAX_FRET).forEach { v -> out.putIfAbsent(v.key, NamedVoicing(v, nameOf(v))) }
        // Da cabeça para o corpo do violão; na mesma casa, as mais cheias e confortáveis primeiro.
        return out.values.sortedWith(
            compareBy({ if (it.voicing.position == "Aberto") 0 else it.voicing.minFret }, { -it.voicing.score }),
        )
    }

    /** Ordem no braço: formas abertas primeiro, depois pela casa média. */
    private fun order(v: Voicing): Double =
        if (v.position == "Aberto") v.pressed.average().takeIf { !it.isNaN() } ?: 0.0
        else 5.0 + v.pressed.average()

    private val fingerNames = listOf("", "indicador", "médio", "anelar", "mindinho")

    /** "6ª", "6ª e 5ª", "4ª, 3ª e 1ª" */
    private fun stringList(ss: List<Int>): String {
        val names = ss.map { "${6 - it}ª" }
        return if (names.size <= 1) names.joinToString() else names.dropLast(1).joinToString(", ") + " e " + names.last()
    }

    /**
     * Como montar a forma, passo a passo (para quem está aprendendo):
     * pestana, cada dedo com corda e casa, cordas soltas e cordas que não tocam.
     */
    fun howTo(v: Voicing): List<String> {
        val out = mutableListOf<String>()
        val fg = v.fingering
        v.barre?.let { b ->
            out += "Dedo 1 (indicador) deitado na casa $b, da ${6 - v.barreFrom}ª até a ${6 - v.strings.last()}ª corda (pestana)."
        }
        for (finger in 1..4) {
            val ss = v.frets.indices.filter { fg[it] == finger && !v.underBarre(it) }
            if (ss.isEmpty()) continue
            val fret = v.frets[ss.first()]
            out += "Dedo $finger (${fingerNames[finger]}): ${stringList(ss)} corda, casa $fret."
        }
        val open = v.frets.indices.filter { v.frets[it] == 0 }
        if (open.isNotEmpty()) out += "Solta${if (open.size > 1) "s" else ""}: ${stringList(open)} corda${if (open.size > 1) "s" else ""}."
        val muted = v.frets.indices.filter { v.frets[it] < 0 }
        if (muted.isNotEmpty()) out += "Não toque: ${stringList(muted)} corda${if (muted.size > 1) "s" else ""}."
        out += "Toque da ${6 - v.strings.first()}ª corda para baixo" +
            (if (muted.any { it > v.strings.first() }) ", abafando a que não toca." else ".")
        return out
    }

    /** Nome genérico de uma forma: pela corda do baixo. */
    private fun nameOf(v: Voicing): String = "Baixo na ${6 - v.strings.first()}ª corda"
}
