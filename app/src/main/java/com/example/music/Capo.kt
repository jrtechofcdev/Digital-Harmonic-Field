package com.example.music

/** Uma forma de tocar um tom com capotraste: a casa e o "desenho" aberto usado. */
data class CapoTip(val fret: Int, val shapeCipher: String, val shapeName: String)

/**
 * Capotraste: em que casa colocar o capo para tocar um tom com os acordes
 * abertos que o iniciante já sabe (formatos de Dó, Lá, Sol, Mi e Ré; no menor,
 * de Lá menor, Mi menor e Ré menor).
 */
object Capo {
    private val majorShapes = listOf("C" to "Dó", "A" to "Lá", "G" to "Sol", "E" to "Mi", "D" to "Ré")
    private val minorShapes = listOf("Am" to "Lá menor", "Em" to "Mi menor", "Dm" to "Ré menor")

    /** Todas as opções para o tom, da casa mais baixa para a mais alta. */
    fun options(key: String): List<CapoTip> {
        val root = rootPc(key) ?: return emptyList()
        val shapes = if (key.endsWith("m")) minorShapes else majorShapes
        return shapes.map { (cipher, name) ->
            CapoTip(Math.floorMod(root - (rootPc(cipher) ?: 0), 12), cipher, name)
        }.sortedBy { it.fret }
    }

    /** A melhor opção: a casa mais perto da cabeça do violão (0 = sem capo). */
    fun best(key: String): CapoTip? = options(key).firstOrNull()

    /** Frase curta para a tela: "Sem capotraste: acordes abertos" / "Capo na 2ª casa, formato de Lá (A)". */
    fun advice(key: String): String {
        val tip = best(key) ?: return ""
        return if (tip.fret == 0) "Sem capotraste: dá para tocar com acordes abertos"
        else "Capotraste na ${tip.fret}ª casa, com formato de ${tip.shapeName} (${tip.shapeCipher})"
    }
}
