package com.example

import android.content.Context
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.legal.LegalDoc
import com.example.music.ChordType
import com.example.music.LabChord
import com.example.music.Progression
import com.example.settings.AppSettings
import com.example.settings.IconSwitcher
import com.example.ui.components.Motion
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.BrandScreen
import com.example.ui.screens.CamposScreen
import com.example.ui.screens.CapoScreen
import com.example.ui.screens.ChordDictionaryScreen
import com.example.ui.screens.DegreeTrainerScreen
import com.example.ui.screens.DonationScreen
import com.example.ui.screens.FieldDetailScreen
import com.example.ui.screens.GuitarHubScreen
import com.example.ui.screens.GuitarScreen
import com.example.ui.screens.GuitarTab
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.IntroVideo
import com.example.ui.screens.KeyFinderScreen
import com.example.ui.screens.LearnScreen
import com.example.ui.screens.LegalScreen
import com.example.ui.screens.MetronomeScreen
import com.example.ui.screens.PassagesScreen
import com.example.ui.screens.ProgressionStageScreen
import com.example.ui.screens.RepertoireScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AppBackground
import com.example.ui.theme.Brass
import com.example.ui.theme.HarmonicTheme
import com.example.ui.theme.Ink
import com.example.ui.theme.IsLightTheme
import com.example.ui.theme.Surface1
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextStrong
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// Persistência dos tons favoritos.
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "favorites_prefs")
internal val FAVORITES_KEY = stringSetPreferencesKey("favorite_keys")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Versão DEV de treino: prepara a pasta de trechos (no app normal não faz nada).
        if (BuildConfig.DEV_TOOLS) com.example.dev.DevStore.init(applicationContext)
        com.example.ui.components.NotationSettings.init(applicationContext)
        com.example.data.Repertoire.init(applicationContext)
        com.example.ui.screens.DominantSettings.init(applicationContext)
        AppSettings.init(applicationContext)        // também aplica o tema (claro/escuro)
        // A abertura toca só quando o app é aberto de verdade (não ao girar a tela).
        val playOpening = savedInstanceState == null
        setContent {
            HarmonicTheme {
                AppRoot(playOpening)
            }
        }
    }

    // A troca do ícone acontece quando o app sai da tela, para não fechar nada aberto.
    override fun onStop() {
        super.onStop()
        IconSwitcher.apply(applicationContext)
    }
}

/** As 5 áreas do app, pelo que o músico quer fazer. */
internal enum class Tab(val label: String, val icon: Int) {
    CULTO("Culto", R.drawable.ic_church),         // na hora de tocar
    CAMPOS("Campos", R.drawable.ic_music_notes),   // tons e progressões
    VIOLAO("Violão", R.drawable.ic_guitar),        // tudo no braço
    APRENDER("Aprender", R.drawable.ic_learn),     // lições e treinos
    AJUSTES("Ajustes", R.drawable.ic_settings),    // configurações, sobre e legal
}

private enum class Startup { VIDEO, BRAND, APP }

/**
 * Abertura (vídeo) → tela JR TECH OFC → app. Cada etapa pode ser desligada nos
 * Ajustes; "Ver a abertura agora" toca tudo de novo e volta para os Ajustes.
 */
@Composable
internal fun AppRoot(playOpening: Boolean) {
    fun first(): Startup = when {
        !playOpening -> Startup.APP
        AppSettings.introVideo -> Startup.VIDEO
        AppSettings.brandScreen -> Startup.BRAND
        else -> Startup.APP
    }
    var stage by rememberSaveable { mutableStateOf(first()) }
    var aboutAtStart by rememberSaveable { mutableStateOf(false) }
    var lastTab by rememberSaveable { mutableStateOf(Tab.CULTO) }

    SystemBars(lightIcons = stage != Startup.APP || !IsLightTheme)

    Crossfade(stage, animationSpec = tween(Motion.ms(550)), label = "abertura") { s ->
        when (s) {
            Startup.VIDEO -> IntroVideo(onDone = { stage = if (AppSettings.brandScreen) Startup.BRAND else Startup.APP })
            Startup.BRAND -> BrandScreen(
                onDone = { stage = Startup.APP },
                onAbout = { aboutAtStart = true; stage = Startup.APP },
            )
            Startup.APP -> HarmonicApp(
                startTab = lastTab,
                openAboutAtStart = aboutAtStart,
                onAboutShown = { aboutAtStart = false },
                onTabChange = { lastTab = it },
                onReplayOpening = { stage = if (AppSettings.brandScreen && !AppSettings.introVideo) Startup.BRAND else Startup.VIDEO },
            )
        }
    }
}

