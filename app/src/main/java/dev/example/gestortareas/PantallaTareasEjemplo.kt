package dev.example.gestortareas

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.JsonParseException
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

@Composable
fun PantallaTareasEjemplo(
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tareas = remember { mutableStateOf<List<TareaEjemplo>>(emptyList()) }
    val cargando = remember { mutableStateOf(false) }
    val mensajeError = remember { mutableStateOf("") }
    val consultaRealizada = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun cargarTareas() {
        if (cargando.value) return

        cargando.value = true
        mensajeError.value = ""
        consultaRealizada.value = true

        scope.launch {
            try {
                tareas.value = ClienteApi.servicio.obtenerTareas()
            } catch (e: IOException) {
                mensajeError.value = "Problema de conexión. Comprueba tu acceso a Internet."
            } catch (e: HttpException) {
                mensajeError.value = "El servidor respondió con un error (código ${e.code()})."
            } catch (e: JsonParseException) {
                mensajeError.value = "No se pudieron interpretar los datos recibidos."
            } finally {
                cargando.value = false
            }
        }
    }

    Column(modifier = modifier.padding(16.dp)) {
        Text(text = "Tareas de ejemplo")

        Button(onClick = onVolver) {
            Text(text = "Volver")
        }

        Button(
            enabled = !cargando.value,
            onClick = { cargarTareas() }
        ) {
            Text(text = "Cargar tareas")
        }

        when {
            cargando.value -> {
                CircularProgressIndicator()
            }
            mensajeError.value.isNotEmpty() -> {
                Text(text = mensajeError.value)
                Button(onClick = { cargarTareas() }) {
                    Text(text = "Reintentar")
                }
            }
            !consultaRealizada.value -> {
                Text(text = "Pulsa Cargar tareas para consultar ejemplos")
            }
            tareas.value.isEmpty() -> {
                Text(text = "No se recibieron tareas")
            }
            else -> {
                LazyColumn {
                    items(
                        items = tareas.value,
                        key = { tarea -> tarea.id }
                    ) { tarea ->
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(text = "Id: ${tarea.id}")
                            Text(text = tarea.title)
                            if (tarea.completed) {
                                Text(text = "Estado: completada")
                            } else {
                                Text(text = "Estado: pendiente")
                            }
                        }
                    }
                }
            }
        }
    }
}