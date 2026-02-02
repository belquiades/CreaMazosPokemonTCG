package com.cesar.creamazospokemontcg.data.repository

import com.cesar.creamazospokemontcg.data.api.PokemonApiClient
import com.cesar.creamazospokemontcg.data.mapper.toCarta
import com.cesar.creamazospokemontcg.data.model.Carta

/**
 * Repositorio que se encarga de buscar cartas en la API.
 */
class PokemonApiRepository {

    /**
     * Busca cartas por nombre y las convierte
     * al modelo interno de la app.
     */
    suspend fun buscarCartas(nombre: String): List<Carta> {

        val respuesta = PokemonApiClient.service.buscarCartas(
            query = "name:$nombre*"
        )

        return respuesta.data.map { it.toCarta() }
    }
}
