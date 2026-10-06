package com.example.music

/**
 * Uma progressão descrita por graus (índices 0..6 do campo). Ao abrir um tom,
 * os índices viram acordes reais daquele tom — então a mesma progressão serve
 * para qualquer tonalidade.
 */
data class Progression(
    val name: String,
    val roman: String,          // ex.: "I – vi – ii – V – I"
    val description: String,
    val degrees: List<Int>,
    val forMinor: Boolean,      // true = pensada para tons menores
    val featured: Boolean = false, // aparece em destaque (ex.: a da Harpa)
    val tip: String = "",       // dica curta e prática para o iniciante
    val group: ProgressionGroup = ProgressionGroup.LOUVOR,
)

/** Como a aba Progressões organiza as sequências (na ordem em que aparecem). */
enum class ProgressionGroup(val title: String, val subtitle: String) {
    HINOS("Essenciais para hinos", "Comece por aqui: servem na maioria dos hinos da Harpa."),
    CADENCIAS("Finais de frase", "Como as frases e os hinos terminam — ajudam a achar a hora de voltar à tônica."),
    LOUVOR("Louvor e outras", "Sequências comuns em corinhos e no louvor contemporâneo."),
}

object ProgressionLibrary {

    private val majorProgressions = listOf(
        // A "volta" clássica dos hinos da Harpa Cristã. No tom de Sol: Sol – Em – Am – Ré – Sol.
        Progression(
            name = "Progressão da Harpa",
            roman = "I – vi – ii – V – I",
            description = "A volta clássica dos hinos: sai da tônica (a casa), passa pela relativa " +
                "menor, pela subdominante que prepara, cria tensão na dominante e resolve de novo na " +
                "tônica. No tom de Sol é Sol – Em – Am – Ré – Sol. Repare que Em → Am → Ré → Sol " +
                "descem de quinta em quinta: é o círculo das quintas girando.",
            degrees = listOf(0, 5, 1, 4, 0),
            forMinor = false,
            featured = true,
            tip = "Decore este caminho: serve na maioria dos hinos, só trocando as notas conforme o tom.",
            group = ProgressionGroup.HINOS,
        ),
        Progression(
            name = "Tríade 1 – 4 – 5",
            roman = "I – IV – V",
            description = "Os três acordes mais importantes do tom. Com eles você acompanha " +
                "um número enorme de hinos e louvores. No tom de Sol é Sol – Dó – Ré.",
            degrees = listOf(0, 3, 4),
            forMinor = false,
            featured = true,
            tip = "Se estiver perdido, tente 1, 4 e 5 do tom: quase sempre um deles encaixa.",
            group = ProgressionGroup.HINOS,
        ),
        Progression(
            name = "Estrofe de hino",
            roman = "I – IV – I – V – I",
            description = "O desenho de muitas estrofes: firma a tônica, passeia na subdominante, " +
                "volta, cria tensão na dominante e fecha em casa.",
            degrees = listOf(0, 3, 0, 4, 0),
            forMinor = false,
            group = ProgressionGroup.HINOS,
        ),
        Progression(
            name = "Gospel clássico",
            roman = "I – vi – IV – V",
            description = "Circular e cantável. Muito usada em hinos de coro e clássicos gospel.",
            degrees = listOf(0, 5, 3, 4),
            forMinor = false,
            group = ProgressionGroup.HINOS,
        ),
        Progression(
            name = "Cadência perfeita",
            roman = "V – I",
            description = "A dominante resolvendo na tônica: é o \"ponto final\" da frase. " +
                "Quando a melodia pede descanso, quase sempre vem um V – I.",
            degrees = listOf(4, 0),
            forMinor = false,
            group = ProgressionGroup.CADENCIAS,
        ),
        Progression(
            name = "Cadência do Amém",
            roman = "IV – I",
            description = "A subdominante indo para a tônica — o som do \"A-mém\" no fim dos hinos. " +
                "Mais suave que o V – I.",
            degrees = listOf(3, 0),
            forMinor = false,
            group = ProgressionGroup.CADENCIAS,
        ),
        Progression(
            name = "Cadência completa",
            roman = "ii – V – I",
            description = "Prepara (ii), tensiona (V) e resolve (I). A resolução preferida de arranjos " +
                "mais elaborados e de finais de refrão.",
            degrees = listOf(1, 4, 0),
            forMinor = false,
            group = ProgressionGroup.CADENCIAS,
        ),
        Progression(
            name = "Meia cadência",
            roman = "I – IV – V",
            description = "Para na dominante, como uma vírgula: a frase fica \"no ar\" esperando a " +
                "próxima. Comum no meio das estrofes.",
            degrees = listOf(0, 3, 4),
            forMinor = false,
            group = ProgressionGroup.CADENCIAS,
        ),
        Progression(
            name = "Eixo (louvor contemporâneo)",
            roman = "I – V – vi – IV",
            description = "A base de boa parte do louvor moderno. Sobe firme e resolve com aconchego.",
            degrees = listOf(0, 4, 5, 3),
            forMinor = false,
        ),
        Progression(
            name = "Balada emotiva",
            roman = "vi – IV – I – V",
            description = "Começa pela relativa menor: dá um ar reflexivo antes de abrir para a tônica.",
            degrees = listOf(5, 3, 0, 4),
            forMinor = false,
        ),
        Progression(
            name = "Descida de adoração",
            roman = "I – V – vi – iii – IV – I – IV – V",
            description = "O baixo desce passo a passo (como no Cânon de Pachelbel). Bonita em " +
                "momentos de adoração e introduções.",
            degrees = listOf(0, 4, 5, 2, 3, 0, 3, 4),
            forMinor = false,
        ),
    )

    private val minorProgressions = listOf(
        Progression(
            name = "Menor natural",
            roman = "i – VI – III – VII",
            description = "Sonoridade épica e melancólica, muito usada em pontes e músicas introspectivas.",
            degrees = listOf(0, 5, 2, 6),
            forMinor = true,
            featured = true,
            group = ProgressionGroup.HINOS,
        ),
        Progression(
            name = "Cadência menor",
            roman = "i – iv – v",
            description = "O equivalente menor do 1 – 4 – 5: grave, direto e resolutivo.",
            degrees = listOf(0, 3, 4),
            forMinor = true,
            featured = true,
            group = ProgressionGroup.HINOS,
        ),
        Progression(
            name = "Andaluza",
            roman = "i – VII – VI – VII",
            description = "Descida característica com tom dramático, comum em baladas.",
            degrees = listOf(0, 6, 5, 6),
            forMinor = true,
        ),
        Progression(
            name = "Ponte para a relativa",
            roman = "i – iv – VII – III",
            description = "Prepara a passagem para o tom maior relativo sem perder o clima menor.",
            degrees = listOf(0, 3, 6, 2),
            forMinor = true,
        ),
        Progression(
            name = "Final em menor",
            roman = "iv – i",
            description = "A subdominante menor resolvendo na tônica: o \"Amém\" dos hinos em tom menor.",
            degrees = listOf(3, 0),
            forMinor = true,
            group = ProgressionGroup.CADENCIAS,
        ),
    )

    fun forField(isMinor: Boolean): List<Progression> =
        if (isMinor) minorProgressions else majorProgressions

    /** Progressões em destaque para mostrar direto na tela do campo. */
    fun featuredFor(isMinor: Boolean): List<Progression> =
        forField(isMinor).filter { it.featured }
}
