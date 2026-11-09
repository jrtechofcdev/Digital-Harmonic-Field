package com.example.music

/**
 * Fundo musical para a pregação: as sequências de acordes que o músico toca
 * baixinho enquanto a Palavra é pregada, na oração, no apelo, na Santa Ceia e
 * na volta para o hino.
 *
 * Regras do conteúdo (pedido do autor):
 * - Só acordes simples (maior, menor) e "cores" leves e fáceis de montar:
 *   (9), sus2, sus4, 7M, m7, 7 e acordes com baixo trocado (1/3, 4/5, 4/1).
 *   Nada de acordes complicados (diminuto com sétima, m7(9), 7M(9)…).
 * - Tudo escrito em GRAUS (distância da tônica), como as passagens: vale para
 *   os 24 tons e segue o tom do momento.
 * - Cada sequência diz a sensação (o porquê) e como tocar (tempo e dinâmica).
 *
 * Base: a prática dos tecladistas e violonistas de igreja (o "vai e vem" do 1
 * com o 4, notas longas, baixo parado, a progressão 4 - 5 - 3m - 6m do louvor
 * e as cadências de hinário), organizada por momento do culto.
 */

/** Os momentos do culto em que se toca um fundo. */
enum class PreachMoment(val title: String, val hint: String) {
    PREGACAO(
        "Pregação",
        "Fundo bem discreto enquanto a Palavra é pregada: poucos acordes, notas longas e volume baixo. A voz do pregador vem primeiro.",
    ),
    ORACAO(
        "Oração",
        "Para a igreja orar: acordes que repousam e quase não puxam para lugar nenhum. Nada de batida.",
    ),
    APELO(
        "Apelo",
        "O convite no fim da pregação: a música começa baixinha e cresce aos poucos, emocionando sem atrapalhar.",
    ),
    CEIA(
        "Ceia e ministração",
        "Santa Ceia, ministração e momentos de reflexão: som calmo e contemplativo, com o baixo andando devagar.",
    ),
    VOLTAR(
        "Voltar ao hino",
        "Para sair do fundo: anunciar o hino, fechar a oração ou encerrar o culto, chegando no tom certo.",
    ),
}

/** Uma sequência de fundo já no tom. */
data class PreachSequence(
    val id: String,
    val name: String,
    val moment: PreachMoment,
    val steps: List<PassageStep>,
    val feel: String,     // a sensação (o porquê)
    val howTo: String,    // como tocar
) {
    /** Usa algum acorde com "cor" ((9), sus, 7M, m7, 7 ou baixo trocado). */
    val colored: Boolean get() = steps.any { it.chord.type !in Preaching.PLAIN || it.chord.bass != null }
    val outsideCount: Int get() = steps.count { it.outside }
}

/** Um acorde de som bonito, comparado com o acorde simples, já no tom. */
data class ColorChord(
    val title: String,
    val plain: PassageStep,
    val colored: PassageStep,
    val sound: String,   // como soa
    val how: String,     // como montar (as notas no tom)
    val where: String,   // onde usar no fundo
)

object Preaching {
    /** Os tipos de acorde liberados nos fundos (nada complicado). */
    val ALLOWED = setOf(
        ChordType.MAJOR, ChordType.MINOR, ChordType.ADD9, ChordType.SUS2, ChordType.SUS4,
        ChordType.MAJ7, ChordType.MIN7, ChordType.DOM7,
    )
    /** Os acordes "secos" (sem cor). */
    val PLAIN = setOf(ChordType.MAJOR, ChordType.MINOR)

    private const val T = 'T'
    private const val S = 'S'
    private const val D = 'D'

    private class St(val label: String, val offset: Int, val type: ChordType, val fn: Char, val bass: Int? = null)
    private class Spec(val name: String, val moment: PreachMoment, val feel: String, val howTo: String, vararg val steps: St)

    // Ritmos que se repetem nas explicações.
    private const val LONGO = "Dois compassos em cada acorde (ou o tempo de uma frase do pregador). Teclado: cordas ou pad, notas longas. Violão: dedilhado lento, sem batida."
    private const val LENTO = "Um acorde a cada compasso, bem devagar. Teclado: notas longas. Violão: dedilhado suave."
    private const val CRESCE = "Comece só com o acorde, bem baixinho. A cada volta, aumente um pouco o volume ou o movimento. Quando a igreja começar a cantar, acompanhe."

