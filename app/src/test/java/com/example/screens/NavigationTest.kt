package com.example.screens

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import com.example.HarmonicApp
import com.example.data.Live
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextClearance
import org.junit.Assert.assertEquals
import com.example.ui.theme.HarmonicTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Navegação de verdade: abre o app inteiro e faz o caminho do culto (trocar e
 * transpor o tom, montar um repertório e seguir a lista, buscar na Biblioteca),
 * confere que a tela certa abriu e que o "Voltar" devolve para a aba. Cada
 * passo grava uma captura.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w393dp-h873dp-xxhdpi")
class NavigationTest {
    @get:Rule val rule = createComposeRule()
    private val out = System.getProperty("shotDir") ?: "build/shots/nav"

    private val isTab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
    private fun tab(label: String) = rule.onNode(hasText(label).and(isTab)).performClick().also { rule.waitForIdle() }
    /** Toca no texto (rolando a tela até ele, se precisar). */
    private fun tapText(text: String) {
        val node = rule.onNode(hasText(text))
        runCatching { node.performScrollTo() }
        // Clique pela ação do próprio nó: depois de rolar, ele pode estar atrás da barra de abas.
        node.performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
    }
    /** Rola a lista da tela de cima até o texto aparecer e toca nele. */
    private fun scrollTap(text: String) {
        rule.onNode(hasScrollAction()).performScrollToNode(hasText(text))
        tapText(text)
    }
    private fun shows(text: String) = rule.onNode(hasText(text)).assertIsDisplayed()
    private fun back() = rule.onNode(hasContentDescription("Voltar")).performClick().also { rule.waitForIdle() }
    private fun shot(name: String) = rule.onRoot().captureRoboImage("$out/$name.png")

    /** Nó com este rótulo de clique (ex.: as teclas de tom da folha "Trocar tom"). */
    private fun clickLabel(label: String) = SemanticsMatcher("clique \"$label\"") {
        it.config.getOrNull(SemanticsActions.OnClick)?.label == label
    }

    @Test fun abasEAtalhos() {
        resetLive()
        rule.setContent { HarmonicTheme { HarmonicApp() } }

        // Tocar (abre por padrão): o tom do momento, detectar, transpor, ferramentas.
        shows("Detectar o tom"); shows("Sol Maior"); shot("tocar")
        tapText("Trocar tom"); rule.onNode(clickLabel("Ré Maior")).performClick(); rule.waitForIdle()
        shows("Ré Maior"); assertEquals("D", Live.key)
        rule.onNode(hasContentDescription("Meio tom acima")).performClick(); rule.waitForIdle()
        assertEquals("D#", Live.key); shows("Voltar ao original (D)")
        tapText("Voltar ao original (D)"); assertEquals("D", Live.key)
        tapText("Metrônomo"); shows("Iniciar"); shot("tocar_metronomo"); back()
        tapText("Capotraste"); shows("Sem capo"); back()
        tapText("Progressões"); shows("Sequências prontas para tocar no tom"); back()

        // Repertório: cria a lista, põe um hino e começa a tocar.
        tab("Repertório"); shows("Novo repertório"); shot("repertorio_vazio")
        tapText("Novo repertório"); shows("Adicionar hino")
        tapText("Adicionar hino")
        rule.onNode(hasSetTextAction().and(hasText("Nome do hino"))).performTextInput("Louvai a Jesus")
        tapText("Salvar"); shows("Louvai a Jesus"); shot("repertorio_lista")
        tapText("Começar a tocar")
        shows("Detectar o tom")                                    // voltou para o Tocar
        rule.onNode(hasText("hino 1 de 1", substring = true)).assertIsDisplayed()
        shot("tocar_seguindo_lista")

        // Biblioteca: busca e ferramentas no tom do momento.
        tab("Biblioteca"); shows("Tons, acordes e ferramentas"); shot("biblioteca")
        rule.onNode(hasSetTextAction()).performTextInput("metronomo")
        tapText("Metrônomo"); shows("Iniciar"); back()
        rule.onNode(hasSetTextAction()).performTextClearance()
        scrollTap("Dicionário de acordes"); shows("Todas as formas de tocar cada acorde"); back()
        scrollTap("Passagens e enfeites"); shows("As \"graças\" que ligam um acorde ao outro"); back()
        scrollTap("Lá menor"); assertEquals("Am", Live.key)          // tocar num tom leva ao Tocar
        shows("Detectar o tom")

        // Pregação: fundos no tom do momento (Lá menor), momento Apelo e tela cheia.
        tab("Pregação"); shows("Fundos para a pregação, a oração e o apelo"); shot("pregacao")
        tapText("Apelo")
        rule.onAllNodes(hasText("Tela cheia"))[0].performSemanticsAction(SemanticsActions.OnClick); rule.waitForIdle()
        shows("Passar sozinho"); shot("pregacao_tela_cheia"); back()

        // Aprender agora fica na Biblioteca: lições e treinos.
        tab("Biblioteca")
        scrollTap("Lições de 1 a 10, na ordem, e os treinos"); shows("Treino de graus")
        tapText("Treino de graus"); shows("Pelo ouvido"); shot("aprender_treino"); back()
        tapText("Decorar o braço"); shows("Notas, acordes e harmonia no braço"); back()
        shot("aprender"); back()
        shows("Lições de 1 a 10, na ordem, e os treinos")   // voltou para a Biblioteca, na mesma rolagem

        // Ajustes: tema, Sobre e documentos legais.
        tab("Ajustes"); shows("Tema"); shot("ajustes")
        scrollTap("Sobre o app"); shows("Quem fez"); shot("sobre")
        scrollTap("Política de privacidade"); shows("Microfone"); back()
        back()
        shows("Sobre o app")              // voltou para os Ajustes, na mesma posição da lista

        // Voltar para o Tocar mantém o tom e a lista.
        tab("Tocar"); shows("Detectar o tom"); shows("Lá menor")
        resetLive()
    }
}
