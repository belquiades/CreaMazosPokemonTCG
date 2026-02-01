package com.cesar.creamazospokemontcg.ui.coleccion

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Pantalla de la colección de cartas del usuario.
 * De momento solo es un marcador visual.
 */
@Composable
fun ColeccionScreen(
    onVolver: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Mi colección",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Aquí se mostrarán las cartas del usuario.")

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onVolver) {
            Text("Volver")
        }
    }
}
