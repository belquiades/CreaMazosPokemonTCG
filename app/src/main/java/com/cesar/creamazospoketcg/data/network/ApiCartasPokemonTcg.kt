package com.cesar.creamazospoketcg.data.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * PokeTcgService
 * --------------
 * Interfaz Retrofit que define los endpoints que usaremos de la Pokémon TCG API.
 * Retrofit genera la implementación en tiempo de ejecución a partir de estas
 * anotaciones.
 *
 * Dónde colocar: app/src/main/java/com/cesar/creamazospoketcg/data/network/ApiCartasPokemonTcg.kt
 */
interface PokeTcgService {
    /**
     * searchCards: busca cartas usando la query de la API.
     * - q: consulta de búsqueda (ej. "name:Pikachu")
     * - pageSize: número máximo de resultados por página
     *
     * Retorna Response<CardsResponse> para poder comprobar código HTTP y body.
     */
    @GET("cards")
    suspend fun searchCards(
        @Query("q") query: String? = null,
        @Query("pageSize") pageSize: Int = 50
    ): Response<RespuestaCartas>

    /**
     * getCardsByIds: obtiene cartas por una lista de ids separadas por comas.
     * La API permite solicitar múltiples ids mediante el parámetro ids.
     * - ids: "id1,id2,id3"
     */
    @GET("cards")
    suspend fun getCardsByIds(
        @Query("ids") ids: String // comma separated ids
    ): Response<RespuestaCartas>
}
