package com.example.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import com.example.data.Live
import com.example.data.Repertoire
import com.example.data.SetItem
import com.example.data.Setlist
import com.example.ui.screens.PlayScreen

/** Volta o estado global ao padrão (tom Sol, sem repertório). */
internal fun resetLive() {
    Repertoire.lists.clear()
    Live.stop()
    Live.setKey("G")
}

/** Dois repertórios de exemplo, como num culto de verdade. */
internal fun demoSetlists(): Setlist {
    resetLive()
    val domingo = Setlist(
        1, "Culto de domingo",
        listOf(
            SetItem(1, 15, "Louvai a Jesus", "G", 0, "Começa só com violão", bpm = 72),
            SetItem(2, 291, "Mais perto quero estar", "D", 2, bpm = 64),
            SetItem(3, 545, "", "Am", 0, "Subir meio tom na última estrofe"),
            SetItem(4, 1, "Chuvas de graça", "C"),
        ),
    )
    Repertoire.lists.add(domingo)
    Repertoire.lists.add(Setlist(2, "Santa ceia", listOf(SetItem(1, 104, "", "E"), SetItem(2, 186, "", "F"))))
    return domingo
}

/** A aba Tocar com tudo no padrão (sem callbacks). */
@Composable
internal fun PlayForTest(padding: PaddingValues = PaddingValues()) =
    PlayScreen(padding, emptySet(), {}, {}, {}, {}, { _, _ -> }, {})