/** Ícones da barra de status/navegação claros (fundo escuro) ou escuros (tema claro). */
@Composable
private fun SystemBars(lightIcons: Boolean) {
    val activity = LocalContext.current as? ComponentActivity ?: return
    LaunchedEffect(lightIcons) {
        val style = if (lightIcons) SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        else SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        activity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
}

/** As telas que abrem por cima das abas (cada uma entra deslizando). */
private sealed interface Dest {
    data class Detail(val key: String) : Dest
    data class Guitar(val key: String, val tab: Int) : Dest
    data class Passages(val key: String) : Dest
    data class Dictionary(val chord: LabChord) : Dest
    data class Stage(val progression: Progression, val key: String) : Dest
    data class Legal(val doc: LegalDoc) : Dest
    data object KeyFinder : Dest
    data object Donation : Dest
    data object Metronome : Dest
    data object Capo : Dest
    data object Repertoire : Dest
    data object Trainer : Dest
    data object DevPanel : Dest
    data object About : Dest
}

/** Uma tela na pilha, com o estado da animação de entrada/saída. */
private class Entry(val dest: Dest) {
    val visible = MutableTransitionState(false).apply { targetState = true }
}

/** Impede que toques "atravessem" a tela de cima e cheguem na de baixo. */
private fun Modifier.blockTouchesBelow() = pointerInput(Unit) {}

@Composable
internal fun HarmonicApp(
    startTab: Tab = Tab.CULTO,
    openAboutAtStart: Boolean = false,
    onAboutShown: () -> Unit = {},
    onTabChange: (Tab) -> Unit = {},
    onReplayOpening: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val favorites by remember {
        context.dataStore.data.map { it[FAVORITES_KEY] ?: emptySet() }
    }.collectAsState(initial = emptySet())

    fun toggleFavorite(key: String) {
        scope.launch {
            context.dataStore.edit { prefs ->
                val current = prefs[FAVORITES_KEY] ?: emptySet()
                prefs[FAVORITES_KEY] = if (current.contains(key)) current - key else current + key
            }
        }
    }

    var tab by rememberSaveable { mutableStateOf(startTab) }
    val entries = remember { mutableStateListOf<Entry>() }
    val open = entries.filter { it.visible.targetState }          // a pilha "de verdade"

    fun push(d: Dest) { entries.add(Entry(d)) }
    fun pop() { entries.lastOrNull { it.visible.targetState }?.visible?.targetState = false }
    /** Fecha a tela de cima e abre outra no lugar dela. */
    fun replace(d: Dest) { pop(); push(d) }

    LaunchedEffect(openAboutAtStart) {
        if (openAboutAtStart) { push(Dest.About); onAboutShown() }
    }

    BackHandler(enabled = open.isNotEmpty()) { pop() }

    val openKey: (String) -> Unit = { push(Dest.Detail(it)) }
    val openDictionary: () -> Unit = { push(Dest.Dictionary(LabChord(7, ChordType.MAJOR))) }
    val insets = WindowInsets.safeDrawing.asPaddingValues()

    Box(Modifier.fillMaxSize()) {
        // As abas (por baixo de tudo).
        AppBackground(if (open.isNotEmpty()) Modifier.clearAndSetSemantics {} else Modifier) {
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    NavigationBar(containerColor = Surface1, tonalElevation = 0.dp) {
                        Tab.entries.forEach { entry ->
                            NavigationBarItem(
                                selected = tab == entry,
                                onClick = { tab = entry; onTabChange(entry) },
                                icon = { Icon(painterResource(entry.icon), contentDescription = null, modifier = Modifier.size(24.dp)) },
                                label = { Text(entry.label, style = MaterialTheme.typography.labelMedium, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Ink,
                                    selectedTextColor = TextStrong,
                                    indicatorColor = Brass,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted,
                                ),
                            )
                        }
                    }
                },
            ) { innerPadding ->
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        (fadeIn(tween(Motion.ms(240), delayMillis = Motion.ms(40))) +
                            slideInVertically(tween(Motion.ms(280), easing = FastOutSlowInEasing)) { it / 28 })
                            .togetherWith(fadeOut(tween(Motion.ms(120))))
                    },
                    label = "abas",
                ) { t ->
                    when (t) {
                        Tab.CULTO -> HomeScreen(
                            contentPadding = innerPadding,
                            onDetectKey = { push(Dest.KeyFinder) },
                            onOpenKey = openKey,
                            onOpenRepertoire = { push(Dest.Repertoire) },
                            onOpenMetronome = { push(Dest.Metronome) },
                            onOpenCapo = { push(Dest.Capo) },
                            onSupport = { push(Dest.Donation) },
                            onOpenDevPanel = { push(Dest.DevPanel) },
                        )
                        Tab.CAMPOS -> CamposScreen(
                            favorites = favorites,
                            onOpenKey = openKey,
                            onPlayFullScreen = { prog, key -> push(Dest.Stage(prog, key)) },
                            contentPadding = innerPadding,
                        )
                        Tab.VIOLAO -> GuitarHubScreen(
                            contentPadding = innerPadding,
                            onOpenLab = { push(Dest.Guitar("G", it)) },
                            onOpenDictionary = openDictionary,
                            onOpenPassages = { push(Dest.Passages("G")) },
                        )
                        Tab.APRENDER -> LearnScreen(
                            onOpenKey = openKey,
                            contentPadding = innerPadding,
                            onOpenTrainer = { push(Dest.Trainer) },
                            onOpenGuitar = { push(Dest.Guitar("G", GuitarTab.NOTAS)) },
                            onOpenPassages = { push(Dest.Passages("G")) },
                            onOpenDictionary = openDictionary,
                            onOpenStudy = { push(Dest.Guitar("G", GuitarTab.DECORAR)) },
                        )
                        Tab.AJUSTES -> SettingsScreen(
                            contentPadding = innerPadding,
                            onReplayIntro = onReplayOpening,
                            onOpenAbout = { push(Dest.About) },
                            onOpenLegal = { push(Dest.Legal(it)) },
                            onSupport = { push(Dest.Donation) },
                            onOpenDevPanel = { push(Dest.DevPanel) },
                        )
                    }
                }
            }
        }

        // As telas abertas por cima, na ordem em que foram abertas.
        entries.forEach { entry ->
            key(entry) {
                val isTop = entry === open.lastOrNull()
                AnimatedVisibility(
                    visibleState = entry.visible,
                    enter = slideInHorizontally(tween(Motion.ms(320), easing = FastOutSlowInEasing)) { it / 5 } + fadeIn(tween(Motion.ms(220))),
                    exit = slideOutHorizontally(tween(Motion.ms(240), easing = FastOutSlowInEasing)) { it / 5 } + fadeOut(tween(Motion.ms(200))),
                ) {
                    AppBackground(
                        Modifier
                            .blockTouchesBelow()
                            .then(if (isTop) Modifier else Modifier.clearAndSetSemantics {}),
                    ) {
                        Screen(entry.dest, insets, favorites, ::toggleFavorite, ::push, ::pop, ::replace, openKey)
                    }
                }
                // Saiu da tela de vez: tira da lista.
                if (!entry.visible.targetState && entry.visible.isIdle) {
                    LaunchedEffect(entry) { entries.remove(entry) }
                }
            }
        }
    }
}

