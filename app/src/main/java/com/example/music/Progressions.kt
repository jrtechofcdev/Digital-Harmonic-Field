package com.example.music

/**
 * Uma progressão descrita por graus (índices 0..6 do campo). Ao abrir um tom,
 * os índices viram acordes reais daquele tom — então a mesma progressão serve
 * para qualquer tonalidade.
 */
data class Progression(
    val name: String,
    val roman: String,          // ex.: "I – V – vi – IV"
    val description: String,
    val degrees: List<Int>,
    val forMinor: Boolean,      // true = pensada para tons menores
)

object ProgressionLibrary {

    private val majorProgressions = listOf(
        Progression(
            name = "Eixo (pop/louvor)",
            roman = "I – V – vi – IV",
            description = "A base de boa parte do louvor contemporâneo. Sobe firme e resolve com aconchego.",
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
            name = "Cadência clássica",
            roman = "I – IV – V",
            description = "O caminho mais direto: repouso, preparação e tensão que pede a volta pra casa.",
            degrees = listOf(0, 3, 4),
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
            description = "A resolução preferida do jazz e dos arranjos mais elaborados.",
            degrees = listOf(1, 4, 0),
            forMinor = false,
        ),
        Progression(
            name = "Cânon (Pachelbel)",
            roman = "I – V – vi – iii – IV – I – IV – V",
            description = "A sequência do Cânon de Pachelbel: oito acordes que giram sozinhos.",
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
        ),
        Progression(
            name = "Cadência menor",
            roman = "i – iv – v",
            description = "O equivalente menor do I – IV – V: grave, direto e resolutivo.",
            degrees = listOf(0, 3, 4),
            forMinor = true,
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
}
