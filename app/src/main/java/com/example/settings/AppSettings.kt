package com.example.settings

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.ui.theme.DarkPalette
import com.example.ui.theme.LightPalette
import com.example.ui.theme.Palette

/** Tema do app. */
enum class ThemeMode(val label: String) { ESCURO("Escuro"), CLARO("Claro"), SISTEMA("Do sistema") }

/** Ícone na tela inicial do celular. */
enum class IconMode(val label: String) { ESCURO("Escuro"), CLARO("Claro"), AUTOMATICO("Pelo tema") }

/** Tamanho do texto do app (multiplica o tamanho escolhido no celular). */
enum class TextSize(val label: String, val scale: Float) {
    NORMAL("Normal", 1f), GRANDE("Grande", 1.12f), MUITO_GRANDE("Muito grande", 1.25f)
}

/**
 * Todas as preferências do app, salvas só no aparelho (SharedPreferences
 * "app_prefs", o mesmo arquivo de Cifra|Grau e das dominantes).
 *
 * Cada valor é um estado do Compose: mudou aqui, a tela muda na hora.
 */
object AppSettings {
    private const val PREFS = "app_prefs"
    private var prefs: SharedPreferences? = null

    var theme by mutableStateOf(ThemeMode.ESCURO); private set
    var icon by mutableStateOf(IconMode.ESCURO); private set
    var introVideo by mutableStateOf(true); private set       // vídeo de abertura
    var introSound by mutableStateOf(true); private set       // som do vídeo
    var brandScreen by mutableStateOf(true); private set      // tela JR TECH depois do vídeo
    var keepScreenOn by mutableStateOf(true); private set     // tela acesa no campo e na tela cheia
    var textSize by mutableStateOf(TextSize.NORMAL); private set
    var reduceMotion by mutableStateOf(false); private set    // animações mais curtas e discretas
    var vibrate by mutableStateOf(true); private set          // vibrar ao terminar a detecção de tom
    var latinNames by mutableStateOf(true); private set       // Dó Ré Mi (true) ou C D E (false)

    fun init(context: Context) {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        theme = enumOr(p.getString("tema", null), ThemeMode.ESCURO)
        icon = enumOr(p.getString("icone", null), IconMode.ESCURO)
        introVideo = p.getBoolean("abertura_video", true)
        introSound = p.getBoolean("abertura_som", true)
        brandScreen = p.getBoolean("abertura_marca", true)
        keepScreenOn = p.getBoolean("tela_acesa", true)
        textSize = enumOr(p.getString("texto", null), TextSize.NORMAL)
        reduceMotion = p.getBoolean("menos_animacao", false)
        vibrate = p.getBoolean("vibrar", true)
        latinNames = p.getBoolean("nomes_latinos", true)
        applyTheme(context)
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default

    private fun save(block: SharedPreferences.Editor.() -> Unit) {
        prefs?.edit()?.apply { block(); apply() }
    }

    fun setTheme(context: Context, value: ThemeMode) {
        theme = value; save { putString("tema", value.name) }; applyTheme(context)
    }
    @JvmName("changeIcon") fun setIcon(value: IconMode) { icon = value; save { putString("icone", value.name) } }
    @JvmName("changeIntroVideo") fun setIntroVideo(v: Boolean) { introVideo = v; save { putBoolean("abertura_video", v) } }
    @JvmName("changeIntroSound") fun setIntroSound(v: Boolean) { introSound = v; save { putBoolean("abertura_som", v) } }
    @JvmName("changeBrandScreen") fun setBrandScreen(v: Boolean) { brandScreen = v; save { putBoolean("abertura_marca", v) } }
    @JvmName("changeKeepScreenOn") fun setKeepScreenOn(v: Boolean) { keepScreenOn = v; save { putBoolean("tela_acesa", v) } }
    @JvmName("changeTextSize") fun setTextSize(v: TextSize) { textSize = v; save { putString("texto", v.name) } }
    @JvmName("changeReduceMotion") fun setReduceMotion(v: Boolean) { reduceMotion = v; save { putBoolean("menos_animacao", v) } }
    @JvmName("changeVibrate") fun setVibrate(v: Boolean) { vibrate = v; save { putBoolean("vibrar", v) } }
    @JvmName("changeLatinNames") fun setLatinNames(v: Boolean) { latinNames = v; save { putBoolean("nomes_latinos", v) } }

    /** Volta tudo ao padrão (não apaga favoritos nem repertório). */
    fun resetAll(context: Context) {
        // Guarda qual ícone está ligado de verdade, para a troca continuar certa.
        val applied = prefs?.getString("icone_aplicado", null)
        prefs?.edit()?.clear()?.apply { if (applied != null) putString("icone_aplicado", applied) }?.apply()
        init(context)
        com.example.ui.components.NotationSettings.init(context)
        com.example.ui.screens.DominantSettings.init(context)
    }

    /** O tema que vale agora (resolve "do sistema" pelo modo noturno do celular). */
    fun isDark(context: Context): Boolean = when (theme) {
        ThemeMode.ESCURO -> true
        ThemeMode.CLARO -> false
        ThemeMode.SISTEMA -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    }

    /** Troca a paleta do app inteiro. */
    fun applyTheme(context: Context) {
        Palette.current = if (isDark(context)) DarkPalette else LightPalette
    }
}

/**
 * Troca do ícone na tela inicial sem reinstalar: o app tem duas "portas de
 * entrada" (activity-alias no AndroidManifest), uma com o ícone escuro e outra
 * com o claro. Só uma fica ligada por vez.
 *
 * A troca é feita quando o app vai para segundo plano (onStop), para não
 * fechar a tela que está aberta. Em alguns celulares o atalho que foi
 * arrastado para a tela inicial precisa ser colocado de novo.
 */
object IconSwitcher {
    private const val DARK = "com.example.IconDark"
    private const val LIGHT = "com.example.IconLight"

    /** O ícone que deve estar ligado, conforme o ajuste e o tema atual. */
    fun wantsLight(context: Context): Boolean = when (AppSettings.icon) {
        IconMode.ESCURO -> false
        IconMode.CLARO -> true
        IconMode.AUTOMATICO -> !AppSettings.isDark(context)
    }

    fun apply(context: Context) {
        val light = wantsLight(context)
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val want = if (light) "CLARO" else "ESCURO"
        if (prefs.getString("icone_aplicado", "ESCURO") == want) return
        val pm = context.packageManager
        val on = ComponentName(context.packageName, if (light) LIGHT else DARK)
        val off = ComponentName(context.packageName, if (light) DARK else LIGHT)
        runCatching {
            // Primeiro liga o novo, depois desliga o antigo: nunca ficam os dois desligados.
            pm.setComponentEnabledSetting(on, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
            pm.setComponentEnabledSetting(off, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            prefs.edit().putString("icone_aplicado", want).apply()
        }
    }
}
