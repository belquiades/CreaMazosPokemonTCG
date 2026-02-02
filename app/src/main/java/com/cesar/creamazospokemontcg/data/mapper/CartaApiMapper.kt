package com.cesar.creamazospokemontcg.data.mapper

import com.cesar.creamazospokemontcg.data.api.PokemonApiCard
import com.cesar.creamazospokemontcg.data.model.Carta

/**
 * Función que transforma una carta de la API
 * en una carta del modelo interno de la app.
 */
fun PokemonApiCard.toCarta(): Carta {

    // Vida: hp viene como String, lo pasamos a Int si es posible
    val vidaCalculada = hp?.toIntOrNull() ?: 0

    return Carta(
        id = id,
        nombre = name,
        tipo = types?.firstOrNull() ?: "Desconocido",
        rareza = rarity ?: "Común",
        ataque = 0, // No attack information available in PokemonApiCard
        vida = vidaCalculada,
        imagenUrl = images?.small ?: "",
        creadaPorUsuario = false
    )
}
