package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Precisão = quão perto do centro precisa estar para marcar "afinado" (em cents). */
enum class Precision(val cents: Double, val label: String) {
    RELAXADA(8.0, "Relaxada"),
    PADRAO(5.0, "Padrão"),
    ALTA(3.0, "Alta");

    companion object {
        fun from(name: String?) = entries.firstOrNull { it.name == name } ?: PADRAO
    }
}

/** Filtro de ruído = exigência de sinal limpo para aceitar uma leitura. */
enum class NoiseFilter(val minClarity: Double, val silenceRms: Double, val label: String) {
    BAIXO(0.85, 0.004, "Baixo"),
    PADRAO(0.90, 0.006, "Padrão"),
    ALTO(0.94, 0.010, "Alto");

    companion object {
        fun from(name: String?) = entries.firstOrNull { it.name == name } ?: PADRAO
    }
}

data class TunerSettings(
    val refA: Float = 440f,
    val precision: Precision = Precision.PADRAO,
    val noiseFilter: NoiseFilter = NoiseFilter.PADRAO,
    val sounds: Boolean = true,
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "tuner_settings")

private val KEY_REF_A = floatPreferencesKey("ref_a")
private val KEY_PRECISION = stringPreferencesKey("precision")
private val KEY_NOISE = stringPreferencesKey("noise_filter")
private val KEY_SOUNDS = booleanPreferencesKey("sounds")

fun Context.tunerSettingsFlow(): Flow<TunerSettings> = settingsDataStore.data.map { p ->
    TunerSettings(
        refA = p[KEY_REF_A] ?: 440f,
        precision = Precision.from(p[KEY_PRECISION]),
        noiseFilter = NoiseFilter.from(p[KEY_NOISE]),
        sounds = p[KEY_SOUNDS] ?: true,
    )
}

suspend fun Context.saveTunerSettings(settings: TunerSettings) {
    settingsDataStore.edit { p ->
        p[KEY_REF_A] = settings.refA
        p[KEY_PRECISION] = settings.precision.name
        p[KEY_NOISE] = settings.noiseFilter.name
        p[KEY_SOUNDS] = settings.sounds
    }
}
