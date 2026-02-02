package com.cesar.creamazospokemontcg.data.repository

import com.cesar.creamazospokemontcg.data.model.Carta
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

/**
 * Repositorio encargado de gestionar la coleccion del usuario en Firebase.
 *
 * Aqui NO hay logica de UI ni ViewModel.
 * Solo acceso a datos.
 */
class ColeccionRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Devuelve la referencia a la coleccion del usuario logueado.
     */
    private fun coleccionUsuario() =
        firestore
            .collection("usuarios")
            .document(auth.currentUser?.uid ?: "")
            .collection("coleccion")

    /**
     * Guarda una carta en la coleccion del usuario.
     */
    fun guardarCarta(carta: Carta) {
        coleccionUsuario()
            .document(carta.id)
            .set(carta)
    }

    /**
     * Escucha cambios en la coleccion del usuario.
     */
    fun escucharColeccion(
        onResultado: (List<Carta>) -> Unit
    ) {
        coleccionUsuario()
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener

                val cartas = snapshot.documents.mapNotNull {
                    it.toObject(Carta::class.java)
                }

                onResultado(cartas)
            }
    }
}
