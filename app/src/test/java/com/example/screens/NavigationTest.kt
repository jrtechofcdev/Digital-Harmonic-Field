package com.example.screens

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.example.HarmonicApp
import com.example.data.Repertoire
import com.example.ui.theme.HarmonicTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Navegação de verdade: abre o app inteiro, toca em cada aba da barra de baixo
 * e nas linhas principais de cada aba, confere que a tela certa abriu e que o
 * "Voltar" devolve para a aba. Cada passo grava uma captura.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w393dp-h873dp-xxhdpi")
class NavigationTest {
    @get:Rule val rule = createComposeRule()
    private val out = System.getProperty("shotDir") ?: "build/shots/nav"

    private val isTab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
    private fun tab(label: String) = rule.onNode(hasText(label).and(isTab)).performClick().also { rule.waitForIdle() }
    private fun tapText(text: String) = rule.onNode(hasText(text)).performClick().also { rule.waitForIdle() }
    private fun shows(text: String) = rule.onNode(hasText(text)).assertIsDisplayed()
    private fun back() = rule.onNode(hasContentDescription("Voltar")).performClick().also { rule.waitForIdle() }
    private fun shot(name: String) = rule.onRoot().captureRoboImage("$out/$name.png")

    @Test fun abasEAtalhos() {
        Repertoire.items.clear()
        rule.setContent { HarmonicTheme { HarmonicApp() } }

        // Culto (abre por padrão): foco em detectar o tom + ferramentas de palco.
        shows("Detectar o tom")
        tapText("Metrônomo"); shows("Iniciar"); shot("culto_metronomo"); back()
        tapText("Capotraste"); shows("Sem capo"); back()
        tapText("Montar a lista"); shows("Repertório do culto"); shot("culto_repertorio"); back()
        shows("Detectar o tom")

        // Campos: tons e, no mesmo lugar, as progressões.
        tab("Campos"); shows("Tons"); shot("campos")
        tapText("Progressões"); rule.onNode(hasText("Ver o campo de", substring = true)).assertIsDisplayed(); shot("campos_progressoes")

        // Violão: o braço, o dicionário e as passagens.
        tab("Violão"); shows("Abrir o braço do violão")
        tapText("Abrir o braço do violão"); shows("Notas, acordes e harmonia no braço"); back()
        tapText("Dicionário de acordes"); shows("Todas as formas de tocar cada acorde"); shot("violao_dicionario"); back()
        tapText("Passagens e enfeites"); shows("As \"graças\" que ligam um acorde ao outro"); back()
        shows("Abrir o braço do violão")

        // Aprender: treinos no topo, lições abaixo.
        tab("Aprender"); shows("Treino de graus")
        tapText("Treino de graus"); shows("Pelo ouvido"); shot("aprender_treino"); back()
        tapText("Decorar o braço"); shows("Notas, acordes e harmonia no braço"); back()
        shot("aprender")

        // Voltar para o Culto mantém a tela inicial.
        tab("Culto"); shows("Detectar o tom")
    }
}
