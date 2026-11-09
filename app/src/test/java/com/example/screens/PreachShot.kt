package com.example.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Surface
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.example.data.Live
import com.example.music.Preaching
import com.example.ui.screens.PreachScreen
import com.example.ui.screens.PreachStageScreen
import com.example.ui.theme.HarmonicTheme
import com.example.ui.theme.Ink
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Capturas da aba Pregação (sequências, acordes bonitos) e da tela cheia com "passar sozinho". */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w393dp-h873dp-xxhdpi")
class PreachShot {
    @get:Rule val rule = createComposeRule()
    private val out = System.getProperty("shotDir") ?: "build/shots/pregacao"
    private fun set(content: @androidx.compose.runtime.Composable () -> Unit) =
        rule.setContent { HarmonicTheme { Surface(color = Ink) { content() } } }
    private fun screen() = set { PreachScreen(PaddingValues(), emptySet(), {}, {}, {}) }

    @Before fun setUp() = resetLive()
    @After fun tearDown() = resetLive()

    @Test fun pregacao() {
        screen()
        rule.onRoot().captureRoboImage("$out/1_pregacao.png")
    }

    @Test @Config(qualifiers = "w393dp-h2400dp-xxhdpi")
    fun apeloInteiro() {
        screen()
        rule.onNode(hasText("Apelo")).performClick()
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("$out/2_apelo.png")
    }

    @Test @Config(qualifiers = "w393dp-h2400dp-xxhdpi")
    fun acordesBonitos() {
        screen()
        rule.onNode(hasText("Acordes bonitos")).performClick()
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("$out/3_acordes_bonitos.png")
    }

    @Test fun tomMenor() {
        Live.setKey("Am")
        screen()
        rule.onNode(hasText("Oração")).performClick()
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("$out/4_oracao_la_menor.png")
    }

    @Test fun telaCheia() {
        val id = Preaching.all(7, false).first { it.name.startsWith("4 - 5 - 3m - 6m") }.id
        set { PreachStageScreen(id, {}, PaddingValues()) }
        // "Passar sozinho" fica rodando: o relógio do teste anda só quando mandamos.
        rule.mainClock.autoAdvance = false
        rule.onNode(hasText("6 s")).performClick()          // passar sozinho a cada 6 s
        rule.mainClock.advanceTimeBy(9_000)                 // passou para o 2º acorde e encheu metade da barra
        rule.onRoot().captureRoboImage("$out/5_tela_cheia.png")
    }

    @Test @Config(qualifiers = "w873dp-h393dp-xxhdpi")
    fun telaCheiaDeitado() {
        val id = Preaching.all(7, false).first { it.name.startsWith("1 - 5 - 6m - 4") }.id
        set { PreachStageScreen(id, {}, PaddingValues()) }
        rule.onRoot().captureRoboImage("$out/6_tela_cheia_paisagem.png")
    }
}
