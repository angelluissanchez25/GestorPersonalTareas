package dev.example.gestortareas

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ClienteApi {

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://jsonplaceholder.typicode.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val servicio: TareasApi by lazy {
        retrofit.create(TareasApi::class.java)
    }
}