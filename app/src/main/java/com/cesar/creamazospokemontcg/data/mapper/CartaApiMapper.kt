package com.cesar.creamazospokemontcg.data.mapper

import com.cesar.creamazospokemontcg.data.api.PokemonApiCardDto
import com.cesar.creamazospokemontcg.data.model.Carta

/**
 * Mapper que convierte una carta de la API
 * en nuestro modelo interno Carta
 */
fun PokemonApiCardDto.toCarta(): Carta {

    val ataque = attacks
        ?.firstOrNull()
        ?.damage
        ?.replace("+", "")
        ?.toIntOrNull() ?: 0

    val vida = hp?.toIntOrNull() ?: 0

    return Carta(
        id = id,
        nombre = name,
        tipo = supertype ?: "Desconocido",
        rareza = rarity ?: "Común",
        ataque = ataque,
        vida = vida,
        imagenUrl = images?.small ?: "",
        creadaPorUsuario = false
    )
}
