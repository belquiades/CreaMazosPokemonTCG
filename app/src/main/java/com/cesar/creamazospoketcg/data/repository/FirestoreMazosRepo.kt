package com.cesar.creamazospoketcg.ui.mazos.repositorio

import com.cesar.creamazospoketcg.ui.mazos.logica.ReglasMazo
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.android.gms.tasks.Task

object FirestoreMazosRepo {

    private val db = FirebaseFirestore.getInstance()

    fun añadirCartaAMazo(
        userId: String,
        mazoId: String,
        cartaId: String,
        datosCarta: Map<String, Any>,
        cantidadActual: Int,
        totalActual: Int
    ): Task<Void>? {

        if (!ReglasMazo.puedeAñadirCarta(cantidadActual, totalActual)) {
            return null
        }

        val docCarta = db.collection("users")
            .document(userId)
            .collection("mazos")
            .document(mazoId)
            .collection("cartas")
            .document(cartaId)

        return db.runTransaction { tx ->
            val snap = tx.get(docCarta)

            if (snap.exists()) {
                tx.update(docCarta, "cantidad", FieldValue.increment(1))
            } else {
                val data = datosCarta.toMutableMap()
                data["cantidad"] = 1
                tx.set(docCarta, data)
            }

            val mazoRef = db.collection("users")
                .document(userId)
                .collection("mazos")
                .document(mazoId)

            tx.update(mazoRef, "totalCartas", FieldValue.increment(1))
            null
        }
    }
}
