package com.cesar.creamazospokemontcg.data.api

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Servicio Retrofit para consultar la API de Pokémon TCG
 * Solo lectura, no guarda nada
 */
interface PokemonApiService {

    // Búsqueda simple por nombre
    @GET("v2/cards")
    suspend fun buscarCartas(
        @Query("q") query: String,
        @Query("pageSize") pageSize: Int = 20
    ): PokemonApiResponse
}
