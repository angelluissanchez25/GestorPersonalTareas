package dev.example.gestortareas

import android.database.sqlite.SQLiteException
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.example.gestortareas.ui.theme.GestorTareasTheme
import kotlinx.coroutines.launch
import java.io.IOException

private val GuardadorTarea = listSaver<Tarea?, Any>(
    save = { tarea ->
        if (tarea == null) {
            emptyList()
        } else {
            listOf(
                tarea.id,
                tarea.titulo,
                tarea.descripcion,
                tarea.prioridad,
                tarea.completada
            )
        }
    },
    restore = { valores ->
        if (valores.isEmpty()) {
            null
        } else {
            Tarea(
                id = valores[0] as Int,
                titulo = valores[1] as String,
                descripcion = valores[2] as String,
                prioridad = valores[3] as String,
                completada = valores[4] as Boolean
            )
        }
    }
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tareaDao = TareasDatabase.obtener(this).tareaDao()
        val preferenciasUsuario = PreferenciasUsuario(applicationContext)

        enableEdgeToEdge()
        setContent {
            val scope = rememberCoroutineScope()
            val anchoVentana = LocalWindowInfo.current.containerSize.width
            val pantallaAncha = with(LocalDensity.current) { anchoVentana.toDp() >= 600.dp }
            val listaTareas = remember { mutableStateOf<List<Tarea>>(emptyList()) }
            val cargandoLista = remember { mutableStateOf(true) }
            val errorCargaLista = remember { mutableStateOf(false) }
            val guardando = remember { mutableStateOf(false) }
            val mensajeError = remember { mutableStateOf("") }
            val tareaEnEdicion = rememberSaveable(stateSaver = GuardadorTarea) {
                mutableStateOf<Tarea?>(null)
            }
            val mostrarFormulario = rememberSaveable { mutableStateOf(false) }
            val mostrarEjemplos = rememberSaveable { mutableStateOf(false) }
            val soloPendientes = remember { mutableStateOf(false) }
            val filtroDisponible = remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                try {
                    listaTareas.value = tareaDao.obtenerTodas()
                } catch (e: SQLiteException) {
                    errorCargaLista.value = true
                } finally {
                    cargandoLista.value = false
                }
            }

            LaunchedEffect(Unit) {
                try {
                    soloPendientes.value = preferenciasUsuario.leerSoloPendientes()
                } catch (e: IOException) {
                    mensajeError.value = "No se pudo leer la preferencia del filtro."
                } finally {
                    filtroDisponible.value = true
                }
            }

            val tareasVisibles = if (soloPendientes.value) {
                listaTareas.value.filter { tarea -> !tarea.completada }
            } else {
                listaTareas.value
            }

            GestorTareasTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (mostrarEjemplos.value) {
                        PantallaTareasEjemplo(
                            onVolver = { mostrarEjemplos.value = false },
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else if (mostrarFormulario.value) {
                        Column(modifier = Modifier.padding(innerPadding)) {
                            FormularioTarea(
                                guardando = guardando.value,
                                tareaInicial = tareaEnEdicion.value,
                                onCancelar = {
                                    tareaEnEdicion.value = null
                                    mensajeError.value = ""
                                    mostrarFormulario.value = false
                                },
                                onGuardar = { tareaRecibida ->
                                    if (!guardando.value) {
                                        guardando.value = true
                                        mensajeError.value = ""

                                        scope.launch {
                                            try {
                                                var guardada = false

                                                try {
                                                    if (tareaRecibida.id == 0) {
                                                        tareaDao.insertar(tareaRecibida)
                                                    } else {
                                                        tareaDao.actualizar(tareaRecibida)
                                                    }
                                                    guardada = true
                                                } catch (e: SQLiteException) {
                                                    mensajeError.value = "No se pudo guardar la tarea."
                                                }

                                                if (guardada) {
                                                    tareaEnEdicion.value = null
                                                    mostrarFormulario.value = false

                                                    try {
                                                        listaTareas.value = tareaDao.obtenerTodas()
                                                        errorCargaLista.value = false
                                                    } catch (e: SQLiteException) {
                                                        mensajeError.value = "La tarea se guardó, pero no se pudo actualizar la lista."
                                                    }
                                                }
                                            } finally {
                                                guardando.value = false
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )

                            if (mensajeError.value.isNotEmpty()) {
                                Text(text = mensajeError.value)
                            }
                        }
                    } else {
                        Column(modifier = Modifier.padding(innerPadding)) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_tareas),
                                    contentDescription = null
                                )
                                Text(text = "Mis tareas")
                            }

                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        tareaEnEdicion.value = null
                                        mensajeError.value = ""
                                        mostrarFormulario.value = true
                                    }
                                ) {
                                    Text(text = "Nueva tarea")
                                }
                                TextButton(
                                    onClick = {
                                        mensajeError.value = ""
                                        mostrarEjemplos.value = true
                                    }
                                ) {
                                    Text(text = "Tareas de ejemplo")
                                }
                            }

                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Mostrar solo pendientes")
                                Switch(
                                    checked = soloPendientes.value,
                                    enabled = filtroDisponible.value,
                                    onCheckedChange = { nuevoValor ->
                                        if (filtroDisponible.value) {
                                            filtroDisponible.value = false
                                            mensajeError.value = ""

                                            scope.launch {
                                                try {
                                                    preferenciasUsuario.guardarSoloPendientes(nuevoValor)
                                                    soloPendientes.value = nuevoValor
                                                } catch (e: IOException) {
                                                    mensajeError.value = "No se pudo guardar la preferencia del filtro."
                                                } finally {
                                                    filtroDisponible.value = true
                                                }
                                            }
                                        }
                                    }
                                )
                            }

                            if (mensajeError.value.isNotEmpty()) {
                                Text(text = mensajeError.value)
                            }

                            if (cargandoLista.value) {
                                CircularProgressIndicator()
                            } else if (errorCargaLista.value && listaTareas.value.isEmpty()) {
                                Text(text = "No se pudieron cargar las tareas.")
                            } else if (tareasVisibles.isEmpty()) {
                                if (soloPendientes.value) {
                                    Text(text = "No hay tareas pendientes")
                                } else {
                                    Text(text = "Todavía no hay tareas")
                                }
                            } else {
                                LazyColumn {
                                    items(
                                        items = tareasVisibles,
                                        key = { tarea -> tarea.id }
                                    ) { tarea ->
                                        MostrarTarea(
                                            tarea = tarea,
                                            pantallaAncha = pantallaAncha,
                                            onCambiarEstado = { nuevoEstado ->
                                                mensajeError.value = ""

                                                scope.launch {
                                                    try {
                                                        val tareaActualizada = tarea.copy(completada = nuevoEstado)
                                                        tareaDao.actualizar(tareaActualizada)
                                                        listaTareas.value = tareaDao.obtenerTodas()
                                                    } catch (e: SQLiteException) {
                                                        mensajeError.value = "No se pudo actualizar la tarea."
                                                    }
                                                }
                                            },
                                            onEditar = {
                                                tareaEnEdicion.value = tarea
                                                mensajeError.value = ""
                                                mostrarFormulario.value = true
                                            },
                                            onCompartir = {
                                                mensajeError.value = ""

                                                val selectorAbierto = compartirTarea(this@MainActivity, tarea)

                                                if (!selectorAbierto) {
                                                    mensajeError.value =
                                                        "No se pudo abrir una aplicación para compartir la tarea."
                                                }
                                            },
                                            onEliminar = {
                                                mensajeError.value = ""

                                                scope.launch {
                                                    try {
                                                        tareaDao.eliminar(tarea)
                                                        listaTareas.value = tareaDao.obtenerTodas()
                                                    } catch (e: SQLiteException) {
                                                        mensajeError.value = "No se pudo completar la eliminación o actualizar la lista"
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GestorTareasTheme {
        MostrarTarea(
            tarea = Tarea(titulo = "Tarea de ejemplo", prioridad = "Alta"),
            onCambiarEstado = { },
            onEditar = { },
            onCompartir = { },
            onEliminar = { }
        )
    }
}

@Composable
fun ConfirmarEliminacion(
    tarea: Tarea,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(text = "Eliminar tarea") },
        text = { Text(text = "¿Seguro que quieres eliminar la tarea \"${tarea.titulo}\"?") },
        confirmButton = {
            TextButton(onClick = onConfirmar) {
                Text(text = "Eliminar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text(text = "Cancelar")
            }
        }
    )
}

@Composable
fun DatosTarea(tarea: Tarea) {
    Text(text = tarea.titulo)
    Text(text = "Prioridad: ${tarea.prioridad}")
    if (tarea.completada) {
        Text(text = "Completada")
    } else {
        Text(text = "Pendiente")
    }
}

@Composable
fun MostrarTarea(
    tarea: Tarea,
    onCambiarEstado: (Boolean) -> Unit,
    onEditar: () -> Unit,
    onCompartir: () -> Unit,
    onEliminar: () -> Unit,
    modifier: Modifier = Modifier,
    pantallaAncha: Boolean = false
) {
    val mostrarConfirmacion = remember { mutableStateOf(false) }

    if (pantallaAncha) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = tarea.completada,
                onCheckedChange = onCambiarEstado
            )
            Column(modifier = Modifier.weight(1f)) {
                DatosTarea(tarea = tarea)
            }
            TextButton(onClick = onEditar) {
                Text(text = "Editar")
            }
            TextButton(onClick = onCompartir) {
                Text(text = "Compartir")
            }
            TextButton(
                onClick = {
                    mostrarConfirmacion.value = true
                }
            ) {
                Text(text = "Eliminar")
            }
        }
    } else {
        Column(modifier = modifier.padding(16.dp)) {
            DatosTarea(tarea = tarea)
            Checkbox(
                checked = tarea.completada,
                onCheckedChange = onCambiarEstado
            )
            TextButton(onClick = onEditar) {
                Text(text = "Editar")
            }
            TextButton(onClick = onCompartir) {
                Text(text = "Compartir")
            }
            TextButton(
                onClick = {
                    mostrarConfirmacion.value = true
                }
            ) {
                Text(text = "Eliminar")
            }
        }
    }

    if (mostrarConfirmacion.value) {
        ConfirmarEliminacion(
            tarea = tarea,
            onConfirmar = {
                mostrarConfirmacion.value = false
                onEliminar()
            },
            onCancelar = {
                mostrarConfirmacion.value = false
            }
        )
    }
}

@Composable
fun FormularioTarea(
    guardando: Boolean,
    onCancelar: () -> Unit,
    onGuardar: (Tarea) -> Unit,
    modifier: Modifier = Modifier,
    tareaInicial: Tarea? = null
) {
    val titulo = rememberSaveable(tareaInicial) {
        mutableStateOf(tareaInicial?.titulo ?: "")
    }
    val descripcion = rememberSaveable(tareaInicial) {
        mutableStateOf(tareaInicial?.descripcion ?: "")
    }
    val prioridad = rememberSaveable(tareaInicial) {
        mutableStateOf(tareaInicial?.prioridad ?: "Media")
    }
    val menuAbierto = remember(tareaInicial) { mutableStateOf(false) }
    val intentoGuardar = rememberSaveable(tareaInicial) { mutableStateOf(false) }
    val tituloLimpio = titulo.value.trim()
    val estadoScroll = rememberScrollState()

    val prioridadesPermitidas = listOf("Baja", "Media", "Alta")
    val prioridadValida = prioridad.value in prioridadesPermitidas

    Column(modifier = modifier.verticalScroll(estadoScroll)) {
        OutlinedTextField(
            value = titulo.value,
            onValueChange = { nuevoTexto ->
                titulo.value = nuevoTexto
            },
            label = { Text(text = "Título de la tarea") }
        )
        if (intentoGuardar.value && tituloLimpio.length < 3) {
            Text(text = "El título debe tener al menos 3 caracteres")
        }
        OutlinedTextField(
            value = descripcion.value,
            onValueChange = { nuevoTexto ->
                descripcion.value = nuevoTexto
            },
            label = { Text(text = "Descripción") }
        )
        Box {
            Button(
                onClick = {
                    menuAbierto.value = true
                }
            ) {
                Text(text = "Prioridad: ${prioridad.value}")
            }

            DropdownMenu(
                expanded = menuAbierto.value,
                onDismissRequest = {
                    menuAbierto.value = false
                }
            ) {
                DropdownMenuItem(
                    text = { Text(text = "Baja") },
                    onClick = {
                        prioridad.value = "Baja"
                        menuAbierto.value = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(text = "Media") },
                    onClick = {
                        prioridad.value = "Media"
                        menuAbierto.value = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(text = "Alta") },
                    onClick = {
                        prioridad.value = "Alta"
                        menuAbierto.value = false
                    }
                )
            }
        }
        if (intentoGuardar.value && !prioridadValida) {
            Text(text = "Elige una prioridad válida: Baja, Media o Alta")
        }
        Button(
            enabled = !guardando,
            onClick = {
                intentoGuardar.value = true
                if (tituloLimpio.length >= 3 && prioridadValida) {
                    val tareaParaGuardar = Tarea(
                        id = tareaInicial?.id ?: 0,
                        titulo = tituloLimpio,
                        descripcion = descripcion.value,
                        prioridad = prioridad.value,
                        completada = tareaInicial?.completada ?: false
                    )
                    onGuardar(tareaParaGuardar)
                }
            }
        ) {
            if (tareaInicial == null) {
                Text(text = "Guardar")
            } else {
                Text(text = "Guardar cambios")
            }
        }
        TextButton(
            enabled = !guardando,
            onClick = onCancelar
        ) {
            Text(text = "Cancelar")
        }
    }
}
