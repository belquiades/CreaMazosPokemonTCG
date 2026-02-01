package com.cesar.creamazospokemontcg.data.model

/**
 * Representa una carta en la colección del usuario.
 * Modelo simple para la fase A (sin API).
 */
data class Carta(
    val id: String = "",
    val nombre: String = "",
    val tipo: String = "",
    val cantidad: Int = 1
)
