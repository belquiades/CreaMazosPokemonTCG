package com.cesar.creamazospoketcg

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.NavOptions
import com.google.android.material.snackbar.Snackbar

/**
 * MainActivity robusta: no asume que todos los ids existen en R.
 * - Busca NavHost correctamente.
 * - Navega solo si el id existe en recursos y en el grafo.
 * - Oculta bottom nav en loginFragment.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Acquire NavController safely via supportFragmentManager
        val maybeNavHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        if (maybeNavHost == null) {
            // Falla pronto con mensaje claro (esto no debería ocurrir si activity_main.xml es correcto)
            throw IllegalStateException("NavHostFragment no encontrado. Revisa activity_main.xml (id=nav_host_fragment).")
        }
        val navHost = maybeNavHost as NavHostFragment
        navController = navHost.navController

        // bottom nav include (puede ser null si no está incluido)
        val bottomNav: View? = findViewById(R.id.bottom_nav_include)

        // iconos (pueden ser null si el layout del bottom no los define)
        val icon1 = findViewById<ImageView?>(R.id.icon_slot1)
        val icon2 = findViewById<ImageView?>(R.id.icon_slot2)
        val icon3 = findViewById<ImageView?>(R.id.icon_slot3)
        val icon4 = findViewById<ImageView?>(R.id.icon_slot4)
        val icon5 = findViewById<ImageView?>(R.id.icon_slot5)

        // slots (pueden ser null)
        val slot1: View? = findViewById(R.id.slot1)
        val slot2: View? = findViewById(R.id.slot2)
        val slot3: View? = findViewById(R.id.slot3)
        val slot4: View? = findViewById(R.id.slot4)
        val slot5: View? = findViewById(R.id.slot5)

        // Visibilidad del bottom nav según destino
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == safeId("loginFragment")) {
                bottomNav?.visibility = View.GONE
            } else {
                bottomNav?.visibility = View.VISIBLE
            }

            // Resaltar iconos solo si existen y solo cuando el destino corresponde a ids que sí están en R.
            val destName = try { resources.getResourceEntryName(destination.id) } catch (e: Exception) { null }

            icon1?.isSelected = destName == "fragmentoBusquedaCartas"
            icon2?.isSelected = destName == "fragmentoMisCartas" || destName == "navigation_mis_cartas"
            icon3?.isSelected = destName == "fragmentoMazos" || destName == "fragment_mazos"
            icon4?.isSelected = destName == "perfilFragment" || destName == "fragment_perfil"
            icon5?.isSelected = destName == "fragmentoHome" || destName == "fragment_home"
        }

        // Navegar de forma segura: comprueba resource id y que exista en el grafo
        fun navigateToByName(destName: String) {
            val destId = safeId(destName)
            if (destId == 0) {
                Snackbar.make(findViewById(android.R.id.content), "Destino no encontrado: $destName", Snackbar.LENGTH_SHORT).show()
                return
            }
            // comprobar que el nodo existe en el grafo actual
            val node = navController.graph.findNode(destId)
            if (node == null) {
                Snackbar.make(findViewById(android.R.id.content), "Destino no está en el grafo: $destName", Snackbar.LENGTH_SHORT).show()
                return
            }
            try {
                val navOptions = NavOptions.Builder().setLaunchSingleTop(true).build()
                navController.navigate(destId, null, navOptions)
            } catch (e: Exception) {
                Toast.makeText(this, "No se pudo navegar a $destName: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }

        // Asignar clicks a slots (si existen)
        slot1?.setOnClickListener { navigateToByName("fragmentoBusquedaCartas") }
        slot2?.setOnClickListener { navigateToByName("fragmentoMisCartas") }
        slot3?.setOnClickListener { navigateToByName("fragmentoMazos") } // si no existe, muestra Snackbar
        slot4?.setOnClickListener { navigateToByName("perfilFragment") }
        slot5?.setOnClickListener { navigateToByName("fragmentoHome") } // si no existe, muestra Snackbar
    }

    /**
     * Helper: obtiene el resId seguro a partir del entryName (devuelve 0 si no existe)
     */
    private fun safeId(name: String): Int {
        return try { resources.getIdentifier(name, "id", packageName) } catch (e: Exception) { 0 }
    }
}
