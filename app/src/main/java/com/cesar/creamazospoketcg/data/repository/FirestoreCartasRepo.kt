package com.cesar.creamazospoketcg.data.repository

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.HashMap

object FirestoreCartasRepo {
    private val db = FirebaseFirestore.getInstance()
    private const val TAG = "FirestoreCartasRepo"

    /**
     * Añadir una carta al usuario: incrementa quantity si ya existe, o crea doc con quantity = 1
     * docId: el id a usar para el documento dentro de users/{uid}/cards — por ejemplo cardId
     */
    fun addCardToUser(userId: String, docId: String, cardData: Map<String, Any?>): Task<Void> {
        val docRef = db.collection("users").document(userId).collection("cards").document(docId)

        // Ejecutamos la transacción y devolvemos un Task<Void>
        val txnTask = db.runTransaction { tx ->
            val snapshot = tx.get(docRef)
            if (snapshot.exists()) {
                tx.update(docRef, "quantity", FieldValue.increment(1))
            } else {
                val data = HashMap<String, Any?>()
                // copie los campos pasados en cardData (si hay)
                data.putAll(cardData)
                data["quantity"] = 1L
                data["addedAt"] = Timestamp.Companion.now()
                tx.set(docRef, data)
            }
            // devolver null desde el lambda; lo convertiremos posteriormente a Task<Void>
            null
        }

        // Convertimos el Task resultante a Task<Void> devolviendo null en continueWith
        @Suppress("UNCHECKED_CAST")
        return txnTask
            .continueWith { _ -> null as Void? }
            .addOnSuccessListener { Log.d(TAG, "addCardToUser: success user=$userId doc=$docId") }
            .addOnFailureListener { e -> Log.e(TAG, "addCardToUser: failed user=$userId doc=$docId", e) } as Task<Void>
    }

    /**
     * Eliminar 1 unidad de la carta: decrementa quantity. Si queda 0 -> delete doc.
     */
    fun removeOneCardFromUser(userId: String, docId: String): Task<Void> {
        val docRef = db.collection("users").document(userId).collection("cards").document(docId)

        val txnTask = db.runTransaction { tx ->
            val snapshot = tx.get(docRef)
            if (!snapshot.exists()) {
                // nada que hacer
                return@runTransaction null
            }

            // Intentamos obtener quantity de forma segura
            val qtyAny = snapshot.get("quantity")
            val curQty: Long = when (qtyAny) {
                is Long -> qtyAny
                is Int -> qtyAny.toLong()
                is Number -> qtyAny.toLong()
                else -> 1L
            }

            if (curQty <= 1L) {
                tx.delete(docRef)
            } else {
                tx.update(docRef, "quantity", curQty - 1)
            }
            null
        }

        @Suppress("UNCHECKED_CAST")
        return txnTask
            .continueWith { _ -> null as Void? }
            .addOnSuccessListener { Log.d(TAG, "removeOneCardFromUser: success user=$userId doc=$docId") }
            .addOnFailureListener { e -> Log.e(TAG, "removeOneCardFromUser: failed user=$userId doc=$docId", e) } as Task<Void>
    }

    /**
     * Borrar completamente el documento de la carta (eliminar todas las unidades)
     */
    fun deleteCardDoc(userId: String, docId: String): Task<Void> {
        val docRef = db.collection("users").document(userId).collection("cards").document(docId)
        return docRef.delete()
            .addOnSuccessListener { Log.d(TAG, "deleteCardDoc: success user=$userId doc=$docId") }
            .addOnFailureListener { e -> Log.e(TAG, "deleteCardDoc: failed user=$userId doc=$docId", e) }
    }
}