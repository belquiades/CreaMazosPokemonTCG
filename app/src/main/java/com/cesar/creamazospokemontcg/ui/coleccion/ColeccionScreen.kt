package com.cesar.creamazospokemontcg.ui.coleccion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cesar.creamazospokemontcg.viewmodel.ColeccionViewModel

/**
 * Muestra la colección de cartas del usuario.
 */
@Composable
fun ColeccionScreen(
    onVolver: () -> Unit
) {
    val viewModel: ColeccionViewModel = viewModel()
    var refrescar by remember { mutableStateOf(false) }

    LaunchedEffect(refrescar) {
        viewModel.cargarColeccion {
            refrescar = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        Text("Mi colección", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(viewModel.cartas) { carta ->
                Text("• ${carta.nombre} (${carta.tipo}) x${carta.cantidad}")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                viewModel.añadirCarta("Pikachu", "Eléctrico")
                refrescar = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Añadir carta de prueba")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onVolver,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver")
        }
    }
}
