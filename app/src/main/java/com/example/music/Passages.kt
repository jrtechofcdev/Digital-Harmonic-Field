package com.example.music

/**
 * Passagens e enfeites — as "graças" que o músico experiente faz entre um
 * acorde e outro do hino: a caída para voltar ao 1, o baixo descendo, a subida
 * com diminuto, o final de "Amém"…
 *
 * Cada passagem é escrita em GRAUS (distância da tônica), então vale para
 * qualquer tom. O app calcula os acordes, marca os que saem do campo harmônico
 * e toca o antes (direto) e o depois (com a passagem).
 */

enum class PassageLevel(val title: String) { BASICO("Básico"), MEDIO("Intermediário"), PRO("Profissional") }

/** Um acorde da passagem já no tom: rótulo em graus, acorde, função e se sai do campo. */
data class PassageStep(
    val label: String,
    val chord: LabChord,
    val function: HarmonicFunction,
    val outside: Boolean,
)

data class Passage(
    val name: String,
    val level: PassageLevel,
    val steps: List<PassageStep>,
    val why: String,
) {
    val outsideCount: Int get() = steps.count { it.outside }
}

/** Os momentos do hino em que cabe uma passagem. */
enum class PassageMove(val title: String, val minorTitle: String, val hint: String) {
    VOLTAR("Voltar ao 1", "Voltar ao 1m", "O fim da frase: a música vai repousar na tônica."),
    DESCER("Descer o baixo", "Descer o baixo", "O baixo desce um degrau por vez, sem pular."),
    UM_QUATRO("1 → 4", "1m → 4m", "A música sai da casa e vai para o 4."),
    UM_CINCO("1 → 5", "", "A música vai da casa para a tensão do 5."),
    UM_SEIS("1 → 6m", "1m → 3", "Vai para o relativo (o 6m no maior, o 3 no menor)."),
    UM_DOIS("1 → 2m", "", "A música vai da casa para o 2m."),
    QUATRO_CINCO("4 → 5", "", "Do 4 para o 5: o meio da frase, preparando a volta."),
    SUBIR("Subir", "", "Uma escada para crescer a música (refrão, última estrofe)."),
    FINAL("Final (Amém)", "Final (Amém)", "O fechamento do hino."),
}

object Passages {
    private val MAJOR_SCALE = setOf(0, 2, 4, 5, 7, 9, 11)
    // No tom menor, o 5 maior (com a sensível) é parte do jeito normal de tocar.
    private val MINOR_SCALE = setOf(0, 2, 3, 5, 7, 8, 10, 11)

    private const val T = 'T'
    private const val S = 'S'
    private const val D = 'D'

    /** Acorde escrito em graus: distância da tônica (semitons), tipo, função e baixo. */
    private class St(val label: String, val offset: Int, val type: ChordType, val fn: Char, val bass: Int? = null)

    private class Spec(val name: String, val level: PassageLevel, val why: String, vararg val steps: St)

    // ------------------------------------------------------------- tom maior
    private val I = St("1", 0, ChordType.MAJOR, T)
    private val I7 = St("1⁷", 0, ChordType.DOM7, D)
    private val I_3 = St("1/3", 0, ChordType.MAJOR, T, 4)
    private val I_5 = St("1/5", 0, ChordType.MAJOR, D, 7)
    private val IIm = St("2m", 2, ChordType.MINOR, S)
    private val IIm7 = St("2m⁷", 2, ChordType.MIN7, S)
    private val II7 = St("2⁷", 2, ChordType.DOM7, D)
    private val IIIm = St("3m", 4, ChordType.MINOR, T)
    private val IIIm7 = St("3m⁷", 4, ChordType.MIN7, T)
    private val III7 = St("3⁷", 4, ChordType.DOM7, D)
    private val IV = St("4", 5, ChordType.MAJOR, S)
    private val IVm = St("4m", 5, ChordType.MINOR, S)
    private val V = St("5", 7, ChordType.MAJOR, D)
    private val V7 = St("5⁷", 7, ChordType.DOM7, D)
    private val Vsus = St("5sus4", 7, ChordType.SUS4, D)
    private val V_7 = St("5/7", 7, ChordType.MAJOR, D, 11)
    private val VIm = St("6m", 9, ChordType.MINOR, T)
    private val VIm7 = St("6m⁷", 9, ChordType.MIN7, T)
    private val VI7 = St("6⁷", 9, ChordType.DOM7, D)
    private val dim1 = St("#1°", 1, ChordType.DIM7, D)
    private val dim2 = St("#2°", 3, ChordType.DIM7, D)
    private val dim4 = St("#4°", 6, ChordType.DIM7, D)

