package com.cesar.creamazospokemontcg.data.repository

import com.cesar.creamazospokemontcg.data.api.PokemonApiClient
import com.cesar.creamazospokemontcg.data.mapper.toCarta
import com.cesar.creamazospokemontcg.data.model.Carta

/**
 * Repositorio encargado SOLO de buscar cartas en la API
 */
class BuscarCartasRepository {

    suspend fun buscarPorNombre(nombre: String): List<Carta> {
        val respuesta = PokemonApiClient.service.buscarCartas(
            query = "name:$nombre"
        )

        return respuesta.data.map { it.toCarta() }
    }
}
