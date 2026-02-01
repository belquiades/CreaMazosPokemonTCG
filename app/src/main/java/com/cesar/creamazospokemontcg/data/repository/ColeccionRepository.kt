package com.cesar.creamazospokemontcg.data.repository

import com.cesar.creamazospokemontcg.data.model.Carta
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Repositorio encargado de gestionar la colección de cartas del usuario.
 *
 * Aquí se centraliza todo el acceso a Firebase Firestore.
 */
class ColeccionRepository {

    // Instancia de Firestore
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Obtiene la colección de cartas de un usuario.
     */
    fun obtenerColeccion(
        userId: String,
        onResult: (List<Carta>) -> Unit
    ) {
        firestore
            .collection("colecciones")
            .document(userId)
            .collection("cartas")
            .get()
            .addOnSuccessListener { result ->
                val cartas = result.documents.mapNotNull { doc ->
                    doc.toObject(Carta::class.java)
                }
                onResult(cartas)
            }
    }

    /**
     * Guarda una carta en la colección del usuario.
     */
    fun guardarCarta(
        userId: String,
        carta: Carta
    ) {
        firestore
            .collection("colecciones")
            .document(userId)
            .collection("cartas")
            .document(carta.id)
            .set(carta)
    }
}
