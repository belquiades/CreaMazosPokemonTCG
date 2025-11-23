package com.cesar.creamazospoketcg

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment

/**
 * MainActivity corregida - manejo del menú inferior y FAB "volver".
 *
 * Nota:
 *  - Ahora no referenciamos R.id.fragmentoMazos en tiempo de compilación para evitar errores
 *    si ese destino no está definido en nav_graph.
 *  - Buscamos el id de "fragmentoMazos" dinámicamente usando resources.getIdentifier(...)
 *    y solo intentamos navegar si el id existe (> 0).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    // IDs de fragments que sí conocemos en tiempo de compilación (ajusta si cambian)
    private val idLoginFragment = R.id.loginFragment
    private val idBusqueda = R.id.fragmentoBusquedaCartas
    private val idMisCartas = R.id.fragmentoMisCartas
    private val idPerfil = R.id.perfilFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Asegúrate de que el layout coincide con el que usas
        setContentView(R.layout.activity_main_with_bottom)

        // Obtenemos NavController desde NavHostFragment
        val host = supportFragmentManager.findFragmentById(R.id.nav_host_main) as NavHostFragment
        navController = host.navController

        // Referencias a las vistas del menú (incluido mediante <include>)
        val slot1 = findViewById<LinearLayout>(R.id.slot1)
        val slot2 = findViewById<LinearLayout>(R.id.slot2)
        val slot3 = findViewById<LinearLayout>(R.id.slot3)
        val slot4 = findViewById<LinearLayout>(R.id.slot4)
        val slot5 = findViewById<LinearLayout>(R.id.slot5)

        val icon1 = findViewById<ImageView>(R.id.icon_slot1)
        val icon2 = findViewById<ImageView>(R.id.icon_slot2)
        val icon3 = findViewById<ImageView>(R.id.icon_slot3)
        val icon4 = findViewById<ImageView>(R.id.icon_slot4)
        val icon5 = findViewById<ImageView>(R.id.icon_slot5)

        val fab = findViewById<View>(R.id.fabVolver)
        val menuInferior = findViewById<View>(R.id.menuInferior)

        // Intentamos obtener dinámicamente el id de fragmentoMazos (puede no existir)
        val idMazosDynamic = resources.getIdentifier("fragmentoMazos", "id", packageName)
        // idMazosDynamic será 0 si no existe

        // Listener para ocultar/mostrar menú según destino (ocultar en login)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == idLoginFragment) {
                menuInferior.visibility = View.GONE
                fab.visibility = View.GONE
            } else {
                menuInferior.visibility = View.VISIBLE
                fab.visibility = View.VISIBLE
            }

            // Actualizamos el resaltado de iconos según destino actual
            actualizarIconosSegunDestino(destination.id, icon1, icon2, icon3, icon4, icon5, idMazosDynamic)
        }

        // Clicks para navegación - usamos navegarSeguro para controlar destinos inexistentes
        slot1.setOnClickListener { navegarSeguro(idBusqueda) }          // Buscar
        slot2.setOnClickListener { navegarSeguro(idMisCartas) }         // Colección / Mis Cartas
        slot3.setOnClickListener {
            // Si idMazosDynamic existe, navegamos; si no, mostramos aviso y fallback a MisCartas
            if (idMazosDynamic != 0) {
                navegarSeguro(idMazosDynamic)
            } else {
                Toast.makeText(this, "Sección 'Mazos' no disponible todavía. Abriendo Mis Cartas.", Toast.LENGTH_SHORT).show()
                navegarSeguro(idMisCartas)
            }
        }
        slot4.setOnClickListener { navegarSeguro(idPerfil) }            // Perfil
        slot5.setOnClickListener { navegarSeguro(idPerfil) }            // Inicio (mapear a Perfil por defecto)

        // FAB volver: hacemos popBackStack; si no hay nada, navegamos a Perfil
        fab.setOnClickListener {
            val popOk = navController.popBackStack()
            if (!popOk) {
                navegarSeguro(idPerfil)
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
            // Antes de navegar comprobamos si el destino existe en el grafo actual
            val node = navController.graph.findNode(destinationId)
            if (node != null) {
                navController.navigate(destinationId)
            } else {
                Toast.makeText(this, "Destino no definido en el grafo de navegación.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            // Capturamos cualquier excepción para evitar crash en tiempo de ejecución
            Toast.makeText(this, "No se pudo navegar: ${e.localizedMessage ?: e}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Actualiza la apariencia de los iconos del menú resaltando el activo.
     * Si idMazosDynamic==0, no se intentará destacar ese slot.
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
        // Atenuamos todos por defecto
        i1.alpha = 0.6f; i2.alpha = 0.6f; i3.alpha = 0.6f; i4.alpha = 0.6f; i5.alpha = 0.6f

        when (destId) {
            R.id.fragmentoBusquedaCartas -> i1.alpha = 1.0f
            R.id.fragmentoMisCartas -> i2.alpha = 1.0f
            // Si idMazosDynamic existe y coincide con el destino, lo resaltamos
            idMazosDynamic -> if (idMazosDynamic != 0) i3.alpha = 1.0f
            R.id.perfilFragment -> i4.alpha = 1.0f
            // No confundir: slot5 (inicio) lo tratamos como perfil por defecto
            else -> {
                // Mantenemos la atenuación por defecto
            }
        }
    }
}
