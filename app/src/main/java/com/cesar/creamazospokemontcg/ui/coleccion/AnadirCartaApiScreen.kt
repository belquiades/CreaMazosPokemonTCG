package com.cesar.creamazospokemontcg.ui.coleccion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cesar.creamazospokemontcg.viewmodel.BuscarCartasViewModel

/**
 * Pantalla para buscar cartas en la API
 * y añadirlas a la colección
 */
@Composable
fun AnadirCartaApiScreen(
    onCartaSeleccionada: (String) -> Unit,
    onVolver: () -> Unit
) {

    val viewModel: BuscarCartasViewModel = viewModel()
    val resultados by viewModel.resultados.collectAsState()

    var texto by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {

        OutlinedTextField(
            value = texto,
            onValueChange = { texto = it },
            label = { Text("Nombre de la carta") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { viewModel.buscar(texto) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Buscar en la API")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(resultados) { carta ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    onClick = { onCartaSeleccionada(carta.id) }
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(carta.nombre)
                        Text("Tipo: ${carta.tipo}")
                        Text("Rareza: ${carta.rareza}")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onVolver) {
            Text("Volver")
        }
    }
}
