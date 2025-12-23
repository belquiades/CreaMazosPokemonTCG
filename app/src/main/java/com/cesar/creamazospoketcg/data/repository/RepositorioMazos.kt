package com.cesar.creamazospoketcg.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.auth.FirebaseAuth

object RepositorioMazos {

    private val db = FirebaseFirestore.getInstance()

    private fun userRef() =
        db.collection("users").document(FirebaseAuth.getInstance().uid!!)

    fun crearMazo(nombre: String) =
        userRef()
            .collection("mazos")
            .add(
                mapOf(
                    "nombre" to nombre,
                    "creadoEn" to System.currentTimeMillis(),
                    "totalCartas" to 0L,
                    "pokemonCount" to 0L,
                    "entrenadorCount" to 0L,
                    "energiaCount" to 0L
                )
            )

    fun añadirCartaAMazo(
        mazoId: String,
        cardId: String,
        nombre: String,
        tipo: String
    ) {
        val mazoRef = userRef().collection("mazos").document(mazoId)
        val cartaRef = mazoRef.collection("cartas").document(cardId)

        db.runTransaction { tx ->

            val cartaSnap = tx.get(cartaRef)
            val mazoSnap = tx.get(mazoRef)

            val totalActual = mazoSnap.getLong("totalCartas") ?: 0L
            if (totalActual >= 60) {
                throw Exception("El mazo ya tiene 60 cartas")
            }

            if (cartaSnap.exists()) {
                val cantidad = cartaSnap.getLong("cantidad") ?: 0L
                if (cantidad >= 4) {
                    throw Exception("Máximo 4 copias por carta")
                }
                tx.update(cartaRef, "cantidad", cantidad + 1)
            } else {
                tx.set(
                    cartaRef,
                    mapOf(
                        "name" to nombre,
                        "tipo" to tipo,
                        "cantidad" to 1L
                    )
                )
            }

            tx.update(mazoRef, "totalCartas", FieldValue.increment(1))
            when (tipo) {
                "POKEMON" -> tx.update(mazoRef, "pokemonCount", FieldValue.increment(1))
                "ENTRENADOR" -> tx.update(mazoRef, "entrenadorCount", FieldValue.increment(1))
                "ENERGIA" -> tx.update(mazoRef, "energiaCount", FieldValue.increment(1))
                else -> 0
            }
        }
    }

    fun añadirCartaAMazoDesdeColeccion(
        mazoId: String,
        cartaId: String,
        nombre: String,
        tipo: String
    ) {
        val mazoRef = userRef().collection("mazos").document(mazoId)
        val cartaRef = mazoRef.collection("cartas").document(cartaId)

        db.runTransaction { tx ->

            val mazoSnap = tx.get(mazoRef)
            val cartaSnap = tx.get(cartaRef)

            val total = mazoSnap.getLong("totalCartas") ?: 0L
            if (total >= 60) {
                throw Exception("El mazo ya tiene 60 cartas")
            }

            if (cartaSnap.exists()) {
                val cantidad = cartaSnap.getLong("cantidad") ?: 0L
                if (cantidad >= 4) {
                    throw Exception("Máximo 4 copias por carta")
                }
                tx.update(cartaRef, "cantidad", cantidad + 1)
            } else {
                tx.set(
                    cartaRef,
                    mapOf(
                        "nombre" to nombre,
                        "tipo" to tipo,
                        "cantidad" to 1L
                    )
                )
            }

            tx.update(mazoRef, "totalCartas", FieldValue.increment(1))

            when (tipo) {
                "POKEMON" -> tx.update(mazoRef, "pokemonCount", FieldValue.increment(1))
                "ENTRENADOR" -> tx.update(mazoRef, "entrenadorCount", FieldValue.increment(1))
                "ENERGIA" -> tx.update(mazoRef, "energiaCount", FieldValue.increment(1))
                else -> { /* obligatorio para when */ }
            }
        }
    }
    fun quitarCartaDelMazo(
        mazoId: String,
        cardId: String,
        tipo: String
    ) {
        val mazoRef = userRef().collection("mazos").document(mazoId)
        val cartaRef = mazoRef.collection("cartas").document(cardId)

        db.runTransaction { tx ->

            val cartaSnap = tx.get(cartaRef)
            val mazoSnap = tx.get(mazoRef)

            if (!cartaSnap.exists()) {
                throw Exception("La carta no está en el mazo")
            }

            val cantidad = cartaSnap.getLong("cantidad") ?: 0L
            if (cantidad <= 1) {
                tx.delete(cartaRef)
            } else {
                tx.update(cartaRef, "cantidad", cantidad - 1)
            }

            // contadores
            tx.update(mazoRef, "totalCartas", FieldValue.increment(-1))
            when (tipo) {
                "POKEMON" -> tx.update(mazoRef, "pokemonCount", FieldValue.increment(1))
                "ENTRENADOR" -> tx.update(mazoRef, "entrenadorCount", FieldValue.increment(1))
                "ENERGIA" -> tx.update(mazoRef, "energiaCount", FieldValue.increment(1))
                else -> {
                    // obligatorio para que compile
                }
            }
        }
    }

    fun obtenerMazos() =
        userRef()
            .collection("mazos")
            .get()

}

