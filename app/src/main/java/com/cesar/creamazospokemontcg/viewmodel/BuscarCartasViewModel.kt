package com.cesar.creamazospokemontcg.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cesar.creamazospokemontcg.data.model.Carta
import com.cesar.creamazospokemontcg.data.repository.BuscarCartasRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para buscar cartas desde la API
 */
class BuscarCartasViewModel : ViewModel() {

    private val repository = BuscarCartasRepository()

    private val _resultados = MutableStateFlow<List<Carta>>(emptyList())
    val resultados: StateFlow<List<Carta>> = _resultados

    fun buscar(nombre: String) {
        viewModelScope.launch {
            _resultados.value = repository.buscarPorNombre(nombre)
        }
    }
}
