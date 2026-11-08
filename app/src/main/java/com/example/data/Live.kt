package com.example.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.HarmonicDatabase
import com.example.music.transposeCipher

/**
 * O "tom do momento": o tom em que o músico está tocando agora.
 *
 * É o centro do app: a aba Tocar mostra o campo dele, e as ferramentas
 * (progressões, passagens, braço, dicionário, capotraste) abrem nele. Quem muda
 * o tom: a folha "Trocar tom", o transpor (+/- meio tom), o Detectar tom, um
 * tom tocado na Biblioteca e o hino do repertório em andamento.
 *
 * Também guarda o repertório em andamento ("Hino 2 de 6") e o tom original do
 * hino, para mostrar quanto foi transposto. Tudo fica salvo no aparelho: se o app
 * fechar no meio do culto, ele volta no mesmo ponto.
 */
object Live {
    private const val PREFS = "ao_vivo"
    private var prefs: SharedPreferences? = null

    /** Tom atual (cifra do campo: "G", "Am", "F#m"...). */
    var key by mutableStateOf("G"); private set
    /** Tom original do hino (do repertório) ou do tom escolhido; base do "transposto". */
    var originalKey by mutableStateOf("G"); private set
    /** Repertório em andamento (null = tocando livre). */
    var setlistId by mutableStateOf<Long?>(null); private set
    /** Posição do hino atual no repertório em andamento. */
    var position by mutableIntStateOf(0); private set

    fun init(context: Context) {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        key = valid(p.getString("tom", null)) ?: "G"
        originalKey = valid(p.getString("original", null)) ?: key
        setlistId = p.getLong("lista", -1L).takeIf { it >= 0 }
        position = p.getInt("posicao", 0)
        // A lista pode ter sido apagada: volta a tocar livre.
        if (setlistId != null && setlist == null) { setlistId = null; position = 0 }
    }

    private fun valid(k: String?): String? = k?.takeIf { HarmonicDatabase.getField(it) != null }

    private fun save() {
        prefs?.edit()
            ?.putString("tom", key)
            ?.putString("original", originalKey)
            ?.putLong("lista", setlistId ?: -1L)
            ?.putInt("posicao", position)
            ?.apply()
    }

    /** O repertório em andamento (se houver). */
    val setlist: Setlist? get() = Repertoire.get(setlistId)

    /** O hino atual do repertório em andamento (se houver). */
    val currentItem: SetItem? get() = setlist?.items?.getOrNull(position)

    /** Quantos semitons o tom atual está acima (+) ou abaixo (-) do original, de -6 a +6. */
    val transposedBy: Int get() {
        val a = com.example.music.rootPc(originalKey) ?: return 0
        val b = com.example.music.rootPc(key) ?: return 0
        val d = Math.floorMod(b - a, 12)
        return if (d > 6) d - 12 else d
    }

    /**
     * Escolhe um tom (folha "Trocar tom", Biblioteca, Detectar tom). Tocando
     * livre, ele vira o novo original. Seguindo um repertório, o original
     * continua sendo o tom do hino (se for do mesmo modo), para mostrar
     * "+2 do original" quando cantam mais alto que o de costume.
     */
    @JvmName("changeKey") fun setKey(newKey: String) {
        val k = valid(newKey) ?: return
        key = k
        val hymnKey = valid(currentItem?.key)
        originalKey = if (hymnKey != null && hymnKey.endsWith("m") == k.endsWith("m")) hymnKey else k
        save()
    }

    /** O tom tocado agora é diferente do salvo no hino do repertório. */
    val differsFromItem: Boolean get() = currentItem?.let { valid(it.key) != key } ?: false

    /** Grava o tom atual no hino do repertório (para o próximo culto já abrir certo). */
    fun saveKeyToItem() {
        val list = setlist ?: return
        val item = currentItem ?: return
        Repertoire.updateItem(list.id, item.copy(key = key))
        originalKey = key
        save()
    }

    /** Sobe (+1) ou desce (-1) meio tom, mantendo o original para comparar. */
    fun transpose(delta: Int) {
        val k = valid(transposeCipher(key, delta)) ?: return
        key = k
        save()
    }

    /** Volta ao tom original. */
    fun resetTranspose() { key = originalKey; save() }

    /** Começa a tocar um repertório a partir de um hino. */
    fun start(listId: Long, at: Int = 0) {
        val list = Repertoire.get(listId) ?: return
        if (list.items.isEmpty()) return
        setlistId = listId
        goTo(at.coerceIn(0, list.items.lastIndex))
    }

    /** Vai para o hino [index] do repertório em andamento (o tom muda junto). */
    fun goTo(index: Int) {
        val list = setlist ?: return
        val item = list.items.getOrNull(index) ?: return
        position = index
        val k = valid(item.key) ?: key
        key = k
        originalKey = k
        save()
    }

    fun next() = goTo(position + 1)
    fun previous() = goTo(position - 1)

    /** Para de seguir o repertório (o tom atual continua). */
    fun stop() { setlistId = null; position = 0; save() }
}