    // ------------------------------------------------------------- tom maior
    private val I = St("1", 0, ChordType.MAJOR, T)
    private val I9 = St("1(9)", 0, ChordType.ADD9, T)
    private val Isus2 = St("1sus2", 0, ChordType.SUS2, T)
    private val Isus4 = St("1sus4", 0, ChordType.SUS4, T)
    private val I7M = St("1⁷M", 0, ChordType.MAJ7, T)
    private val I_3 = St("1/3", 0, ChordType.MAJOR, T, 4)
    private val I_5 = St("1/5", 0, ChordType.MAJOR, T, 7)
    private val IIm = St("2m", 2, ChordType.MINOR, S)
    private val IIm7 = St("2m⁷", 2, ChordType.MIN7, S)
    private val IIIm = St("3m", 4, ChordType.MINOR, T)
    private val IIIm7 = St("3m⁷", 4, ChordType.MIN7, T)
    private val IV = St("4", 5, ChordType.MAJOR, S)
    private val IV9 = St("4(9)", 5, ChordType.ADD9, S)
    private val IV7M = St("4⁷M", 5, ChordType.MAJ7, S)
    private val IV_1 = St("4/1", 5, ChordType.MAJOR, S, 0)
    private val IV_5 = St("4/5", 5, ChordType.MAJOR, D, 7)
    private val IVm = St("4m", 5, ChordType.MINOR, S)
    private val V = St("5", 7, ChordType.MAJOR, D)
    private val V7 = St("5⁷", 7, ChordType.DOM7, D)
    private val Vsus = St("5sus4", 7, ChordType.SUS4, D)
    private val V_7 = St("5/7", 7, ChordType.MAJOR, D, 11)
    private val V_1 = St("5/1", 7, ChordType.MAJOR, D, 0)
    private val VIm = St("6m", 9, ChordType.MINOR, T)
    private val VIm7 = St("6m⁷", 9, ChordType.MIN7, T)
    private val bVII = St("b7", 10, ChordType.MAJOR, S)