    private val major: Map<PassageMove, Pair<List<St>, List<Spec>>> = mapOf(
        PassageMove.VOLTAR to (listOf(V, I) to listOf(
            Spec("5 com sétima", PassageLevel.BASICO,
                "A sétima deixa o 5 mais \"pendurado\": a vontade de voltar ao 1 fica maior.", V7, I),
            Spec("4 - 5 - 1", PassageLevel.BASICO,
                "A cadência completa: prepara (4), tensiona (5) e repousa (1).", IV, V, I),
            Spec("2m - 5 - 1 (a caída mais usada)", PassageLevel.MEDIO,
                "O 2m prepara o 5 melhor que o 4. É a caída dos tocadores de igreja.", IIm7, V7, I),
            Spec("Segura no sus4", PassageLevel.MEDIO,
                "Toque o 5sus4 e solte para o 5: a tensão fica mais longa e a volta mais bonita.", Vsus, V7, I),
            Spec("Cadência de hinário (1/5)", PassageLevel.MEDIO,
                "O 1 com o baixo no 5 soa \"quase chegando\". É a caída dos corais e dos hinários.", I_5, V7, I),
            Spec("5 do 5: 2⁷ - 5⁷ - 1", PassageLevel.MEDIO,
                "O 2 vira maior com sétima e empurra para o 5, que empurra para o 1.", II7, V7, I),
            Spec("Ciclo: 6m - 2m - 5 - 1", PassageLevel.PRO,
                "Cada acorde puxa o próximo (de quinta em quinta). Ótimo para esticar o fim da estrofe.", VIm7, IIm7, V7, I),
            Spec("A volta longa: 3m - 6m - 2m - 5 - 1", PassageLevel.PRO,
                "O ciclo completo de quintas, sem sair do campo.", IIIm7, VIm7, IIm7, V7, I),
            Spec("Caída com cores", PassageLevel.PRO,
                "A mesma caída 2-5-1 com nona no 5 e 6(9) no 1: som de quarteto.",
                IIm7, St("5⁷⁽⁹⁾", 7, ChordType.DOM9, D), St("1⁶⁽⁹⁾", 0, ChordType.SIX9, T)),
        )),
        PassageMove.DESCER to (listOf(I, VIm) to listOf(
            Spec("1 - 5/7 - 6m", PassageLevel.BASICO,
                "O baixo desce 1 → 7 → 6, degrau por degrau, sem pular.", I, V_7, VIm),
            Spec("Descida de fim de hino", PassageLevel.BASICO,
                "4 - 1/3 - 2m - 1: o baixo desce 4 → 3 → 2 → 1 até a casa.", IV, I_3, IIm, I),
            Spec("1 - 5/7 - 6m - 1/5 - 4", PassageLevel.MEDIO,
                "A descida mais famosa: o baixo desce a escala até o 4.", I, V_7, VIm, St("1/5", 0, ChordType.MAJOR, T, 7), IV),
            Spec("Linha que desce por dentro", PassageLevel.MEDIO,
                "1 - 1⁷M - 1⁷ - 4: o baixo fica parado e uma nota desce 1 → 7 → b7 → 6.",
                I, St("1⁷M", 0, ChordType.MAJ7, T), I7, IV),
            Spec("A escada inteira", PassageLevel.PRO,
                "O baixo desce do 1 até o 1 de novo, passando por todos os degraus.",
                I, V_7, VIm, St("3m/5", 4, ChordType.MINOR, T, 7), IV, I_3, IIm, V7, I),
        )),
        PassageMove.UM_QUATRO to (listOf(I, IV) to listOf(
            Spec("1 - 1⁷ - 4", PassageLevel.BASICO,
                "O 1 com sétima vira o \"5 do 4\" e puxa direto para ele.", I, I7, IV),
            Spec("Baixo subindo", PassageLevel.BASICO,
                "1 - 1/3 - 4: o baixo sobe 1 → 3 → 4.", I, I_3, IV),
            Spec("Os dois juntos", PassageLevel.MEDIO,
                "1 - 1⁷/3 - 4: baixo subindo e a sétima puxando ao mesmo tempo.", I, St("1⁷/3", 0, ChordType.DOM7, D, 4), IV),
            Spec("2-5 do 4", PassageLevel.PRO,
                "1 - 5m⁷ - 1⁷ - 4: prepara o 4 como se ele fosse a nova casa.",
                I, St("5m⁷", 7, ChordType.MIN7, S), I7, IV),
        )),
        PassageMove.UM_CINCO to (listOf(I, V) to listOf(
            Spec("1 - 4 - 5", PassageLevel.BASICO, "Passa pelo 4 antes de chegar no 5.", I, IV, V),
            Spec("5 do 5", PassageLevel.BASICO,
                "1 - 2⁷ - 5: o 2 maior com sétima empurra para o 5.", I, II7, V),
            Spec("Baixo subindo até o 5", PassageLevel.MEDIO,
                "1 - 1/3 - 4 - 5: o baixo sobe 1 → 3 → 4 → 5.", I, I_3, IV, V),
            Spec("1 - 6m - 2⁷ - 5", PassageLevel.MEDIO,
                "Passa pelo 6m e usa o \"5 do 5\" para chegar.", I, VIm, II7, V),
            Spec("Subida de hinário", PassageLevel.PRO,
                "1 - 4 - #4° - 1/5 - 5: o baixo sobe 4 → #4 → 5 com um diminuto no meio.", I, IV, dim4, I_5, V),
        )),
        PassageMove.UM_SEIS to (listOf(I, VIm) to listOf(
            Spec("5 do 6m", PassageLevel.BASICO,
                "1 - 3⁷ - 6m: o 3 maior com sétima puxa para o 6m. Muito usado nos hinos.", I, III7, VIm),
            Spec("Baixo descendo", PassageLevel.BASICO,
                "1 - 5/7 - 6m: o baixo desce 1 → 7 → 6.", I, V_7, VIm),
            Spec("O 3m que vira maior", PassageLevel.MEDIO,
                "1 - 3m - 3⁷ - 6m: o mesmo acorde muda de menor para maior e a passagem \"chora\".", I, IIIm, III7, VIm),
            Spec("2-5 do 6m", PassageLevel.PRO,
                "1 - 7ø - 3⁷ - 6m: prepara o 6m como se fosse um tom menor.",
                I, St("7ø", 11, ChordType.HALFDIM, S), III7, VIm),
        )),
        PassageMove.UM_DOIS to (listOf(I, IIm) to listOf(
            Spec("5 do 2m", PassageLevel.BASICO,
                "1 - 6⁷ - 2m: o 6 maior com sétima puxa para o 2m.", I, VI7, IIm),
            Spec("Diminuto de passagem", PassageLevel.BASICO,
                "1 - #1° - 2m: o baixo sobe meio tom com um diminuto no meio.", I, dim1, IIm),
            Spec("1 - 6m - 2m", PassageLevel.BASICO, "Passa pelo 6m, sem sair do campo.", I, VIm, IIm),
            Spec("2-5 do 2m", PassageLevel.PRO,
                "1 - 3m⁷ - 6⁷ - 2m: o 3m e o 6⁷ preparam o 2m.", I, IIIm7, VI7, IIm),
        )),
        PassageMove.QUATRO_CINCO to (listOf(IV, V) to listOf(
            Spec("Diminuto subindo", PassageLevel.BASICO,
                "4 - #4° - 5: o baixo sobe 4 → #4 → 5.", IV, dim4, V),
            Spec("Cadência de hinário", PassageLevel.MEDIO,
                "4 - 1/5 - 5⁷: o 1 com baixo no 5 antes do 5.", IV, I_5, V7),
            Spec("A subida completa", PassageLevel.PRO,
                "4 - #4° - 1/5 - 5⁷: diminuto e cadência de hinário juntos.", IV, dim4, I_5, V7),
        )),
        PassageMove.SUBIR to (listOf(I, IV) to listOf(
            Spec("Escada pelo campo", PassageLevel.BASICO,
                "1 - 2m - 3m - 4: um degrau por vez, sem sair do campo.", I, IIm, IIIm, IV),
            Spec("Cadência de engano", PassageLevel.MEDIO,
                "4 - 5 - 6m: parece que vai voltar ao 1, mas vai para o 6m e a frase continua.", IV, V, VIm),
            Spec("Subindo com diminutos", PassageLevel.MEDIO,
                "1 - #1° - 2m - #2° - 3m: sobe de meio em meio tom.", I, dim1, IIm, dim2, IIIm),
            Spec("O baixo sobe até o 5 e volta", PassageLevel.PRO,
                "1 - 1/3 - 4 - #4° - 1/5 - 5⁷ - 1: a subida de hinário inteira.", I, I_3, IV, dim4, I_5, V7, I),
        )),
        PassageMove.FINAL to (listOf(V, I) to listOf(
            Spec("O Amém", PassageLevel.BASICO, "4 - 1: a cadência do \"Amém\", suave e sem tensão.", IV, I),
            Spec("4 com saudade", PassageLevel.BASICO,
                "4 - 4m - 1: o 4m vem emprestado do tom menor.", IV, IVm, I),
            Spec("Final completo", PassageLevel.MEDIO,
                "1 - 1⁷ - 4 - 4m - 1: leva ao 4 e fecha com o 4m.", I, I7, IV, IVm, I),
            Spec("Segura e abre", PassageLevel.MEDIO,
                "5sus4 - 5⁷ - 1(9): segura a tensão e termina com o 1 \"aberto\".",
                Vsus, V7, St("1(9)", 0, ChordType.ADD9, T)),
            Spec("Final de vitória", PassageLevel.PRO,
                "b6 - b7 - 1: dois acordes emprestados sobem até o 1. Final grandioso.",
                St("b6", 8, ChordType.MAJOR, S), St("b7", 10, ChordType.MAJOR, S), I),
            Spec("Amém de coral", PassageLevel.PRO,
                "4 - 4m⁶ - 1⁶⁽⁹⁾: o 4m⁶ é o \"Amém\" mais bonito dos corais.",
                IV, St("4m⁶", 5, ChordType.MIN6, S), St("1⁶⁽⁹⁾", 0, ChordType.SIX9, T)),
        )),
    )

