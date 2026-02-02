package com.cesar.creamazospokemontcg.data.api

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Servicio de la API PokemonTCG
 */
interface PokemonApiService {

    @GET("cards")
    suspend fun buscarCartas(
        @Query("q") query: String
    ): PokemonApiResponse
}