    private val major = listOf(
        // Pregação
        Spec("1 - 4: o vai e vem", PreachMoment.PREGACAO,
            "Casa e passeio. É a base de todo fundo musical: não cansa, não tem tensão e não chama atenção.", LONGO, I, IV),
        Spec("Baixo parado: 1 - 4/1", PreachMoment.PREGACAO,
            "O baixo fica no 1 o tempo todo e só o acorde de cima muda. Dá sensação de chão firme e quase não se percebe.",
            "Mão esquerda (ou o baixo) segura a nota do 1 sem parar. Troque o acorde de cima bem devagar.", I, IV_1),
        Spec("1(9) - 4(9): o vai e vem com brilho", PreachMoment.PREGACAO,
            "O mesmo vai e vem, agora com nona: um brilho suave, o som dos fundos de louvor de hoje.", LONGO, I9, IV9),
        Spec("1 - 6m: claro e escuro", PreachMoment.PREGACAO,
            "Alterna o claro (1) com o mais sério (6m), sem tensão nenhuma. Combina com uma pregação reflexiva.", LONGO, I, VIm),
        Spec("6m - 4: em aberto", PreachMoment.PREGACAO,
            "Dois acordes que não resolvem: a música fica em suspenso, esperando a Palavra.", LONGO, VIm, IV),
        Spec("Um acorde vivo: 1 - 1sus4 - 1 - 1sus2", PreachMoment.PREGACAO,
            "Quase um acorde só, mas uma nota se mexe. Bom quando o pregador conta uma história e a música não pode distrair.",
            "Deixe a mão no acorde do 1 e mexa só um dedo: a 3ª sobe para a 4ª, volta, desce para a 2ª. Bem devagar.", I, Isus4, I, Isus2),
        Spec("1 - 4 - 6m - 4", PreachMoment.PREGACAO,
            "Um pouco mais de movimento, mas sem o 5: a música anda e nunca fica tensa.", LONGO, I, IV, VIm, IV),
        Spec("1 - 3m - 4 - 1", PreachMoment.PREGACAO,
            "Caminha 1, 3, 4 e volta para casa: movimento suave, sem nenhuma tensão.", LENTO, I, IIIm, IV, I),

        // Oração
        Spec("4 - 1: o amém", PreachMoment.ORACAO,
            "A cadência do \"Amém\": repouso sem tensão. Perfeita para a igreja orar.", LONGO, IV, I),
        Spec("4 - 4m - 1: o aperto e o alívio", PreachMoment.ORACAO,
            "Por um instante o 4 vira menor: dá um aperto no coração e um alívio na volta ao 1. Clássico de oração e de Santa Ceia.",
            "Um compasso no 4, um no 4m e dois no 1. No 4m só uma nota muda: a 3ª do acorde desce meio tom.", IV, IVm, I),
        Spec("Baixo parado: 1 - 4/1 - 5/1 - 1", PreachMoment.ORACAO,
            "O baixo não sai do 1 e os acordes passam por cima. A igreja sente segurança para orar.",
            "Segure a nota do 1 no baixo do começo ao fim. Troque só os acordes de cima, com calma.", I, IV_1, V_1, I),
        Spec("6m⁷ - 4(9): macio e aberto", PreachMoment.ORACAO,
            "Dois acordes macios, sem começo nem fim. Ótimo para clamor e oração longa.", LONGO, VIm7, IV9),
        Spec("1 - 3m - 6m - 4", PreachMoment.ORACAO,
            "Desce suave até o 6m e respira no 4. Calma e profunda.", LENTO, I, IIIm, VIm, IV),
        Spec("2m⁷ - 4/5 - 1: a caída macia", PreachMoment.ORACAO,
            "O 4/5 faz o papel do 5, mas sem a tensão forte: chama o 1 com carinho.",
            "No 4/5, toque o acorde do 4 e, no baixo, a nota do 5. Segure o 1 no fim.", IIm7, IV_5, I),
        Spec("Baixo descendo: 1 - 5/7 - 6m - 4", PreachMoment.ORACAO,
            "O baixo desce 1, 7, 6 e 4: sensação de quebrantamento. Muito usado em oração.",
            "Deixe o baixo bem claro (é ele que desce). Um compasso em cada acorde.", I, V_7, VIm, IV),

        // Apelo
        Spec("1 - 5 - 6m - 4: a do louvor", PreachMoment.APELO,
            "A sequência mais usada no louvor de hoje. Abre, cresce e emociona; repita quantas vezes precisar.", CRESCE, I, V, VIm, IV),
        Spec("6m - 4 - 1 - 5: o clamor", PreachMoment.APELO,
            "A mesma ideia começando no 6m: mais intensa, com cara de clamor.", CRESCE, VIm, IV, I, V),
        Spec("4 - 5 - 3m - 6m: a entrega", PreachMoment.APELO,
            "Sobe (4, 5) e cai no menor (3m, 6m): sensação de entrega e quebrantamento. Muito usada em apelo e ministração.", CRESCE, IV, V, IIIm, VIm),
        Spec("4 - 5 - 6m: a escada", PreachMoment.APELO,
            "Sobe três degraus e para no 6m. A cada volta, um pouco mais forte.", CRESCE, IV, V, VIm),
        Spec("Baixo subindo: 1 - 1/3 - 4 - 5", PreachMoment.APELO,
            "O baixo sobe 1, 3, 4, 5: sensação de levantar, de crescer. Bom quando o pregador faz o convite.", CRESCE, I, I_3, IV, V),
        Spec("A escada com cor: 4(9) - 5sus4 - 5 - 6m⁷", PreachMoment.APELO,
            "A escada do apelo com cores: o sus4 segura a emoção um instante antes de cair no 6m.", CRESCE, IV9, Vsus, V, VIm7),
        Spec("1 - b7 - 4 - 1: esperança", PreachMoment.APELO,
            "O b7 é um acorde maior de fora do campo (em Dó, o Si bemol). Dá um ar de vitória e esperança no fim do apelo.",
            "Use quando a igreja já estiver respondendo. O b7 fica um tom abaixo do 1.", I, bVII, IV, I),
        Spec("A grande descida: 1 - 5/7 - 6m - 1/5 - 4 - 1/3 - 2m - 5", PreachMoment.APELO,
            "O baixo desce a escala inteira, degrau por degrau. Emociona sem pressa; serve para apelos longos.",
            "Um compasso em cada acorde. O baixo desce sempre um degrau; no fim, o 5 leva de volta ao começo.",
            I, V_7, VIm, I_5, IV, I_3, IIm, V),

        // Ceia e ministração
        Spec("1⁷M - 4⁷M: sonhador", PreachMoment.CEIA,
            "Suave e sonhador. Para momentos de reflexão e para a Santa Ceia.", LONGO, I7M, IV7M),
        Spec("A descida suave: 4 - 1/3 - 2m - 1", PreachMoment.CEIA,
            "O baixo desce 4, 3, 2, 1 até a casa: calmo, como um suspiro.", LENTO, IV, I_3, IIm, I),
        Spec("1 - 3m - 4 - 4m: contemplação", PreachMoment.CEIA,
            "Sobe tranquilo e termina no 4m: profundo, de contemplação. Volte ao 1 para recomeçar.", LENTO, I, IIIm, IV, IVm),
        Spec("6m - 3m - 4 - 1", PreachMoment.CEIA,
            "Começa sério e termina em paz.", LENTO, VIm, IIIm, IV, I),
        Spec("Baixo subindo devagar: 1 - 2m - 1/3 - 4", PreachMoment.CEIA,
            "O baixo sobe degrau por degrau (1, 2, 3, 4). Ótimo para a ministração.", LENTO, I, IIm, I_3, IV),
        Spec("2m⁷ - 3m⁷ - 4 - 5sus4: expectativa", PreachMoment.CEIA,
            "Sobe pelos acordes do campo e segura no sus4: fica uma expectativa no ar.", LENTO, IIm7, IIIm7, IV, Vsus),
        Spec("1 - 6m - 2m - 5: o ciclo do hinário", PreachMoment.CEIA,
            "O caminho clássico dos hinários: anda sem pressa e sempre volta para casa.", LENTO, I, VIm, IIm, V),

        // Voltar ao hino
        Spec("5sus4 - 5: anunciar o hino", PreachMoment.VOLTAR,
            "Segure o 5sus4 e solte para o 5: a igreja percebe que o hino vai começar. Depois, entre no 1 do hino.",
            "Use quando o pregador estiver terminando. Fique no 5 até o dirigente anunciar o hino.", Vsus, V),
        Spec("4 - 5 - 1: a cadência", PreachMoment.VOLTAR,
            "Prepara (4), tensiona (5) e chega (1). Fecha o fundo e começa o hino no tom.",
            "Um compasso em cada acorde, crescendo um pouco até o 1.", IV, V, I),
        Spec("2m - 5 - 1: a caída", PreachMoment.VOLTAR,
            "A caída dos tocadores de igreja: suave e certeira.",
            "Um compasso em cada acorde. No 5, pode colocar a sétima (5⁷) para puxar mais.", IIm, V, I),
        Spec("1/5 - 5⁷ - 1: final de hinário", PreachMoment.VOLTAR,
            "Soa como o fim dos hinos de coral: firme e conhecido de todo mundo.",
            "Toque o 1 com o baixo no 5, depois o 5⁷, e termine no 1 segurando bem.", I_5, V7, I),
        Spec("1 - 4 - 1: o amém do fim", PreachMoment.VOLTAR,
            "O amém do encerramento: simples e tranquilo.",
            "Dois compassos em cada acorde, diminuindo até o silêncio.", I, IV, I),
    )

