package com.example.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject

/**
 * Um hino do repertório: número (na Harpa Cristã, 1 a 636), nome, tom, casa do
 * capotraste e uma observação livre ("subir meio tom na última estrofe").
 */
data class SetItem(
    val id: Long,
    val number: Int?,
    val title: String,
    val key: String,          // cifra do tom: "G", "Am"…
    val capo: Int = 0,
    val note: String = "",
) {
    /** "Hino 15 · Nome" / "Hino 15" / "Nome" */
    val heading: String
        get() = listOfNotNull(number?.let { "Hino $it" }, title.trim().takeIf { it.isNotEmpty() }).joinToString(" · ").ifEmpty { "Hino" }
}

/**
 * Repertório do culto: a lista de hinos com o tom de cada um, na ordem do culto.
 * Fica salvo só no aparelho (sem internet, sem conta).
 */
object Repertoire {
    private const val PREFS = "repertorio"
    private const val KEY = "itens"

    val items = mutableStateListOf<SetItem>()
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        items.clear()
        items.addAll(fromJson(prefs?.getString(KEY, null)))
    }

    fun add(item: SetItem) { items.add(item); save() }

    fun update(item: SetItem) {
        val i = items.indexOfFirst { it.id == item.id }
        if (i >= 0) { items[i] = item; save() }
    }

    fun remove(id: Long) { items.removeAll { it.id == id }; save() }

    /** Sobe (−1) ou desce (+1) o hino na ordem do culto. */
    fun move(id: Long, delta: Int) {
        val i = items.indexOfFirst { it.id == id }
        val j = i + delta
        if (i < 0 || j !in items.indices) return
        val tmp = items[i]; items[i] = items[j]; items[j] = tmp
        save()
    }

    fun nextId(): Long = (items.maxOfOrNull { it.id } ?: 0L) + 1

    private fun save() { prefs?.edit()?.putString(KEY, toJson(items))?.apply() }

    fun toJson(list: List<SetItem>): String = JSONArray().apply {
        list.forEach { s ->
            put(JSONObject().apply {
                put("id", s.id)
                if (s.number != null) put("numero", s.number)
                put("nome", s.title)
                put("tom", s.key)
                put("capo", s.capo)
                put("obs", s.note)
            })
        }
    }.toString()

    fun fromJson(json: String?): List<SetItem> {
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
                )
            }
        }.getOrDefault(emptyList())
    }
}
