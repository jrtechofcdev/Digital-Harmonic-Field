package com.example.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject

/**
 * Um hino do repertório: número (na Harpa Cristã, 1 a 636), nome, tom, casa do
 * capotraste, andamento (BPM, opcional) e uma observação livre ("subir meio tom
 * na última estrofe").
 */
data class SetItem(
    val id: Long,
    val number: Int?,
    val title: String,
    val key: String,          // cifra do tom: "G", "Am"…
    val capo: Int = 0,
    val note: String = "",
    val bpm: Int? = null,
) {
    /** "Hino 15 · Nome" / "Hino 15" / "Nome" */
    val heading: String
        get() = listOfNotNull(number?.let { "Hino $it" }, title.trim().takeIf { it.isNotEmpty() }).joinToString(" · ").ifEmpty { "Hino" }
}

/** Uma lista de hinos com nome ("Culto de domingo", "Santa ceia"…), na ordem do culto. */
data class Setlist(val id: Long, val name: String, val items: List<SetItem>)

/**
 * Repertórios do culto: várias listas de hinos, cada uma com o tom de cada hino.
 * Ficam salvas só no aparelho (sem internet, sem conta).
 *
 * Versões antigas tinham uma lista só ("itens"); ela vira "Meu repertório" na
 * primeira vez que esta versão abre, sem perder nenhum hino.
 */
object Repertoire {
    private const val PREFS = "repertorio"
    private const val KEY_LISTS = "listas"
    private const val KEY_LEGACY = "itens"

    val lists = mutableStateListOf<Setlist>()
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        lists.clear()
        val saved = p.getString(KEY_LISTS, null)
        if (saved != null) {
            lists.addAll(listsFromJson(saved))
        } else {
            val legacy = itemsFromJson(p.getString(KEY_LEGACY, null))
            if (legacy.isNotEmpty()) {
                lists.add(Setlist(1, "Meu repertório", legacy))
                save()
                p.edit().remove(KEY_LEGACY).apply()
            }
        }
    }

    fun get(id: Long?): Setlist? = id?.let { i -> lists.firstOrNull { it.id == i } }

    // ---------------------------------------------------------------- listas

    fun createList(name: String): Setlist {
        val list = Setlist((lists.maxOfOrNull { it.id } ?: 0L) + 1, name.trim().ifEmpty { "Novo repertório" }, emptyList())
        lists.add(0, list)
        save()
        return list
    }

    fun renameList(id: Long, name: String) = edit(id) { it.copy(name = name.trim().ifEmpty { it.name }) }

    /** Copia a lista (para montar o próximo culto a partir de um que já existe). */
    fun duplicateList(id: Long): Setlist? {
        val src = get(id) ?: return null
        val copy = Setlist((lists.maxOfOrNull { it.id } ?: 0L) + 1, "${src.name} (cópia)", src.items)
        lists.add(0, copy)
        save()
        return copy
    }

    fun deleteList(id: Long) { lists.removeAll { it.id == id }; save() }

    /** Apaga todos os repertórios (Ajustes > Dados). */
    fun clear() { lists.clear(); save() }

    // ---------------------------------------------------------------- hinos

    fun nextItemId(listId: Long): Long = (get(listId)?.items?.maxOfOrNull { it.id } ?: 0L) + 1

    fun addItem(listId: Long, item: SetItem) = edit(listId) { it.copy(items = it.items + item) }

    fun updateItem(listId: Long, item: SetItem) = edit(listId) { l ->
        l.copy(items = l.items.map { if (it.id == item.id) item else it })
    }

    fun removeItem(listId: Long, itemId: Long) = edit(listId) { l -> l.copy(items = l.items.filterNot { it.id == itemId }) }

    /** Sobe (−1) ou desce (+1) o hino na ordem do culto. */
    fun moveItem(listId: Long, itemId: Long, delta: Int) = edit(listId) { l ->
        val items = l.items.toMutableList()
        val i = items.indexOfFirst { it.id == itemId }
        val j = i + delta
        if (i < 0 || j !in items.indices) return@edit l
        val tmp = items[i]; items[i] = items[j]; items[j] = tmp
        l.copy(items = items)
    }

    private inline fun edit(id: Long, change: (Setlist) -> Setlist) {
        val i = lists.indexOfFirst { it.id == id }
        if (i < 0) return
        lists[i] = change(lists[i])
        save()
    }

    // ---------------------------------------------------------------- texto

    /**
     * A lista do culto pronta para colar no WhatsApp do grupo de louvor:
     * negrito com *asteriscos*, itálico com _sublinhado_, um hino por linha.
     */
    fun shareText(list: List<SetItem>, title: String = "Repertório do culto"): String = buildString {
        appendLine("*$title* 🎶")
        appendLine()
        list.forEachIndexed { i, item ->
            val keyName = com.example.HarmonicDatabase.ptNameByCipher[item.key] ?: item.key
            append("${i + 1}. *${item.heading}*: ${item.key} ($keyName)")
            if (item.capo > 0) append(", capotraste na ${item.capo}ª casa")
            item.bpm?.let { append(", $it BPM") }
            appendLine()
            if (item.note.isNotBlank()) appendLine("   _${item.note.trim()}_")
        }
        appendLine()
        append("Enviado pelo Digital Harmonic Field")
    }

    // ---------------------------------------------------------------- JSON

    private fun save() { prefs?.edit()?.putString(KEY_LISTS, listsToJson(lists))?.apply() }

    fun listsToJson(all: List<Setlist>): String = JSONArray().apply {
        all.forEach { l ->
            put(JSONObject().apply {
                put("id", l.id)
                put("nome", l.name)
                put("hinos", JSONArray(itemsToJson(l.items)))
            })
        }
    }.toString()

    fun listsFromJson(json: String?): List<Setlist> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Setlist(o.getLong("id"), o.optString("nome", "Repertório"), itemsFromJson(o.optJSONArray("hinos")?.toString()))
            }
        }.getOrDefault(emptyList())
    }

    fun itemsToJson(list: List<SetItem>): String = JSONArray().apply {
        list.forEach { s ->
            put(JSONObject().apply {
                put("id", s.id)
                if (s.number != null) put("numero", s.number)
                put("nome", s.title)
                put("tom", s.key)
                put("capo", s.capo)
                put("obs", s.note)
                if (s.bpm != null) put("bpm", s.bpm)
            })
        }
    }.toString()

    fun itemsFromJson(json: String?): List<SetItem> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                SetItem(
                    id = o.getLong("id"),
                    number = if (o.has("numero")) o.getInt("numero") else null,
                    title = o.optString("nome"),
                    key = o.optString("tom", "C"),
                    capo = o.optInt("capo"),
                    note = o.optString("obs"),
                    bpm = if (o.has("bpm")) o.getInt("bpm") else null,
                )
            }
        }.getOrDefault(emptyList())
    }
}
