package com.cesar.creamazospokemontcg.ui.mazos

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cesar.creamazospokemontcg.viewmodel.CrearMazoViewModel

/**
 * Pantalla para crear un mazo manual.
 * En esta fase solo se introduce el nombre.
 */
@Composable
fun CrearMazoScreen(
    onMazoCreado: () -> Unit
) {
    val viewModel: CrearMazoViewModel = viewModel()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Crear mazo",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = viewModel.nombreMazo.value,
            onValueChange = { viewModel.nombreMazo.value = it },
            label = { Text("Nombre del mazo") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                // Creamos el mazo (aun no lo guardamos)
                viewModel.crearMazo()
                onMazoCreado()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = viewModel.nombreMazo.value.isNotBlank()
        ) {
            Text("Crear mazo")
        }
    }
}
