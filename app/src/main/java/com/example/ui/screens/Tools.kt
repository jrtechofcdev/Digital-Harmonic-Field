package com.example.ui.screens

import com.example.HarmonicDatabase
import com.example.R
import com.example.music.ChordType
import com.example.music.LabChord
import com.example.music.pitchClassCiphers
import com.example.music.rootPc
import com.example.music.transposePtNote
import java.text.Normalizer

/**
 * As ferramentas do app. Todas abrem no tom do momento (Live): quem escolheu
 * Sol no Tocar encontra o braço, as passagens e as progressões já em Sol.
 * [short] é o rótulo dos atalhos em grade; [words] ajuda a busca da Biblioteca.
 */
enum class Tool(val short: String, val label: String, val subtitle: String, val icon: Int, val words: String) {
    PROGRESSOES("Progressões", "Progressões", "Sequências prontas da Harpa e do louvor, para tocar em tela cheia", R.drawable.ic_list_numbers, "progressao sequencia caminho cadencia harpa"),
    PASSAGENS("Passagens", "Passagens e enfeites", "Caídas para o 1, baixo descendo, diminutos e finais de Amém", R.drawable.ic_path, "passagem enfeite baixo descendo diminuto amem graca"),
    BRACO("Braço", "Braço do violão", "Toque numa nota e veja onde ela está em todo o braço", R.drawable.ic_guitar, "braco violao notas casa corda"),
    DICIONARIO("Dicionário", "Dicionário de acordes", "As formas mais usadas e todas as formas, com os dedos", R.drawable.ic_book, "dicionario acorde forma dedo pestana"),
    METRONOMO("Metrônomo", "Metrônomo", "Tempo e compasso, com batida visual e marcar no toque", R.drawable.ic_metronome, "metronomo bpm tempo andamento compasso"),
    CAPO("Capotraste", "Capotraste", "Em que casa colocar para tocar com acordes abertos", R.drawable.ic_capo, "capotraste capo casa aberto"),
    ACORDES("Acordes", "Acordes do tom no braço", "Escolha o grau: o acorde se monta com variações", R.drawable.ic_music_notes, "acordes tom grau variacao inversao"),
    HARMONIZAR("Harmonizar", "Harmonizar", "Trocar o acorde seco por um com mais cor: (9), 7M, sus4", R.drawable.ic_sparkle, "harmonizar cor nona setima sus"),
    DECORAR("Decorar", "Decorar o braço", "Treino rápido das notas, corda por corda", R.drawable.ic_target, "decorar treinar notas braco"),
    DETECTAR("Detectar", "Detectar o tom", "Aponte para quem canta: o tom em 5 a 15 segundos", R.drawable.ic_mic, "detectar tom microfone ouvir descobrir"),
    TREINAR("Treinar", "Treinar graus", "Perguntas rápidas de ouvido e de cifra", R.drawable.ic_learn, "treinar graus ouvido quiz exercicio"),
    ;

    companion object {
        /** Os atalhos da aba Tocar (em grade de 3). */
        val playGrid = listOf(PROGRESSOES, PASSAGENS, BRACO, DICIONARIO, METRONOMO, CAPO)
    }
}

/** Um resultado da busca da Biblioteca. */
sealed interface SearchHit {
    data class Key(val cipher: String, val name: String) : SearchHit
    data class Chord(val chord: LabChord, val name: String) : SearchHit
    data class ToolHit(val tool: Tool) : SearchHit
}

/**
 * Busca da Biblioteca: tons ("sol", "Am", "fá sustenido menor"), acordes
 * ("D7", "Bm7", "C7M", "G(9)") e ferramentas ("metrônomo", "capo"). Ignora
 * acentos e maiúsculas. Sem servidor: tudo vem dos dados do app.
 */
object LibrarySearch {
    private val aliases = mapOf(
        "maj7" to "7M", "7m" to "7M", "dim" to "°", "º" to "°", "o" to "°", "9" to "(9)", "add9" to "(9)",
        "m9" to "m(9)", "madd9" to "m(9)", "dim7" to "°7", "º7" to "°7", "ø" to "m7(b5)", "m7b5" to "m7(b5)",
        "79" to "7(9)", "7/9" to "7(9)", "sus" to "sus4", "7sus" to "7sus4",
    )

    fun normalize(s: String): String =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("\\s+sustenido"), "#")
            .replace(Regex("\\s+bemol"), "b")
            .replace(Regex("\\s+"), " ")
            .trim()

    /** Lê uma cifra digitada ("g", "F#m", "Bb7", "c7m") como acorde do dicionário. */
    fun parseChord(raw: String): LabChord? {
        val t = raw.trim().replace(" ", "")
        if (t.isEmpty() || !t[0].isLetter() || t[0].uppercaseChar() !in "CDEFGAB") return null
        val head = t[0].uppercaseChar().toString()
        val accidental = t.getOrNull(1)?.takeIf { it == '#' || it == 'b' }?.toString() ?: ""
        val root = rootPc(head + accidental) ?: return null
        val rest = t.substring(1 + accidental.length)
        val suffix = aliases[rest] ?: aliases[rest.lowercase()] ?: rest
        val type = ChordType.entries.firstOrNull { it.suffix == suffix } ?: return null
        return LabChord(root, type)
    }

    fun search(query: String): List<SearchHit> {
        val q = normalize(query)
        if (q.isEmpty()) return emptyList()
        val hits = mutableListOf<SearchHit>()

        // 1) Cifra exata: o tom (se for maior/menor) e o acorde no dicionário.
        parseChord(query)?.let { chord ->
            if (chord.type == ChordType.MAJOR || chord.type == ChordType.MINOR) {
                val k = pitchClassCiphers[chord.root] + chord.type.suffix
                hits += SearchHit.Key(k, HarmonicDatabase.ptNameByCipher[k] ?: k)
            }
            hits += SearchHit.Chord(chord, chordName(chord))
        }

        // 2) Tons pelo nome em português ("sol", "re menor", "fa#").
        val words = q.split(" ")
        HarmonicDatabase.allKeys().forEach { f ->
            val name = normalize(f.keyNamePt)
            val nameWords = name.split(" ")
            val match = name.startsWith(q) || words.all { w -> nameWords.any { it.startsWith(w) } }
            if (match && hits.none { it is SearchHit.Key && it.cipher == f.keyCipher }) {
                hits += SearchHit.Key(f.keyCipher, f.keyNamePt)
            }
        }

        // 3) Ferramentas (a partir de 2 letras, para "g" não trazer tudo).
        if (q.length >= 2) {
            Tool.entries.forEach { t ->
                val text = normalize(t.label + " " + t.words)
                if (text.split(" ").any { it.startsWith(q) } || text.contains(q)) hits += SearchHit.ToolHit(t)
            }
        }
        return hits
    }

    /** "Ré com sétima", "Si menor com sétima". */
    fun chordName(chord: LabChord): String = transposePtNote("Dó", chord.root) + " " + chord.type.ptName
}
