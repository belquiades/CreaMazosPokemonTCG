package com.cesar.creamazospokemontcg.data.model

data class Mazo(
    val id: String = "",
    val nombre: String = "",
    val cartas: Map<String, Int> = emptyMap()
)