    // ------------------------------------------------------------- tom menor
    private val i = St("1m", 0, ChordType.MINOR, T)
    private val i7 = St("1m⁷", 0, ChordType.MIN7, T)
    private val i_b7 = St("1m/b7", 0, ChordType.MINOR, T, 10)
    private val III = St("3", 3, ChordType.MAJOR, T)
    private val III9 = St("3(9)", 3, ChordType.ADD9, T)
    private val iv = St("4m", 5, ChordType.MINOR, S)
    private val iv7 = St("4m⁷", 5, ChordType.MIN7, S)
    private val iv_1 = St("4m/1", 5, ChordType.MINOR, S, 0)
    private val Vmaj = St("5", 7, ChordType.MAJOR, D)
    private val Vmsus = St("5sus4", 7, ChordType.SUS4, D)
    private val VI = St("6", 8, ChordType.MAJOR, S)
    private val VI9 = St("6(9)", 8, ChordType.ADD9, S)
    private val VI7M = St("6⁷M", 8, ChordType.MAJ7, S)
    private val VII = St("7", 10, ChordType.MAJOR, D)

    private val minor = listOf(
        // Pregação
        Spec("1m - 4m: o vai e vem do menor", PreachMoment.PREGACAO,
            "O vai e vem do tom menor: sério e calmo, sem tensão.", LONGO, i, iv),
        Spec("1m - 6: um raio de luz", PreachMoment.PREGACAO,
            "Do 1m para o 6 maior: um pouco de luz no meio do clima sério.", LONGO, i, VI),
        Spec("Baixo parado: 1m - 4m/1", PreachMoment.PREGACAO,
            "O baixo fica no 1 e só o acorde de cima muda. Chão firme para a pregação.",
            "Segure a nota do 1 no baixo e troque o acorde de cima bem devagar.", i, iv_1),
        Spec("1m⁷ - 4m⁷: o menor macio", PreachMoment.PREGACAO,
            "Com sétima, o menor fica mais macio e menos triste.", LONGO, i7, iv7),

        // Oração
        Spec("4m - 1m: o amém do menor", PreachMoment.ORACAO,
            "O \"Amém\" do tom menor: repouso, sem tensão.", LONGO, iv, i),
        Spec("6(9) - 3(9): luz e esperança", PreachMoment.ORACAO,
            "Os acordes maiores do tom menor, com nona: luz e esperança no meio da oração.", LONGO, VI9, III9),
        Spec("O lamento: 1m - 1m/b7 - 6 - 5", PreachMoment.ORACAO,
            "O baixo desce 1, b7, b6, 5: saudade e clamor.",
            "Deixe o baixo bem claro. Um compasso em cada acorde e volte ao 1m.", i, i_b7, VI, Vmaj),

        // Apelo
        Spec("1m - 6 - 3 - 7: a do louvor no menor", PreachMoment.APELO,
            "Em Lá menor: Am, F, C, G. A sequência mais usada no louvor em tom menor.", CRESCE, i, VI, III, VII),
        Spec("6 - 7 - 1m: a subida", PreachMoment.APELO,
            "Sobe dois degraus e chega no 1m: clássico de clamor.", CRESCE, VI, VII, i),
        Spec("A descida espanhola: 1m - 7 - 6 - 5", PreachMoment.APELO,
            "Desce 1, 7, 6, 5: dramática e forte. O 5 maior pede a volta ao 1m.", CRESCE, i, VII, VI, Vmaj),
        Spec("4m - 6 - 7 - 1m: a escada do menor", PreachMoment.APELO,
            "Sobe até o 1m como uma escada. A cada volta, um pouco mais forte.", CRESCE, iv, VI, VII, i),

        // Ceia e ministração
        Spec("1m - 3 - 4m - 1m", PreachMoment.CEIA,
            "Passeia pelo relativo maior (3) e volta: calmo e reflexivo.", LENTO, i, III, iv, i),
        Spec("6⁷M - 7 - 1m: sonhador", PreachMoment.CEIA,
            "O 6 com sétima maior é suave e sonhador; o 7 leva de volta ao 1m.", LENTO, VI7M, VII, i),
        Spec("1m - 6 - 4m - 5", PreachMoment.CEIA,
            "Contemplativo, com a volta pelo 5 maior (que puxa para o 1m).", LENTO, i, VI, iv, Vmaj),

        // Voltar ao hino
        Spec("4m - 5 - 1m: a cadência", PreachMoment.VOLTAR,
            "A cadência completa do tom menor. O 5 maior puxa com força para o 1m.",
            "Um compasso em cada acorde, crescendo até o 1m.", iv, Vmaj, i),
        Spec("5sus4 - 5: anunciar o hino", PreachMoment.VOLTAR,
            "Segure o 5sus4 e solte para o 5: o hino vai começar.",
            "Fique no 5 até o dirigente anunciar. O hino começa no 1m.", Vmsus, Vmaj),
        Spec("6 - 5 - 1m: final dramático", PreachMoment.VOLTAR,
            "O 6 desce meio tom até o 5 e resolve no 1m: um fechamento forte.",
            "Segure o 5 um pouco mais e termine no 1m, diminuindo.", VI, Vmaj, i),
    )

