package com.cesar.creamazospoketcg.data.model

/**
 * Carta.kt
 * Modelo de datos para mapear la API Pokémon TCG.
 *
 * He añadido campos opcionales 'attacks' y 'abilities' para poder mostrar
 * ataques y habilidades en la pantalla de detalle si la API los devuelve.
 */


data class Carta(
    val id: String,
    val localId: String? = null,
    val name: String? = null,
    val superType: String? = null,
    val subTypes: List<String>? = null,
    val types: List<String>? = null,
    val hp: Int? = null,
    val evolvesFrom: String? = null,
    val attacks: List<Ataque>? = null,
    val weaknesses: List<ElementoValor>? = null,
    val resistances: List<ElementoValor>? = null,
    val retreat: Int? = null,
    val illustrator: String? = null,
    val rarity: String? = null,
    val set: SetInfo? = null,
    val images: ImagenesCarta? = null,
    val flavorText: String? = null
)

data class Ataque(
    val name: String? = null,
    val cost: List<String>? = null,
    val damage: String? = null,
    val effect: String? = null
)

data class ElementoValor(
    val type: String? = null,
    val value: String? = null
)

data class SetInfo(
    val id: String? = null,
    val name: String? = null,
    val series: String? = null,
    val printedTotal: Int? = null,
    val total: Int? = null,
    val releaseDate: String? = null
)

data class ImagenesCarta(
    val small: String? = null,
    val large: String? = null
)
