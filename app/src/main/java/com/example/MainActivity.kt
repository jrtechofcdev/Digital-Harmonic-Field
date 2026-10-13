package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.music.ChordType
import com.example.music.LabChord
import com.example.ui.screens.CamposScreen
import com.example.ui.screens.CapoScreen
import com.example.ui.screens.ChordDictionaryScreen
import com.example.ui.screens.DegreeTrainerScreen
import com.example.ui.screens.DonationScreen
import com.example.ui.screens.GuitarHubScreen
import com.example.ui.screens.GuitarScreen
import com.example.ui.screens.GuitarTab
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MetronomeScreen
import com.example.ui.screens.PassagesScreen
import com.example.ui.screens.RepertoireScreen
import com.example.ui.theme.TextStrong
import com.example.ui.screens.FieldDetailScreen
import com.example.ui.screens.LearnScreen
import com.example.ui.screens.KeyFinderScreen
import com.example.ui.screens.ProgressionStageScreen
import com.example.music.Progression
import com.example.ui.theme.Brass
import com.example.ui.theme.HarmonicTheme
import com.example.ui.theme.Ink
import com.example.ui.theme.Surface1
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// Persistência dos tons favoritos.
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "favorites_prefs")
private val FAVORITES_KEY = stringSetPreferencesKey("favorite_keys")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Versão DEV de treino: prepara a pasta de trechos (no app normal não faz nada).
        if (BuildConfig.DEV_TOOLS) com.example.dev.DevStore.init(applicationContext)
        // Cifra ou grau: preferência global salva no aparelho.
        com.example.ui.components.NotationSettings.init(applicationContext)
        com.example.data.Repertoire.init(applicationContext)
        com.example.ui.screens.DominantSettings.init(applicationContext)
        setContent {
            HarmonicTheme {
                HarmonicApp()
            }
        }
    }
}

/** As 4 áreas do app, pelo que o músico quer fazer. */
private enum class Tab(val label: String, val icon: Int) {
    CULTO("Culto", R.drawable.ic_church),         // na hora de tocar
    CAMPOS("Campos", R.drawable.ic_music_notes),   // tons e progressões
    VIOLAO("Violão", R.drawable.ic_guitar),        // tudo no braço
    APRENDER("Aprender", R.drawable.ic_learn),     // lições e treinos
}

