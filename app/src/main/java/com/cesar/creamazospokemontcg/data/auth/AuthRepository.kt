package com.cesar.creamazospokemontcg.data.auth

import com.google.firebase.auth.FirebaseAuth

/**
 * Repositorio de autenticación.
 * Encapsula FirebaseAuth para no usarlo directamente en la UI.
 */
class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * Login con email y contraseña.
     */
    fun login(
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.message)
                }
            }
    }

    /**
     * Registro de usuario nuevo.
     */
    fun register(
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.message)
                }
            }
    }
}
