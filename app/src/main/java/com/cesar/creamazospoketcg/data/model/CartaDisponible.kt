package com.cesar.creamazospoketcg.data.model

data class CartaDisponible(
    val cardId: String = "",
    val nombre: String = "",
    val tipo: String = "",
    val imageUrl: String? = null,
    val disponibles: Long = 0L
)
