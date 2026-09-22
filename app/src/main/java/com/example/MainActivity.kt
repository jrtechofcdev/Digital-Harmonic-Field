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
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.screens.FieldDetailScreen
import com.example.ui.screens.FieldsScreen
import com.example.ui.screens.LearnScreen
import com.example.ui.screens.ProgressionsScreen
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
        setContent {
            HarmonicTheme {
                HarmonicApp()
            }
        }
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    CAMPOS("Campos", Icons.Filled.Home),
    PROGRESSOES("Progressões", Icons.AutoMirrored.Filled.List),
    APRENDER("Aprender", Icons.Filled.Info),
    FERRAMENTAS("Ferramentas", Icons.Filled.Settings),
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

    BackHandler(enabled = detailKey != null) { detailKey = null }

    val openKey: (String) -> Unit = { detailKey = it }

    if (detailKey != null) {
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
                            icon = { Icon(entry.icon, contentDescription = entry.label) },
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
                Tab.CAMPOS -> FieldsScreen(favorites, openKey, innerPadding)
                Tab.PROGRESSOES -> ProgressionsScreen(openKey, innerPadding)
                Tab.APRENDER -> LearnScreen(openKey, innerPadding)
                Tab.FERRAMENTAS -> ToolsScreen(innerPadding)
            }
        }
    }
}
