package com.cesar.creamazospokemontcg.ui.mazos

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Pantalla que muestra la lista de mazos del usuario.
 */
@Composable
fun ListaMazosScreen(
    onCrearMazo: () -> Unit,
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

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onCrearMazo() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Crear nuevo mazo")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { onVolver() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver")
        }
    }
}
