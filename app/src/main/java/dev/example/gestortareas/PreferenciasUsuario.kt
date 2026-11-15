package dev.example.gestortareas

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(
    name = "preferencias_usuario"
)

class PreferenciasUsuario(private val context: Context) {

    private val SOLO_PENDIENTES =
        booleanPreferencesKey("solo_pendientes")

    suspend fun leerSoloPendientes(): Boolean {
        val preferencias = context.dataStore.data.first()
        return preferencias[SOLO_PENDIENTES] ?: false
    }

    suspend fun guardarSoloPendientes(valor: Boolean) {
        context.dataStore.edit { preferencias ->
            preferencias[SOLO_PENDIENTES] = valor
        }
    }
}