package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.Live
import com.example.data.Repertoire
import com.example.data.SetItem
import com.example.music.Capo
import com.example.music.ChordType
import com.example.ui.screens.LibrarySearch
import com.example.ui.screens.SearchHit
import com.example.ui.screens.Tool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Repertórios (várias listas), o tom do momento (Live), o capotraste e a busca
 * da Biblioteca. Tudo salva no aparelho e volta igual.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RepertoireTest {
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()

    @Before fun clean() {
        ctx.getSharedPreferences("repertorio", Context.MODE_PRIVATE).edit().clear().commit()
        ctx.getSharedPreferences("ao_vivo", Context.MODE_PRIVATE).edit().clear().commit()
        Repertoire.init(ctx)
        Live.init(ctx)
    }

    @Test fun listasSalvamReordenamEVoltamIguais() {
        val list = Repertoire.createList("Culto de domingo")
        Repertoire.addItem(list.id, SetItem(Repertoire.nextItemId(list.id), 15, "Louvai a Jesus", "G", 0, "só violão", bpm = 72))
        Repertoire.addItem(list.id, SetItem(Repertoire.nextItemId(list.id), null, "Sem número", "Am", 2))
        Repertoire.addItem(list.id, SetItem(Repertoire.nextItemId(list.id), 545, "", "D"))
        Repertoire.moveItem(list.id, 3, -1)                 // o 3º sobe para 2º
        Repertoire.init(ctx)                                 // lê de novo do aparelho
        val items = Repertoire.get(list.id)!!.items
        assertEquals(listOf(1L, 3L, 2L), items.map { it.id })
        assertEquals("Hino 15 · Louvai a Jesus", items[0].heading)
        assertEquals("Hino 545", items[1].heading)
        assertEquals("Sem número", items[2].heading)
        assertEquals(2, items[2].capo)
        assertEquals("Am", items[2].key)
        assertEquals("só violão", items[0].note)
        assertEquals(72, items[0].bpm)
        assertNull(items[1].bpm)

        val copy = Repertoire.duplicateList(list.id)!!
        assertEquals("Culto de domingo (cópia)", copy.name)
        assertEquals(3, copy.items.size)
        Repertoire.renameList(copy.id, "  Santa ceia ")
        Repertoire.removeItem(list.id, 1)
        Repertoire.init(ctx)
        assertEquals("Santa ceia", Repertoire.get(copy.id)!!.name)
        assertEquals(2, Repertoire.get(list.id)!!.items.size)
        assertEquals(3, Repertoire.get(copy.id)!!.items.size)   // a cópia não muda junto
        Repertoire.deleteList(copy.id)
        assertNull(Repertoire.get(copy.id))
        assertEquals(emptyList<SetItem>(), Repertoire.itemsFromJson("lixo"))
    }

    @Test fun listaAntigaViraMeuRepertorio() {
        val legacy = Repertoire.itemsToJson(listOf(SetItem(1, 15, "Louvai a Jesus", "G"), SetItem(2, 291, "", "Am", 2)))
        ctx.getSharedPreferences("repertorio", Context.MODE_PRIVATE).edit().clear().putString("itens", legacy).commit()
        Repertoire.init(ctx)
        assertEquals(1, Repertoire.lists.size)
        assertEquals("Meu repertório", Repertoire.lists[0].name)
        assertEquals(listOf("G", "Am"), Repertoire.lists[0].items.map { it.key })
        // A migração acontece uma vez só: a lista antiga sai do aparelho.
        assertFalse(ctx.getSharedPreferences("repertorio", Context.MODE_PRIVATE).contains("itens"))
    }

    @Test fun tomDoMomentoSegueORepertorio() {
        val list = Repertoire.createList("Culto")
        Repertoire.addItem(list.id, SetItem(1, 15, "", "G"))
        Repertoire.addItem(list.id, SetItem(2, 291, "", "Am"))
        Live.setKey("C")
        assertEquals("C", Live.key)
        assertEquals(0, Live.transposedBy)

        Live.start(list.id)
        assertEquals("G", Live.key)
        Live.transpose(+2)                    // cantaram mais alto
        assertEquals("A", Live.key)
        assertEquals(2, Live.transposedBy)
        assertTrue(Live.differsFromItem)
        Live.next()
        assertEquals("Am", Live.key)          // o tom muda junto com o hino
        assertEquals(0, Live.transposedBy)
        Live.next()                           // fim da lista: fica no último
        assertEquals(1, Live.position)

        // Trocar o tom no meio do hino mantém o original do hino para comparar.
        Live.setKey("Bm")
        assertEquals("Am", Live.originalKey)
        assertEquals(2, Live.transposedBy)
        Live.saveKeyToItem()
        assertEquals("Bm", Repertoire.get(list.id)!!.items[1].key)
        assertFalse(Live.differsFromItem)

        // Fechou e abriu o app: volta no mesmo ponto.
        Live.init(ctx)
        assertEquals("Bm", Live.key)
        assertEquals(list.id, Live.setlistId)
        assertEquals(1, Live.position)

        // A lista foi apagada: volta a tocar livre, sem perder o tom.
        Repertoire.deleteList(list.id)
        Live.init(ctx)
        assertNull(Live.setlistId)
        assertEquals("Bm", Live.key)

        Live.setKey("X#") // tom que não existe: ignora
        assertEquals("Bm", Live.key)
    }

    @Test fun capotrasteSugereAcordesAbertos() {
        assertEquals(0, Capo.best("G")!!.fret)                // Sol já é aberto
        val bb = Capo.best("A#")!!                            // Si bemol: capo 1 com formato de Lá
        assertEquals(1, bb.fret); assertEquals("A", bb.shapeCipher)
        val fm = Capo.best("Fm")!!                            // Fá menor: capo 1 com formato de Mi menor
        assertEquals(1, fm.fret); assertEquals("Em", fm.shapeCipher)
        assertEquals(5, Capo.options("G#").size)
        assertTrue(Capo.options("G#").zipWithNext().all { (a, b) -> a.fret <= b.fret })
        assertEquals("Capotraste na 1ª casa, com formato de Lá (A)", Capo.advice("A#"))
    }

    @Test fun buscaDaBiblioteca() {
        // Tom pelo nome, sem acento, e pela cifra.
        val sol = LibrarySearch.search("sol").filterIsInstance<SearchHit.Key>().map { it.cipher }
        assertEquals(listOf("G", "G#", "Gm", "G#m").sorted(), sol.sorted())
        assertEquals("C#m", LibrarySearch.search("do sustenido menor").filterIsInstance<SearchHit.Key>().single().cipher)
        assertEquals("F#", (LibrarySearch.search("F#").first() as SearchHit.Key).cipher)
        assertEquals("A#", (LibrarySearch.search("Bb").first() as SearchHit.Key).cipher)  // bemol vira sustenido
        assertEquals("Am", (LibrarySearch.search("am").first() as SearchHit.Key).cipher)

        // Acordes para o dicionário.
        val d7 = LibrarySearch.search("D7").filterIsInstance<SearchHit.Chord>().single()
        assertEquals(2, d7.chord.root); assertEquals(ChordType.DOM7, d7.chord.type)
        assertEquals("Ré com sétima", d7.name)
        assertEquals(ChordType.MAJ7, LibrarySearch.parseChord("c7m")!!.type)
        assertEquals(ChordType.MAJ7, LibrarySearch.parseChord("Cmaj7")!!.type)
        assertEquals(ChordType.HALFDIM, LibrarySearch.parseChord("Bm7b5")!!.type)
        assertNull(LibrarySearch.parseChord("metronomo"))

        // Ferramentas.
        assertTrue(LibrarySearch.search("metrônomo").contains(SearchHit.ToolHit(Tool.METRONOMO)))
        assertTrue(LibrarySearch.search("capo").contains(SearchHit.ToolHit(Tool.CAPO)))
        assertTrue(LibrarySearch.search("").isEmpty())
        assertTrue(LibrarySearch.search("zzz").isEmpty())
    }
}
