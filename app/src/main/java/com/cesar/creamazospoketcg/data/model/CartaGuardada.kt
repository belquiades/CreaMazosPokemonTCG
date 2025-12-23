package com.cesar.creamazospoketcg.model

/**
 * Modelo único de carta guardada en la colección del usuario.
 * ESTE ES EL CONTRATO DE DATOS DE TODA LA APP.
 */
data class CartaGuardada(
    val id: String = "",
    val name: String = "",
    val imageResolvedUrl: String? = null,
    val imageOriginalUrl: String? = null,
    val quantity: Long = 1L,
    val localId: String? = null
)