    // ------------------------------------------------------------- acordes bonitos
    private class ColorSpec(val title: String, val plain: St, val colored: St, val sound: String, val how: (Int) -> String, val where: String)

    private val majorColors = listOf(
        ColorSpec("Com nona (9)", I, I9,
            "Brilho e paz. É o som dos fundos de louvor de hoje.",
            { r -> "Junte ao acorde a 2ª nota da escala dele (a nona). Em ${ptNote(r)}: ${ptNote(r)}, ${ptNote(r + 4)}, ${ptNote(r + 7)} + ${ptNote(r + 2)}." },
            "No 1 e no 4, em fundos de pregação e oração."),
        ColorSpec("Suspenso (sus4)", V, Vsus,
            "Uma tensão que pede para resolver. Soltando para o 5 normal, dá alívio.",
            { r -> "Troque a 3ª do acorde pela 4ª. ${ptNote(r + 7)} (${ptNote(r + 7)}, ${ptNote(r + 11)}, ${ptNote(r + 2)}) vira ${ptNote(r + 7)}, ${ptNote(r)}, ${ptNote(r + 2)}." },
            "No 5, antes de voltar ao 1 ou para anunciar o hino."),
        ColorSpec("Suspenso com 2ª (sus2)", I, Isus2,
            "Aberto e flutuante: nem alegre nem triste. Deixa o ar parado, bom para orar.",
            { r -> "Troque a 3ª pela 2ª: ${ptNote(r)}, ${ptNote(r + 2)}, ${ptNote(r + 7)}." },
            "Num acorde parado, na oração ou no começo da ministração."),
        ColorSpec("Sétima maior (7M)", IV, IV7M,
            "Suave e sonhador, sem tensão.",
            { r -> "Junte a nota meio tom abaixo da fundamental: ${ptNote(r + 5)}, ${ptNote(r + 9)}, ${ptNote(r)} + ${ptNote(r + 4)}." },
            "No 1 e no 4, em momentos calmos (Ceia, reflexão)."),
        ColorSpec("Menor com sétima (m7)", VIm, VIm7,
            "O menor fica mais macio e menos triste.",
            { r -> "Junte a nota um tom abaixo da fundamental: ${ptNote(r + 9)}, ${ptNote(r)}, ${ptNote(r + 4)} + ${ptNote(r + 7)}." },
            "No 6m e no 2m, em qualquer fundo."),
        ColorSpec("O 4 com baixo no 5 (4/5)", V, IV_5,
            "Faz o papel do 5, mas sem a tensão forte: chama o 1 com carinho.",
            { r -> "Toque o acorde do 4 (${ptNote(r + 5)}) e, no baixo, a nota do 5 (${ptNote(r + 7)})." },
            "No lugar do 5, antes de voltar ao 1 no fundo da pregação."),
        ColorSpec("Baixo na terça (1/3)", I, I_3,
            "O mesmo acorde, mas o baixo anda: liga um acorde ao outro sem pulo.",
            { r -> "Toque o acorde de ${ptNote(r)} com ${ptNote(r + 4)} no baixo." },
            "Entre o 1 e o 4 (1 - 1/3 - 4): o baixo sobe 1, 3, 4."),
        ColorSpec("Baixo parado (pedal)", IV, IV_1,
            "O baixo fica no 1 enquanto os acordes mudam por cima: chão firme, não distrai.",
            { r -> "Segure ${ptNote(r)} no baixo e toque o acorde de ${ptNote(r + 5)} por cima." },
            "Na oração e na pregação (1 - 4/1 - 5/1 - 1)."),
        ColorSpec("O 4 menor (4m)", IV, IVm,
            "Um aperto no coração que se resolve no 1. Clássico de Ceia e oração.",
            { r -> "Abaixe meio tom a 3ª do 4: ${ptNote(r + 5)}, ${ptNote(r + 9)}, ${ptNote(r)} vira ${ptNote(r + 5)}, ${ptNote(r + 8)}, ${ptNote(r)}." },
            "Entre o 4 e o 1 (4 - 4m - 1)."),
    )

