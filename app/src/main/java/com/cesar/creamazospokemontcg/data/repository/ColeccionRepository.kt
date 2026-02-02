package com.cesar.creamazospokemontcg.data.repository

import com.cesar.creamazospokemontcg.data.model.Carta
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Repositorio encargado de acceder a la colección del usuario en Firestore
 */
class ColeccionRepository {

    private val db = FirebaseFirestore.getInstance()

    fun obtenerColeccion(
        userId: String,
        onResult: (List<Carta>) -> Unit
    ) {
        db.collection("usuarios")
            .document(userId)
            .collection("coleccion")
            .get()
            .addOnSuccessListener { snapshot ->
                val cartas = snapshot.documents.mapNotNull {
                    it.toObject(Carta::class.java)
                }
                onResult(cartas)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }
}
