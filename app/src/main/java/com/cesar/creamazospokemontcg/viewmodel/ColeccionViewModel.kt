package com.cesar.creamazospokemontcg.viewmodel

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import com.cesar.creamazospokemontcg.data.model.Carta
import com.cesar.creamazospokemontcg.data.repository.ColeccionRepository

/**
 * ViewModel de la pantalla Coleccion.
 *
 * Mantiene en memoria las cartas del usuario.
 */
class ColeccionViewModel : ViewModel() {

    private val repository = ColeccionRepository()

    // Estado observable desde Compose
    val cartas = mutableStateOf<List<Carta>>(emptyList())

    init {
        // Escuchamos cambios en Firebase
        repository.escucharColeccion {
            cartas.value = it
        }
    }
}