/** Monta a tela de um destino. */
@Composable
private fun Screen(
    dest: Dest,
    insets: PaddingValues,
    favorites: Set<String>,
    toggleFavorite: (String) -> Unit,
    push: (Dest) -> Unit,
    pop: () -> Unit,
    replace: (Dest) -> Unit,
    openKey: (String) -> Unit,
) {
    when (dest) {
        is Dest.Detail -> FieldDetailScreen(
            keyCipher = dest.key,
            isFavorite = favorites.contains(dest.key),
            onToggleFavorite = { toggleFavorite(dest.key) },
            onBack = pop,
            contentPadding = insets,
            onOpenGuitar = { push(Dest.Guitar(it, GuitarTab.ACORDES)) },
            onOpenPassages = { push(Dest.Passages(it)) },
        )
        is Dest.Guitar -> GuitarScreen(
            initialKey = dest.key,
            onBack = pop,
            contentPadding = insets,
            onOpenDictionary = { push(Dest.Dictionary(it)) },
            startTab = dest.tab,
        )
        is Dest.Passages -> PassagesScreen(
            initialKey = dest.key,
            onBack = pop,
            contentPadding = insets,
            onOpenDictionary = { push(Dest.Dictionary(it)) },
        )
        is Dest.Dictionary -> ChordDictionaryScreen(initial = dest.chord, onBack = pop, contentPadding = insets)
        is Dest.Stage -> ProgressionStageScreen(
            progression = dest.progression,
            keyCipher = dest.key,
            onBack = pop,
            contentPadding = insets,
        )
        is Dest.Legal -> LegalScreen(dest.doc, onBack = pop, contentPadding = insets)
        Dest.KeyFinder -> KeyFinderScreen(
            onOpenKey = { replace(Dest.Detail(it)) },
            onBack = pop,
            onOpenDevPanel = { replace(Dest.DevPanel) },
            contentPadding = insets,
        )
        Dest.Donation -> DonationScreen(onBack = pop, contentPadding = insets)
        Dest.Metronome -> MetronomeScreen(onBack = pop, contentPadding = insets)
        Dest.Capo -> CapoScreen(onBack = pop, contentPadding = insets)
        Dest.Repertoire -> RepertoireScreen(
            onBack = pop,
            contentPadding = insets,
            onOpenField = openKey,
            onOpenPassages = { push(Dest.Passages(it)) },
        )
        Dest.Trainer -> DegreeTrainerScreen(onBack = pop, contentPadding = insets)
        Dest.DevPanel -> com.example.ui.dev.DevPanelScreen(onBack = pop, contentPadding = insets)
        Dest.About -> AboutScreen(
            onBack = pop,
            contentPadding = insets,
            onOpenLegal = { push(Dest.Legal(it)) },
            onSupport = { push(Dest.Donation) },
        )
    }
}
