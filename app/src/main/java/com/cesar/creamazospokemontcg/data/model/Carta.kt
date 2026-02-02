package com.cesar.creamazospokemontcg.data.model

/**
 * Modelo base de una carta en la coleccion del usuario
 */
data class Carta(
    val id: String = "",
    val nombre: String = "",
    val tipo: String = "",
    val imagenUrl: String = "",
    val cantidad: Int = 1
)
