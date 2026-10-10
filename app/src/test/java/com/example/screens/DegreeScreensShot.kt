package com.example.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.foundation.verticalScroll
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.example.ui.components.Notation
import com.example.ui.components.NotationSettings
import com.example.ui.screens.DegreeTrainerScreen
import com.example.ui.screens.FieldDetailScreen
import com.example.ui.screens.GuitarScreen
import com.example.ui.theme.HarmonicTheme
import com.example.ui.theme.Ink
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Capturas das telas de graus/violão (para conferir o layout num celular). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w393dp-h2600dp-xxhdpi")
class DegreeScreensShot {
    @get:Rule val rule = createComposeRule()
    private val out = System.getProperty("shotDir") ?: "build/shots"

    private fun shot(name: String, content: @androidx.compose.runtime.Composable () -> Unit) {
        rule.setContent { HarmonicTheme { Surface(color = Ink, modifier = Modifier) { content() } } }
        rule.onRoot().captureRoboImage("$out/$name.png")
    }

    @Test fun violao() = shot("violao") { GuitarScreen("G", {}, PaddingValues()) }
    @Test fun treino() = shot("treino") { DegreeTrainerScreen({}, PaddingValues()) }
    @Test fun campoPorGrau() {
        NotationSettings.set(ApplicationProvider.getApplicationContext(), Notation.GRAU)
        shot("campo_grau") { FieldDetailScreen("G", false, {}, {}, PaddingValues()) }
        NotationSettings.set(ApplicationProvider.getApplicationContext(), Notation.CIFRA)
    }

    @Test fun aprenderGrausAFundo() {
        rule.setContent {
            HarmonicTheme {
                Surface(color = Ink) {
                    androidx.compose.foundation.layout.Column(
                        Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())
                    ) { com.example.ui.screens.DegreesDeepDiveLesson() }
                }
            }
        }
        rule.onNode(androidx.compose.ui.test.hasText("Graus a fundo", substring = true)).performClick()
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("$out/graus_a_fundo.png")
    }
}
