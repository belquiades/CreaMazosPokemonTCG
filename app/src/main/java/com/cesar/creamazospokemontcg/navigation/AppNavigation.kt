package com.cesar.creamazospokemontcg.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cesar.creamazospokemontcg.ui.home.HomeScreen
import com.cesar.creamazospokemontcg.ui.coleccion.ColeccionScreen
import com.cesar.creamazospokemontcg.ui.mazos.MazosScreen
import com.cesar.creamazospokemontcg.ui.perfil.PerfilScreen

/**
 * Gestiona la navegación principal de la aplicación.
 *
 * Aquí se definen todas las pantallas y rutas.
 * (Alumno DAM: separación clara entre UI y navegación)
 */
@Composable
fun AppNavigation() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {

        composable("home") {
            HomeScreen(
                onIrColeccion = { navController.navigate("coleccion") },
                onIrMazos = { navController.navigate("mazos") },
                onIrPerfil = { navController.navigate("perfil") }
            )
        }


        composable("coleccion") {
            ColeccionScreen()
        }

        composable("mazos") {
            MazosScreen()
        }

        composable("perfil") {
            PerfilScreen()
        }
    }
}
