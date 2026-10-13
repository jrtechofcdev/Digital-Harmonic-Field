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

    @Test fun repertorioParaWhatsApp() {
        val text = Repertoire.shareText(listOf(
            SetItem(1, 15, "Louvai a Jesus", "G", 0, "Começa só com violão"),
            SetItem(2, 291, "", "Am", 2),
        ))
        assertTrue(text.startsWith("*Repertório do culto*"))
        assertTrue(text.contains("1. *Hino 15 · Louvai a Jesus*: G (Sol Maior)"))
        assertTrue(text.contains("   _Começa só com violão_"))
        assertTrue(text.contains("2. *Hino 291*: Am (Lá menor), capotraste na 2ª casa"))
    }
}
