package com.cesar.creamazospoketcg.data.network

import com.cesar.creamazospoketcg.data.model.Carta

/**
 * RespuestaCartas
 *
 * Clase que mapea la estructura JSON devuelta por la API en la ruta /cards.
 * La API devuelve un objeto con la propiedad "data" que contiene la lista de cartas.
 *
 */
data class RespuestaCartas(
    val data: List<Carta>?
)
