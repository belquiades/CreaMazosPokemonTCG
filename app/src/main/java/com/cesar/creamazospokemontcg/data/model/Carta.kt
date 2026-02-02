package com.cesar.creamazospokemontcg.data.model

/**
 * Modelo de datos que representa una carta del juego.
 *
 * Este modelo se usa en:
 * - La colección del usuario (Firebase)
 * - Los resultados de búsqueda desde la API
 *
 * IMPORTANTE:
 * - No usar ñ en nombres de variables
 * - Los valores por defecto evitan crashes con Firebase
 */
data class Carta(
    val id: String = "",                 // ID unico de la carta
    val nombre: String = "",             // Nombre visible de la carta
    val tipo: String = "",               // Tipo (Pokemon, Entrenador, Energia...)
    val rareza: String = "",             // Rareza (Comun, Rara, Ultra Rara...)
    val ataque: Int = 0,                 // Valor de ataque (si aplica)
    val vida: Int = 0,                   // Puntos de vida
    val imagenUrl: String = "",          // URL de la imagen
    val creadaPorUsuario: Boolean = false // true si es carta creada manualmente
)
