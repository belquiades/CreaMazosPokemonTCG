package com.cesar.creamazospoketcg.data.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.QueryMap

interface ServicioTCGdex {

    @GET("v2/{idioma}/cards")
    suspend fun listarCartas(
        @Path("idioma") idioma: String = "en",
        @QueryMap filtros: Map<String, String> = emptyMap()
    ): Response<List<CartaTCGdexBreve>>

    // Endpoint de detalle: /v2/{idioma}/card/{id}
    @GET("v2/{idioma}/card/{id}")
    suspend fun obtenerCartaPorId(
        @Path("idioma") idioma: String = "en",
        @Path("id") idCarta: String
    ): Response<CartaTCGdexCompleta>
}
