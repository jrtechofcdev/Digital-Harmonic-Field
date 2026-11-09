package com.example

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

/**
 * Atalhos do ícone: segurando o ícone do app na tela inicial aparecem
 * "Detectar tom", "Pregação", "Repertório" e "Metrônomo". Abrem direto (sem a
 * abertura).
 *
 * São atalhos dinâmicos (criados pelo app ao abrir), então funcionam igual no
 * app normal e na versão DEV, e com o ícone claro ou escuro.
 */
enum class AppShortcut(val id: String, val short: String, val long: String, val icon: Int) {
    DETECTAR("detectar", "Detectar tom", "Detectar o tom", R.drawable.shortcut_detect),
    PREGACAO("pregacao", "Pregação", "Fundo da pregação", R.drawable.shortcut_preach),
    REPERTORIO("repertorio", "Repertório", "Abrir o repertório", R.drawable.shortcut_setlist),
    METRONOMO("metronomo", "Metrônomo", "Abrir o metrônomo", R.drawable.shortcut_metronome),
    ;

    companion object {
        const val EXTRA = "atalho"
        fun from(intent: Intent?): AppShortcut? =
            intent?.getStringExtra(EXTRA)?.let { id -> entries.firstOrNull { it.id == id } }
    }
}

object AppShortcuts {
    /** Publica (ou atualiza) os atalhos. Seguro chamar a cada abertura. */
    fun publish(context: Context) {
        val list = AppShortcut.entries.mapIndexed { i, s ->
            ShortcutInfoCompat.Builder(context, s.id)
                .setShortLabel(s.short)
                .setLongLabel(s.long)
                .setIcon(IconCompat.createWithResource(context, s.icon))
                .setRank(i)
                .setIntent(
                    Intent(context, MainActivity::class.java)
                        .setAction(Intent.ACTION_VIEW)
                        .putExtra(AppShortcut.EXTRA, s.id)
                )
                .build()
        }
        runCatching { ShortcutManagerCompat.setDynamicShortcuts(context, list) }
    }
}
