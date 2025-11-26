package com.cesar.creamazospoketcg.data.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap

/**
 * Servicio Retrofit para TCGdex (ajusta baseUrl en ModuloRed).
 * - listarCartas devuelve el wrapper RespuestaListaCartasTCGdex
 * - obtenerCartaPorId devuelve CartaCompletaDTO (detalle)
 */

interface ServicioTCGdex {
    @GET("v2/en/cards")
    suspend fun listarCartas(
        @Query("q") q: String? = null,
        @QueryMap filtros: Map<String, String> = emptyMap()
    ): Response<RespuestaListaCartasTCGdex>

    @GET("v2/en/cards")
    suspend fun getCardsByIds(
        @Query("ids") ids: String
    ): Response<RespuestaListaCartasTCGdex>

    @GET("v2/en/cards/{id}")
    suspend fun obtenerCartaPorId(
        @retrofit2.http.Path("id") id: String
    ): Response<CartaCompletaDTO>
}
