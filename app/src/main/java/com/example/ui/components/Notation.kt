package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.music.degreeLabel
import com.example.ui.theme.Selection
import com.example.ui.theme.Hairline
import com.example.ui.theme.Ink
import com.example.ui.theme.Surface2
import com.example.ui.theme.TextBody

/** Como os acordes aparecem: pela cifra ("C", "Am") ou pelo grau ("1", "6m"). */
enum class Notation { CIFRA, GRAU }

/**
 * Preferência global Cifra/Grau (salva no aparelho). Quem toca "por número"
 * liga o modo Grau e o app inteiro passa a mostrar 1, 4, 5, 6m… em destaque,
 * com a cifra pequena embaixo.
 */
object NotationSettings {
    private const val PREFS = "app_prefs"
    private const val KEY = "notacao"

    var mode by mutableStateOf(Notation.CIFRA)
        private set

    fun init(context: Context) {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        mode = Notation.entries.firstOrNull { it.name == saved } ?: Notation.CIFRA
    }

    fun set(context: Context, value: Notation) {
        mode = value
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, value.name).apply()
    }

    val byDegree: Boolean get() = mode == Notation.GRAU
}

/**
 * Rótulos de um acorde do campo conforme o modo: (principal, secundário).
 * Cifra: ("C", "4"). Grau: ("4", "C").
 */
fun chordLabels(index: Int, cipher: String): Pair<String, String> {
    val degree = degreeLabel(index, cipher)
    return if (NotationSettings.byDegree) degree to cipher else cipher to degree
}

/** Chave "Cifra | Grau": muda o app inteiro. Seleção neutra, como todo segmento. */
@Composable
fun NotationToggle(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Row(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Surface2)
            .border(1.dp, Hairline, RoundedCornerShape(10.dp))
            .padding(3.dp),
    ) {
        Notation.entries.forEach { n ->
            val on = NotationSettings.mode == n
            Box(
                Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (on) Selection else Surface2)
                    .semantics { selected = on }
                    .clickable(role = Role.Tab) { NotationSettings.set(context, n) }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (n == Notation.CIFRA) "Cifra" else "Grau",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (on) Ink else TextBody,
                )
            }
        }
    }
}
