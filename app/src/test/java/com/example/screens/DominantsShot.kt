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
import com.example.ui.screens.FieldDetailScreen
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

/** Dominantes secundárias no detalhe do campo (tom de Dó, como no vídeo de referência). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w393dp-h2400dp-xxhdpi")
class DominantsShot {
    @get:Rule val rule = createComposeRule()
    private val out = System.getProperty("shotDir") ?: "build/shots/dominantes"
    private val ctx get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before fun on() = DominantSettings.set(ctx, true)
    @After fun off() {
        DominantSettings.set(ctx, false)
        NotationSettings.set(ctx, Notation.CIFRA)
    }

    private fun open() = rule.setContent {
        HarmonicTheme { Surface(color = Ink) { FieldDetailScreen("C", false, {}, {}, PaddingValues()) } }
    }

    @Test fun retrato() {
        open()
        rule.onNode(hasText("chama o Dm")).performClick()
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("$out/retrato_dm.png")
    }

    @Test fun retratoGrau() {
        NotationSettings.set(ctx, Notation.GRAU)
        open()
        rule.onNode(hasText("chama o 6m")).performClick()
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("$out/retrato_grau_6m.png")
    }

    @Test fun desligado() {
        DominantSettings.set(ctx, false)
        open()
        rule.onNode(hasText("Dominantes secundárias")).performClick()
        rule.waitForIdle()
        rule.onNode(hasText("chama o F")).performClick()
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("$out/ligou_e_escolheu_f.png")
    }

    @Test @Config(qualifiers = "w873dp-h393dp-xxhdpi")
    fun paisagem() {
        open()
        rule.onNode(hasText("A7")).performClick()
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("$out/paisagem.png")
    }
}