    private val minorColors = listOf(
        ColorSpec("Menor com sétima (m7)", i, i7,
            "O menor fica macio, menos triste.",
            { r -> "Junte a nota um tom abaixo da fundamental: ${ptNote(r)}, ${ptNote(r + 3)}, ${ptNote(r + 7)} + ${ptNote(r + 10)}." },
            "No 1m e no 4m, em qualquer fundo."),
        ColorSpec("O 5 maior", St("5m", 7, ChordType.MINOR, D), Vmaj,
            "No tom menor, o 5 maior (com a sensível) puxa com força para o 1m.",
            { r -> "Suba meio tom a 3ª do 5: ${ptNote(r + 7)}, ${ptNote(r + 10)}, ${ptNote(r + 2)} vira ${ptNote(r + 7)}, ${ptNote(r + 11)}, ${ptNote(r + 2)}." },
            "Antes de voltar ao 1m e no fim das sequências."),
        ColorSpec("Suspenso (sus4)", Vmaj, Vmsus,
            "Segura a tensão antes do 5: anuncia que algo vai acontecer.",
            { r -> "Troque a 3ª do 5 pela 4ª: ${ptNote(r + 7)}, ${ptNote(r)}, ${ptNote(r + 2)}." },
            "Para anunciar o hino ou antes do fim."),
        ColorSpec("Com nona (9)", VI, VI9,
            "Os acordes maiores do tom menor ganham brilho e esperança.",
            { r -> "Junte a 2ª nota da escala do acorde: ${ptNote(r + 8)}, ${ptNote(r)}, ${ptNote(r + 3)} + ${ptNote(r + 10)}." },
            "No 6 e no 3, em oração e apelo."),
        ColorSpec("Sétima maior (7M)", VI, VI7M,
            "Suave e sonhador.",
            { r -> "Junte a nota meio tom abaixo da fundamental: ${ptNote(r + 8)}, ${ptNote(r)}, ${ptNote(r + 3)} + ${ptNote(r + 7)}." },
            "No 6, em momentos calmos."),
        ColorSpec("Baixo parado (pedal)", iv, iv_1,
            "O baixo fica no 1 enquanto o acorde muda por cima: chão firme.",
            { r -> "Segure ${ptNote(r)} no baixo e toque o acorde de ${ptNote(r + 5)} menor por cima." },
            "Na pregação e na oração (1m - 4m/1)."),
    )

