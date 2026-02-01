package com.cesar.creamazospokemontcg.ui.coleccion

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.cesar.creamazospokemontcg.data.model.Carta
import java.util.UUID

/**
 * ViewModel de la colección de cartas.
 * Maneja datos en memoria (sin Firebase todavía).
 */
class ColeccionViewModel : ViewModel() {

    // Lista observable de cartas
    val cartas = mutableStateListOf<Carta>()

    /**
     * Añade una carta nueva o incrementa su cantidad si ya existe.
     */
    fun añadirCarta(nombre: String, tipo: String) {

        val cartaExistente = cartas.find { it.nombre == nombre }

        if (cartaExistente != null) {
            // Si existe, aumentamos cantidad
            val index = cartas.indexOf(cartaExistente)
            cartas[index] = cartaExistente.copy(
                cantidad = cartaExistente.cantidad + 1
            )
        } else {
            // Si no existe, la creamos
            cartas.add(
                Carta(
                    id = UUID.randomUUID().toString(),
                    nombre = nombre,
                    tipo = tipo,
                    cantidad = 1
                )
            )
        }
    }
}
