package com.cesar.creamazospokemontcg.viewmodel

import androidx.lifecycle.ViewModel
import com.cesar.creamazospokemontcg.data.auth.AuthRepository

/**
 * ViewModel del login.
 * Conecta la interfaz con Firebase Auth.
 */
class LoginViewModel : ViewModel() {

    private val repository = AuthRepository()

    fun login(
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        repository.login(email, password, onResult)
    }

    fun register(
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        repository.register(email, password, onResult)
    }
}
