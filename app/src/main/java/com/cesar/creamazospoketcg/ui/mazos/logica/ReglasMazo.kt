package com.cesar.creamazospoketcg.ui.mazos.logica

object ReglasMazo {

    const val MAX_CARTAS_POR_MAZO = 60
    const val MAX_COPIAS_POR_CARTA = 4

    fun puedeAñadirCarta(
        cantidadActual: Int,
        totalCartasMazo: Int
    ): Boolean {
        if (cantidadActual >= MAX_COPIAS_POR_CARTA) return false
        if (totalCartasMazo >= MAX_CARTAS_POR_MAZO) return false
        return true
    }
}
