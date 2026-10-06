package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.screens.DonationScreen
import com.example.ui.screens.FieldDetailScreen
import com.example.ui.screens.FieldsScreen
import com.example.ui.screens.LearnScreen
import com.example.ui.screens.KeyFinderScreen
import com.example.ui.screens.ProgressionStageScreen
import com.example.ui.screens.ProgressionsScreen
import com.example.music.Progression
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.Brass
import com.example.ui.theme.HarmonicTheme
import com.example.ui.theme.Hairline
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
        setContent {
            HarmonicTheme {
                HarmonicApp()
            }
        }
    }
}

private enum class Tab(
    val label: String,
    val iconVector: ImageVector? = null,
    val iconRes: Int? = null,
) {
    CAMPOS("Campos", iconVector = Icons.Filled.Home),
    PROGRESSOES("Progressões", iconVector = Icons.AutoMirrored.Filled.List),
    APRENDER("Aprender", iconVector = Icons.Filled.Info),
    FERRAMENTAS("Ferramentas", iconVector = Icons.Filled.Settings),
}

@Composable
private fun HarmonicApp() {
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

    var tab by remember { mutableStateOf(Tab.CAMPOS) }
    var detailKey by remember { mutableStateOf<String?>(null) }
    var showDonation by remember { mutableStateOf(false) }
    var showKeyFinder by remember { mutableStateOf(false) }
    var stage by remember { mutableStateOf<Pair<Progression, String>?>(null) }
    var showDevPanel by remember { mutableStateOf(false) }

    BackHandler(enabled = detailKey != null || showDonation || showKeyFinder || stage != null || showDevPanel) {
        when {
            showDevPanel -> showDevPanel = false
            stage != null -> stage = null
            showKeyFinder -> showKeyFinder = false
            showDonation -> showDonation = false
            else -> detailKey = null
        }
    }

    val openKey: (String) -> Unit = { detailKey = it }
    val insets = WindowInsets.safeDrawing.asPaddingValues()

    if (showDevPanel) {
        Surface(color = Ink, modifier = Modifier.fillMaxSize()) {
            com.example.ui.dev.DevPanelScreen(onBack = { showDevPanel = false }, contentPadding = insets)
        }
    } else if (stage != null) {
        Surface(color = Ink, modifier = Modifier.fillMaxSize()) {
            ProgressionStageScreen(
                progression = stage!!.first,
                keyCipher = stage!!.second,
                onBack = { stage = null },
                contentPadding = insets,
            )
        }
    } else if (showKeyFinder) {
        Surface(color = Ink, modifier = Modifier.fillMaxSize()) {
            KeyFinderScreen(
                onOpenKey = { showKeyFinder = false; openKey(it) },
                onBack = { showKeyFinder = false },
                onOpenDevPanel = { showKeyFinder = false; showDevPanel = true },
                contentPadding = insets,
            )
        }
    } else if (showDonation) {
        Surface(color = Ink, modifier = Modifier.fillMaxSize()) {
            DonationScreen(
                onBack = { showDonation = false },
                contentPadding = insets,
            )
        }
    } else if (detailKey != null) {
        Surface(color = Ink, modifier = Modifier.fillMaxSize()) {
            FieldDetailScreen(
                keyCipher = detailKey!!,
                isFavorite = favorites.contains(detailKey),
                onToggleFavorite = { detailKey?.let { toggleFavorite(it) } },
                onBack = { detailKey = null },
                contentPadding = WindowInsets.safeDrawing.asPaddingValues(),
            )
        }
    } else {
        Scaffold(
            containerColor = Ink,
            bottomBar = {
                NavigationBar(containerColor = Surface1, tonalElevation = 0.dp) {
                    Tab.entries.forEach { entry ->
                        NavigationBarItem(
                            selected = tab == entry,
                            onClick = { tab = entry },
                            icon = {
                                when {
                                    entry.iconRes != null -> Icon(
                                        painterResource(entry.iconRes),
                                        contentDescription = entry.label,
                                        modifier = Modifier.size(22.dp),
                                    )
                                    entry.iconVector != null -> Icon(entry.iconVector, contentDescription = entry.label)
                                }
                            },
                            label = { Text(entry.label, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Ink,
                                selectedTextColor = Brass,
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
                Tab.CAMPOS -> FieldsScreen(
                    favorites = favorites,
                    onOpenKey = openKey,
                    onDetectKey = { showKeyFinder = true },
                    onSupport = { showDonation = true },
                    contentPadding = innerPadding,
                )
                Tab.PROGRESSOES -> ProgressionsScreen(
                    onOpenKey = openKey,
                    onPlayFullScreen = { prog, key -> stage = prog to key },
                    contentPadding = innerPadding,
                )
                Tab.APRENDER -> LearnScreen(openKey, innerPadding)
                Tab.FERRAMENTAS -> ToolsScreen(
                    onOpenKeyFinder = { showKeyFinder = true },
                    onOpenDevPanel = { showDevPanel = true },
                    contentPadding = innerPadding,
                )
            }
        }
    }
}
