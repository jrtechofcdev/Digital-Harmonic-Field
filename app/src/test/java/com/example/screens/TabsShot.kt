package com.example.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.data.Live
import com.example.ui.screens.CapoScreen
import com.example.ui.screens.KeySheet
import com.example.ui.screens.LearnScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.MetronomeScreen
import com.example.ui.screens.ProgressionsScreen
import com.example.ui.screens.SetlistScreen
import com.example.ui.screens.SetlistsScreen
import com.example.ui.theme.HarmonicTheme
import com.example.ui.theme.Ink
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Capturas das abas (Tocar, Repertório, Biblioteca, Aprender) e das ferramentas, num celular de 393 x 873 dp. */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w393dp-h873dp-xxhdpi")
class TabsShot {
    @get:Rule val rule = createComposeRule()
    private val out = System.getProperty("shotDir") ?: "build/shots/tabs"
    private fun set(content: @androidx.compose.runtime.Composable () -> Unit) =
        rule.setContent { HarmonicTheme { Surface(color = Ink, modifier = Modifier) { content() } } }

    @Before fun setUp() = resetLive()
    @After fun tearDown() = resetLive()

    @Test fun tocar() {
        set { PlayForTest() }
        rule.onRoot().captureRoboImage("$out/1_tocar.png")
    }
    @Test fun tocarRepertorio() {
        Live.start(demoSetlists().id, 1)
        Live.transpose(+1)
        set { PlayForTest() }
        rule.onRoot().captureRoboImage("$out/1_tocar_repertorio.png")
    }
    @Test fun tocarFolhaDoAcorde() {
        set { PlayForTest() }
        rule.onAllNodes(hasText("Em").and(hasText("6m")))[0].performClick()
        rule.waitForIdle()
        captureScreenRoboImage("$out/1_tocar_acorde.png")
    }
    @Test fun trocarTom() {
        set { KeySheet(setOf("G", "D", "Am"), onDetectKey = {}, onDismiss = {}) }
        rule.waitForIdle()
        captureScreenRoboImage("$out/1_trocar_tom.png")
    }
    @Test fun repertorio() {
        val l = demoSetlists()
        Live.start(l.id, 1)
        set { SetlistsScreen(PaddingValues(), {}, {}) }
        rule.onRoot().captureRoboImage("$out/2_repertorio.png")
    }
    @Test fun repertorioVazio() {
        set { SetlistsScreen(PaddingValues(), {}, {}) }
        rule.onRoot().captureRoboImage("$out/2_repertorio_vazio.png")
    }
    @Test fun lista() {
        val l = demoSetlists()
        set { SetlistScreen(l.id, {}, PaddingValues(), {}) }
        rule.onRoot().captureRoboImage("$out/2_lista.png")
        rule.onNode(hasText("Adicionar hino")).performClick()
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("$out/2_lista_novo_hino.png")
    }
    @Test fun biblioteca() {
        set { LibraryScreen(PaddingValues(), setOf("G", "Am"), {}, {}, {}) }
        rule.onRoot().captureRoboImage("$out/3_biblioteca.png")
    }
    @Test fun bibliotecaBusca() {
        set { LibraryScreen(PaddingValues(), emptySet(), {}, {}, {}) }
        rule.onNode(hasSetTextAction()).performTextInput("D7")
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("$out/3_biblioteca_busca_d7.png")
    }
    @Test fun aprender() {
        set { LearnScreen({}, PaddingValues()) }
        rule.onRoot().captureRoboImage("$out/4_aprender.png")
    }
    @Test fun progressoes() {
        set { ProgressionsScreen("G", {}, {}, { _, _ -> }, PaddingValues()) }
        rule.onRoot().captureRoboImage("$out/5_progressoes.png")
    }
    @Test fun metronomo() {
        set { MetronomeScreen({}, PaddingValues(), initialBpm = 72) }
        rule.onRoot().captureRoboImage("$out/5_metronomo.png")
    }
    @Test fun capo() {
        set { CapoScreen({}, PaddingValues(), initialKey = "A#") }
        rule.onRoot().captureRoboImage("$out/5_capo.png")
    }
}
