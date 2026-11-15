package dev.example.gestortareas

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Tarea::class], version = 1)
abstract class TareasDatabase : RoomDatabase() {
    abstract fun tareaDao(): TareaDao

    companion object {
        @Volatile
        private var instancia: TareasDatabase? = null

        fun obtener(contexto: Context): TareasDatabase {
            return instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    contexto.applicationContext,
                    TareasDatabase::class.java,
                    "tareas.db"
                ).build().also { instancia = it }
            }
        }
    }
}