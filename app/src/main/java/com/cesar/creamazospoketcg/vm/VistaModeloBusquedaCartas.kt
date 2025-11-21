package com.cesar.creamazospoketcg.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cesar.creamazospoketcg.data.model.Carta
import com.cesar.creamazospoketcg.data.repository.RepositorioCartas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * VistaModeloBusquedaCartas
 *
 * ViewModel encargado de coordinar la búsqueda de cartas entre la UI y el repositorio.
 * - Usa StateFlow para exponer el estado de la UI (ideal para usar con LiveData/Flow en la UI).
 * - Las funciones son públicas y se pueden llamar desde un Fragmento o Activity.
 *
 */

/**
 * Estado que representa la UI de la pantalla de búsqueda.
 * - Cargando: la búsqueda está en curso.
 * - Resultado: lista de cartas recibida correctamente.
 * - Error: ocurrió un fallo (con mensaje).
 */
sealed class EstadoBusqueda {
    object Cargando : EstadoBusqueda()
    data class Resultado(val lista: List<Carta>) : EstadoBusqueda()
    data class Error(val mensaje: String) : EstadoBusqueda()
    object Vacío : EstadoBusqueda() // estado inicial o sin resultados
}

class VistaModeloBusquedaCartas(
    // Inyectamos el repositorio (por defecto nueva instancia; después puedes cambiar por DI)
    private val repositorioCartas: RepositorioCartas = RepositorioCartas()
) : ViewModel() {

    // StateFlow privado y mutable que mantiene el estado actual de la UI.
    private val _estadoUI = MutableStateFlow<EstadoBusqueda>(EstadoBusqueda.Vacío)

    // Exposición inmutable para la UI (Fragment/Activity).
    val estadoUI: StateFlow<EstadoBusqueda> = _estadoUI

    /**
     * buscarCartas
     * ------------
     * Realiza la búsqueda de cartas usando la query proporcionada.
     * La query debe seguir la sintaxis de la API si el usuario quiere filtrar por nombre, tipo, etc.
     *
     * Ejemplos de query:
     * - "name:Pikachu"
     * - "supertype:Pokémon"
     *
     * Esta función actualiza el StateFlow con los estados Cargando -> Resultado o Error.
     */
    fun buscarCartas(consulta: String?) {
        // Lanzamos una coroutine ligada al ciclo de vida del ViewModel.
        viewModelScope.launch {
            // Indicamos que la UI está cargando.
            _estadoUI.value = EstadoBusqueda.Cargando

            // Llamamos al repositorio (suspend) que hace la petición de red.
            val resultado = repositorioCartas.buscarCartas(consulta)

            // Procesamos resultado
            resultado.fold(
                onSuccess = { lista ->
                    // Si la lista está vacía, podemos devolver Vacío o Resultado con lista vacía.
                    if (lista.isEmpty()) {
                        _estadoUI.value = EstadoBusqueda.Resultado(emptyList())
                    } else {
                        _estadoUI.value = EstadoBusqueda.Resultado(lista)
                    }
                },
                onFailure = { excepcion ->
                    // Formateamos un mensaje de error entendible para la UI.
                    val mensaje = excepcion.message ?: "Error desconocido al consultar la API"
                    _estadoUI.value = EstadoBusqueda.Error(mensaje)
                }
            )
        }
    }

    /**
     * obtenerCartasPorIds
     * -------------------
     * Pide al repositorio varias cartas por su lista de ids. Devuelve el resultado
     * en el mismo StateFlow (puede usarse para rellenar un mazo con cartas seleccionadas).
     */
    fun obtenerCartasPorIds(ids: List<String>) {
        viewModelScope.launch {
            _estadoUI.value = EstadoBusqueda.Cargando
            val resultado = repositorioCartas.obtenerCartasPorIds(ids)
            resultado.fold(
                onSuccess = { lista ->
                    _estadoUI.value = EstadoBusqueda.Resultado(lista)
                },
                onFailure = { excepcion ->
                    val mensaje = excepcion.message ?: "Error desconocido al obtener cartas por IDs"
                    _estadoUI.value = EstadoBusqueda.Error(mensaje)
                }
            )
        }
    }

    /**
     * limpiarEstado
     * -------------
     * Función de utilidad para volver al estado inicial (Vacío).
     * Útil por ejemplo al abrir la pantalla o al resetear filtros.
     */
    fun limpiarEstado() {
        _estadoUI.value = EstadoBusqueda.Vacío
    }
}
