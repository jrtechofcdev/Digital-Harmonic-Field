package com.example

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.platform.LocalConfiguration
import com.example.music.rootPc
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlayScreen
import com.example.ui.screens.ProgressionsScreen
import com.example.ui.screens.SetlistScreen
import com.example.ui.screens.SetlistsScreen
import com.example.ui.screens.Tool
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
import com.example.ui.screens.CapoScreen
import com.example.ui.screens.ChordDictionaryScreen
import com.example.ui.screens.DegreeTrainerScreen
import com.example.ui.screens.DonationScreen
import com.example.ui.screens.GuitarScreen
import com.example.ui.screens.GuitarTab
import com.example.ui.screens.IntroVideo
import com.example.ui.screens.KeyFinderScreen
import com.example.ui.screens.LearnScreen
import com.example.ui.screens.LegalScreen
import com.example.ui.screens.MetronomeScreen
import com.example.ui.screens.PassagesScreen
import com.example.ui.screens.ProgressionStageScreen
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
    /** Atalho do ícone a abrir (Detectar tom, Repertório, Metrônomo). */
    private var shortcut by mutableStateOf<AppShortcut?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Versão DEV de treino: prepara a pasta de trechos (no app normal não faz nada).
        if (BuildConfig.DEV_TOOLS) com.example.dev.DevStore.init(applicationContext)
        com.example.ui.components.NotationSettings.init(applicationContext)
        com.example.data.Repertoire.init(applicationContext)
        com.example.data.Live.init(applicationContext)  // depois do repertório (confere a lista em andamento)
        com.example.ui.screens.DominantSettings.init(applicationContext)
        AppSettings.init(applicationContext)        // também aplica o tema (claro/escuro)
        AppShortcuts.publish(applicationContext)
        if (savedInstanceState == null) shortcut = AppShortcut.from(intent)
        // A abertura toca só quando o app é aberto de verdade (não ao girar a tela
        // nem por um atalho do ícone, que é para ir direto ao ponto).
        val playOpening = savedInstanceState == null && shortcut == null
        setContent {
            HarmonicTheme {
                AppRoot(playOpening, shortcut, onShortcutHandled = { shortcut = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        AppShortcut.from(intent)?.let { shortcut = it }
    }

    // A troca do ícone acontece quando o app sai da tela, para não fechar nada aberto.
    override fun onStop() {
        super.onStop()
        IconSwitcher.apply(applicationContext)
    }
}

/**
 * As 5 áreas do app, pelo que o músico faz no culto: tocar (o painel do tom),
 * seguir o repertório, consultar a biblioteca, aprender e ajustar.
 */
internal enum class Tab(val label: String, val icon: Int) {
    TOCAR("Tocar", R.drawable.ic_play_circle),          // o painel do tom do momento
    REPERTORIO("Repertório", R.drawable.ic_playlist),   // listas de hinos do culto
    BIBLIOTECA("Biblioteca", R.drawable.ic_books),      // busca, 24 tons, ferramentas
    APRENDER("Aprender", R.drawable.ic_learn),          // lições e treinos
    AJUSTES("Ajustes", R.drawable.ic_settings),         // configurações, sobre e legal
}

private enum class Startup { VIDEO, BRAND, APP }

/** "2026-11-08": o dia de hoje, para a abertura tocar só uma vez por dia. */
private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

/**
 * Abertura (vídeo) → tela JR TECH OFC → app. Cada etapa pode ser desligada nos
 * Ajustes, e por padrão a abertura toca só na primeira vez do dia (no culto o
 * app é aberto muitas vezes). "Ver a abertura agora" toca tudo de novo.
 */
@Composable
internal fun AppRoot(
    playOpening: Boolean,
    shortcut: AppShortcut? = null,
    onShortcutHandled: () -> Unit = {},
) {
    fun first(): Startup = when {
        !playOpening || !AppSettings.openingDue(today()) -> Startup.APP
        AppSettings.introVideo -> Startup.VIDEO
        AppSettings.brandScreen -> Startup.BRAND
        else -> Startup.APP
    }
    var stage by rememberSaveable { mutableStateOf(first()) }
    var aboutAtStart by rememberSaveable { mutableStateOf(false) }
    var lastTab by rememberSaveable { mutableStateOf(Tab.TOCAR) }
    LaunchedEffect(Unit) { if (stage != Startup.APP) AppSettings.markOpeningPlayed(today()) }

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
                shortcut = shortcut,
                onShortcutHandled = onShortcutHandled,
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
    data class Guitar(val key: String, val tab: Int) : Dest
    data class Passages(val key: String) : Dest
    data class Dictionary(val chord: LabChord) : Dest
    data class Stage(val progression: Progression, val key: String) : Dest
    data class Progressions(val key: String) : Dest
    data class Setlist(val id: Long) : Dest
    data class Metronome(val bpm: Int?) : Dest
    data class Capo(val key: String) : Dest
    data class Legal(val doc: LegalDoc) : Dest
    data object KeyFinder : Dest
    data object Donation : Dest
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

/** O acorde da tônica de um tom ("Am" → Lá menor), para abrir o dicionário. */
private fun tonicChord(key: String) =
    LabChord(rootPc(key) ?: 7, if (key.endsWith("m")) ChordType.MINOR else ChordType.MAJOR)

/** O que as telas podem pedir para a navegação. */
private class Nav(
    val push: (Dest) -> Unit,
    val pop: () -> Unit,
    val replace: (Dest) -> Unit,
    val playKey: (String) -> Unit,
    val showPlay: () -> Unit,
    val openTool: (Tool) -> Unit,
    val showTab: (Tab) -> Unit,
)

@Composable
internal fun HarmonicApp(
    startTab: Tab = Tab.TOCAR,
    openAboutAtStart: Boolean = false,
    onAboutShown: () -> Unit = {},
    onTabChange: (Tab) -> Unit = {},
    onReplayOpening: () -> Unit = {},
    shortcut: AppShortcut? = null,
    onShortcutHandled: () -> Unit = {},
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

    fun showTab(t: Tab) { tab = t; onTabChange(t) }
    fun push(d: Dest) { entries.add(Entry(d)) }
    fun pop() { entries.lastOrNull { it.visible.targetState }?.visible?.targetState = false }
    /** Fecha a tela de cima e abre outra no lugar dela. */
    fun replace(d: Dest) { pop(); push(d) }
    /** Fecha tudo e mostra a aba Tocar (o tom do momento). */
    fun showPlay() { entries.forEach { it.visible.targetState = false }; showTab(Tab.TOCAR) }
    /** Toca num tom: ele vira o tom do momento e a aba Tocar aparece. */
    fun playKey(k: String) { com.example.data.Live.setKey(k); showPlay() }
    /** Abre uma ferramenta já no tom do momento. */
    fun openTool(t: Tool) {
        val key = com.example.data.Live.key
        when (t) {
            Tool.PROGRESSOES -> push(Dest.Progressions(key))
            Tool.PASSAGENS -> push(Dest.Passages(key))
            Tool.BRACO -> push(Dest.Guitar(key, GuitarTab.NOTAS))
            Tool.ACORDES -> push(Dest.Guitar(key, GuitarTab.ACORDES))
            Tool.HARMONIZAR -> push(Dest.Guitar(key, GuitarTab.HARMONIZAR))
            Tool.DECORAR -> push(Dest.Guitar(key, GuitarTab.DECORAR))
            Tool.DICIONARIO -> push(Dest.Dictionary(tonicChord(key)))
            Tool.METRONOMO -> push(Dest.Metronome(com.example.data.Live.currentItem?.bpm))
            Tool.CAPO -> push(Dest.Capo(key))
            Tool.DETECTAR -> push(Dest.KeyFinder)
            Tool.TREINAR -> push(Dest.Trainer)
        }
    }
    val nav = Nav(::push, ::pop, ::replace, ::playKey, ::showPlay, ::openTool, ::showTab)

    LaunchedEffect(openAboutAtStart) {
        if (openAboutAtStart) { push(Dest.About); onAboutShown() }
    }
    // Atalho do ícone: vai direto para a ferramenta.
    LaunchedEffect(shortcut) {
        when (shortcut) {
            AppShortcut.DETECTAR -> { showTab(Tab.TOCAR); push(Dest.KeyFinder) }
            AppShortcut.REPERTORIO -> showTab(Tab.REPERTORIO)
            AppShortcut.METRONOMO -> push(Dest.Metronome(com.example.data.Live.currentItem?.bpm))
            null -> return@LaunchedEffect
        }
        onShortcutHandled()
    }

    // Voltar: fecha a tela de cima; nas abas, volta para o Tocar antes de sair.
    BackHandler(enabled = open.isNotEmpty() || tab != Tab.TOCAR) {
        if (open.isNotEmpty()) pop() else showTab(Tab.TOCAR)
    }

    val insets = WindowInsets.safeDrawing.asPaddingValues()
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val tabsState = rememberSaveableStateHolder()
    val tabContent: @Composable (PaddingValues) -> Unit = { padding ->
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                (fadeIn(tween(Motion.ms(240), delayMillis = Motion.ms(40))) +
                    slideInVertically(tween(Motion.ms(280), easing = FastOutSlowInEasing)) { it / 28 })
                    .togetherWith(fadeOut(tween(Motion.ms(120))))
            },
            label = "abas",
        ) { t ->
            // Cada aba guarda a própria rolagem e a busca ao trocar de aba.
            tabsState.SaveableStateProvider(t.name) {
                TabScreen(t, padding, favorites, ::toggleFavorite, nav, onReplayOpening)
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        // As abas (por baixo de tudo).
        AppBackground(if (open.isNotEmpty()) Modifier.clearAndSetSemantics {} else Modifier) {
            if (landscape) {
                // Deitado: abas numa barra lateral, para sobrar altura para os acordes.
                Row(Modifier.fillMaxSize()) {
                    NavigationRail(
                        containerColor = Surface1,
                        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical),
                    ) {
                        Spacer(Modifier.weight(1f))
                        Tab.entries.forEach { entry ->
                            NavigationRailItem(
                                selected = tab == entry,
                                onClick = { showTab(entry) },
                                icon = { Icon(painterResource(entry.icon), contentDescription = null, modifier = Modifier.size(22.dp)) },
                                label = { Text(entry.label, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = Ink,
                                    selectedTextColor = TextStrong,
                                    indicatorColor = Brass,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted,
                                ),
                            )
                        }
                        Spacer(Modifier.weight(1f))
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.End)),
                    ) {
                        tabContent(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical).asPaddingValues())
                    }
                }
            } else {
                Scaffold(
                    containerColor = Color.Transparent,
                    bottomBar = {
                        NavigationBar(containerColor = Surface1, tonalElevation = 0.dp) {
                            Tab.entries.forEach { entry ->
                                NavigationBarItem(
                                    selected = tab == entry,
                                    onClick = { showTab(entry) },
                                    icon = { Icon(painterResource(entry.icon), contentDescription = null, modifier = Modifier.size(24.dp)) },
                                    label = { Text(entry.label, style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false) },
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
                ) { innerPadding -> tabContent(innerPadding) }
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
                        Screen(entry.dest, insets, nav)
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

/** O conteúdo de uma aba. */
@Composable
private fun TabScreen(
    t: Tab,
    padding: PaddingValues,
    favorites: Set<String>,
    toggleFavorite: (String) -> Unit,
    nav: Nav,
    onReplayOpening: () -> Unit,
) {
    when (t) {
        Tab.TOCAR -> PlayScreen(
            contentPadding = padding,
            favorites = favorites,
            onToggleFavorite = toggleFavorite,
            onDetectKey = { nav.push(Dest.KeyFinder) },
            onOpenRepertoire = {
                com.example.data.Live.setlistId?.let { nav.push(Dest.Setlist(it)) } ?: nav.showTab(Tab.REPERTORIO)
            },
            onOpenTool = nav.openTool,
            onPlayFullScreen = { prog, key -> nav.push(Dest.Stage(prog, key)) },
            onOpenDictionary = { nav.push(Dest.Dictionary(it)) },
        )
        Tab.REPERTORIO -> SetlistsScreen(
            contentPadding = padding,
            onOpenList = { nav.push(Dest.Setlist(it)) },
            onPlay = nav.showPlay,
        )
        Tab.BIBLIOTECA -> LibraryScreen(
            contentPadding = padding,
            favorites = favorites,
            onPlayKey = nav.playKey,
            onOpenTool = nav.openTool,
            onOpenChord = { nav.push(Dest.Dictionary(it)) },
            onOpenDevPanel = { nav.push(Dest.DevPanel) },
        )
        Tab.APRENDER -> LearnScreen(
            onOpenKey = nav.playKey,
            contentPadding = padding,
            onOpenTrainer = { nav.push(Dest.Trainer) },
            onOpenGuitar = { nav.openTool(Tool.BRACO) },
            onOpenPassages = { nav.openTool(Tool.PASSAGENS) },
            onOpenDictionary = { nav.openTool(Tool.DICIONARIO) },
            onOpenStudy = { nav.openTool(Tool.DECORAR) },
        )
        Tab.AJUSTES -> SettingsScreen(
            contentPadding = padding,
            onReplayIntro = onReplayOpening,
            onOpenAbout = { nav.push(Dest.About) },
            onOpenLegal = { nav.push(Dest.Legal(it)) },
            onSupport = { nav.push(Dest.Donation) },
            onOpenDevPanel = { nav.push(Dest.DevPanel) },
        )
    }
}

/** Monta a tela de um destino. */
@Composable
private fun Screen(dest: Dest, insets: PaddingValues, nav: Nav) {
    val pop = nav.pop
    when (dest) {
        is Dest.Guitar -> GuitarScreen(
            initialKey = dest.key,
            onBack = pop,
            contentPadding = insets,
            onOpenDictionary = { nav.push(Dest.Dictionary(it)) },
            startTab = dest.tab,
        )
        is Dest.Passages -> PassagesScreen(
            initialKey = dest.key,
            onBack = pop,
            contentPadding = insets,
            onOpenDictionary = { nav.push(Dest.Dictionary(it)) },
        )
        is Dest.Dictionary -> ChordDictionaryScreen(initial = dest.chord, onBack = pop, contentPadding = insets)
        is Dest.Stage -> ProgressionStageScreen(
            progression = dest.progression,
            keyCipher = dest.key,
            onBack = pop,
            contentPadding = insets,
        )
        is Dest.Progressions -> ProgressionsScreen(
            initialKey = dest.key,
            onBack = pop,
            onPlayKey = nav.playKey,
            onPlayFullScreen = { prog, key -> nav.push(Dest.Stage(prog, key)) },
            contentPadding = insets,
        )
        is Dest.Setlist -> SetlistScreen(
            listId = dest.id,
            onBack = pop,
            contentPadding = insets,
            onPlay = nav.showPlay,
            onOpenList = { nav.replace(Dest.Setlist(it)) },
        )
        is Dest.Metronome -> MetronomeScreen(onBack = pop, contentPadding = insets, initialBpm = dest.bpm)
        is Dest.Capo -> CapoScreen(onBack = pop, contentPadding = insets, initialKey = dest.key)
        is Dest.Legal -> LegalScreen(dest.doc, onBack = pop, contentPadding = insets)
        Dest.KeyFinder -> KeyFinderScreen(
            onOpenKey = nav.playKey,
            onBack = pop,
            onOpenDevPanel = { nav.replace(Dest.DevPanel) },
            contentPadding = insets,
        )
        Dest.Donation -> DonationScreen(onBack = pop, contentPadding = insets)
        Dest.Trainer -> DegreeTrainerScreen(onBack = pop, contentPadding = insets)
        Dest.DevPanel -> com.example.ui.dev.DevPanelScreen(onBack = pop, contentPadding = insets)
        Dest.About -> AboutScreen(
            onBack = pop,
            contentPadding = insets,
            onOpenLegal = { nav.push(Dest.Legal(it)) },
            onSupport = { nav.push(Dest.Donation) },
        )
    }
}
