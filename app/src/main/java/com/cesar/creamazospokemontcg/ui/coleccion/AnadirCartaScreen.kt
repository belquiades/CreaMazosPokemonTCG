// ui/coleccion/AnadirCartaScreen.kt
package com.cesar.creamazospokemontcg.ui.coleccion

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cesar.creamazospokemontcg.data.model.Carta

@Composable
fun AnadirCartaScreen(
    onCartaCreada: (Carta) -> Unit,
    onVolver: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("") }
    var cantidad by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text("Añadir carta a la colección", style = MaterialTheme.typography.titleLarge)

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre") })
        OutlinedTextField(tipo, { tipo = it }, label = { Text("Tipo") })
        OutlinedTextField(cantidad, { cantidad = it }, label = { Text("Cantidad") })

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                val carta = Carta(
                    nombre = nombre,
                    tipo = tipo,
                    cantidad = cantidad.toIntOrNull() ?: 1
                )
                onCartaCreada(carta)
                onVolver()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Añadir")
        }
    }
}
