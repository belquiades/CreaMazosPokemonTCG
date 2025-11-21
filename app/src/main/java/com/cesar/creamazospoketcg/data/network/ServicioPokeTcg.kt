package com.cesar.creamazospoketcg.data.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * ServicioPokeTcg
 *
 * Interfaz Retrofit que define los endpoints que vamos a usar de la Pokémon TCG API.
 * - searchCartas: búsqueda general con query (ej. "name:Pikachu")
 * - obtenerCartasPorIds: obtener varias cartas por sus ids (separadas por comas)
 *
 * Dónde colocar:
 * app/src/main/java/com/cesar/creamazospoketcg/data/network/ServicioPokeTcg.kt
 */
interface ServicioPokeTcg {

    // Buscar cartas con una query (compatible con la sintaxis de la API).
    @GET("cards")
    suspend fun buscarCartas(
        @Query("q") consulta: String? = null,
        @Query("pageSize") tamanoPagina: Int = 50
    ): Response<RespuestaCartas>

    // Obtener cartas especificando una lista de ids separados por comas.
    @GET("cards")
    suspend fun obtenerCartasPorIds(
        @Query("ids") ids: String // ejemplo: "xy7-54,sm3-12"
    ): Response<RespuestaCartas>
}
