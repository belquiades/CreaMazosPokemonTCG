package com.cesar.creamazospokemontcg.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Cliente Retrofit para acceder a la API de Pokémon TCG.
 *
 * Este objeto se encarga de:
 * - Configurar Retrofit
 * - Añadir logs para depuración
 * - Crear el servicio que usará la app
 */
object PokemonApiClient {

    // URL base de la API Pokémon TCG
    private const val BASE_URL = "https://api.pokemontcg.io/v2/"

    // Interceptor para ver las peticiones en Logcat (muy útil)
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Cliente HTTP con interceptor
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    // Instancia de Retrofit
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // Servicio que usará la app para hacer llamadas a la API
    val service: PokemonApiService =
        retrofit.create(PokemonApiService::class.java)
}
