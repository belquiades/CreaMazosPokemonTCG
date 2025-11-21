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
    val name: String,
    val supertype: String? = null,
    val subtypes: List<String>? = null,
    val types: List<String>? = null,
    val rarity: String? = null,
    val set: SetInfo? = null,
    val images: ImagenesCarta? = null,
    val attacks: List<Ataque>? = null,      // Optional - lista de ataques
    val abilities: List<Habilidad>? = null  // Optional - habilidades (si existen)
)

data class CartaTCGdexBreve(
    val id: String,
    val localId: String? = null,
    val name: String,
    val image:  String? = null
)

data class SetInfo(
    val id: String? = null,
    val name: String? = null
)

data class ImagenesCarta(
    val small: String? = null,
    val large: String? = null
)

// Representa un ataque de la carta (campo 'attacks' en la API)
data class Ataque(
    val name: String? = null,
    val damage: String? = null,
    val text: String? = null
)

// Representa una habilidad (campo 'abilities' en la API)
data class Habilidad(
    val name: String? = null,
    val text: String? = null,
    val type: String? = null
)
