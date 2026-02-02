package com.cesar.creamazospokemontcg.data.api

/**
 * Respuesta mínima de la API
 */
data class PokemonApiResponse(
    val data: List<PokemonApiCard>
)

data class PokemonApiCard(
    val id: String,
    val name: String,
    val types: List<String>?,
    val rarity: String?,
    val hp: String?,
    val images: PokemonApiImages?
)

data class PokemonApiImages(
    val small: String?,
    val large: String?
)
