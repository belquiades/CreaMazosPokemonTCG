package com.cesar.creamazospokemontcg.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.cesar.creamazospokemontcg.data.model.Carta
import com.cesar.creamazospokemontcg.data.repository.ColeccionRepository
import com.google.firebase.auth.FirebaseAuth

/**
 * ViewModel de la pantalla Colección
 */
class ColeccionViewModel : ViewModel() {

    private val repository = ColeccionRepository()
    private val auth = FirebaseAuth.getInstance()

    val cartas = mutableStateOf<List<Carta>>(emptyList())

    init {
        cargarColeccion()
    }

    private fun cargarColeccion() {
        val userId = auth.currentUser?.uid ?: return

        repository.obtenerColeccion(userId) { lista ->
            cartas.value = lista
        }
    }
}
