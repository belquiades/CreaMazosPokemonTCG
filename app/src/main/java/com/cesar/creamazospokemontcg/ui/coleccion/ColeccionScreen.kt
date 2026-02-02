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
 * Pantalla que muestra la colección del usuario
 */
@Composable
fun ColeccionScreen(
    onIrAnadirCarta: () -> Unit,
    onVolver: () -> Unit
) {
    val viewModel: ColeccionViewModel = viewModel()

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

        LazyColumn {
            items(viewModel.cartas.value) { carta ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = carta.nombre)
                        Text(text = "Tipo: ${carta.tipo}")
                        Text(text = "Cantidad: ${carta.cantidad}")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onVolver,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver")
        }

        Button(
            onClick = { onIrAnadirCarta() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Añadir carta")
        }

    }
}
