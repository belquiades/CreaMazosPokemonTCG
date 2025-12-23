package com.cesar.creamazospoketcg.model

data class Mazo(
    val id: String = "",
    val nombre: String = "",
    val creadoEn: Long = 0L,
    val totalCartas: Long = 0L,
    val pokemonCount: Long = 0L,
    val entrenadorCount: Long = 0L,
    val energiaCount: Long = 0L
)
