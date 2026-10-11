package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.Repertoire
import com.example.data.SetItem
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** O repertório salva no aparelho e volta igual (ordem, tom, capo e observação). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RepertoireTest {
    @Test fun saveReloadAndReorder() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        Repertoire.init(ctx)
        Repertoire.items.toList().forEach { Repertoire.remove(it.id) }
        Repertoire.add(SetItem(Repertoire.nextId(), 15, "Louvai a Jesus", "G", 0, "só violão"))
        Repertoire.add(SetItem(Repertoire.nextId(), null, "Sem número", "Am", 2))
        Repertoire.add(SetItem(Repertoire.nextId(), 545, "", "D"))
        Repertoire.move(3, -1)                       // o 3º sobe para 2º
        Repertoire.init(ctx)                         // lê de novo do aparelho
        assertEquals(listOf(1L, 3L, 2L), Repertoire.items.map { it.id })
        assertEquals("Hino 15 · Louvai a Jesus", Repertoire.items[0].heading)
        assertEquals("Hino 545", Repertoire.items[1].heading)
        assertEquals("Sem número", Repertoire.items[2].heading)
        assertEquals(2, Repertoire.items[2].capo)
        assertEquals("Am", Repertoire.items[2].key)
        assertEquals("só violão", Repertoire.items[0].note)
        Repertoire.remove(1)
        Repertoire.init(ctx)
        assertEquals(2, Repertoire.items.size)
        assertEquals(emptyList<SetItem>(), Repertoire.fromJson("lixo"))
    }
}
