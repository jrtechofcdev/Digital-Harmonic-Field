package com.example

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import com.example.data.Repertoire
import com.example.data.SetItem
import com.example.settings.AppSettings
import com.example.settings.IconMode
import com.example.settings.IconSwitcher
import com.example.settings.ThemeMode
import com.example.ui.theme.Palette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tema, troca de ícone (nunca os dois desligados) e o texto do repertório para o WhatsApp. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsAndShareTest {
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()
    private fun state(alias: String) = ctx.packageManager.getComponentEnabledSetting(ComponentName(ctx.packageName, alias))

    @Test fun temaClaroEscuro() {
        AppSettings.init(ctx)
        AppSettings.setTheme(ctx, ThemeMode.CLARO)
        assertTrue(Palette.current.isLight)
        AppSettings.setTheme(ctx, ThemeMode.ESCURO)
        assertFalse(Palette.current.isLight)
    }

    @Test fun trocaDeIcone() {
        AppSettings.init(ctx)
        AppSettings.setIcon(IconMode.CLARO)
        IconSwitcher.apply(ctx)
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED, state("com.example.IconLight"))
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_DISABLED, state("com.example.IconDark"))

        // "Pelo tema": tema escuro → ícone escuro.
        AppSettings.setTheme(ctx, ThemeMode.ESCURO)
        AppSettings.setIcon(IconMode.AUTOMATICO)
        IconSwitcher.apply(ctx)
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED, state("com.example.IconDark"))
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_DISABLED, state("com.example.IconLight"))

        // Voltar ao padrão não deixa o app sem ícone.
        AppSettings.setIcon(IconMode.CLARO); IconSwitcher.apply(ctx)
        AppSettings.resetAll(ctx); IconSwitcher.apply(ctx)
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED, state("com.example.IconDark"))
    }

    @Test fun aberturaUmaVezPorDia() {
        AppSettings.init(ctx)
        AppSettings.resetAll(ctx)
        assertTrue(AppSettings.introDaily)                       // padrão: só 1 vez por dia
        assertTrue(AppSettings.openingDue("2026-11-08"))
        AppSettings.markOpeningPlayed("2026-11-08")
        assertFalse(AppSettings.openingDue("2026-11-08"))        // abriu de novo no mesmo dia: direto
        assertTrue(AppSettings.openingDue("2026-11-09"))         // outro dia: toca de novo
        AppSettings.setIntroDaily(false)
        assertTrue(AppSettings.openingDue("2026-11-08"))         // "sempre"
        AppSettings.setIntroVideo(false); AppSettings.setBrandScreen(false)
        assertFalse(AppSettings.openingDue("2026-11-09"))        // tudo desligado: nunca
        AppSettings.resetAll(ctx)
    }

    @Test fun atalhosDoIcone() {
        val i = android.content.Intent().putExtra(com.example.AppShortcut.EXTRA, "metronomo")
        assertEquals(com.example.AppShortcut.METRONOMO, com.example.AppShortcut.from(i))
        assertEquals(null, com.example.AppShortcut.from(android.content.Intent()))
        com.example.AppShortcuts.publish(ctx)
        val ids = androidx.core.content.pm.ShortcutManagerCompat.getDynamicShortcuts(ctx).map { it.id }
        assertEquals(setOf("detectar", "pregacao", "repertorio", "metronomo"), ids.toSet())
    }

    @Test fun repertorioParaWhatsApp() {
        val text = Repertoire.shareText(listOf(
            SetItem(1, 15, "Louvai a Jesus", "G", 0, "Começa só com violão"),
            SetItem(2, 291, "", "Am", 2, bpm = 64),
        ), "Culto de domingo")
        assertTrue(text.startsWith("*Culto de domingo*"))
        assertTrue(text.contains("1. *Hino 15 · Louvai a Jesus*: G (Sol Maior)"))
        assertTrue(text.contains("   _Começa só com violão_"))
        assertTrue(text.contains("2. *Hino 291*: Am (Lá menor), capotraste na 2ª casa, 64 BPM"))
    }
}
