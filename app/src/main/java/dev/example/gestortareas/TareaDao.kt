package dev.example.gestortareas

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface TareaDao {

    @Insert
    suspend fun insertar(tarea: Tarea)

    @Query("SELECT * FROM tareas")
    suspend fun obtenerTodas(): List<Tarea>

    @Update
    suspend fun actualizar(tarea: Tarea)

    @Delete
    suspend fun eliminar(tarea: Tarea)
}