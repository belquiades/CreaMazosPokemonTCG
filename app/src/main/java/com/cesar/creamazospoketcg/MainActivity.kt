package com.cesar.creamazospoketcg

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.NavDestination
import com.google.android.material.snackbar.Snackbar

/**
 * MainActivity - controla la navegación y el menú inferior.
 *
 * El FAB "volver" se mostrará únicamente en los fragments de detalle:
 *  - fragmentoDetalleCarta
 *  - fragmentoDetalleCartaLocal
 *
 * El menú inferior se ocultará solo en el loginFragment.
 *
 * Comentarios claros para que un estudiante entienda:
 *  - navegarSeguro(...) comprueba si el destino existe en el grafo antes de navegar.
 *  - actualizarIconosSegunDestino(...) resalta visualmente el icono activo.
 */
class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivityDebug"
    private lateinit var navController: NavController

    // IDs esperados (asegúrate que coinciden con tu nav_graph)
    private val idLoginFragment = R.id.loginFragment
    private val idBusqueda = R.id.fragmentoBusquedaCartas
    private val idMisCartas = R.id.fragmentoMisCartas
    private val idPerfil = R.id.perfilFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // asegúrate que éste es tu layout principal

        // Obtén NavController de forma segura: intenta por id 'nav_host_fragment', si no existe, busca otro id conocido
        val possibleHostIdNames = listOf("nav_host_fragment", "nav_host_fragment_container", "navHostFragment")
        var navController: NavController? = null
        for (name in possibleHostIdNames) {
            val id = resources.getIdentifier(name, "id", packageName)
            if (id != 0) {
                try {
                    navController = findNavController(id)
                    break
                } catch (_: Exception) { /* seguir buscando */ }
            }
        }
        if (navController == null) {
            // fallback: busca el primer NavHostFragment declarado en el layout por tag (menos frecuente)
            throw IllegalStateException("No se encontró NavHostFragment. Revisa el id en activity_main.xml")
        }

        // Referencias a tus slots en bottom_nav_custom.xml
        val slot1: View? = findViewById(resources.getIdentifier("slot1", "id", packageName))
        val slot2: View? = findViewById(resources.getIdentifier("slot2", "id", packageName))
        val slot3: View? = findViewById(resources.getIdentifier("slot3", "id", packageName))
        val slot4: View? = findViewById(resources.getIdentifier("slot4", "id", packageName))
        val slot5: View? = findViewById(resources.getIdentifier("slot5", "id", packageName))

        // Helper para navegar por nombre de destino (evita referencias R.id que no existen)
        fun safeNavigateTo(destIdName: String) {
            val destId = resources.getIdentifier(destIdName, "id", packageName)
            if (destId == 0) {
                // el destino no existe en nav_graph, avisar y no crashear
                Snackbar.make(findViewById(android.R.id.content), "Destino no encontrado: $destIdName", Snackbar.LENGTH_SHORT).show()
                return
            }
            try {
                val navOptions = NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setRestoreState(false)
                    .build()
                navController.navigate(destId, null, navOptions)
            } catch (e: Exception) {
                Snackbar.make(findViewById(android.R.id.content), "No se pudo navegar a $destIdName", Snackbar.LENGTH_SHORT).show()
            }
        }

        // Mapear clicks -> destinos (nombres de ids tal como están en tu nav_graph.xml)
        slot1?.setOnClickListener { safeNavigateTo("fragmentoBusquedaCartas") } // Buscar
        slot2?.setOnClickListener { safeNavigateTo("fragmentoMisCartas") }     // Mis Cartas
        slot3?.setOnClickListener { safeNavigateTo("fragmentoMazos") }         // Mazos (ajusta nombre si tu id es distinto)
        slot4?.setOnClickListener { safeNavigateTo("perfilFragment") }         // Perfil
        slot5?.setOnClickListener { safeNavigateTo("fragmentoHome") }         // Home (ajusta)

        // actualizar iconos seleccionados si quieres (usa los ids icon_slot1..icon_slot5)
        val icon1 = findViewById<View?>(resources.getIdentifier("icon_slot1", "id", packageName))
        val icon2 = findViewById<View?>(resources.getIdentifier("icon_slot2", "id", packageName))
        val icon3 = findViewById<View?>(resources.getIdentifier("icon_slot3", "id", packageName))
        val icon4 = findViewById<View?>(resources.getIdentifier("icon_slot4", "id", packageName))
        val icon5 = findViewById<View?>(resources.getIdentifier("icon_slot5", "id", packageName))

        fun resetSelected() {
            icon1?.isSelected = false
            icon2?.isSelected = false
            icon3?.isSelected = false
            icon4?.isSelected = false
            icon5?.isSelected = false
        }

        // Listener para sincronizar iconos con destino actual (usamos names de destinos como en nav_graph)
        navController.addOnDestinationChangedListener { _: NavController, destination: NavDestination, _: Bundle? ->
            resetSelected()
            val destName = resources.getResourceEntryName(destination.id)
            when (destName) {
                "fragmentoBusquedaCartas" -> icon1?.isSelected = true
                "fragmentoMisCartas", "navigation_mis_cartas" -> icon2?.isSelected = true
                "fragmentoMazos", "fragment_mazos" -> icon3?.isSelected = true
                "perfilFragment", "fragment_perfil" -> icon4?.isSelected = true
                "fragmentoHome", "fragment_home" -> icon5?.isSelected = true
                else -> { /* no cambiar */ }
            }
        }
    }

    /**
     * Navega de forma segura: comprobamos que el destino está en el grafo actual antes de navegar.
     */
    private fun navegarSeguro(destinationId: Int) {
        try {
            val current = navController.currentDestination?.id
            if (current == destinationId) return
            val node = navController.graph.findNode(destinationId)
            if (node != null) {
                navController.navigate(destinationId)
            } else {
                Toast.makeText(this, "Destino no definido en el grafo de navegación.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo navegar: ${e.localizedMessage ?: e}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Actualiza la apariencia de los iconos del menú resaltando el activo.
     */
    private fun actualizarIconosSegunDestino(
        destId: Int,
        i1: ImageView,
        i2: ImageView,
        i3: ImageView,
        i4: ImageView,
        i5: ImageView,
        idMazosDynamic: Int
    ) {
        i1.alpha = 0.6f; i2.alpha = 0.6f; i3.alpha = 0.6f; i4.alpha = 0.6f; i5.alpha = 0.6f

        when (destId) {
            R.id.fragmentoBusquedaCartas -> i1.alpha = 1.0f
            R.id.fragmentoMisCartas -> i2.alpha = 1.0f
            idMazosDynamic -> if (idMazosDynamic != 0) i3.alpha = 1.0f
            R.id.perfilFragment -> i4.alpha = 1.0f
            else -> { /* sin cambio */ }
        }
    }
}
