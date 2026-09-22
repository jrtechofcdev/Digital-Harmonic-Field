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
)

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
        ),
        Progression(
            name = "Tríade 1 – 4 – 5",
            roman = "I – IV – V",
            description = "Os três acordes mais importantes do tom. Com eles você acompanha " +
                "um número enorme de louvores. No tom de Sol é Sol – Dó – Ré.",
            degrees = listOf(0, 3, 4),
            forMinor = false,
            featured = true,
            tip = "Se estiver perdido, tente 1, 4 e 5 do tom: quase sempre um deles encaixa.",
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
            name = "Anos 50 / gospel",
            roman = "I – vi – IV – V",
            description = "Circular e cantável. Muito usada em hinos e clássicos.",
            degrees = listOf(0, 5, 3, 4),
            forMinor = false,
        ),
        Progression(
            name = "Cadência ii – V – I",
            roman = "ii – V – I",
            description = "A resolução preferida de arranjos mais elaborados.",
            degrees = listOf(1, 4, 0),
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
        ),
        Progression(
            name = "Cadência menor",
            roman = "i – iv – v",
            description = "O equivalente menor do 1 – 4 – 5: grave, direto e resolutivo.",
            degrees = listOf(0, 3, 4),
            forMinor = true,
            featured = true,
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
    )

    fun forField(isMinor: Boolean): List<Progression> =
        if (isMinor) minorProgressions else majorProgressions

    /** Progressões em destaque para mostrar direto na tela do campo. */
    fun featuredFor(isMinor: Boolean): List<Progression> =
        forField(isMinor).filter { it.featured }
}
