package dev.example.gestortareas

import retrofit2.http.GET
import retrofit2.http.Query

interface TareasApi {

    @GET("todos")
    suspend fun obtenerTareas(
        @Query("userId") userId: Int = 1
    ): List<TareaEjemplo>
}