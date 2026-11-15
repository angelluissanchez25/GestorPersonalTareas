package dev.example.gestortareas

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

fun compartirTarea(context: Context, tarea: Tarea): Boolean {
    val descripcion = if (tarea.descripcion.isBlank()) {
        "Sin descripción"
    } else {
        tarea.descripcion
    }

    val texto = "Tarea: ${tarea.titulo}\n" +
            "Descripción: $descripcion\n" +
            "Prioridad: ${tarea.prioridad}"

    val intent = Intent(Intent.ACTION_SEND)
    intent.type = "text/plain"
    intent.putExtra(Intent.EXTRA_TEXT, texto)

    if (intent.resolveActivity(context.packageManager) == null) {
        return false
    }

    try {
        context.startActivity(
            Intent.createChooser(intent, "Compartir tarea")
        )
        return true
    } catch (e: ActivityNotFoundException) {
        return false
    }
}