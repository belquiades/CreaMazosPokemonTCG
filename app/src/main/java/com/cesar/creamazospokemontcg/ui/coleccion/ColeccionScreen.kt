package com.cesar.creamazospokemontcg.ui.coleccion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cesar.creamazospokemontcg.viewmodel.ColeccionViewModel

/**
 * Pantalla que muestra la colección de cartas del usuario.
 */
@Composable
fun ColeccionScreen(
    onVolver: () -> Unit,
    onIrAnadirCarta: () -> Unit
) {
    val viewModel: ColeccionViewModel = viewModel()
    val cartas = viewModel.cartas.value

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

        // Lista de cartas
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(cartas) { carta ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = carta.nombre, style = MaterialTheme.typography.titleMedium)
                        Text(text = "Tipo: ${carta.tipo}")
                        Text(text = "Cantidad: ${carta.cantidad}")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onIrAnadirCarta() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Añadir carta")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onVolver() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver")
        }
    }
}
