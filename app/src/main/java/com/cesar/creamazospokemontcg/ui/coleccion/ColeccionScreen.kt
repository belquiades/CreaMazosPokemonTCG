package com.cesar.creamazospokemontcg.ui.coleccion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Pantalla que muestra la colección de cartas del usuario.
 */
@Composable
fun ColeccionScreen(
    onVolver: () -> Unit
) {
    val viewModel: ColeccionViewModel = viewModel()

    var nombreCarta by remember { mutableStateOf("") }
    var tipoCarta by remember { mutableStateOf("") }

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

        // Campos para añadir carta
        OutlinedTextField(
            value = nombreCarta,
            onValueChange = { nombreCarta = it },
            label = { Text("Nombre de la carta") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = tipoCarta,
            onValueChange = { tipoCarta = it },
            label = { Text("Tipo (Pokémon, Energía, Entrenador)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (nombreCarta.isNotBlank()) {
                    viewModel.añadirCarta(nombreCarta, tipoCarta)
                    nombreCarta = ""
                    tipoCarta = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Añadir carta")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lista de cartas
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(viewModel.cartas) { carta ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(carta.nombre, style = MaterialTheme.typography.titleMedium)
                        Text("Tipo: ${carta.tipo}")
                        Text("Cantidad: ${carta.cantidad}")
                    }
                }
            }
        }

        Button(
            onClick = onVolver,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver")
        }
    }
}