@Composable
internal fun HarmonicApp() {
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

    var tab by rememberSaveable { mutableStateOf(Tab.CULTO) }
    var detailKey by remember { mutableStateOf<String?>(null) }
    var showDonation by remember { mutableStateOf(false) }
    var showKeyFinder by remember { mutableStateOf(false) }
    var stage by remember { mutableStateOf<Pair<Progression, String>?>(null) }
    var showDevPanel by remember { mutableStateOf(false) }
    var showTrainer by remember { mutableStateOf(false) }
    var guitar by remember { mutableStateOf<Pair<String, Int>?>(null) }     // (tom, aba interna)
    var dictionary by remember { mutableStateOf<LabChord?>(null) }
    var passagesKey by remember { mutableStateOf<String?>(null) }
    var showRepertoire by remember { mutableStateOf(false) }
    var showMetronome by remember { mutableStateOf(false) }
    var showCapo by remember { mutableStateOf(false) }

    BackHandler(
        enabled = detailKey != null || showDonation || showKeyFinder || stage != null || showDevPanel ||
            showTrainer || guitar != null || dictionary != null || passagesKey != null || showRepertoire ||
            showMetronome || showCapo,
    ) {
        when {
            dictionary != null -> dictionary = null
            guitar != null -> guitar = null
            passagesKey != null -> passagesKey = null
            showTrainer -> showTrainer = false
            showDevPanel -> showDevPanel = false
            stage != null -> stage = null
            showKeyFinder -> showKeyFinder = false
            showDonation -> showDonation = false
            showMetronome -> showMetronome = false
            showCapo -> showCapo = false
            detailKey != null -> detailKey = null
            else -> showRepertoire = false
        }
    }

    val openKey: (String) -> Unit = { detailKey = it }
    val openDictionary: () -> Unit = { dictionary = LabChord(7, ChordType.MAJOR) }
    val insets = WindowInsets.safeDrawing.asPaddingValues()

    @Composable
    fun Overlay(content: @Composable () -> Unit) =
        Surface(color = Ink, modifier = Modifier.fillMaxSize()) { content() }

    // O dicionário de acordes abre POR CIMA da tela atual (braço do violão,
    // passagens…): ao voltar, a tela de baixo continua exatamente onde estava.
    Box(Modifier.fillMaxSize()) {
        when {
            guitar != null -> Overlay {
                GuitarScreen(
                    initialKey = guitar!!.first,
                    onBack = { guitar = null },
                    contentPadding = insets,
                    onOpenDictionary = { dictionary = it },
                    startTab = guitar!!.second,
                )
            }
            passagesKey != null -> Overlay {
                PassagesScreen(
                    initialKey = passagesKey!!,
                    onBack = { passagesKey = null },
                    contentPadding = insets,
                    onOpenDictionary = { dictionary = it },
                )
            }
            showTrainer -> Overlay { DegreeTrainerScreen(onBack = { showTrainer = false }, contentPadding = insets) }
            showDevPanel -> Overlay { com.example.ui.dev.DevPanelScreen(onBack = { showDevPanel = false }, contentPadding = insets) }
            stage != null -> Overlay {
                ProgressionStageScreen(
                    progression = stage!!.first,
                    keyCipher = stage!!.second,
                    onBack = { stage = null },
                    contentPadding = insets,
                )
            }
            showKeyFinder -> Overlay {
                KeyFinderScreen(
                    onOpenKey = { showKeyFinder = false; openKey(it) },
                    onBack = { showKeyFinder = false },
                    onOpenDevPanel = { showKeyFinder = false; showDevPanel = true },
                    contentPadding = insets,
                )
            }
            showDonation -> Overlay { DonationScreen(onBack = { showDonation = false }, contentPadding = insets) }
            showMetronome -> Overlay { MetronomeScreen(onBack = { showMetronome = false }, contentPadding = insets) }
            showCapo -> Overlay { CapoScreen(onBack = { showCapo = false }, contentPadding = insets) }
            detailKey != null -> Overlay {
                FieldDetailScreen(
                    keyCipher = detailKey!!,
                    isFavorite = favorites.contains(detailKey),
                    onToggleFavorite = { detailKey?.let { toggleFavorite(it) } },
                    onBack = { detailKey = null },
                    contentPadding = insets,
                    onOpenGuitar = { guitar = it to GuitarTab.ACORDES },
                    onOpenPassages = { passagesKey = it },
                )
            }
            showRepertoire -> Overlay {
                RepertoireScreen(
                    onBack = { showRepertoire = false },
                    contentPadding = insets,
                    onOpenField = { detailKey = it },
                    onOpenPassages = { passagesKey = it },
                )
            }
            else -> Scaffold(
                containerColor = Ink,
                bottomBar = {
                    NavigationBar(containerColor = Surface1, tonalElevation = 0.dp) {
                        Tab.entries.forEach { entry ->
                            NavigationBarItem(
                                selected = tab == entry,
                                onClick = { tab = entry },
                                icon = { Icon(painterResource(entry.icon), contentDescription = null, modifier = Modifier.size(24.dp)) },
                                label = { Text(entry.label, style = MaterialTheme.typography.labelMedium) },
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
                when (tab) {
                    Tab.CULTO -> HomeScreen(
                        contentPadding = innerPadding,
                        onDetectKey = { showKeyFinder = true },
                        onOpenKey = openKey,
                        onOpenRepertoire = { showRepertoire = true },
                        onOpenMetronome = { showMetronome = true },
                        onOpenCapo = { showCapo = true },
                        onSupport = { showDonation = true },
                        onOpenDevPanel = { showDevPanel = true },
                    )
                    Tab.CAMPOS -> CamposScreen(
                        favorites = favorites,
                        onOpenKey = openKey,
                        onPlayFullScreen = { prog, key -> stage = prog to key },
                        contentPadding = innerPadding,
                    )
                    Tab.VIOLAO -> GuitarHubScreen(
                        contentPadding = innerPadding,
                        onOpenLab = { guitar = "G" to it },
                        onOpenDictionary = openDictionary,
                        onOpenPassages = { passagesKey = "G" },
                    )
                    Tab.APRENDER -> LearnScreen(
                        onOpenKey = openKey,
                        contentPadding = innerPadding,
                        onOpenTrainer = { showTrainer = true },
                        onOpenGuitar = { guitar = "G" to GuitarTab.NOTAS },
                        onOpenPassages = { passagesKey = "G" },
                        onOpenDictionary = openDictionary,
                        onOpenStudy = { guitar = "G" to GuitarTab.DECORAR },
                    )
                }
            }
        }
        dictionary?.let { chord ->
            Overlay { ChordDictionaryScreen(initial = chord, onBack = { dictionary = null }, contentPadding = insets) }
        }
    }
}
