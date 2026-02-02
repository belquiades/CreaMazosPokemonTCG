package com.cesar.creamazospokemontcg.data.api

/**
 * Respuesta raíz de la API
 */
data class PokemonApiResponse(
    val data: List<PokemonApiCardDto>
)

/**
 * DTO de carta según la API
 * OJO: esto NO es nuestro modelo Carta
 */
data class PokemonApiCardDto(
    val id: String,
    val name: String,
    val supertype: String?,
    val rarity: String?,
    val images: PokemonApiImages?,
    val hp: String?,
    val attacks: List<PokemonApiAttack>?
)

data class PokemonApiImages(
    val small: String?,
    val large: String?
)

data class PokemonApiAttack(
    val damage: String?
)
