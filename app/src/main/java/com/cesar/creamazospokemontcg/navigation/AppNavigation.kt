package com.cesar.creamazospokemontcg.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cesar.creamazospokemontcg.ui.home.HomeScreen
import com.cesar.creamazospokemontcg.ui.login.LoginScreen
import com.cesar.creamazospokemontcg.ui.coleccion.ColeccionScreen
import com.cesar.creamazospokemontcg.ui.mazos.ListaMazosScreen
import com.cesar.creamazospokemontcg.ui.perfil.PerfilScreen

/**
 * Navegación principal de la aplicación.
 * Aquí se definen todas las rutas disponibles.
 */
@Composable
fun AppNavigation() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {

        composable("login") {
            LoginScreen(
                onLoginCorrecto = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                onIrColeccion = { navController.navigate("coleccion") },
                onIrMazos = { navController.navigate("mazos") },
                onIrPerfil = { navController.navigate("perfil") },
                onCerrarSesion = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }

        composable("coleccion") {
            ColeccionScreen(
                onVolver = { navController.popBackStack() }
            )
        }

        composable("mazos") {
            ListaMazosScreen(
                onVolver = { navController.popBackStack() }
            )
        }

        composable("perfil") {
            PerfilScreen(
                onCerrarSesion = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
    }
}
