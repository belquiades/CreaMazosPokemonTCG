package com.cesar.creamazospokemontcg.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.cesar.creamazospokemontcg.data.model.Mazo
import java.util.UUID

/**
 * ViewModel encargado de la creación de mazos manuales.
 * En C1 solo gestiona nombre y estructura básica.
 */
class CrearMazoViewModel : ViewModel() {

    // Nombre introducido por el usuario
    val nombreMazo = mutableStateOf("")

    // Cartas del mazo (idCarta -> cantidad)
    val cartasMazo = mutableStateOf<Map<String, Int>>(emptyMap())

    /**
     * Crea un mazo con los datos actuales.
     */
    fun crearMazo(): Mazo {
        return Mazo(
            id = UUID.randomUUID().toString(),
            nombre = nombreMazo.value,
            cartas = cartasMazo.value
        )
    }
}
