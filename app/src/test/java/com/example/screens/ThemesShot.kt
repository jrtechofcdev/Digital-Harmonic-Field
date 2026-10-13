package com.example.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.example.data.Repertoire
import com.example.data.SetItem
import com.example.legal.LegalDoc
import com.example.settings.AppSettings
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.BrandScreen
import com.example.ui.screens.CamposScreen
import com.example.ui.screens.FieldDetailScreen
import com.example.ui.screens.GuitarHubScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LegalScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AppBackground
import com.example.ui.theme.DarkPalette
import com.example.ui.theme.HarmonicTheme
import com.example.ui.theme.LightPalette
import com.example.ui.theme.Palette
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Capturas do tema claro, dos Ajustes, do Sobre e da tela JR TECH OFC. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w393dp-h873dp-xxhdpi")
class ThemesShot {
    @get:Rule val rule = createComposeRule()
    private val out = System.getProperty("shotDir") ?: "build/shots/temas"

    @Before fun setUp() = AppSettings.init(ApplicationProvider.getApplicationContext())
    @After fun tearDown() { Palette.current = DarkPalette; Repertoire.items.clear() }

    private fun shot(name: String, light: Boolean, content: @androidx.compose.runtime.Composable () -> Unit) {
        Palette.current = if (light) LightPalette else DarkPalette
        rule.setContent { HarmonicTheme { AppBackground { content() } } }
        rule.onRoot().captureRoboImage("$out/$name.png")
    }

    @Test fun claroCulto() {
        Repertoire.items.clear()
        Repertoire.items.add(SetItem(1, 15, "Louvai a Jesus", "G"))
        Repertoire.items.add(SetItem(2, 291, "Mais perto quero estar", "D", 2))
        shot("claro_culto", true) { HomeScreen(PaddingValues(), {}, {}, {}, {}, {}, {}) }
    }
    @Test fun claroCampos() = shot("claro_campos", true) { CamposScreen(emptySet(), {}, { _, _ -> }, PaddingValues()) }
    @Test fun claroViolao() = shot("claro_violao", true) { GuitarHubScreen(PaddingValues(), {}, {}, {}) }
    @Test fun claroCampo() = shot("claro_campo_sol", true) { FieldDetailScreen("G", true, {}, {}, PaddingValues()) }
    @Test fun claroAjustes() = shot("claro_ajustes", true) { SettingsScreen(PaddingValues(), {}, {}, {}, {}) }
    @Test fun escuroAjustes() = shot("escuro_ajustes", false) { SettingsScreen(PaddingValues(), {}, {}, {}, {}) }
    @Test fun escuroSobre() = shot("escuro_sobre", false) { AboutScreen({}, PaddingValues(), {}, {}) }
    @Test fun claroSobre() = shot("claro_sobre", true) { AboutScreen({}, PaddingValues(), {}, {}) }
    @Test fun privacidade() = shot("escuro_privacidade", false) { LegalScreen(LegalDoc.PRIVACIDADE, {}, PaddingValues()) }

    @Test fun marcaJrTech() {
        rule.mainClock.autoAdvance = false
        rule.setContent { HarmonicTheme { BrandScreen(onDone = {}, onAbout = {}) } }
        rule.mainClock.advanceTimeBy(1800)
        rule.onRoot().captureRoboImage("$out/marca_jrtech.png")
    }
}
