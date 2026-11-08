package com.example.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.example.music.ChordType
import com.example.music.LabChord
import com.example.ui.screens.ChordDictionaryScreen
import com.example.ui.screens.PassagesScreen
import com.example.ui.theme.HarmonicTheme
import com.example.ui.theme.Ink
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Capturas do dicionário de acordes, das passagens e do repertório. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w393dp-h2600dp-xxhdpi")
class NewModulesShot {
    @get:Rule val rule = createComposeRule()
    private val out = System.getProperty("shotDir") ?: "build/shots"

    private fun set(content: @androidx.compose.runtime.Composable () -> Unit) =
        rule.setContent { HarmonicTheme { Surface(color = Ink, modifier = Modifier) { content() } } }

    @Test fun dicionario() {
        set { ChordDictionaryScreen(LabChord(7, ChordType.MAJOR), {}, PaddingValues()) }
        rule.onRoot().captureRoboImage("$out/dicionario.png")
    }

    @Test fun dicionarioTodas() {
        set { ChordDictionaryScreen(LabChord(7, ChordType.MAJOR), {}, PaddingValues()) }
        rule.onNode(hasText("Todas", substring = true).and(hasText("(", substring = true))).performClick()
        rule.waitUntil(10_000) { rule.onAllNodes(hasText("Mostrar mais", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        rule.onRoot().captureRoboImage("$out/dicionario_todas.png")
    }

    @Test fun passagens() {
        set { PassagesScreen("G", {}, PaddingValues()) }
        rule.onAllNodes(hasText("Formas"))[2].performClick()
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("$out/passagens.png")
    }

    @Test fun passagensDescer() {
        set { PassagesScreen("G", {}, PaddingValues()) }
        rule.onNode(hasText("Descer o baixo")).performClick()
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("$out/passagens_descer.png")
    }

    @Test @Config(qualifiers = "w873dp-h393dp-xxhdpi")
    fun tocarPaisagem() {
        com.example.data.Live.start(demoSetlists().id, 0)
        set { PlayForTest() }
        rule.onRoot().captureRoboImage("$out/tocar_paisagem.png")
        resetLive()
    }

    @Test @Config(qualifiers = "w873dp-h393dp-xxhdpi")
    fun appPaisagem() {
        resetLive()
        rule.setContent { HarmonicTheme { com.example.HarmonicApp() } }
        rule.onRoot().captureRoboImage("$out/app_paisagem.png")
    }
}
