package com.cesar.creamazospokemontcg.ui.coleccion

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cesar.creamazospokemontcg.viewmodel.ColeccionViewModel
import androidx.compose.foundation.clickable

/**
 * Pantalla que muestra la coleccion del usuario.
 */
@Composable
fun ColeccionScreen(
    onVolver: () -> Unit,
    modoSeleccion: Boolean = false,
    onCartaSeleccionada: (String) -> Unit = {}
) {
    val viewModel: ColeccionViewModel = viewModel()
    val cartas = viewModel.cartas.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Button(onClick = onVolver) {
            Text("Volver")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (cartas.isEmpty()) {
            Text("No tienes cartas en tu colección")
        } else {
            LazyColumn {
                items(cartas) { carta ->
                    Text(
                        text = carta.nombre,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = if (modoSeleccion) {
                            Modifier.clickable { onCartaSeleccionada(carta.id) }
                        } else {
                            Modifier
                        }
                    )
                }
            }
        }
    }
}
