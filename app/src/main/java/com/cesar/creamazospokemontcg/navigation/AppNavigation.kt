package com.cesar.creamazospokemontcg.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cesar.creamazospokemontcg.ui.coleccion.AnadirCartaScreen
import com.cesar.creamazospokemontcg.ui.home.HomeScreen
import com.cesar.creamazospokemontcg.ui.login.LoginScreen
import com.cesar.creamazospokemontcg.ui.coleccion.ColeccionScreen
import com.cesar.creamazospokemontcg.ui.mazos.CrearMazoScreen
import com.cesar.creamazospokemontcg.ui.mazos.ListaMazosScreen
import com.cesar.creamazospokemontcg.ui.perfil.PerfilScreen
import com.cesar.creamazospokemontcg.viewmodel.AnadirCartaViewModel
import com.cesar.creamazospokemontcg.viewmodel.CrearMazoViewModel

/**
 * Navegación principal de la aplicación.
 * Aquí se definen todas las rutas disponibles.
 */
@Composable
fun AppNavigation() {

    val navController = rememberNavController()
    val anadirCartaViewModel: AnadirCartaViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {

        composable("login") {
            LoginScreen(
                onLoginCorrecto = {
                    navController.navigate("home") { popUpTo("login") { inclusive = true } }
                }
            )
        }

        composable("home") {
            HomeScreen(
                onIrColeccion = { navController.navigate("coleccion") },
                onIrMazos = { navController.navigate("mazos") },
                onIrPerfil = { navController.navigate("perfil") },
                onCerrarSesion = {
                    navController.navigate("login") { popUpTo("home") { inclusive = true } }
                }
            )
        }

        composable("anadirCarta") {
            AnadirCartaScreen(
                onCartaCreada = { carta ->
                    anadirCartaViewModel.nuevaCarta = carta
                    navController.popBackStack()
                },
                onVolver = { navController.popBackStack() }
            )
        }

        composable("coleccion") {
            ColeccionScreen(
                onVolver = { navController.popBackStack() }
            )
        }

        composable("mazos") {
            ListaMazosScreen(
                onCrearMazo = { navController.navigate("crearMazo") },
                onVolver = { navController.popBackStack() }
            )
        }

        composable("crearMazo") {
            CrearMazoScreen(
                onAnadirCarta = { navController.navigate("seleccionarCarta") },
                onMazoCreado = { navController.popBackStack() }
            )
        }

        composable("seleccionarCarta") {
            val crearMazoViewModel: CrearMazoViewModel = viewModel()
            ColeccionScreen(
                onVolver = { navController.popBackStack() },
                modoSeleccion = true,
                onCartaSeleccionada = {
                    crearMazoViewModel.anadirCartaAlMazo(it)
                    navController.popBackStack()
                }
            )
        }

        composable("perfil") {
            PerfilScreen(
                onCerrarSesion = {
                    navController.navigate("login") { popUpTo("home") { inclusive = true } }
                }
            )
        }
    }
}
