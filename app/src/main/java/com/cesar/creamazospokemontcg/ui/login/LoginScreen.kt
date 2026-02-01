package com.cesar.creamazospokemontcg.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cesar.creamazospokemontcg.viewmodel.LoginViewModel

/**
 * Pantalla de login con Firebase Authentication (email / contraseña).
 * Código comentado de forma sencilla como alumno de DAM.
 */
@Composable
fun LoginScreen(
    onLoginCorrecto: () -> Unit
) {
    // ViewModel asociado a esta pantalla
    val loginViewModel: LoginViewModel = viewModel()

    // Estados de la interfaz
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "CreaMazosPokemonTCG",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo electrónico") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Botón de inicio de sesión
        Button(
            onClick = {
                mensajeError = null
                loginViewModel.login(email, password) { correcto, error ->
                    if (correcto) {
                        onLoginCorrecto()
                    } else {
                        mensajeError = error
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Entrar")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Botón de registro
        OutlinedButton(
            onClick = {
                mensajeError = null
                loginViewModel.register(email, password) { correcto, error ->
                    if (correcto) {
                        onLoginCorrecto()
                    } else {
                        mensajeError = error
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registrarse")
        }

        // Mensaje de error (si lo hay)
        mensajeError?.let { texto ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = texto,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
