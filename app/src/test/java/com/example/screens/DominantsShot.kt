package com.example.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Surface
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.Notation
import com.example.ui.components.NotationSettings
import com.example.ui.screens.DominantSettings
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

/** Dominantes secundárias na aba Tocar (tom de Dó, como no vídeo de referência). */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w393dp-h2400dp-xxhdpi")
class DominantsShot {
    @get:Rule val rule = createComposeRule()
    private val out = System.getProperty("shotDir") ?: "build/shots/dominantes"
    private val ctx get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before fun on() {
        DominantSettings.set(ctx, true)
        resetLive()
        com.example.data.Live.setKey("C")
    }
    @After fun off() {
        DominantSettings.set(ctx, false)
        NotationSettings.set(ctx, Notation.CIFRA)
        resetLive()
    }

    private fun open() = rule.setContent { HarmonicTheme { Surface(color = Ink) { PlayForTest() } } }

    @Test fun retrato() {
        open()
        rule.onRoot().captureRoboImage("$out/retrato.png")
        rule.onAllNodes(hasText("Dm").and(hasText("2m")))[0].performClick()
        rule.waitForIdle()
        rule.onAllNodes(hasText("A7 chama o Dm", substring = true))[0].assertExists()
        captureScreenRoboImage("$out/retrato_dm.png")
    }

    @Test fun retratoGrau() {
        NotationSettings.set(ctx, Notation.GRAU)
        open()
        rule.onAllNodes(hasText("6m").and(hasText("Am")))[0].performClick()
        rule.waitForIdle()
        captureScreenRoboImage("$out/retrato_grau_6m.png")
    }

    @Test fun desligado() {
        DominantSettings.set(ctx, false)
        open()
        rule.onNode(hasText("Dominantes")).performClick()
        rule.waitForIdle()
        rule.onAllNodes(hasText("E7", substring = true))[0].assertExists()   // o 5 do 6m (Am) apareceu
        rule.onRoot().captureRoboImage("$out/ligou.png")
    }

    @Test @Config(qualifiers = "w873dp-h393dp-xxhdpi")
    fun paisagem() {
        open()
        rule.onRoot().captureRoboImage("$out/paisagem.png")
    }
}
