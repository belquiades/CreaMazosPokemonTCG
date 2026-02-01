package com.cesar.creamazospokemontcg.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.cesar.creamazospokemontcg.data.model.Carta
import java.util.UUID

/**
 * ViewModel de la colección de cartas.
 * Gestiona la lista de cartas y las operaciones sobre ella.
 */
class ColeccionViewModel : ViewModel() {

    // Lista observable de cartas de la colección
    val cartas = mutableStateOf<List<Carta>>(emptyList())

    init {
        cargarColeccionInicial()
    }

    /**
     * Carga inicial con datos de ejemplo.
     * Esto permite probar la app sin depender todavía de Firebase o APIs.
     */
    private fun cargarColeccionInicial() {
        cartas.value = listOf(
            Carta(id = "1", nombre = "Pikachu", tipo = "Eléctrico", cantidad = 2),
            Carta(id = "2", nombre = "Charmander", tipo = "Fuego", cantidad = 1)
        )
    }

    /**
     * Añade una carta a la colección.
     * Si ya existe una carta con el mismo nombre, se suma la cantidad.
     */
    fun anadirCarta(nombre: String, tipo: String, cantidad: Int) {

        val listaActual = cartas.value.toMutableList()

        val cartaExistente = listaActual.find { it.nombre == nombre }

        if (cartaExistente != null) {
            // Si la carta ya existe, se actualiza la cantidad
            val cartaActualizada = cartaExistente.copy(
                cantidad = cartaExistente.cantidad + cantidad
            )
            listaActual.remove(cartaExistente)
            listaActual.add(cartaActualizada)
        } else {
            // Si no existe, se crea una carta nueva
            listaActual.add(
                Carta(
                    id = UUID.randomUUID().toString(),
                    nombre = nombre,
                    tipo = tipo,
                    cantidad = cantidad
                )
            )
        }

        cartas.value = listaActual
    }
}