    // ------------------------------------------------------------- tom menor
    private val i = St("1m", 0, ChordType.MINOR, T)
    private val iv = St("4m", 5, ChordType.MINOR, S)
    private val Vm = St("5", 7, ChordType.MAJOR, D)
    private val V7m = St("5⁷", 7, ChordType.DOM7, D)
    private val VI = St("6", 8, ChordType.MAJOR, S)
    private val VII = St("7", 10, ChordType.MAJOR, D)
    private val III = St("3", 3, ChordType.MAJOR, T)

    private val minor: Map<PassageMove, Pair<List<St>, List<Spec>>> = mapOf(
        PassageMove.VOLTAR to (listOf(Vm, i) to listOf(
            Spec("5 com sétima", PassageLevel.BASICO, "O 5 maior com sétima puxa forte para o 1m.", V7m, i),
            Spec("4m - 5 - 1m", PassageLevel.BASICO, "A cadência completa do tom menor.", iv, Vm, i),
            Spec("2-5 do tom menor", PassageLevel.MEDIO,
                "2ø - 5⁷ - 1m: o meio-diminuto prepara o 5 com suavidade.", St("2ø", 2, ChordType.HALFDIM, S), V7m, i),
            Spec("O 6 que desce", PassageLevel.MEDIO,
                "6 - 5⁷ - 1m: o 6 desce meio tom até o 5. Bem dramático.", VI, V7m, i),
            Spec("Segura e resolve", PassageLevel.PRO,
                "5sus4 - 5⁷ - 1m(9): segura a tensão e termina com nona.",
                St("5sus4", 7, ChordType.SUS4, D), V7m, St("1m(9)", 0, ChordType.MADD9, T)),
        )),
        PassageMove.DESCER to (listOf(i, VI) to listOf(
            Spec("1m - 1m/b7 - 6", PassageLevel.BASICO,
                "O baixo desce 1 → b7 → b6.", i, St("1m/b7", 0, ChordType.MINOR, T, 10), VI),
            Spec("Descida espanhola", PassageLevel.MEDIO,
                "1m - 7 - 6 - 5: a \"descida andaluza\", clima de hino de saudade.", i, St("7", 10, ChordType.MAJOR, S), VI, Vm),
            Spec("Lamento", PassageLevel.PRO,
                "1m - 1m/7 - 1m/b7 - 6: o baixo desce de meio em meio tom.",
                i, St("1m/7", 0, ChordType.MINOR, T, 11), St("1m/b7", 0, ChordType.MINOR, T, 10), VI),
        )),
        PassageMove.UM_QUATRO to (listOf(i, iv) to listOf(
            Spec("1 maior com sétima", PassageLevel.BASICO,
                "1m - 1⁷ - 4m: o 1 vira MAIOR com sétima e puxa para o 4m.", i, St("1⁷", 0, ChordType.DOM7, D), iv),
            Spec("2-5 do 4m", PassageLevel.PRO,
                "1m - 5ø - 1⁷ - 4m: prepara o 4m como se fosse a nova casa.",
                i, St("5ø", 7, ChordType.HALFDIM, S), St("1⁷", 0, ChordType.DOM7, D), iv),
        )),
        PassageMove.UM_SEIS to (listOf(i, III) to listOf(
            Spec("Para o relativo maior", PassageLevel.BASICO,
                "1m - 7 - 3: o 7 maior é o \"5 do 3\" e leva ao relativo maior.", i, VII, III),
            Spec("Caminho completo", PassageLevel.MEDIO,
                "1m - 4m - 7⁷ - 3: o 2-5-1 do relativo maior.", i, iv, St("7⁷", 10, ChordType.DOM7, D), III),
            Spec("Com cores", PassageLevel.PRO,
                "1m - 4m⁷ - 7⁷ - 3⁷M: o mesmo caminho com sétimas.",
                i, St("4m⁷", 5, ChordType.MIN7, S), St("7⁷", 10, ChordType.DOM7, D), St("3⁷M", 3, ChordType.MAJ7, T)),
        )),
        PassageMove.FINAL to (listOf(Vm, i) to listOf(
            Spec("O Amém", PassageLevel.BASICO, "4m - 1m: o \"Amém\" do tom menor.", iv, i),
            Spec("Terça de Picardia", PassageLevel.MEDIO,
                "4m - 5⁷ - 1: termina no 1 MAIOR e o hino \"abre\" no Amém. Clássico de igreja.",
                iv, V7m, St("1", 0, ChordType.MAJOR, T)),
            Spec("Final de vitória", PassageLevel.MEDIO, "6 - 7 - 1m: sobe até a casa.", VI, VII, i),
            Spec("Amém de coral", PassageLevel.PRO,
                "4m⁶ - 5⁷ - 1m(9): cores de coral no fechamento.",
                St("4m⁶", 5, ChordType.MIN6, S), V7m, St("1m(9)", 0, ChordType.MADD9, T)),
        )),
    )

