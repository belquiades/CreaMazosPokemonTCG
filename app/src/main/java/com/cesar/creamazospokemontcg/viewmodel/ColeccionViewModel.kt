package com.cesar.creamazospokemontcg.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cesar.creamazospokemontcg.data.model.Carta
import com.cesar.creamazospokemontcg.data.repository.ColeccionRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * ViewModel de la colección del usuario.
 */
class ColeccionViewModel : ViewModel() {

    private val repository = ColeccionRepository()
    private val auth = FirebaseAuth.getInstance()

    var cartas: List<Carta> = emptyList()
        private set

    fun cargarColeccion(onActualizado: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return

        repository.obtenerColeccion(userId) {
            cartas = it
            onActualizado()
        }
    }

    fun añadirCarta(nombre: String, tipo: String) {
        val userId = auth.currentUser?.uid ?: return

        val carta = Carta(
            id = System.currentTimeMillis().toString(),
            nombre = nombre,
            tipo = tipo,
            cantidad = 1
        )

        repository.guardarCarta(userId, carta)
    }
}