    private fun ptNote(pc: Int): String = transposePtNote("Dó", Math.floorMod(pc, 12))

    private fun fnOf(c: Char) = when (c) { T -> HarmonicFunction.TONIC; S -> HarmonicFunction.SUBDOMINANT; else -> HarmonicFunction.DOMINANT }
    private fun step(s: St, tonic: Int, minor: Boolean) = Passages.degreeStep(s.label, s.offset, s.type, fnOf(s.fn), s.bass, tonic, minor)

    private fun specs(minor: Boolean) = if (minor) this.minor else major

    /** Todas as sequências do tom (maior ou menor), na ordem dos momentos. */
    fun all(tonic: Int, minor: Boolean): List<PreachSequence> =
        specs(minor).mapIndexed { idx, sp ->
            PreachSequence(
                id = (if (minor) "m" else "M") + idx,
                name = sp.name,
                moment = sp.moment,
                steps = sp.steps.map { step(it, tonic, minor) },
                feel = sp.feel,
                howTo = sp.howTo,
            )
        }

    fun forMoment(moment: PreachMoment, tonic: Int, minor: Boolean): List<PreachSequence> =
        all(tonic, minor).filter { it.moment == moment }

    fun byId(id: String, tonic: Int, minor: Boolean): PreachSequence? = all(tonic, minor).firstOrNull { it.id == id }

    /** Os acordes de som bonito, explicados no tom. */
    fun colors(tonic: Int, minor: Boolean): List<ColorChord> =
        (if (minor) minorColors else majorColors).map { c ->
            ColorChord(c.title, step(c.plain, tonic, minor), step(c.colored, tonic, minor), c.sound, c.how(tonic), c.where)
        }
}