    private fun table(minor: Boolean) = if (minor) this.minor else major

    /** Os momentos com passagens para o tipo de tom. */
    fun moves(minor: Boolean): List<PassageMove> = table(minor).keys.toList()

    private fun step(s: St, tonic: Int, minor: Boolean): PassageStep {
        val root = Math.floorMod(tonic + s.offset, 12)
        val bass = s.bass?.let { Math.floorMod(tonic + it, 12) }
        val chord = LabChord(root, s.type, bass)
        val scale = if (minor) MINOR_SCALE else MAJOR_SCALE
        val outside = (chord.pitchClasses + chord.bassPc).any { Math.floorMod(it - tonic, 12) !in scale }
        val fn = when (s.fn) { T -> HarmonicFunction.TONIC; S -> HarmonicFunction.SUBDOMINANT; else -> HarmonicFunction.DOMINANT }
        return PassageStep(s.label, chord, fn, outside)
    }

    /** O caminho "direto", sem enfeite (para comparar). */
    fun plain(move: PassageMove, tonic: Int, minor: Boolean): List<PassageStep> =
        table(minor)[move]?.first.orEmpty().map { step(it, tonic, minor) }

    /** As passagens de um momento, do básico ao profissional. */
    fun forMove(move: PassageMove, tonic: Int, minor: Boolean): List<Passage> =
        table(minor)[move]?.second.orEmpty().map { sp ->
            Passage(sp.name, sp.level, sp.steps.map { step(it, tonic, minor) }, sp.why)
        }.sortedBy { it.level.ordinal }
}
