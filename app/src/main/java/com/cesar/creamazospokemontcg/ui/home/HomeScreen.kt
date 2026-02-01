package com.cesar.creamazospokemontcg.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Pantalla principal de la app.
 *
 * Desde aquí el usuario accede al resto de secciones.
 * (Alumno DAM: pantalla de menú principal)
 */
@Composable
fun HomeScreen(
    onIrColeccion: () -> Unit,
    onIrMazos: () -> Unit,
    onIrPerfil: () -> Unit,
    onCerrarSesion: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "CreaMazos Pokémon TCG",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onIrColeccion, modifier = Modifier.fillMaxWidth()) {
            Text("Colección")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = onIrMazos, modifier = Modifier.fillMaxWidth()) {
            Text("Mazos")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = onIrPerfil, modifier = Modifier.fillMaxWidth()) {
            Text("Perfil")
        }
    }
}
