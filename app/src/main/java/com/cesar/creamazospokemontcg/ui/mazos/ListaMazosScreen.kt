package com.cesar.creamazospokemontcg.ui.mazos

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Pantalla que mostrará los mazos creados por el usuario.
 */
@Composable
fun ListaMazosScreen(
    onVolver: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Mis mazos",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Aquí se listarán los mazos del usuario.")

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onVolver) {
            Text("Volver")
        }
    }
}
