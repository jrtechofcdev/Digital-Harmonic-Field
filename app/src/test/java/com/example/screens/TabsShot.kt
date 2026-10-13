package com.example.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.example.data.Repertoire
import com.example.data.SetItem
import com.example.ui.screens.CamposScreen
import com.example.ui.screens.CapoScreen
import com.example.ui.screens.GuitarHubScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LearnScreen
import com.example.ui.screens.MetronomeScreen
import com.example.ui.theme.HarmonicTheme
import com.example.ui.theme.Ink
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Capturas das 4 abas e das telas de palco, num celular de 393 x 873 dp. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w393dp-h873dp-xxhdpi")
class TabsShot {
    @get:Rule val rule = createComposeRule()
    private val out = System.getProperty("shotDir") ?: "build/shots/tabs"
    private fun set(content: @androidx.compose.runtime.Composable () -> Unit) =
        rule.setContent { HarmonicTheme { Surface(color = Ink, modifier = Modifier) { content() } } }

    @Test fun culto() {
        Repertoire.items.clear()
        Repertoire.items.add(SetItem(1, 15, "Louvai a Jesus", "G"))
        Repertoire.items.add(SetItem(2, 291, "Mais perto quero estar", "D", 2))
        set { HomeScreen(PaddingValues(), {}, {}, {}, {}, {}, {}) }
        rule.onRoot().captureRoboImage("$out/1_culto.png")
        Repertoire.items.clear()
    }
    @Test fun cultoVazio() {
        Repertoire.items.clear()
        set { HomeScreen(PaddingValues(), {}, {}, {}, {}, {}, {}) }
        rule.onRoot().captureRoboImage("$out/1_culto_vazio.png")
    }
    @Test fun campos() {
        set { CamposScreen(emptySet(), {}, { _, _ -> }, PaddingValues()) }
        rule.onRoot().captureRoboImage("$out/2_campos.png")
        rule.onNode(hasText("Progressões")).performClick()
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("$out/2_campos_progressoes.png")
    }
    @Test fun violao() {
        set { GuitarHubScreen(PaddingValues(), {}, {}, {}) }
        rule.onRoot().captureRoboImage("$out/3_violao.png")
    }
    @Test fun aprender() {
        set { LearnScreen({}, PaddingValues()) }
        rule.onRoot().captureRoboImage("$out/4_aprender.png")
    }
    @Test fun metronomo() {
        set { MetronomeScreen({}, PaddingValues()) }
        rule.onRoot().captureRoboImage("$out/5_metronomo.png")
    }
    @Test fun capo() {
        set { CapoScreen({}, PaddingValues()) }
        rule.onRoot().captureRoboImage("$out/5_capo.png")
    }
}
